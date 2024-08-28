/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amonboardingservice.infrastructure.client;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import static com.ericsson.amonboardingservice.utils.RequestUtils.noSslRestTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.amonboardingservice.presentation.exceptions.InternalRuntimeException;
import com.ericsson.amonboardingservice.presentation.exceptions.PushLayerException;
import com.ericsson.amonboardingservice.presentation.services.fileservice.FileService;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RestClient {

    private static final String UNKNOWN_EXCEPTION_LOG_MESSAGE = "Unknown exception occurred: %s";
    private static final String UNKNOWN_EXCEPTION_ERROR_MESSAGE = "Unknown exception occurred: %s : %s";
    private static final String HTTP_STATUS_ERROR_MESSAGE = "Call to %s %s responded with: %s %s";
    private static final String HTTP_GET_FOR_RETURNED_AN_ERROR = "Http GET for %s returned an error : %s";
    private static final String HTTP_HEAD_FOR_RETURNED_AN_ERROR = "Http HEAD for %s returned an error : %s";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("basicRetryTemplate")
    private RetryTemplate retryTemplate;

    @Autowired
    private FileService fileService;

    public String buildUrl(String host, String uri) {
        final String urlTemplate = host.contains("http") ? "%s%s" : "http://%s%s";
        return String.format(urlTemplate, host, uri);
    }

    /**
     * Execute REST GET
     *
     * @param url
     * @return String response
     */
    public String get(String url) {
        return retryTemplate.execute(retryContext -> {
            LOGGER.info("Executing GET request by url: {}. Attempt: {}", url, retryContext.getRetryCount());
            return restTemplate.getForObject(url, String.class);
        });
    }

    /**
     * Execute REST GET to download a file.
     * Note: The file created is temporary so there will be a UUID between the filename and the extension
     *
     * @param url      the url to retrieve the file from
     * @param filename the name to give the file
     * @return
     */
    public Optional<File> getFile(String url, String filename, String user, String password) {
        File file = null;

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authenticationHeader(user, password));

        HttpEntity<Object> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = executeGetWithRetries(entity, url, byte[].class);
            if (response.getStatusCode() == HttpStatus.OK) {
                Path temp = fileService.createTempPath(filename);
                Files.write(temp, response.getBody());
                file = temp.toFile();
            }
        } catch (HttpStatusCodeException e) {
            String message = String.format(HTTP_GET_FOR_RETURNED_AN_ERROR, url, e.getMessage());
            LOGGER.error(message, e);
        } catch (Throwable e) {
            LOGGER.error("Failed to write file to filesystem", e);
        }
        return Optional.ofNullable(file);
    }

    @SneakyThrows
    private <T> ResponseEntity<T> executeGetWithRetries(HttpEntity<Object> entity, String url, Class<T> responseType) {
        return retryTemplate.execute((RetryCallback<ResponseEntity<T>, Throwable>) retryContext -> {
            try {
                return restTemplate.exchange(url, HttpMethod.GET, entity,
                        responseType);
            } catch (HttpStatusCodeException e) {
                if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                    LOGGER.info("Resource at {} does not exist", url);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                throw e;
            }
        });
    }

    /**
     * Execute REST for HEAD with headers
     *
     * @param url
     * @param username
     * @param password
     * @return HttpStatus
     */
    public HttpStatusCode head(final String url, final String username, String password) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authenticationHeader(username, password));
        final HttpEntity<Object> entity = new HttpEntity<>(headers);
        try {
            return executeHeadMethodWithRetries(entity, url).getStatusCode();
        } catch (final Exception ex) {
            LOGGER.error(UNKNOWN_EXCEPTION_LOG_MESSAGE, ex);
            throw new InternalRuntimeException(String.format(UNKNOWN_EXCEPTION_ERROR_MESSAGE, url, ex.getMessage()));
        }
    }

    public ResponseEntity<Void> pushLayerToContainerRegistry(RequestEntity<byte[]> request,
                                                             String registryUser,
                                                             String registryPassword) throws PushLayerException {
        ResponseEntity<Void> response;
        try {
            response = retryTemplate.execute(retryContext -> {
                LOGGER.info("Pushing Layer to Container Registry. Attempt: {}", retryContext.getRetryCount());
                return noSslRestTemplate(registryUser, registryPassword)
                        .exchange(request, Void.class);
            });
        } catch (GeneralSecurityException e) {
            throw new PushLayerException("Cannot send request to docker registry", e);
        }
        return response;
    }

    @SneakyThrows
    private <T> ResponseEntity<T> executeHeadMethodWithRetries(HttpEntity<Object> entity, String url) {
        return retryTemplate.execute((RetryCallback<ResponseEntity<T>, Throwable>) retryContext -> {
            try {
                return restTemplate.exchange(url, HttpMethod.HEAD, entity,
                        (Class<T>) String.class);
            } catch (HttpStatusCodeException e) {
                if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                    LOGGER.info(String.format(HTTP_HEAD_FOR_RETURNED_AN_ERROR, url, e.getMessage()));
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                LOGGER.error(String.format(UNKNOWN_EXCEPTION_LOG_MESSAGE, e.getMessage()));
                throw e;
            }
        });
    }

    /**
     * Execute REST for GET with headers
     *
     * @param url
     * @param username
     * @param password
     * @return ResponseEntity<String> response
     */
    public ResponseEntity<String> get(final String url, final String username, final String password) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authenticationHeader(username, password));
        return get(url, headers);
    }

    /**
     * Execute REST for GET with headers
     *
     * @param url
     * @param headers
     * @return ResponseEntity<String> response
     */
    public ResponseEntity<String> get(final String url, final HttpHeaders headers) {
        final HttpEntity<Object> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = executeGetWithRetries(entity, url, String.class);
            if (HttpStatus.NOT_FOUND == response.getStatusCode()) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "resource is not found");
            } else {
                return response;
            }
        } catch (final HttpClientErrorException ex) {
            String message = String.format(HTTP_GET_FOR_RETURNED_AN_ERROR, url, ex.getMessage());
            LOGGER.error(message, ex);
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (final Exception ex) {
            LOGGER.error(UNKNOWN_EXCEPTION_LOG_MESSAGE, ex);
            throw new InternalRuntimeException(String.format(UNKNOWN_EXCEPTION_ERROR_MESSAGE, url, ex.getMessage()));
        }
    }

    /**
     * Execute REST POST with file as body
     *
     * @param url
     * @param fileLocation
     * @param user
     * @param password
     * @return ResponseEntity response
     */
    public ResponseEntity<String> postFile(String url, Path fileLocation, String user, String password) {
        FileSystemResource fileSystemResource = new FileSystemResource(fileLocation.toFile());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(APPLICATION_OCTET_STREAM);
        httpHeaders.set(HttpHeaders.AUTHORIZATION, authenticationHeader(user, password));

        HttpEntity<Object> entity = new HttpEntity<>(fileSystemResource, httpHeaders);
        try {
            return retryTemplate.execute(retryContext -> {
                LOGGER.info("Executing POST request by url: {}. Attempt: {}", url, retryContext.getRetryCount());
                return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            });
        } catch (final HttpClientErrorException e) {
            String message = String.format(HTTP_STATUS_ERROR_MESSAGE, HttpMethod.POST, url, e.getStatusCode(), e.getStatusText());
            LOGGER.info(message, e);

            return ResponseEntity
                    .status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            LOGGER.warn("Failed to execute POST request", e);
            throw e;
        }
    }

    /**
     * Execute REST PUT with file as body
     *
     * @param url
     * @param body
     * @param user
     * @param password
     * @param contentType
     * @return ResponseEntity response
     */
    public ResponseEntity<String> put(String url, String body, String user, String password, String contentType)
            throws RestClientException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", contentType);
        httpHeaders.set(HttpHeaders.AUTHORIZATION, authenticationHeader(user, password));

        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
        try {
            return retryTemplate.execute(retryContext -> {
                LOGGER.info("Executing PUT request by url: {}. Attempt: {}", url, retryContext.getRetryCount());
                return restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            });
        } catch (RestClientException e) {
            LOGGER.warn("Failed to execute PUT request", e);
            throw e;
        }
    }

    /**
     * Execute REST POST with Authentication
     *
     * @param url
     * @param user
     * @param password
     * @return ResponseEntity
     */
    public ResponseEntity<String> post(String url, String user, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authenticationHeader(user, password));

        HttpEntity<Object> entity = new HttpEntity<>(headers);
        ResponseEntity<String> exchange = retryTemplate.execute(retryContext -> {
            LOGGER.info("Executing POST request by url: {}. Attempt: {}", url, retryContext.getRetryCount());
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        });
        int responseCode = exchange.getStatusCode().value();

        if (responseCode == 308) { //308 redirects are not followed by redirection strategy
            List<String> locationList = exchange.getHeaders().get(HttpHeaders.LOCATION);
            Optional<String> location = getLocation(locationList);
            if (location.isPresent()) {
                exchange = retryTemplate.execute(retryContext -> {
                    LOGGER.info("Executing POST request by url: {}. Attempt: {}", url, retryContext.getRetryCount());
                    return restTemplate.exchange(location.get(), HttpMethod.POST, entity, String.class);
                });
            }
        }
        return exchange;
    }

    private static Optional<String> getLocation(List<String> locationList) {
        if (!CollectionUtils.isEmpty(locationList)) {
            return locationList.stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

    private static String authenticationHeader(String user, String password) {
        String auth = String.format("%s:%s", user, password);
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII), false);
        return "Basic " + new String(encodedAuth, StandardCharsets.US_ASCII);
    }

    /**
     * Execute REST DELETE
     *
     * @param url
     * @param user
     * @param password
     */
    public void delete(String url, String user, String password) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.AUTHORIZATION, authenticationHeader(user, password));
        HttpEntity<?> request = new HttpEntity<>(headers);
        int httpStatus = retryTemplate.execute(retryContext -> {
            LOGGER.info("Executing DELETE request by url: {}. Attempt: {}", url, retryContext.getRetryCount());
            return restTemplate.exchange(url, HttpMethod.DELETE, request, String.class).getStatusCode().value();
        });
        if (httpStatus == HttpStatus.OK.value()) {
            LOGGER.info("Chart by URL {} has been deleted from repository.", url);
        } else if (httpStatus == HttpStatus.NOT_FOUND.value()) {
            LOGGER.info("Chart by URL {} had already been deleted or did not exist.", url);
        } else {
            throw new InternalRuntimeException(String.format("Failed to DELETE %s with %s", url, httpStatus));
        }
    }

    /**
     * Execute REST for any method with headers and auth parameters
     *
     * @return ResponseEntity<String> response
     */
    public ResponseEntity<String> exchange(String url, HttpMethod method, HttpHeaders headers,
                                           String user, String password) {
        headers.add(HttpHeaders.AUTHORIZATION, authenticationHeader(user, password));
        final HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return executeExchangeWithRetries(entity, method, url);
        } catch (HttpStatusCodeException exception) {
            String message = String.format(HTTP_STATUS_ERROR_MESSAGE, method, url, exception.getStatusCode(), exception.getStatusText());
            LOGGER.info(message, exception);
            return new ResponseEntity<>(exception.getResponseBodyAsString(),
                    exception.getResponseHeaders(), exception.getStatusCode());
        } catch (RestClientException exception) {
            LOGGER.error(UNKNOWN_EXCEPTION_LOG_MESSAGE, exception);
            throw new InternalRuntimeException(
                    String.format(UNKNOWN_EXCEPTION_ERROR_MESSAGE, url, exception.getMessage()));
        }
    }

    @SneakyThrows
    private ResponseEntity<String> executeExchangeWithRetries(HttpEntity<?> entity, HttpMethod method, String url) {
        return retryTemplate.execute((RetryCallback<ResponseEntity<String>, Throwable>) retryContext -> {
            try {
                LOGGER.info("Executing {} request by url: {}. Attempt: {}", method, url, retryContext.getRetryCount());
                return restTemplate.exchange(url, method, entity, String.class);
            } catch (final HttpStatusCodeException e) {
                if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                    LOGGER.info("Resource at {} does not exist", url);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                throw e;
            }
        });
    }
}

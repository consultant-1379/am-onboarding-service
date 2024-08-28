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
package com.ericsson.amonboardingservice.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import com.amazonaws.ClientConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TLSConfigurationUtil {

    private TLSConfigurationUtil() {
    }

    public static ClientConfiguration getTlsClientConfiguration(final String certificateFile, String keyStorePassword) throws IOException,
            GeneralSecurityException {
        X509TrustManager trustManager;
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        LOGGER.info("Creating AmazonS3 client configuration");

        try (InputStream certificateInputStream = Files.newInputStream(Path.of(certificateFile))) {
            trustManager = trustManagerForCertificates(certificateInputStream, keyStorePassword);
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                    new String[]{"TLSv1.2", "TLSv1.3"},
                    null,
                    NoopHostnameVerifier.INSTANCE);
        }

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");
        clientConfiguration.getApacheHttpClientConfig().withSslSocketFactory(sslConnectionSocketFactory);

        return clientConfiguration;
    }

    private static X509TrustManager trustManagerForCertificates(final InputStream inputStream, String keyStorePassword)
            throws GeneralSecurityException, IOException {


        final KeyStore keyStore = newTrustKeyStore(inputStream, keyStorePassword);

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    // load cert file to keystore
    private static KeyStore newTrustKeyStore(final InputStream certInputStream, String keyStorePassword)
            throws GeneralSecurityException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(certInputStream);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("Expected non-empty set of trusted certificates");
        }

        final KeyStore keyStore = newEmptyKeyStore(keyStorePassword);
        int index = 0;
        for (final Certificate certificate : certificates) {
            final String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }
        return keyStore;
    }

    // create a empty keystore
    private static KeyStore newEmptyKeyStore(String keyStorePassword) throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, keyStorePassword.toCharArray());
        return keyStore;
    }
}

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
package contracts.api.vnf_packages.positive.getPackage
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for an onboarded vnf package using the vnf package id

```
given:
  client requests to retrieve an onboarded package by its id
when:
  a valid request is made to retrieve the onboarded package by id
then:
  the onboarded package is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()),producer("d3def1ce-4cf4-477c-aab3-21cb04e6a380"))}"
        headers {
            accept(applicationJson())}
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("vnfpackage.json").asString().replaceFirst("<APP_PKG_ID>", "${fromRequest().path(4).serverValue}"))
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$.vnfdId', byCommand("assertThat(parsedJson.read(\"\$.vnfdId\", String.class)).isNotNull()"))
            jsonPath('$.vnfProvider', byCommand("assertThat(parsedJson.read(\"\$.vnfProvider\", String.class)).isNotNull()"))
            jsonPath('$.vnfProductName', byCommand("assertThat(parsedJson.read(\"\$.vnfProductName\", String.class)).isNotNull()"))
            jsonPath('$.vnfSoftwareVersion', byCommand("assertThat(parsedJson.read(\"\$.vnfSoftwareVersion\", String.class)).isNotNull()"))
            jsonPath('$.vnfdVersion', byCommand("assertThat(parsedJson.read(\"\$.vnfdVersion\", String.class)).isNotNull()"))
            jsonPath('$.onboardingState', byRegex("CREATED|UPLOADING|PROCESSING|ONBOARDED"))
            jsonPath('$.packageSecurityOption',byRegex("OPTION_1|OPTION_2|UNSIGNED"))
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.self', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))

        }
    }
    priority 2
}

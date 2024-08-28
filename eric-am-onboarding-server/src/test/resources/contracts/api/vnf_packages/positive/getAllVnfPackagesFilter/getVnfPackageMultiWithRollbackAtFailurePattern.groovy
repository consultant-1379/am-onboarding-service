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
package contracts.api.vnf_packages.positive.getAllPackagesFilter

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for all onboarded vnf packages

```
given:
  client requests to retrieve all onboarded packages
when:
  a valid request is made to retrieve all of the onboarded vnf packages
then:
  all of the onboarded vnf packages are returned
```

""")
    request {
        method GET()
        url("/api/vnfpkgm/v1/vnf_packages") {
            queryParameters {
                parameter 'filter': value(consumer(regex("^\\((?:eq|neq|in|nin|gt|gte|lt|lte|cont|ncont)," +
                        "(?:id|vnfdId|vnfProvider|vnfProductName|vnfSoftwareVersion|vnfdVersion|onboardingState|operationalState|usageState)," +
                        "multi-helm-rollback\\)\$")),
                        producer("(eq,vnfdId,multi-helm-rollback)"))

            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }

        body(file("filteredVnfPackagesWithMultiHelmChartForManualRollbackInstantiate.json").asString())
        bodyMatchers {
            jsonPath('$.[0].id', byCommand("assertThat(parsedJson.read(\"\$.[0].id\", String.class)).isNotNull()"))
            jsonPath('$.[0].vnfdId', byCommand("assertThat(parsedJson.read(\"\$.[0].vnfdId\", String.class)).isNotNull()"))
            jsonPath('$.[0].vnfProvider', byCommand("assertThat(parsedJson.read(\"\$.[0].vnfProvider\", String.class)).isNotNull()"))
            jsonPath('$.[0].vnfProductName', byCommand("assertThat(parsedJson.read(\"\$.[0].vnfProductName\", String.class)).isNotNull()"))
            jsonPath('$.[0].vnfSoftwareVersion', byCommand("assertThat(parsedJson.read(\"\$.[0].vnfSoftwareVersion\", String.class)).isNotNull()"))
            jsonPath('$.[0].vnfdVersion', byCommand("assertThat(parsedJson.read(\"\$.[0].vnfdVersion\", String.class)).isNotNull()"))
            jsonPath('$.[0].onboardingState', byRegex("CREATED|UPLOADING|PROCESSING|ONBOARDED"))
            jsonPath('$.[0]._links', byCommand("assertThat(parsedJson.read(\"\$.[0]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[0]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[0]._links.self.href\", String.class)).isNotNull()"))
        }
    }
    priority 1
}

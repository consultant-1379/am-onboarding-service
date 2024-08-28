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
package contracts.api.vnf_packages.positive.createVnfPkgInfoRequestWithoutUserData

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Successful scenario for creation of an individual VNF package resource with no userDefinedData.

```
given:
  client requests to create a VNF package resource
when:
  a request is made to create the VNF package resource
then:
  the VNF package resource is created and resource information is returned.
```

""")
    request {
        method POST()
        url "/api/vnfpkgm/v1/vnf_packages"
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body("{}")
        bodyMatchers { byRegex(/\{([\t\n\r\s])*\}/) }
    }
    response {
        status CREATED()
        headers {
            header(location(), "http://localhost/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/97d82f0d-4dab-4c97-a2d7-b882868f37ac")
            contentType(applicationJson())
        }
        body("""
                {
                    "id":"97d82f0d-4dab-4c97-a2d7-b882868f37ac",
                    "vnfdId":null,
                    "vnfProvider":null,
                    "vnfProductName":null,
                    "vnfSoftwareVersion":null,
                    "vnfdVersion":null,
                    "checksum":null,
                    "softwareImages":null,
                    "additionalArtifacts":null,
                    "onboardingState":"CREATED",
                    "operationalState":"DISABLED",
                    "usageState":"NOT_IN_USE",
                    "userDefinedData":"",
                    "_links":{
                      "self": {
                          "href": "http://localhost/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/97d82f0d-4dab-4c97-a2d7-b882868f37ac"
                        }
                    }
                }
            """
        )
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.self', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))
        }
    }
    priority(1)
}

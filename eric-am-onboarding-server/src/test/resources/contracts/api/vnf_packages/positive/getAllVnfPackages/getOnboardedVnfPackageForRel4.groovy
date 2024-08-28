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
package contracts.api.vnf_packages.positive.getPackages

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for all onboarded vnf packages

```
given:
  client requests to retrieve an onboarded package with a certain vnfdId
when:
  a valid request is made to retrieve the onboarded vnf package
then:
  the onboarded vnf packages are returned
```

""")
    request {
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,rel4-1ce-4cf4-477c-aab3-21cb04e6a380)"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("filteredVnfPackageForRel4.json").asString())
    }
    priority 1
}

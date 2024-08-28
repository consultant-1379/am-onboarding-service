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
package contracts.api.vnf_packages.positive.getAllPackages

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
        url "/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,3d02c5c9-7a9b-48da-8ceb-46fcc83f694c)"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("filteredVnfPackageForDowngrade2.json").asString())
    }
    priority 1
}

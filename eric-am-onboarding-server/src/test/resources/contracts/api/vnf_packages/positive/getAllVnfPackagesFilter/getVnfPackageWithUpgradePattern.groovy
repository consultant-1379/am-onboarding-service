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
Returns successful scenario for onboarded vnf package with instantiation levels and mapping file

```
given:
  client requests to retrieve onboarded packages based on filter
when:
  a valid request is made to retrieve all of the onboarded vnf packages
then:
  a vnf package with instantiation levels and mapping file is returned
```

""")
    request {
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,4c096964-69e7-11ee-8c99-0242ac120002)"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }

        body(file("filteredVnfPackageWithUpgradePattern.json").asString())
    }
    priority 1
}

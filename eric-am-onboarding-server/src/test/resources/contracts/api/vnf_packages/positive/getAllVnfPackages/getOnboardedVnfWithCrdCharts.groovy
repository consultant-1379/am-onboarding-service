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
Returns successful scenario for empty result of vnf packages.

given:
  client requests to retrieve all onboarded packages
when:
  a valid request is made to retrieve all of the onboarded vnf packages
then:
  zero onboarded vnf packages are returned

""")
    request {
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,d3def1ce-4cf4-477c-aab3-123456789101)"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }

        body(file("filteredVnfPackagesWithCrdCharts.json"))
    }
    priority 1
}

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
package contracts.api.vnf_packages.positive.getAllVnfPackagesV2

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for vnf packages with CRD charts.

given:
  client requests to retrieve all onboarded packages
when:
  a valid request is made to retrieve all of the onboarded vnf packages
then:
  onboarded vnf packages with CRD charts are returned

""")
    request {
        method GET()
        url "/api/vnfpkgm/v2/vnf_packages?filter=(eq,vnfdId,a3f91625-c12c-4897-854e-3bd49b84263b)"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }

        body(file("filteredVnfPackagesV2WithCrdChartsForUpgrade.json"))
    }
    priority 1
}

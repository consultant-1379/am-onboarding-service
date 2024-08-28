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
package contracts.api.vnf_packages.positive.healthCheck

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for checking health of Onboarding service

```
given:
  client requests to check health
when:
  a valid request is made to check health
then:
  the health of Onboarding service has been returned
```

""")
    request {
        method GET()
        url("/actuator/health")
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
    }
}

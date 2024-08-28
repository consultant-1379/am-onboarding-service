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
package contracts.api.packages.positive.getAllPackagesV2

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario of retrieving all onboarded packages with UI verbosity

```
given:
  client requests to retrieve all onboarded packages with verbosity level
when:
  a valid request is made to retrieve packages with verbosity level
then:
  all onboarded packages are returned in specific format
```

""")
    request{
        method GET()
        urlPath("/api/v2/packages") {
            queryParameters {
                parameter 'verbosity': value(consumer(regex("^(?:ui|default)\$")),
                        producer("ui"))
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("allPackageDetailsUIResponse.json"))
    }
}

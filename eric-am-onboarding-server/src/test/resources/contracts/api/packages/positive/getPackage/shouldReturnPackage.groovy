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
package contracts.api.packages.positive.allPackages

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario of retrieving all onboarded packages

```
given:
  client requests to retrieve onboarded package by id
when:
  a valid request is made to retrieve package
then:
  specific onboarded package is returned
```

""")
    request{
        method GET()
        url "/api/v1/packages/d3def1ce-4cf4-477c-aab3-21cb04e6a381"
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("packageDetailsResponse.json"))
    }
}

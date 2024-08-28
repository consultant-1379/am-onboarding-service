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
package contracts.api.packages.negative.notfound

import org.springframework.cloud.contract.spec.Contract



Contract.make {
    description("""
Captures cases when there is no onboarded package found by an AppPkgId

```
given:
  client requests to retrieve onboarded package by an AppPkgId
when:
  a request is made to retrieve the onboarded package by an AppPkgId
then:
  An error message is returned
```

""")
    request{
        method GET()
        url "/api/v1/packages/${value("a-non-existent-id")}"
    }
    response {
        status NOT_FOUND()
        body(file("packageNotFoundResponse.json"))
        headers {
            contentType(applicationJson())
        }
    }
priority(1)
}

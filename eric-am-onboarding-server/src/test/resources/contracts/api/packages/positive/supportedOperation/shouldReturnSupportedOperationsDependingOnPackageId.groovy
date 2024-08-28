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
package contracts.api.packages.positive.supportedOperation

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for a supported operations response depending on a valid packageId

```
given:
  client requests to retrieve a supported operations by a packageId
when:
  a valid request is made to retrieve the supported operations by packageId
then:
  the supported operations response is returned
```

""")
    request{
        method GET()
        url "/api/v1/packages/${value(consumer(anyNonEmptyString()),producer("spider-app-multi-v2-2cb5"))}/supported_operations"
        headers {
            accept(applicationJson())}
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("supportedOperations.json"))
    }

}


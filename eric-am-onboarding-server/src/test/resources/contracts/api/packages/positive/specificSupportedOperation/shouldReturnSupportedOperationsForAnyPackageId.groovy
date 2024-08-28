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
package contracts.api.packages.positive.stubSupportedOperation

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for a supported operations response for any valid packageId

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
        urlPath("/api/v1/packages/d3def1ce-4cf4-477c-aab3-21cb04e6a379/supported_operations")
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("supportedOperations.json"))
    }
}


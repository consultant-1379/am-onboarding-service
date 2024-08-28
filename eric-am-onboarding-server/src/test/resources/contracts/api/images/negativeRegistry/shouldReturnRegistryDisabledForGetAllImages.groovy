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
package contracts.api.images.negativeRegistry;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns unsupported response for get all images when container registry is disabled

```
given:
  client requests to get all images
when:
  a valid request is made to get all images api
then:
  error message saying this feature has been disabled
```

""")
    request{
        method GET()
        url "/api/v1/images"
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(file("errorContainerRegistry.json"))
    }
}

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
Returns unsupported response for get image tag when container registry is disabled

```
given:
  client requests to chart details by name
when:
  a valid request is made to list charts by name
then:
  the chart details of the specified chart are returned
```

""")
    request{
        method GET()
        url "/api/v1/images/${value(consumer(regex(/([a-zA-Z0-9]+[-]?[a-zA-Z0-9]+)+/)))}/${value(consumer(regex(/([a-zA-Z0-9]+[-]?[a-zA-Z0-9]+)+/)))}"
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(file("errorContainerRegistry.json"))
    }
}

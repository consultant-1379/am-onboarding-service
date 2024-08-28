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
package contracts.api.charts.forEmptyResponse

import org.springframework.cloud.contract.spec.Contract



Contract.make {
    description("""
Captures cases when there is no chart found by a name

```
given:
  client requests to chart details by name
when:
  a request is made to list charts by name
then:
  An empty json response is returned
```

""")
    request{
        method GET()
        url "/api/v1/charts/${value(consumer(regex(/test-([a-zA-Z0-9]+[-]?[a-zA-Z0-9]+)+/)))}"
    }
    response {
        status OK()
        headers {
            contentType(textPlain())
        }
        body( "{}")
    }
}

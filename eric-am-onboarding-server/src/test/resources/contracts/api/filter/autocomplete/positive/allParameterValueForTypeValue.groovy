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
package contracts.api.filter.autocomplete.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Valid scenario for returning all the values of the filter parameter

```
given:
  client requests to retrieve filter data
when:
  a valid request is submitted
then:
  a valid response with the filter autocomplete data is returned
```

""")
    request{
        method GET()
        urlPath("/api/v1/packages/filter/autocomplete") {
            queryParameters {
                parameter 'type': value(consumer("S"), producer("S"))
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("typeParameterValue.json"))
    }
}

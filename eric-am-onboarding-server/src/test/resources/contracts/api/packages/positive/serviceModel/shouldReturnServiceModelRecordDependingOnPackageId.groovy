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
package contracts.api.packages.positive.serviceModel

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for a service model record response depending on a valid packageId

```
given:
  client requests to retrieve a service model record by a packageId
when:
  a valid request is made to retrieve the service model record by packageId
then:
  the service model record response is returned
```

""")
    request{
        method GET()
        url "/api/v1/packages/${value(consumer(anyNonEmptyString()),producer("b3def1ce-4cf4-477c-aab3-21cb04e6a379"))}/service_model"
        headers {
            accept(applicationJson())}
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("serviceModelResponse.json"))
    }

}


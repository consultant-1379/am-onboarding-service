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
package contracts.api.packages.negative.serviceModel

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns faliure scenario for a non-existent service model record with a specific package id

```
given:
  client requests to retrieve a service model record by a packageId
when:
  a request is made to retrieve the non-existent service model record by packageId
then:
  the error message is returned
```

""")
    request{
        method GET()
        url "/api/v1/packages/${value(consumer(anyNonEmptyString()),producer("not-found"))}/service_model"
        headers {
            accept(applicationJson())}
    }
    response {
        status NOT_FOUND()
        headers {
            contentType(applicationJson())
        }
        body(file("serviceModelNotFoundResponse.json"))
    }

}


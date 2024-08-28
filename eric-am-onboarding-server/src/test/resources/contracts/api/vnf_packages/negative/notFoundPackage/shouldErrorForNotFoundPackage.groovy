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
package contracts.api.vnf_packages.positive.getPackage
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns failure scenario for a non-existent onboarded vnf package using vnf package id

```
given:
  client requests to retrieve an onboarded package by its id
when:
  a request is made to retrieve the non-existent onboarded package by id
then:
  an error is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}NOTFOUND"
        headers {
            accept(applicationJson())}
    }
    response {
        status NOT_FOUND()
        headers {
            contentType(applicationJson())
        }
        body(
                """
                   {
                       "type":"about:blank",
                       "title":"Package not found",
                       "status":404,
                       "detail":"Vnf package with id ${fromRequest().path(4)} does not exist.",
                       "instance":"about:blank"
                   }
                """
        )
    }
    priority 1
}

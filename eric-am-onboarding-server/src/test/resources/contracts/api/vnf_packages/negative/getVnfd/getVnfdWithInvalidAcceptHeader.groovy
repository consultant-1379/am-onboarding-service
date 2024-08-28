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
package contracts.api.vnf_packages.negative.getVnfd
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns failure scenario for getting a vnfd

```
given:
  client requests to retrieve a vnfd
when:
  a invalid request with invalid unsupported accept header
then:
  a error response is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()),producer("d3def1ce-4cf4-477c-aab3-21cb04e6a380"))}/vnfd"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                """
                   {
                       "type":"about:blank",
                       "title":"User input is not correct",
                       "status":400,
                       "detail":"Invalid accept type provided application/json, only text/plain, application/zip or text/plain,application/zip is supported",
                       "instance":"about:blank"
                   }
                """
        )

    }
    priority 2
}

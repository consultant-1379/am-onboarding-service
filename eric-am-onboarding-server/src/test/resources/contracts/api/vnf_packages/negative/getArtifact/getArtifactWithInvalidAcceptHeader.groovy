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
package contracts.api.vnf_packages.negative.getArtifact
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns failure scenario for getting an artifact

```
given:
  client requests to retrieve an artifact
when:
  a invalid request with invalid unsupported accept header
then:
  a error response is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}/artifacts/${value(consumer(anyNonEmptyString()))}"
        headers {
            accept(applicationPdf())
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
                       "title":"Media type not supported for this api",
                       "status":400,
                       "detail":"No acceptable representation",
                       "instance":"about:blank"
                   }
                """
        )

    }
    priority 2
}

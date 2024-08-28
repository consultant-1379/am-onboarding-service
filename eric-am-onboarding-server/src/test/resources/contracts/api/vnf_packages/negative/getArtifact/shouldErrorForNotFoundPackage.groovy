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
Returns failure scenario for using vnf package id which does not exist

```
given:
  client requests to retrieve an artifact by its id
when:
  a request is made to retrieve the artifact by id
then:
  an error is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}NOTFOUND/artifacts/${value(consumer(anyNonEmptyString()))}"
        headers {
            accept(textPlain())}
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
                       "detail":"Package with id: ${fromRequest().path(4)} not found",
                       "instance":"about:blank"
                   }
                """
        )
    }
    priority 1
}

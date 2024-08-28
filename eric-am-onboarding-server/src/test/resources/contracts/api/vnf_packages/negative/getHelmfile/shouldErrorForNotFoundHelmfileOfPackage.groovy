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
package contracts.api.vnf_packages.negative.getHelmfile
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns failure scenario for a non-existent helmfile inside onboarded vnf package

```
given:
  client requests to retrieve a helmfile by package id
when:
  a request is made to retrieve the non-existent helmfile by package id
then:
  an error is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}NO_HELMFILE/helmfile"
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
                    "message": "Helmfile for package ${value(consumer(anyNonEmptyString()))}NO_HELMFILE does not exist. Try another package." 
                    }
                """)
    }
    priority 1
}

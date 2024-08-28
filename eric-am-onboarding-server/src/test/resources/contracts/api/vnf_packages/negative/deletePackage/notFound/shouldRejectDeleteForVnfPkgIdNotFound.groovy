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
package contracts.api.vnf_packages.negative.deletePackage.notFound;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Captures cases when there is no onboarded package found by a VnfPkgId.

```
given:
  client requests to delete a package by VnfPkgId which does not exist
when:
  a request is made to delete the onboarded package by a VnfPkgId
then:
  an error message is returned.
```

""")
    request{
        method DELETE()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}NOTFOUND"
        headers {
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status NOT_FOUND()
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
        headers {
            contentType(applicationJson())
        }
    }
    priority 2
}

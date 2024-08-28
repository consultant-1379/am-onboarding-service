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
package contracts.api.vnf_packages.negative.deletePackage.usagestate;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Captures cases when the usage state of the onboarded package is not NOT_IN_USE.

```
given:
  client requests to delete a package by VnfPkgId whose usage state is not NOT_IN_USE
when:
  a request is made to delete the onboarded package by a VnfPkgId
then:
  an error message is returned.
```

""")
    request{
        method DELETE()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}USAGE_STATE_IN_USE"
        headers {
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                           "type":"about:blank",
                           "title":"Invalid package state",
                           "status":409,
                           "detail":"Invalid package usage state. Usage state must be set to NOT_IN_USE.",
                           "instance":"about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(2)
}

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
package contracts.api.vnf_packages.positive.deletePackage

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Successful scenario for deleting a package by VnfPkgId.

```
given:
  client requests to delete onboarded package by a VnfPkgId
when:
  a request is made to delete the onboarded package by a VnfPkgId
then:
  the package is deleted.
```

""")
    request{
        method DELETE()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()), producer("8b57fd6e-94e0-4763-8e7c-80a8c8ba1cc3"))}"
        headers {
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status NO_CONTENT()
    }
}

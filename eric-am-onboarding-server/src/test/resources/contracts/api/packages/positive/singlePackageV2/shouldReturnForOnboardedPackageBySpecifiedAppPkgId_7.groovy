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
package contracts.api.packages.positive.singlePackageV2

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for an onboarded package using a specific AppPkgId

```
given:
  client requests to retrieve an onboarded package by AppPkgId
when:
  a valid request is made to retrieve the onboarded package by AppPkgId
then:
  the onboarded package with that id is returned
```

""")
    request{
        method GET()
        url "/api/v2/packages/d2def1ce-4cdff1-477c-aab3-21cb04e6a3236"
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("packageDetailsSpecific7.json"))

    }
    priority 3
}

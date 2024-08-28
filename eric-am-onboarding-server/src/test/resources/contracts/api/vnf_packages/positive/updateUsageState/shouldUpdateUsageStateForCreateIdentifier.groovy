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
package contracts.api.vnf_packages.positive.updateUsageStateForCreate

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Successful scenario for updating the usage state of a package.

```
given:
  client requests to update usage state on create vnf identifier
when:
  a request is made to update the usage state
then:
  a successful response is returned
```

""")
    request{
        method PUT()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}/update_usage_state"
        headers {
            contentType(applicationJson())
        }
        body(
                "vnfId": "a1def1ce-4cf4-477c-aab3-21cb04e6a379",
                "isInUse": true
        )
        bodyMatchers {
            jsonPath('$.vnfId', byRegex(nonEmpty()).asString())
            jsonPath('$.isInUse', byRegex("true|false"))
        }
    }
    response {
        status OK()
    }
}

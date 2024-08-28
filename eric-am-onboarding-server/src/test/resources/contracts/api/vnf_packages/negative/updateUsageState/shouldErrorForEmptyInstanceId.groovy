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
package contracts.api.vnf_packages.negative.updateUsageStateForCreate

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an Error scenario in Update usage state

```
given:
  client requests to update usage state
when:
  a request is made with empty instance Id
then:
  an error response is returned
```

""")
    request{
        method PUT()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}/update_usage_state"
        headers {
            contentType(applicationJson())
        }
        body(
                "vnfId": "",
                "isInUse": true
        )
        bodyMatchers {
            jsonPath('$.isInUse', byRegex("true|false"))
        }
    }
    response {
        status UNPROCESSABLE_ENTITY()
        body(
                 """
                    {
                        "type": "about:blank",
                        "title": "Mandatory parameter missing",
                        "status": 422,
                        "detail": "vnfId size must be between 1 and 2147483647",
                        "instance": "about:blank"
                    }
                 """
        )
        headers {
            contentType(applicationJson())
        }
    }
}

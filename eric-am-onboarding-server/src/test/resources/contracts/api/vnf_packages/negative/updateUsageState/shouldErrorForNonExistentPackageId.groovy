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
  a request is made with non existent package Id
then:
  an error response is returned
```

""")
    request{
        method PUT()
        url "/api/vnfpkgm/v1/vnf_packages/non-existent-packageId/update_usage_state"
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
        status NOT_FOUND()
        body(
                 """
                    {
                        "type": "about:blank",
                        "title": "Package not found",
                        "status": 404,
                        "detail": "Package with id: non-existent-packageId not found",
                        "instance": "about:blank"
                    }
                 """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}

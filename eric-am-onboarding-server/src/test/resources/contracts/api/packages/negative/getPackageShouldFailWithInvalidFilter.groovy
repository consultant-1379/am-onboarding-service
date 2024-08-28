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
package contracts.api.packages.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns invalid scenario of get onboarded packages with invalid filter

```
given:
  client requests to retrieve all onboarded packages with invalid filter
when:
  a request is made to retrieve packages with invalid filter with wrong parameter
then:
  an error response is returned with error details
```

""")
    request{
        method GET()
        urlPath("/api/v1/packages") {
            queryParameters {
                parameter 'filter': value(consumer(regex("^\\((?:eq|neq|in|nin|gt|gte|lt|lte|cont|ncont),((?!" +
                        "packages/appPkgId|packages/appDescriptorId|packages/appProvider|packages/appProductName" +
                        "|packages/appSoftwareVersion|packages/descriptorVersion|packages/onboardingState" +
                        "|packages/usageState).*),(?:\\w+)\\)\$")), producer("(eq,test,false)"))
            }
        }
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                message: $(value("Filter eq,test,false not supported"))
        )
    }
}

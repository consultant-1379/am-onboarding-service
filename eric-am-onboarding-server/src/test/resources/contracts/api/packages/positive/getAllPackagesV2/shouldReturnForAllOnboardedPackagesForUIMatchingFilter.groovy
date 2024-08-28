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
package contracts.api.packages.positive.getAllPackagesV2

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario of retrieving all onboarded packages with UI verbosity and filter

```
given:
  client requests to retrieve all onboarded packages with verbosity level and filter
when:
  a valid request is made to retrieve packages with verbosity level and filter
then:
  all onboarded packages are returned in specific format
```

""")
    request{
        method GET()
        urlPath("/api/v2/packages") {
            queryParameters {
                parameter 'verbosity': value(consumer(regex("^(?:ui|default)\$")), producer("ui"))
                parameter 'filter': value(consumer(regex("^\\((?:eq|neq|in|nin|gt|gte|lt|lte|cont|ncont),(?:" +
                        "packages/appPkgId|packages/appDescriptorId|packages/appProvider|packages/appProductName" +
                        "|packages/appSoftwareVersion|packages/descriptorVersion|packages/onboardingState" +
                        "|packages/usageState),([a-zA-Z0-9- ]+)\\)\$")),
                        producer("(eq,packages/appProductName,SGSN-MME)"))
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("allPackageDetailsUIResponse.json"))
    }
}

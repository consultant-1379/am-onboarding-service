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
Returns successful scenario of retrieving all onboarded packages matching filter

```
given:
  client requests to retrieve all onboarded packages with filter
when:
  a valid request is made to retrieve packages
then:
  all onboarded packages are returned
```

""")
    request{
        method GET()
        urlPath("/api/v2/packages") {
            queryParameters {
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
        body(file("allPackageDetailsResponse.json"))
    }
}

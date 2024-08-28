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
package contracts.api.charts.onboarding

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario of listing chart details

```
given:
  client requests to list chart details
when:
  a valid request is made to list charts
then:
  the chart details of the available charts are returned
```

""")
    request{
        method GET()
        url "/api/v1/charts"
    }
    response {
        status OK()
        headers {
            contentType(textPlain())
        }
        body(file("allChartDetails.json"))
    }
}

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
package contracts.api.vnf_packages.positive.getHelmfiles
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for helmfiles content using the vnf package id

```
given:
  client requests to retrieve the list of content by package id
when:
  a valid request is made to retrieve the list of content by package id
then:
  the list of content is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()),producer("d3def1ce-4cf4-477c-aab3-21cb04e6a380"))}/helmfile"
        headers {
            accept(applicationJson())}
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body('''\u0001\u0002\u0003''')
    }
    priority 2
}

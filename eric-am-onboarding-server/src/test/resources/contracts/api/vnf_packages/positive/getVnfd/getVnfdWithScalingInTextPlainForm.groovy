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
package contracts.api.vnf_packages.positive.getVnfd
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for getting a vnfd

```
given:
  client requests to retrieve a vnfd which has scaling information
when:
  a valid request is made to retrieve a vnfd
then:
  a valid vnfd is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()),producer("d3def1ce-4cf4-477c-aab3-21cb04e6a380"))}/vnfd"
        headers {
            accept(textPlain())
        }
    }
    response {
        status OK()
        headers {
            contentType(textPlain())
        }
        body(file("sample_vnfd_with_scaling.yaml.properties"))

    }
    priority 2
}

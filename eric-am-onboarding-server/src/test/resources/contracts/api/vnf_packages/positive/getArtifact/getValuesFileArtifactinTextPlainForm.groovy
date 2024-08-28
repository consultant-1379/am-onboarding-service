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
package contracts.api.vnf_packages.positive.getArtifact
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for getting an artifact

```
given:
  client requests to retrieve an artifact
when:
  a valid request is made to retrieve an artifact
then:
  a valid artfact is returned
```

""")
    request{
        method GET()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(anyNonEmptyString()))}/artifacts/Definitions/OtherTemplates/values_1.yaml"
        headers {
            accept(textPlain())
        }
    }
    response {
        status OK()
        headers {
            contentType(textPlain())
        }
        body('''
{ "values": { "name": "chart_values" }, "resources": { "memory": "20Gi", "cpu": "400m" }, "limits": { "memory": "30Gi", "cpu": "1000" }}                
''')

    }
    priority 4
}

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
        url "/api/vnfpkgm/v1/vnf_packages/scale-non-scalable-chart/artifacts/Definitions/OtherTemplates/scaling_mapping.yaml"
        headers {
            accept(textPlain())
        }
    }
    response {
        status OK()
        headers {
            contentType(textPlain())
        }
        body(file("scaling_mapping_scalable_one_chart.yaml.properties"))

    }
    priority 1
}

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
package contracts.api.packages.getAdditionalAttributesForOperationType
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Returns successful scenario for getting additional parameters from vnfd


```
given:
  client requests additional parameters for operation and package
when:
  a valid request is made to retrieve additional parameters
then:
  aadditional parameters are returned
```

""")
    request{
        method GET()
        urlPath( "/api/v1/packages/spider-app-multi-v2-2cb5/instantiate/additional_parameters") {
            queryParameters {
                parameter('targetDescriptorId' : "")
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file("additionalAttributesForInstantiateVnfdVersion1dot2.json"))

    }
    priority 1
}

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
package contracts.api.vnf_packages.positive.uploadVnfPkgRequest

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Successful scenario for uploading package as octet stream.

```
given:
  client requests to upload package content
when:
  a request is made to upload package content
then:
  a successful response is returned
```

""")
    request{
        method PUT()
        url "/api/vnfpkgm/v1/vnf_packages/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/package_content"
        headers {
            contentType(applicationOctetStream())
            accept(applicationJson())
        }
        body(
                fileAsBytes('sampledescriptor.csar')
        )
    }
    response {
        status ACCEPTED()
    }
}

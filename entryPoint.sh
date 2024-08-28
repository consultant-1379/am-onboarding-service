#!/bin/bash
#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# Symbolic link creation for backward compatibility
ln -svfT /run/secrets/ca/root /obs/ca

cat /run/secrets/cacerts/ca/tls >> /etc/ssl/ca-bundle.pem
JAVA_OPTS=$@

if [[ -n ${DOCKER_REGISTRY_ADDRESS} ]]; then
  echo "DOCKER_REGISTRY_ADDRESS:" ${DOCKER_REGISTRY_ADDRESS}

  touch empty
  # catting an empty file into the command ensures it completes immediately, otherwise it waits for user input
  RESULT=$(cat empty | openssl s_client -showcerts -debug -servername ${DOCKER_REGISTRY_ADDRESS} -connect ${DOCKER_REGISTRY_ADDRESS}:443 2>&1 | grep error)
  echo "RESULT:" ${RESULT}
  if [[ -z ${RESULT} ]]; then
    echo "Openssl connection to ${DOCKER_REGISTRY_ADDRESS} was successful"
  else
    echo "Openssl connection to ${DOCKER_REGISTRY_ADDRESS} failed, logging ca cert bundle for debugging purposes"
    cat /run/secrets/cacerts/ca/tls
    echo "Logging /etc/ssl/ca-bundle for debugging purposes"
    cat /etc/ssl/ca-bundle.pem
    echo "Logging response for SSL connection check"
    cat empty | openssl s_client -showcerts -servername ${DOCKER_REGISTRY_ADDRESS} -connect ${DOCKER_REGISTRY_ADDRESS}:443
    echo "Below are the errors:"
    echo $RESULT
    exit 1
  fi
fi


mkdir individualCerts && cd $_
FILE_COUNT=$(csplit -f individual- /run/secrets/cacerts/ca/tls '/-----BEGIN CERTIFICATE-----/' '{*}' --elide-empty-files | wc -l)
echo "Number of certs in cacert bundle is ${FILE_COUNT}"
for i in $(ls); do
  echo "Adding ${i} to java keystore"
  keytool -storepass 'changeit' -noprompt -trustcacerts -importcert -file ${i} -alias ${i} -keystore /var/lib/ca-certificates/java-cacerts 2>&1
done
cd ../
rm -rf individualCerts
java $JAVA_OPTS -Djdk.tls.client.protocols="TLSv1.3,TLSv1.2" -Djava.security.egd=file:/dev/./urandom -jar /eric-am-onboarding-service.jar
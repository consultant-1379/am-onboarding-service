#!/usr/bin/env bash
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


echo "Starting acceptance test execution"

function exitErrorNoIngress() {
    echo "[ERROR] Mandatory argument '--ingress=<ingress host>' must be specified"
    exit 1
}

if [[ $1 != "--ingress="* ]]
then
    exitErrorNoIngress
else
    ingress=${1#*=}
    if [[ -z ${ingress} ]]
    then
        exitErrorNoIngress
    fi
    echo "ingress.host defined: ${ingress}"
fi

if [[ $2 == "--jvmArgs="* ]]
then
    jvmArgs=${2#*=}
else
    jvmArgs=""
fi

command_to_run="exec java -Dcontainer.host=${ingress} ${jvmArgs} -jar /acc_tests/eric-am-onboarding-service-testware.jar"

echo "Command to run: ${command_to_run}"

${command_to_run}

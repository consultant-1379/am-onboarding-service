# This README is dedicated for running onboarding acceptance test

## Overview
This module is dedicated for testing onboarding service with the task of adding a new csar into EVNFM.

## Running the Onboarding MS tests from the IDE towards an installed system in a cluster
Preconditions:

* Deploy EVNFM
  * links:
    * https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ESO/%5B+INTERNAL+ONLY+%5D+How+To%3A+Manually+deploy+EO+EVNFM+from+start+to+finish
    * OR
    * https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ESO/How+to+deploy+into+EVNFM+cluster
* Download all necessary CSARs
  * links:
    * https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ESO/EVNFM+Testing+CSARs
    * OR
    * https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/#view-repositories;evnfm_testing_artifacts~browsestorage
* Update path to your CSARs in json config files here eric-am-onboarding-service-testware/src/main/resources. Change value "csarDownloadUrl" to
  your local path

Steps:

* Run Class "TestRunner" or method with @Test
* Open "Edit Configuration"
* Edit VM options with next values:
```
-Dcontainer.host=https://<Your deployed EVNFM URL>/vnfm/onboarding
-Drun.type=local
-Dvnfm.user=<User of your deployed EVNFM>
-Dvnfm.password=<User's password of your deployed EVNFM>
-Dnamespace=<namespece with your app>
```
Save and run

### Notes
* For local run of acceptance test with tocsao flag "skipToscaoValidation" in the onboarding deployment should be true.


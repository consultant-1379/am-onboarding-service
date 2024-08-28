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

ARG BASE_IMAGE_VERSION
FROM armdocker.rnd.ericsson.se/proj-am/sles/sles-corretto-openjdk17:${BASE_IMAGE_VERSION}
ARG GIT_COMMIT=""
ARG APP_VERSION=""
ARG BUILD_TIME=""

LABEL product.number="CXC 201 1721/1" \
      product.revision="R1A" \
      com.ericsson.product-name="EVNFM Onboarding Service" \
      com.ericsson.product-number="CXU 101 0686" \
      com.ericsson.product-revision="R1A" \
      org.opencontainers.image.title="EVNFM Onboarding Service" \
      org.opencontainers.image.created=${BUILD_TIME} \
      org.opencontainers.image.revision=${GIT_COMMIT} \
      org.opencontainers.image.version=${APP_VERSION} \
      org.opencontainers.image.vendor="Ericsson"


ENV OBS_DATA_DIR=/obs
# User Id generated based on ADP rule DR-D1123-122 (am-onboarding-service : 198763)
ENV OBS_GID=198763
ENV OBS_UID=198763
ENV JAVA_OPTS ""

ADD eric-am-onboarding-server/target/eric-am-onboarding-service.jar eric-am-onboarding-service.jar

COPY entryPoint.sh /entryPoint.sh
COPY entryPoint-cm.sh /entryPoint-cm.sh
COPY waitAndShutdown.sh /waitAndShutdown.sh

ARG OBS_HOME_DIR="${OBS_DATA_DIR}/home/onboarding-user"

RUN mkdir -p ${OBS_HOME_DIR}
RUN chown ${OBS_UID}:${OBS_UID} ${OBS_HOME_DIR}

RUN echo "${OBS_UID}:x:${OBS_UID}:${OBS_GID}:Onboarding User:${OBS_HOME_DIR}:/bin/false" >> /etc/passwd \
    && sed -i '/root/s/bash/false/g' /etc/passwd

RUN sh -c 'touch /eric-am-onboarding-service.jar' \
    && chmod 777 /entryPoint.sh \
    && chmod 777 /entryPoint-cm.sh \
    && chmod 777 /waitAndShutdown.sh

RUN zypper install -l -y shadow util-linux unzip \
    && zypper clean --all \
    && mkdir -p "$OBS_DATA_DIR" \
    && chown -fR ${OBS_UID}:0 "$OBS_DATA_DIR" \
    && chmod -R g=u "$OBS_DATA_DIR" \
    && chmod 777 "$OBS_DATA_DIR" /tmp \
    && chown ${OBS_UID}:0 /var/lib/ca-certificates/java-cacerts \
    && chmod -R g=u /var/lib/ca-certificates/java-cacerts \
    && chmod 755 /var/lib/ca-certificates/java-cacerts \
    && chown ${OBS_UID}:0 /var/lib/ca-certificates/ca-bundle.pem \
    && chmod -R g=u /var/lib/ca-certificates/ca-bundle.pem \
    && chown ${OBS_UID}:0 /etc/ssl/ca-bundle.pem \
    && chmod -R g=u /etc/ssl/ca-bundle.pem \
    && chmod 755 /etc/ssl/ca-bundle.pem

RUN zypper -n install skopeo

USER ${OBS_UID}:${OBS_GID}
WORKDIR ${OBS_DATA_DIR}
ENTRYPOINT ["sh", "-c", "/entryPoint.sh $JAVA_OPTS"]

EXPOSE 8888

{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-am-onboarding-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "eric-am-onboarding-service.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- template "eric-am-onboarding-service.name" . -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-am-onboarding-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create main image registry url
*/}}
{{- define "eric-am-onboarding-service.mainImagePath" -}}
  {{- include "eric-eo-evnfm-library-chart.mainImagePath" (dict "ctx" . "svcRegistryName" "onboardingService") -}}
{{- end -}}

{/*
The pgInitContainer image registry url
*/}}
{{- define "eric-am-onboarding-service.pgInitContainerPath" -}}
  {{- include "eric-eo-evnfm-library-chart.pgInitContainerPath" . -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-am-onboarding-service.pullSecrets" -}}
  {{- include "eric-eo-evnfm-library-chart.pullSecrets" . -}}
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-am-onboarding-service.prometheus" -}}
  {{- include "eric-eo-evnfm-library-chart.prometheus" . -}}
{{- end -}}

{{/*
Create pullPolicy for onboarding service init container
*/}}
{{- define "eric-am-onboarding-service.pgInitContainer.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "pgInitContainer") -}}
{{- end -}}

{{/*
Create pullPolicy for onboarding service container
*/}}
{{- define "eric-am-onboarding-service.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "onboardingService") -}}
{{- end -}}

{{/*
Define nodeSelector property
*/}}
{{- define "eric-am-onboarding-service.nodeSelector" -}}
  {{- include "eric-eo-evnfm-library-chart.nodeSelector" . -}}
{{- end -}}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-am-onboarding-service.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Kubernetes labels
*/}}
{{- define "eric-am-onboarding-service.kubernetes-labels" -}}
app.kubernetes.io/name: {{ include "eric-am-onboarding-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ include "eric-am-onboarding-service.version" . }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-am-onboarding-service.labels" -}}
  {{- $kubernetesLabels := include "eric-am-onboarding-service.kubernetes-labels" . | fromYaml -}}
  {{- $globalLabels := (.Values.global).labels -}}
  {{- $serviceLabels := .Values.labels -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $kubernetesLabels $globalLabels $serviceLabels)) }}
{{- end -}}

{{/*
Merged labels for extended defaults
*/}}
{{- define "eric-am-onboarding-service.labels.extended-defaults" -}}
  {{- $extendedLabels := dict -}}
  {{- $_ := set $extendedLabels "app" (include "eric-am-onboarding-service.name" .) -}}
  {{- $_ := set $extendedLabels "chart" (include "eric-am-onboarding-service.chart" .) -}}
  {{- $_ := set $extendedLabels "release" (.Release.Name) -}}
  {{- $_ := set $extendedLabels "heritage" (.Release.Service) -}}
  {{- $_ := set $extendedLabels "eric-eo-lm-consumer-access" "true" -}}
  {{- $_ := set $extendedLabels "eric-data-object-storage-mn-access" "true" -}}
  {{- $_ := set $extendedLabels "logger-communication-type" "direct" -}}
  {{- $commonLabels := include "eric-am-onboarding-service.labels" . | fromYaml -}}
  {{- $serviceMesh := include "eric-am-onboarding-service.service-mesh-inject" . | fromYaml -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $commonLabels $extendedLabels $serviceMesh)) | trim }}
{{- end -}}

{{/*
Create Ericsson product specific annotations
*/}}
{{- define "eric-am-onboarding-service.helm-annotations_product_name" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations_product_name" . -}}
{{- end -}}
{{- define "eric-am-onboarding-service.helm-annotations_product_number" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations_product_number" . -}}
{{- end -}}
{{- define "eric-am-onboarding-service.helm-annotations_product_revision" -}}
  {{- include "eric-eo-evnfm-library-chart.helm-annotations_product_revision" . -}}
{{- end -}}

{{/*
Create a dict of annotations for the product information (DR-D1121-064, DR-D1121-067).
*/}}
{{- define "eric-am-onboarding-service.product-info" }}
ericsson.com/product-name: {{ template "eric-am-onboarding-service.helm-annotations_product_name" . }}
ericsson.com/product-number: {{ template "eric-am-onboarding-service.helm-annotations_product_number" . }}
ericsson.com/product-revision: {{ template "eric-am-onboarding-service.helm-annotations_product_revision" . }}
{{- end }}

{{/*
Common annotations
*/}}
{{- define "eric-am-onboarding-service.annotations" -}}
  {{- $productInfo := include "eric-am-onboarding-service.product-info" . | fromYaml -}}
  {{- $globalAnn := (.Values.global).annotations -}}
  {{- $serviceAnn := .Values.annotations -}}
  {{- include "eric-eo-evnfm-library-chart.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $globalAnn $serviceAnn)) | trim }}
{{- end -}}

{{/*
Define probes property
*/}}
{{- define "eric-am-onboarding-service.probes" -}}
{{- $default := .Values.probes -}}
{{- if .Values.probing }}
  {{- if .Values.probing.liveness }}
    {{- if .Values.probing.liveness.onboarding }}
      {{- $default := mergeOverwrite $default.onboarding.livenessProbe .Values.probing.liveness.onboarding  -}}
    {{- end }}
  {{- end }}
  {{- if .Values.probing.readiness }}
    {{- if .Values.probing.readiness.onboarding }}
      {{- $default := mergeOverwrite $default.onboarding.readinessProbe .Values.probing.readiness.onboarding  -}}
    {{- end }}
  {{- end }}
{{- end }}
{{- $default | toJson -}}
{{- end -}}

{{/*
To support Dual stack.
*/}}
{{- define "eric-am-onboarding-service.internalIPFamily" -}}
  {{- include "eric-eo-evnfm-library-chart.internalIPFamily" . -}}
{{- end -}}

{{/*
Define podPriority property
*/}}
{{- define "eric-am-onboarding-service.podPriority" -}}
  {{- include "eric-eo-evnfm-library-chart.podPriority" ( dict "ctx" . "svcName" "onboarding" ) -}}
{{- end -}}

{{/*
Define tolerations property
*/}}
{{- define "eric-am-onboarding-service.tolerations.onboarding" -}}
  {{- include "eric-eo-evnfm-library-chart.merge-tolerations" (dict "root" . "podbasename" "onboarding" ) -}}
{{- end -}}

{{/*
Define DB connection pool max life time property
If not set by user, defaults to 14 minutes.
*/}}
{{ define "eric-am-onboarding-service.db.connection.pool.max.lifetime" -}}
- name: "spring.datasource.hikari.max-lifetime"
  value: {{ index .Values "global" "db" "connection" "max-lifetime" | default "840000" | quote -}}
{{- end -}}

{{/*
Check global.security.tls.enabled
*/}}
{{- define "eric-am-onboarding-service.global-security-tls-enabled" -}}
  {{- include "eric-eo-evnfm-library-chart.global-security-tls-enabled" . -}}
{{- end -}}

{{- define "eric-am-onboarding-service.rootCASecretName" -}}
  eric-sec-sip-tls-trusted-root-cert
{{- end -}}

{{- define "eric-am-onboarding-service.rootCAVolumeName" -}}
  eric-am-onboarding-service-root-ca
{{- end -}}

{{- define "eric-am-onboarding-service.rootCAMountPath" -}}
  /run/secrets/ca/root
{{- end -}}

{{- define "eric-am-onboarding-service.rootCACertPath" -}}
  /run/secrets/ca/root/cacertbundle.pem
{{- end -}}

{{/*
DR-D470217-007-AD
This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-am-onboarding-service.service-mesh-enabled" -}}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-enabled" . -}}
{{- end -}}

{{/*
DR-D470217-011
This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-am-onboarding-service.service-mesh-inject" -}}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-inject" . -}}
{{- end -}}

{{/*
This helper defines log level for Service Mesh.
*/}}
{{- define "eric-am-onboarding-service.service-mesh-logs" -}}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-logs" . -}}
{{- end -}}

{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-am-onboarding-service.service-mesh-version" -}}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-version" . -}}
{{- end -}}

{{/*
DR-D1123-124
Evaluating the Security Policy Cluster Role Name
*/}}
{{- define "eric-am-onboarding-service.securityPolicy.reference" -}}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.reference" . -}}
{{- end -}}

{{/*
DR-D1123-136
Define fsGroup property
*/}}
{{- define "eric-am-onboarding-service.fsGroup" -}}
  {{- include "eric-eo-evnfm-library-chart.fsGroup" . -}}
{{- end -}}

{{/*
DR-D470222-010
Configuration of Log Collection Streaming Method
*/}}
{{- define "eric-am-onboarding-service.log.streamingMethod" -}}
  {{- include "eric-eo-evnfm-library-chart.log.streamingMethod" . -}}
{{- end }}

{{/*
Istio excludeOutboundPorts. Outbound ports to be excluded from redirection to Envoy.
*/}}
{{- define "eric-am-onboarding-service.excludeOutboundPorts" -}}
  {{- include "eric-eo-evnfm-library-chart.excludeOutboundPorts" . -}}
{{- end -}}

{{/*
Define ServiceAccount template
*/}}
{{- define "eric-am-onboarding-service.serviceAccount.name" -}}
  {{- printf "%s-sa" (include "eric-am-onboarding-service.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
DR-D1123-134
Rolekind parameter for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-onboarding-service.securityPolicy.rolekind" -}}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolekind" . }}
{{- end -}}

{{/*
DR-D1123-134
Rolename parameter for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-onboarding-service.securityPolicy.rolename" -}}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolename" . }}
{{- end -}}

{{/*
DR-D1123-134
RoleBinding name for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-am-onboarding-service.securityPolicy.rolebinding.name" -}}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolebinding.name" . }}
{{- end -}}

{{/*
JVM customization
*/}}
{{- define "eric-am-onboarding-service.javaToolOptions" -}}
 -Xms{{.Values.jvm.memory.heapMin}}m -Xmx{{.Values.jvm.memory.heapMax}}m
{{- end -}}

{{/*
Storage class name for /tmp dir ephemeral PV
*/}}
  {{- define "eric-am-onboarding-service.storageClassName" }}
      {{- if .Values.global.persistence }}
          {{- if .Values.global.persistence.persistentVolumeClaim }}
              {{- if .Values.global.persistence.persistentVolumeClaim.storageClassName }}
                  {{- .Values.global.persistence.persistentVolumeClaim.storageClassName }}
              {{- end }}
          {{- end }}
      {{- else }}
          {{- if .Values.ephemeral.storageClassName }}
              {{- .Values.ephemeral.storageClassName }}
          {{- end }}
      {{- end }}
  {{- end }}

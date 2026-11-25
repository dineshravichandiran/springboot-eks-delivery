{{- define "items-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "items-service.labels" -}}
app.kubernetes.io/name: {{ include "items-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Values.image.tag | default .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
environment: {{ .Values.environment }}
{{- end -}}

{{- define "items-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "items-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

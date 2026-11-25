#!/usr/bin/env bash
# Configures a FRESH SonarQube (default admin/admin) for CI: new admin password, an analysis token,
# and the quality gate this repository uses. Prints GitHub Actions outputs: token= and password=.
# The server lives only for the duration of the CI job, so every run is evaluated on the whole code base.
# Every API error is also written as a workflow annotation, so it is visible without opening the raw log.
set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
GATE="items-service-gate"
PASSWORD="admin"

LAST_BODY="$(mktemp)"
trap 'echo "::error title=sonar-provision failed at line ${LINENO}::last response: $(tr "\n" " " < "${LAST_BODY}" | head -c 500)"' ERR

call() { # call <label> <api-path> [curl args...]; prints the body; annotates every call; fails unless HTTP 2xx
  local label="$1" path="$2"; shift 2
  local code
  code="$(curl -s -o "${LAST_BODY}" -w '%{http_code}' -u "admin:${PASSWORD}" -X POST "${SONAR_URL}/api/${path}" "$@" || true)"
  echo "::notice title=SonarQube API ${label}::HTTP ${code}"
  case "${code}" in
    2??) cat "${LAST_BODY}" ;;
    *) echo "::error title=SonarQube API ${label} HTTP ${code}::$(tr '\n' ' ' < "${LAST_BODY}" | head -c 600)"; return 1 ;;
  esac
}

# SonarQube enforces a password policy (length, upper and lower case, digit, special character).
NEW_PASSWORD="Ci_$(openssl rand -hex 12)_Zz9-"
echo "::add-mask::${NEW_PASSWORD}"

call "change_password" users/change_password \
  --data-urlencode login=admin --data-urlencode previousPassword=admin --data-urlencode "password=${NEW_PASSWORD}" > /dev/null
PASSWORD="${NEW_PASSWORD}"

TOKEN_JSON="$(call "generate_token" user_tokens/generate -d name=ci)"
TOKEN="$(printf '%s' "${TOKEN_JSON}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"
echo "::add-mask::${TOKEN}"

call "create_gate" qualitygates/create -d "name=${GATE}" > /dev/null

add_condition() { # <metric> <op> <error-threshold> [<alternative metric name for newer SonarQube>]
  if ! call "condition ${1}" qualitygates/create_condition -d "gateName=${GATE}" -d "metric=${1}" -d "op=${2}" -d "error=${3}" > /dev/null; then
    [ -n "${4:-}" ] || return 1
    echo "::warning::${1} rejected, trying ${4}"
    call "condition ${4}" qualitygates/create_condition -d "gateName=${GATE}" -d "metric=${4}" -d "op=${2}" -d "error=${3}" > /dev/null
  fi
}
# The gate FAILS when the condition is true: coverage < 80, duplication > 3, a rating worse than A.
add_condition coverage LT 80
add_condition duplicated_lines_density GT 3
add_condition reliability_rating GT 1 software_quality_reliability_rating
add_condition security_rating GT 1 software_quality_security_rating

call "set_as_default" qualitygates/set_as_default -d "name=${GATE}" > /dev/null

{
  echo "token=${TOKEN}"
  echo "password=${NEW_PASSWORD}"
} >> "${GITHUB_OUTPUT:?must run inside GitHub Actions}"
echo "Quality gate '${GATE}' created and set as default."

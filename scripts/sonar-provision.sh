#!/usr/bin/env bash
# Configures a FRESH SonarQube (default admin/admin) for CI: new admin password, an analysis token,
# and the quality gate this repository uses. Prints GitHub Actions outputs: token= and password=.
# The server lives only for the duration of the CI job, so every run is evaluated on the whole code base.
set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
GATE="items-service-gate"
CURL=(curl --silent --show-error --fail-with-body)

NEW_PASSWORD="$(openssl rand -hex 16)"
echo "::add-mask::${NEW_PASSWORD}"

"${CURL[@]}" -u admin:admin -X POST "${SONAR_URL}/api/users/change_password" \
  --data-urlencode login=admin --data-urlencode previousPassword=admin --data-urlencode "password=${NEW_PASSWORD}"

TOKEN="$("${CURL[@]}" -u "admin:${NEW_PASSWORD}" -X POST "${SONAR_URL}/api/user_tokens/generate" -d name=ci \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"
echo "::add-mask::${TOKEN}"

api() { "${CURL[@]}" -u "admin:${NEW_PASSWORD}" -X POST "${SONAR_URL}/api/$1" "${@:2}" > /dev/null; }

api qualitygates/create -d "name=${GATE}"
# metric operator error-threshold: the gate FAILS when the condition is true.
while read -r metric op error; do
  api qualitygates/create_condition -d "gateName=${GATE}" -d "metric=${metric}" -d "op=${op}" -d "error=${error}"
done <<'CONDITIONS'
coverage LT 80
duplicated_lines_density GT 3
reliability_rating GT 1
security_rating GT 1
CONDITIONS
api qualitygates/set_as_default -d "name=${GATE}"

{
  echo "token=${TOKEN}"
  echo "password=${NEW_PASSWORD}"
} >> "${GITHUB_OUTPUT:?must run inside GitHub Actions}"
echo "Quality gate '${GATE}' created and set as default."

#!/usr/bin/env bash
# Prints an access token for a local QA user (password grant against the local realm, UI client).
#
#   scripts/local/token.sh qa-student            # or the full email, qa-student@elimika.local
#   TOKEN=$(scripts/local/token.sh qa-admin); curl -H "Authorization: Bearer $TOKEN" localhost:38080/...
#
# Local-only values; see docs/guides/local-environment.md.
set -euo pipefail
# shellcheck source=lib/env.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib/env.sh"

user="${1:?usage: token.sh <user>   e.g. qa-student}"
[[ "$user" == *@* ]] || user="${user}@elimika.local"

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:58080}"
REALM="${KEYCLOAK_REALM:-elimika-local}"
CLIENT_ID="${KEYCLOAK_UI_CLIENT_ID:-elimika-ui}"
CLIENT_SECRET="${KEYCLOAK_UI_CLIENT_SECRET:-elimika-local-ui-secret}"
PASSWORD="${QA_PASSWORD:-Passw0rd!}"

response=$(curl -sS -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    --data-urlencode grant_type=password \
    --data-urlencode "client_id=$CLIENT_ID" \
    --data-urlencode "client_secret=$CLIENT_SECRET" \
    --data-urlencode "username=$user" \
    --data-urlencode "password=$PASSWORD" \
    --data-urlencode scope=openid)

token=$(jq -r '.access_token // empty' <<<"$response")
if [[ -z "$token" ]]; then
    echo "token.sh: no token for $user: $(jq -r '.error_description // .error // .' <<<"$response")" >&2
    exit 1
fi
printf '%s\n' "$token"

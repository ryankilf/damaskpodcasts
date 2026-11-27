#!/usr/bin/env sh
# Prints the gateway's search-only Meilisearch key for the values in .env.
# Meilisearch derives a key's value as HMAC-SHA256(master key, key UID), so the importer can create
# the key with a fixed UID and this script can compute the same value without ever printing it
# from the server. (Covered by MeiliClientTest, so a Meilisearch upgrade that changes it fails CI.)
set -eu
cd "$(dirname "$0")"
# shellcheck source=/dev/null  # .env exists only on the server
. ./.env
: "${MEILI_MASTER_KEY:?set MEILI_MASTER_KEY in .env}"
: "${MEILI_SEARCH_KEY_UID:?set MEILI_SEARCH_KEY_UID in .env}"
printf %s "$MEILI_SEARCH_KEY_UID" | openssl dgst -sha256 -hmac "$MEILI_MASTER_KEY" | awk '{print $NF}'

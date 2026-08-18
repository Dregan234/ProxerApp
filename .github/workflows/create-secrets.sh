#!/usr/bin/env bash
set -euo pipefail

if [[ ${KEYSTORE:+1} ]] && [[ ${SECRETS:+1} ]]; then
    mkdir -p temp
    echo "${KEYSTORE}" | base64 -d >temp/keystore.jks
    echo "${SECRETS}" | base64 -d >secrets.properties
    echo "Created secrets."
else
    echo "Environment variables not set, skipping."
fi

#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 4 ]]; then
  echo "Usage: $0 <vault-path> <key> <value> <deployment> [namespace]" >&2
  exit 1
fi

VAULT_PATH=$1
KEY=$2
VALUE=$3
DEPLOYMENT=$4
NAMESPACE=${5:-1cai}

: "${VAULT_ADDR?set VAULT_ADDR}"
: "${VAULT_TOKEN?set VAULT_TOKEN}"

vault kv patch "$VAULT_PATH" "$KEY"="$VALUE"

echo "[rotate] Updated Vault secret $VAULT_PATH ($KEY)"

kubectl rollout restart deployment "$DEPLOYMENT" -n "$NAMESPACE"
kubectl rollout status deployment "$DEPLOYMENT" -n "$NAMESPACE"

echo "[rotate] Deployment $NAMESPACE/$DEPLOYMENT restarted"

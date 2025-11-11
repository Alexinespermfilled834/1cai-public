#!/usr/bin/env bash
set -euo pipefail

: "${VAULT_ADDR?set VAULT_ADDR}" 
: "${VAULT_TOKEN?set VAULT_TOKEN}" 

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)
PATH=$ROOT_DIR/.venv/bin:$PATH

log(){ printf '[vault-test] %s\n' "$*"; }

SECRET_PATH=${1:-secret/data/1cai/database}
KUBE_SECRET=${2:-db-credentials}
KUBE_KEY=${3:-url}

log "fetching from Vault: $SECRET_PATH"
VALUE=$(vault kv get -format=json "$SECRET_PATH" | jq -r '.data.data.'"${KUBE_KEY}")
if [[ -z "$VALUE" ]]; then
  log "Vault secret missing key $KUBE_KEY"
  exit 1
fi
log "Vault value: $VALUE"

if command -v kubectl >/dev/null 2>&1; then
  log "checking Kubernetes secret $KUBE_SECRET"
  KUBE_VALUE=$(kubectl get secret "$KUBE_SECRET" -n 1cai -o jsonpath="{.data.$KUBE_KEY}" | base64 -d)
  if [[ "$VALUE" != "$KUBE_VALUE" ]]; then
    log "Mismatch between Vault and Kubernetes secret"
    exit 1
  fi
  log "Kubernetes secret matches"
else
  log "kubectl not available, skipping Kubernetes comparison"
fi

log "Vault sync test passed"

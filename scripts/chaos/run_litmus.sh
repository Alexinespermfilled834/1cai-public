#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")"/../.. && pwd)
CHAOS_DIR="$ROOT_DIR/infrastructure/chaos/litmus"

kubectl apply -f "$CHAOS_DIR/pod-delete.yaml"
kubectl apply -f "$CHAOS_DIR/chaos-engine.yaml"

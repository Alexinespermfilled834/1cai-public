# Service Mesh Blueprint (Istio)

## 1. Зачем
- mTLS между сервисами (`1cai` ↔ внешние).
- Тонкое управление трафиком (canary, A/B).
- Telemetry (HTTP metrics, traces) → Grafana/Tempo.

## 2. Структура
- `infrastructure/service-mesh/istio/` — IstioOperator профиль (`profile-default.yaml`) + README.
- Make цель `mesh-istio-apply` — применяет Kustomize (создаёт `istio-system`, установку выполняет IstioOperator).

## 3. После установки
```bash
kubectl label namespace 1cai istio-injection=enabled
kubectl rollout restart deployment -n 1cai
```
- Обновить Helm values, если требуется (annotations для ingress).
- Метрики Istio автоматически окажутся в Prometheus/Grafana (см. observability stack).

## 4. Roadmap
- AuthorizationPolicy + PeerAuthentication (mTLS strict).
- VirtualService/ DestinationRule для API/MCP роутинга.
- Linkerd шаблон как альтернатива.

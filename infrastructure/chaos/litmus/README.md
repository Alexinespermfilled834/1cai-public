# Litmus Chaos Experiments

> Требуется Litmus 3.x (chaos-center или `kubectl apply -f litmus-operator.yaml`).

## Эксперимент: Pod Restart (1cai API)
- `chaos-engine.yaml` — привязывает эксперимент к namespace `1cai`, метке `app.kubernetes.io/component=api`.
- `pod-delete.yaml` — сценарий удаления пода (litmus generic).

### Запуск
```bash
kubectl apply -f infrastructure/chaos/litmus/pod-delete.yaml
kubectl apply -f infrastructure/chaos/litmus/chaos-engine.yaml
# Наблюдать через kubectl describe chaosengine api-pod-delete
```

### Cleanup
```bash
kubectl delete -f infrastructure/chaos/litmus/chaos-engine.yaml
kubectl delete -f infrastructure/chaos/litmus/pod-delete.yaml
```

## TODO
- Добавить эксперименты для Istio fault injection и network latency.
- Интегрировать отчётность в Grafana/Prometheus.

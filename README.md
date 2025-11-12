# 🤖 1C AI Stack

> Платформа, которая переводит DevOps и AI-практики для 1C:Enterprise из «ручного режима» в повторяемые процессы: от анализа конфигураций и MCP-инструментов до CI/CD, FinOps и эксплуатационных регламентов.

## Что это даёт
- Консолидация: единый стек скриптов, сервисов и документации (`src/`, `scripts/`, `docs/`).
- Прозрачность: диаграммы, ADR и регламенты обновляются автоматически (`docs/architecture/`, `docs/process/`, `docs/runbooks/`).
- Ускорение изменений: готовые пайплайны GitOps/FinOps/Observability (`infrastructure/`, `docs/ops/`, `observability/`).

![Контейнерная схема платформы](docs/architecture/uml/c4/png/container_overview.png)

## 5 минут до первого результата
1. Подготовьте окружение → `python 3.11`, Docker, Docker Compose ([гайд](docs/setup/python_311.md)).
2. Быстрая проверка среды: `make check-runtime` (использует `scripts/setup/check_runtime.py`).
3. Мини-демо локально:
   ```bash
   make docker-up      # инфраструктура: БД, брокеры, Neo4j, Qdrant
   make migrate        # первая миграция данных
   make servers        # Graph API + MCP server
   open http://localhost:6001/mcp
   ```
   > На Windows есть аналоги в `scripts/windows/`. Если всё стартовало — можно углубляться.

## Маршруты по ролям
- **DevOps / SRE** → `docs/ops/devops_platform.md`, `docs/ops/gitops.md`, `infrastructure/helm/1cai-stack`.
- **1С-разработчики и архитекторы** → `docs/06-features/EDT_PARSER_GUIDE.md`, `docs/06-features/MCP_SERVER_GUIDE.md`, `src/ai/mcp_server.py`.
- **ML / аналитики** → `docs/06-features/ML_DATASET_GENERATOR_GUIDE.md`, `docs/06-features/TESTING_GUIDE.md`, `scripts/analysis/generate_documentation.py`.
- **Операции и on-call** → `docs/runbooks/dr_rehearsal_plan.md`, `docs/process/oncall_rotations.md`, `observability/SLO.md`.

## Экспресс-тур по стеку
1. **Разбор конфигураций** — `make docker-up` → `make generate-docs`, подробности в `docs/06-features/EDT_PARSER_GUIDE.md`.
2. **Автоматизация через MCP** — запуск инструментов из `src/ai/mcp_server.py`, сценарии в `docs/06-features/MCP_SERVER_GUIDE.md`.
3. **Эксплуатация** — GitOps (`make gitops-apply`), Vault CSI (`make vault-csi-apply`), Linkerd (`make linkerd-install`), описано в `docs/ops/devops_platform.md` и `docs/ops/service_mesh.md`.

## Что уже готово
- ✅ **MCP и AI tooling** — сервер, bsl-language-server и spec-driven workflow (`src/ai/`, `docs/06-features/AST_TOOLING_BSL_LANGUAGE_SERVER.md`).
- ✅ **Инфраструктурные профили** — Helm chart, Terraform модули, Argo CD GitOps (`infrastructure/helm/`, `infrastructure/terraform/`, `infrastructure/argocd/`).
- ✅ **Надёжность и операции** — runbooks, DR rehearsal, SLO/наблюдаемость (`docs/runbooks/`, `docs/process/`, `observability/`).
- ✅ **Security & FinOps** — Rego/Conftest, Checkov/Trivy, cost-отчёты и алерты (`policy/`, `scripts/security/`, `scripts/finops/`).

## В работе и планы
- 🛠 Детализация spec-driven процессов и интеграция GitHub Spec Kit — см. `docs/research/spec_kit_analysis.md` и `docs/research/constitution.md`.
- 🛠 Расширение тестовых раннеров (YAxUnit, edt-test-runner) — отслеживаем в `docs/06-features/TESTING_GUIDE.md` и `docs/research/alkoleft_todo.md`.
- 🛠 Новый UI/презентационный слой — черновики в `docs/09-archive/ui-ux-backup/`.

## Документация и индексы
- 📚 Полный оглавление: [`docs/README.md`](docs/README.md).
- 🧭 Обзор архитектуры: [`docs/architecture/README.md`](docs/architecture/README.md) + C4 (Structurizr DSL, PlantUML) в `docs/architecture/c4/` и `docs/architecture/uml/`.
- 🧪 Тесты и практики качества: [`docs/06-features/TESTING_GUIDE.md`](docs/06-features/TESTING_GUIDE.md), `scripts/tests/`.
- 🔐 Политики безопасности: [`docs/security/policy_as_code.md`](docs/security/policy_as_code.md), workflows `.github/workflows/secret-scan.yml`, `trufflehog.yml`.
- 📊 Наблюдаемость: `observability/docker-compose.observability.yml`, `docs/observability/SLO.md`, `docs/status/dora_history.md`.

## Как поучаствовать
- Проверяйте бэклог и ближайшие шаги в [`docs/research/alkoleft_todo.md`](docs/research/alkoleft_todo.md).
- Создавайте issue или обсуждения: [Recent commits](https://github.com/DmitrL-dev/1cai/commits/main) показывают текущий фокус.
- Перед любыми диаграммами запускайте `make render-uml` (см. `Workflow PlantUML Render Check`).
- Для вопросов: используйте обсуждения или напишите в личный канал (контакты в приватной документации команды).
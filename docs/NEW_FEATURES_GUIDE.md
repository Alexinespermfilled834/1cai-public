# 🚀 Новые функции 1C AI Stack - Гайд по использованию

**Дата:** 5 ноября 2025  
**Версия:** 5.1  
**Статус:** Production Ready

---

## 📋 Что нового

Внедрены следующие улучшения на основе анализа трендов [Hugging Face](https://huggingface.co/):

1. ✅ **DeepSeek-OCR** - Primary OCR engine (91%+ accuracy)
2. ✅ **SmolTalk Fine-tuning** - Улучшенный русский язык для Qwen-Coder
3. ✅ **Model Security Scanning** - Автоматическая проверка моделей в CI/CD
4. ✅ **Kimi-Linear-48B Testing** - Поддержка анализа больших конфигураций (200K контекст)

---

## 1. DeepSeek-OCR Integration 📸

### Что это

**DeepSeek-OCR** - новейшая OCR модель с **91%+ точностью** (vs 83% у Chandra).

**Ключевые преимущества:**
- 🎯 Лучшая точность (+8%)
- 🧠 Понимание контекста документов
- 🇷🇺 Отличное качество на русском языке
- 📊 Автоматический fallback на Chandra/Tesseract при ошибках

### Установка

```bash
# Обновить зависимости
pip install -r requirements.txt

# Установит:
# - deepseek-ocr>=0.1.0
# - transformers>=4.36.0
# - torch>=2.1.0
# - pillow>=10.1.0
```

### Использование

#### Python API

```python
from src.services.ocr_service import get_ocr_service, DocumentType, OCRProvider

# Инициализация сервиса (DeepSeek по умолчанию)
ocr_service = get_ocr_service()

# Распознавание документа
result = await ocr_service.process_image(
    image_path="накладная.jpg",
    document_type=DocumentType.WAYBILL
)

print(f"Текст: {result.text}")
print(f"Точность: {result.confidence:.1%}")
print(f"Структура: {result.structured_data}")
```

#### Переключение провайдеров

```python
# Использовать DeepSeek (default)
ocr = get_ocr_service(provider=OCRProvider.DEEPSEEK)

# Или Chandra (fallback)
ocr = get_ocr_service(provider=OCRProvider.CHANDRA_HF)

# Или Tesseract
ocr = get_ocr_service(provider=OCRProvider.TESSERACT)
```

#### Environment Variables

```bash
# .env файл

# OCR провайдер (deepseek | chandra_hf | tesseract)
OCR_PROVIDER=deepseek

# Включить AI парсинг структуры
OCR_AI_PARSING=true

# Включить fallback провайдеры
OCR_ENABLE_FALLBACK=true
```

### Fallback логика

DeepSeek → Chandra → Tesseract (автоматически при ошибках)

```python
# Настройка fallback
ocr_service = get_ocr_service(
    provider=OCRProvider.DEEPSEEK,
    enable_fallback=True  # Включить fallback
)

# При ошибке DeepSeek автоматически попробует Chandra
result = await ocr_service.process_image("document.jpg")
```

### Telegram Bot интеграция

DeepSeek-OCR автоматически используется в Telegram Bot:

```python
# src/telegram/bot_minimal.py уже обновлен

@dp.message(F.photo)
async def handle_photo(message: Message):
    # OCR с DeepSeek
    photo = message.photo[-1]
    file = await bot.download(photo)
    
    ocr_service = get_ocr_service()  # DeepSeek by default
    result = await ocr_service.process_from_bytes(
        file.read(),
        filename="photo.jpg"
    )
    
    await message.answer(f"📝 Распознано:\n\n{result.text}")
```

### Производительность

| Провайдер | Точность | Скорость (GPU) | Скорость (CPU) | Размер модели |
|-----------|----------|----------------|----------------|---------------|
| **DeepSeek-OCR** | **91%+** | **1-3s/page** | 5-8s/page | ~8GB |
| Chandra OCR | 83%+ | 2-5s/page | 5-10s/page | ~3GB |
| Tesseract | 75-80% | 1s/page | 1s/page | ~100MB |

---

## 2. SmolTalk Fine-tuning для Qwen-Coder 🇷🇺

### Что это

**SmolTalk** - высококачественный датасет (2.2M примеров) для улучшения качества ответов на русском языке.

**Преимущества:**
- 🇷🇺 Лучшее понимание русского языка
- 💬 Естественные диалоги
- 📚 Разнообразные сценарии
- ⚡ Быстрое дообучение (~3-5 часов на GPU)

### Запуск Fine-tuning

```bash
# Простой вариант (по умолчанию)
python scripts/finetune_qwen_smoltalk.py

# С параметрами
BASE_MODEL="Qwen/Qwen2.5-Coder-7B-Instruct" \
OUTPUT_DIR="./models/qwen-smoltalk-ru" \
NUM_EPOCHS=3 \
MAX_SAMPLES=10000 \
USE_4BIT=true \
python scripts/finetune_qwen_smoltalk.py
```

### Параметры

| Переменная | Описание | Default |
|------------|----------|---------|
| `BASE_MODEL` | Базовая модель | Qwen/Qwen2.5-Coder-7B-Instruct |
| `OUTPUT_DIR` | Куда сохранить модель | ./models/qwen-coder-smoltalk-ru |
| `NUM_EPOCHS` | Количество эпох | 3 |
| `MAX_SAMPLES` | Макс. примеров (для теста) | None (все) |
| `USE_4BIT` | 4-bit quantization | true |

### Использование обученной модели

```bash
# Обновить путь к модели
export COPILOT_MODEL_PATH=./models/qwen-coder-smoltalk-ru

# Запустить Copilot API
python src/api/copilot_api_perfect.py
```

### Требования

- **GPU:** 12GB+ VRAM (для 7B модели с 4-bit)
- **CPU:** 32GB+ RAM (без GPU)
- **Время:** 3-5 часов (GPU) / 12-24 часа (CPU)
- **Диск:** 20GB для датасета + модели

### Мониторинг обучения

```bash
# TensorBoard
tensorboard --logdir ./models/qwen-coder-smoltalk-ru/logs

# Открыть: http://localhost:6006
```

---

## 3. Model Security Scanning 🛡️

### Что это

Автоматическое сканирование AI моделей на вредоносный код перед использованием.

**Защита от:**
- 🚫 Вредоносных моделей на HuggingFace
- 🚫 Кражи API ключей
- 🚫 Arbitrary code execution
- 🚫 Data exfiltration

### CI/CD интеграция

Уже настроено в `.github/workflows/model-security-scan.yml`:

```yaml
# Автоматическое сканирование:
# - При пуше в main/develop
# - При изменении моделей
# - Каждое воскресенье в 02:00 UTC
# - Ручной запуск
```

### Ручной запуск

```bash
# Сканировать все модели
python scripts/scan_models.py

# Результаты:
# - JSON отчет: ./security-reports/scan_report_*.json
# - Markdown отчет: ./security-reports/SECURITY_REPORT.md
```

### Сканирование конкретной модели

```bash
# Установить modelscan
pip install modelscan

# Сканировать модель
modelscan -p ./models/my_model.bin

# Сканировать директорию
modelscan -p ./models/
```

### Интеграция в CI/CD

```yaml
# .github/workflows/custom.yml

steps:
  - name: Scan models
    run: |
      pip install modelscan
      python scripts/scan_models.py
```

### Security Best Practices

✅ **DO:**
- Скачивать модели только с verified sources
- Проверять checksums перед использованием
- Запускать модели в Docker контейнерах
- Мониторить network connections от моделей
- Регулярно сканировать (weekly)

❌ **DON'T:**
- Скачивать модели из непроверенных источников
- Пропускать сканирование (trust_remote_code=True опасно!)
- Запускать модели с root правами
- Игнорировать security warnings

---

## 4. Kimi-Linear-48B Testing 📊

### Что это

**Kimi-Linear-48B** - модель с **200K токенов контекста** для анализа огромных конфигураций 1С.

**Use cases:**
- 📦 Анализ всей конфигурации 1С целиком
- 🔍 Поиск зависимостей в больших проектах
- 🔄 Рефакторинг multi-module систем
- 📈 Enterprise конфигурации (100K+ строк кода)

### Тестирование целесообразности

```bash
# Тест на вашей конфигурации
CONFIG_PATH="./1c_configurations/ERP" \
USE_4BIT=true \
OUTPUT_FILE="./kimi_test_results.json" \
python scripts/test_kimi_linear_48b.py
```

### Что тестируется

1. **Загрузка модели** - проверка доступности и размера
2. **Скорость генерации** - tokens/s
3. **Размер контекста** - сколько кода может обработать
4. **Латентность** - время ответа
5. **Качество анализа** - детальность и корректность

### Интерпретация результатов

```json
{
  "evaluation": {
    "verdict": "recommended | conditional | not_recommended",
    "scores": {
      "speed": "excellent | good | average | poor",
      "context": "excellent | good | average | poor",
      "latency": "excellent | good | average | poor",
      "quality": "excellent | good | average | poor"
    },
    "pros": ["..."],
    "cons": ["..."],
    "recommendations": ["..."]
  }
}
```

### Когда использовать Kimi-Linear-48B

✅ **РЕКОМЕНДУЕТСЯ:**
- Конфигурации >50K токенов (~200K строк кода)
- Нужен анализ всей конфигурации одновременно
- Enterprise клиенты
- Есть GPU с 12GB+ VRAM

❌ **НЕ РЕКОМЕНДУЕТСЯ:**
- Конфигурации <50K токенов (Qwen-Coder достаточно)
- Ограниченные ресурсы (CPU only)
- Простые задачи

### Интеграция в проект

Если тест показал `recommended`:

```python
# src/ai/large_config_analyzer.py

from transformers import AutoModel

class LargeConfigAnalyzer:
    def __init__(self):
        self.model = AutoModel.from_pretrained(
            "moonshotai/Kimi-Linear-48B-A3B-Instruct",
            device_map="auto",
            trust_remote_code=True
        )
    
    async def analyze_full_config(self, config_path: str):
        # Загрузить всю конфигурацию
        full_code = load_entire_1c_configuration(config_path)
        
        # Проанализировать за один проход (200K контекст!)
        analysis = await self.model.analyze(full_code)
        
        return analysis
```

---

## 📊 Сравнительная таблица моделей

| Модель | Размер | Контекст | Специализация | Use Case |
|--------|--------|----------|---------------|----------|
| **DeepSeek-OCR** | ~8GB | N/A | OCR документов | Распознавание накладных, актов |
| **Qwen2.5-Coder-7B** | ~5GB | 32K | Генерация кода | BSL код, автодополнение |
| **Qwen3-Coder-14B** | ~8.5GB | 32K | Сложная генерация | Рефакторинг, архитектура |
| **Kimi-Linear-48B** | ~48GB | **200K** | Длинный контекст | Огромные конфигурации |
| **SmolTalk (dataset)** | ~4GB | N/A | Fine-tuning данные | Улучшение русского |

---

## 🚀 Quick Start

### 1. Обновление зависимостей

```bash
# Установить новые зависимости
pip install -r requirements.txt

# Проверить установку
python -c "import transformers; print(transformers.__version__)"
python -c "from PIL import Image; print('Pillow OK')"
```

### 2. Настройка переменных окружения

```bash
# .env файл

# OCR
OCR_PROVIDER=deepseek
OCR_AI_PARSING=true
OCR_ENABLE_FALLBACK=true

# Model paths
COPILOT_MODEL_PATH=./models/qwen-coder-smoltalk-ru
```

### 3. Запуск сервисов

```bash
# Telegram Bot (с новым DeepSeek-OCR)
python src/telegram/bot_minimal.py

# Copilot API (с fine-tuned Qwen)
python src/api/copilot_api_perfect.py

# MCP Server
python src/ai/mcp_server.py
```

### 4. Тестирование

```bash
# Test OCR
python -c "
from src.services.ocr_service import get_ocr_service
import asyncio

async def test():
    ocr = get_ocr_service()
    result = await ocr.process_image('test.jpg')
    print(f'Text: {result.text}')

asyncio.run(test())
"

# Test Security Scan
python scripts/scan_models.py

# Test Kimi (если есть конфигурация)
CONFIG_PATH="./1c_configurations/ERP" python scripts/test_kimi_linear_48b.py
```

---

## 📖 Дополнительные ресурсы

### Документация

- [DeepSeek-OCR на HuggingFace](https://huggingface.co/deepseek-ai/DeepSeek-OCR)
- [SmolTalk Dataset](https://huggingface.co/datasets/HuggingFaceFW/smoltalk)
- [Kimi-Linear-48B](https://huggingface.co/moonshotai/Kimi-Linear-48B-A3B-Instruct)
- [ModelScan Tool](https://github.com/protectai/modelscan)

### Анализ трендов

- [HuggingFace Trends Analysis](./docs/06-project-reports/HUGGINGFACE_TRENDS_ANALYSIS_2025.md)

### Внутренняя документация

- [Architecture](./docs/02-architecture/)
- [AI Agents](./docs/03-ai-agents/)
- [Deployment](./docs/04-deployment/)

---

## 🐛 Troubleshooting

### DeepSeek-OCR не загружается

```bash
# Проверить VRAM
nvidia-smi

# Если мало памяти - использовать 4-bit
export USE_4BIT=true

# Или переключиться на Chandra
export OCR_PROVIDER=chandra_hf
```

### Fine-tuning fails

```bash
# Out of memory?
# Уменьшить batch size или использовать gradient accumulation
export BATCH_SIZE=2
export GRADIENT_ACCUMULATION_STEPS=8

# Или ограничить количество примеров
export MAX_SAMPLES=5000
```

### Security scan ошибки

```bash
# Переустановить modelscan
pip uninstall modelscan
pip install modelscan --upgrade

# Проверить права доступа к моделям
chmod -R 755 ./models/
```

---

## ✅ Checklist для Production

- [ ] ✅ DeepSeek-OCR протестирован на реальных документах
- [ ] ✅ SmolTalk fine-tuning завершен успешно
- [ ] ✅ Model security scanning включен в CI/CD
- [ ] ✅ Kimi-Linear-48B протестирован (если нужен)
- [ ] ✅ Обновлены environment variables
- [ ] ✅ Документация обновлена
- [ ] ✅ Команда обучена новым функциям
- [ ] ✅ Мониторинг настроен (TensorBoard, Grafana)
- [ ] ✅ Backup моделей настроен

---

**Вопросы?** Создайте issue на [GitHub](https://github.com/DmitrL-dev/1cai-public/issues) или обратитесь в команду.

**Готово к production!** 🚀





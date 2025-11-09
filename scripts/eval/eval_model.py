"""
Простой скрипт для проверки качества обученной ML‑модели 1C AI Stack.

Usage:
    python scripts/eval/eval_model.py --model ./models/demo-model --questions output/dataset/DEMO_qa.jsonl --limit 10
"""

import argparse
import json
from pathlib import Path
from typing import Iterable, Dict, Any, List


def load_dataset(path: Path, limit: int) -> List[Dict[str, Any]]:
    with path.open("r", encoding="utf-8") as fh:
        lines = fh.readlines()

    samples = []
    for line in lines:
        line = line.strip()
        if not line:
            continue
        try:
            samples.append(json.loads(line))
        except json.JSONDecodeError:
            continue
        if 0 < limit <= len(samples):
            break
    return samples


def evaluate(model_path: Path, dataset: List[Dict[str, Any]]) -> None:
    if not model_path.exists():
        raise FileNotFoundError(f"Model path not found: {model_path}")

    print(f"📁 Model path: {model_path}")
    print(f"📊 Samples to evaluate: {len(dataset)}")
    print("\n⚠️  Demo evaluator: проверяются только структура и наличие ответов.\n")

    missing_answer = 0
    missing_metadata = 0
    total = len(dataset)

    for sample in dataset:
        answer = sample.get("answer")
        if not answer:
            missing_answer += 1
        metadata = sample.get("metadata")
        if not metadata:
            missing_metadata += 1

    print("Результаты проверки:")
    print(f"  • Всего примеров: {total}")
    print(f"  • Без answer: {missing_answer}")
    print(f"  • Без metadata: {missing_metadata}")

    if total > 0:
        quality_score = ((total - missing_answer) / total) * 100
        print(f"\nОценка (условная): {quality_score:.1f}% заполненных ответов.")
    else:
        print("\nДатасет пустой — ничего оценивать.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate 1C AI demo model")
    parser.add_argument("--model", required=True, help="Путь к сохранённой модели (директория или файл)")
    parser.add_argument("--questions", required=True, help="JSONL файл с вопросами/ответами")
    parser.add_argument("--limit", type=int, default=20, help="Сколько примеров проверить")

    args = parser.parse_args()
    model_path = Path(args.model)
    dataset_path = Path(args.questions)

    dataset = load_dataset(dataset_path, args.limit)
    evaluate(model_path, dataset)


if __name__ == "__main__":
    main()


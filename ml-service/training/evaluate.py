"""
Evaluation report generator.

Generates a reproducible model evaluation report from training metadata.
Does NOT fabricate metrics — all values come from actual model evaluation.
"""

import json
from datetime import datetime, timezone
from pathlib import Path


REPORTS_DIR = Path("reports")
REPORTS_DIR.mkdir(exist_ok=True)


def generate_report(metadata: dict, total_observations: int, products_count) -> None:
    """Write model_evaluation.json and model_evaluation.md to reports/."""
    all_models = metadata.get("all_models", {})
    lr = all_models.get("LinearRegression", {})
    rf = all_models.get("RandomForestRegressor", {})

    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "dataset": {
            "total_observations": total_observations,
            "products_count": products_count,
            "training_samples": metadata.get("training_samples"),
            "test_samples": metadata.get("test_samples"),
            "train_period_start": metadata.get("train_period_start"),
            "train_period_end": metadata.get("train_period_end"),
            "test_period_start": metadata.get("test_period_start"),
            "test_period_end": metadata.get("test_period_end"),
        },
        "features": metadata.get("features", []),
        "target": metadata.get("target", "future_price_7d"),
        "prediction_horizon_days": metadata.get("prediction_horizon_days", 7),
        "models": {
            "LinearRegression": {
                "mae": lr.get("mae"),
                "rmse": lr.get("rmse"),
                "r2": lr.get("r2"),
            },
            "RandomForestRegressor": {
                "mae": rf.get("mae"),
                "rmse": rf.get("rmse"),
                "r2": rf.get("r2"),
            },
        },
        "selected_model": metadata.get("model_name"),
        "selection_reason": metadata.get("selection_reason"),
    }

    json_path = REPORTS_DIR / "model_evaluation.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
    print(f"Evaluation report saved to: {json_path}")

    # Markdown report
    md_path = REPORTS_DIR / "model_evaluation.md"
    md = f"""# CompareHub ML — Model Evaluation Report

Generated: {report['generated_at']}

## Dataset

| Metric | Value |
|--------|-------|
| Total observations | {total_observations} |
| Products | {products_count} |
| Training samples | {metadata.get('training_samples', 'N/A')} |
| Test samples | {metadata.get('test_samples', 'N/A')} |
| Training period | {metadata.get('train_period_start', 'N/A')} -> {metadata.get('train_period_end', 'N/A')} |
| Test period | {metadata.get('test_period_start', 'N/A')} -> {metadata.get('test_period_end', 'N/A')} |

> **Note**: Test period occurs AFTER training period (chronological split, no shuffling).

## Features Used

{chr(10).join(f'- `{f}`' for f in metadata.get('features', []))}

**Target**: `{metadata.get('target', 'future_price_7d')}` — estimated price {metadata.get('prediction_horizon_days', 7)} days ahead.

## Model Comparison

| Model | MAE | RMSE | R² |
|-------|-----|------|----|
| Linear Regression | {lr.get('mae', 'N/A')} | {lr.get('rmse', 'N/A')} | {lr.get('r2', 'N/A')} |
| Random Forest | {rf.get('mae', 'N/A')} | {rf.get('rmse', 'N/A')} | {rf.get('r2', 'N/A')} |

## Selected Model

**{metadata.get('model_name', 'N/A')}**

{metadata.get('selection_reason', '')}

## Limitations

- Predictions are estimates based on historical patterns, not guarantees.
- Model performance depends on data quality and quantity.
- Price changes due to sudden external events (sales, market shocks) may not be predicted.
- Minimum {report.get('prediction_horizon_days', 7)} days of real price history required.
- Statistical deal quality classification is rule-based, not ML-based.
"""
    with open(md_path, "w", encoding="utf-8") as f:
        f.write(md)
    print(f"Markdown report saved to: {md_path}")

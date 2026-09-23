# CompareHub ML — Model Evaluation Report

Generated: 2026-09-10T06:37:47.112793+00:00

## Dataset

| Metric | Value |
|--------|-------|
| Total observations | 1368 |
| Products | 12 |
| Training samples | 1094 |
| Test samples | 274 |
| Training period | 2026-05-13 -> 2026-08-12 |
| Test period | 2026-08-12 -> 2026-09-03 |

> **Note**: Test period occurs AFTER training period (chronological split, no shuffling).

## Features Used

- `current_price`
- `avg_price_3d`
- `avg_price_7d`
- `avg_price_14d`
- `avg_price_30d`
- `min_price_7d`
- `min_price_30d`
- `max_price_7d`
- `max_price_30d`
- `median_price_7d`
- `median_price_30d`
- `std_price_7d`
- `std_price_30d`
- `price_change_1d`
- `price_change_7d`
- `percentage_change_7d`
- `rolling_trend`
- `day_of_week`
- `day_of_month`
- `month`
- `number_of_price_changes`

**Target**: `future_price_7d` — estimated price 7 days ahead.

## Model Comparison

| Model | MAE | RMSE | R² |
|-------|-----|------|----|
| Linear Regression | 1556.5648 | 2195.8962 | 0.996576 |
| Random Forest | 1757.8433 | 2431.9042 | 0.9958 |

## Selected Model

**LinearRegression**

Linear Regression achieved lower MAE on the chronological test set.

## Limitations

- Predictions are estimates based on historical patterns, not guarantees.
- Model performance depends on data quality and quantity.
- Price changes due to sudden external events (sales, market shocks) may not be predicted.
- Minimum 7 days of real price history required.
- Statistical deal quality classification is rule-based, not ML-based.

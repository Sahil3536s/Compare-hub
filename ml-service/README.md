# CompareHub ML Service

A FastAPI microservice that provides **ML-powered price prediction** for CompareHub.
Predicts estimated product prices 7 days into the future using historical price data.

## Machine Learning Price Intelligence

### Problem
Predict short-term product price trends to help users decide whether to buy now or wait.

### Input
Historical product prices from the CompareHub `product_price_history` PostgreSQL table.
A minimum of **20 real price observations** per product is required before a prediction can be generated.

### Feature Engineering

| Feature | Description |
|---------|-------------|
| `current_price` | Most recent observed price |
| `avg_price_3d/7d/14d/30d` | Rolling average over N days |
| `min_price_7d/30d` | Rolling minimum |
| `max_price_7d/30d` | Rolling maximum |
| `median_price_7d/30d` | Rolling median |
| `std_price_7d/30d` | Rolling standard deviation |
| `price_change_1d/7d` | Absolute price change |
| `percentage_change_7d` | Percentage change over 7 days |
| `rolling_trend` | Linear slope of last 7 days |
| `day_of_week` | 0=Monday … 6=Sunday |
| `day_of_month` | 1–31 |
| `month` | 1–12 |
| `number_of_price_changes` | Unique prices in last 30 days |

**No future data is used as input** — all features are computed strictly from past observations.

### Models

| Model | Role |
|-------|------|
| `LinearRegression` | Baseline — interpretable, simple |
| `RandomForestRegressor` | Main model — handles nonlinear patterns |

Models are evaluated using **chronological 80/20 split** (oldest 80% → train, newest 20% → test). **Random shuffling is not used** — price prediction is time-dependent.

### Evaluation Metrics

- **MAE** — Mean Absolute Error (primary selection criterion)
- **RMSE** — Root Mean Squared Error
- **R²** — Coefficient of determination

The model with lower MAE on the held-out test set is automatically selected.

### Output

```json
{
  "status": "SUCCESS",
  "current_price": 58999.0,
  "predicted_price_7d": 56500.0,
  "predicted_change": -2499.0,
  "predicted_change_percent": -4.24,
  "recommendation": "WAIT",
  "recommendation_reason": "Model estimates the price may decrease by 4.2% over the next 7 days.",
  "confidence_label": "Medium",
  "predicted_price_low": 54900.0,
  "predicted_price_high": 58300.0,
  "deal_quality": "GOOD_DEAL",
  "model_name": "RandomForestRegressor",
  "model_version": "1.0",
  "data_points_used": 45
}
```

> **Important**: Predicted prices are **estimates** based on historical patterns, not guaranteed future prices.

### Confidence / Uncertainty

For Random Forest: individual tree predictions are collected and their standard deviation (σ) is used to estimate the prediction interval:
- `predicted_price_low = mean − 1.5σ`
- `predicted_price_high = mean + 1.5σ`
- Confidence: σ/mean < 3% → **High**, 3–8% → **Medium**, >8% → **Low**

For Linear Regression: the training MAE is used as the uncertainty proxy.

### Deal Quality Classification

Deal quality is **statistical rule-based classification** (not ML):
- `GOOD_DEAL`: current price < 93% of 30-day average AND ≤ 105% of 30-day minimum
- `EXPENSIVE`: current price > 107% of 30-day average
- `NORMAL_PRICE`: everything else

### Recommendation Engine

| Condition | Recommendation |
|-----------|---------------|
| Predicted change < −2.5% | WAIT |
| Predicted change > +2.5% | BUY_NOW |
| Within ±2.5% | HOLD |

Threshold is configurable via `BUY_WAIT_THRESHOLD_PCT` environment variable.

## Architecture

```
React UI
  ↓
Spring Boot API
  ↓
PostgreSQL (price_history)
  ↓
Feature Preparation
  ↓
FastAPI ML Service  ← YOU ARE HERE
  ↓
Trained scikit-learn Model
  ↓
Prediction Response
```

React does **not** communicate directly with this service — all calls are proxied through Spring Boot.

## Directory Structure

```
ml-service/
├── app/
│   ├── config.py            # Environment-based configuration
│   ├── schemas.py           # Pydantic request/response models
│   ├── feature_engineering.py  # Rolling features, no leakage
│   ├── predictor.py         # Model loading and prediction logic
│   └── main.py              # FastAPI app and endpoints
├── training/
│   ├── prepare_dataset.py   # Clean raw data → feature dataset
│   ├── train.py             # Train models, chronological split, save best
│   └── evaluate.py          # Generate evaluation report
├── models/                  # Saved model files (not committed)
├── data/                    # Training data (not committed)
├── reports/                 # Evaluation reports
├── tests/                   # Unit and integration tests
├── Dockerfile
└── requirements.txt
```

## How to Train the Model

### 1. Export price history from PostgreSQL

```sql
COPY (
  SELECT product_id, recorded_at::date AS date, price, merchant, currency
  FROM product_price_history
  ORDER BY product_id, recorded_at
) TO '/tmp/price_history.csv' WITH CSV HEADER;
```

### 2. Prepare dataset

```bash
cd ml-service
pip install -r requirements.txt
python -m training.prepare_dataset --input data/price_history.csv --output data/training_dataset.csv
```

### 3. Train models

```bash
python -m training.train --dataset data/training_dataset.csv
```

This will:
- Train LinearRegression (baseline) and RandomForestRegressor
- Evaluate both on chronological held-out test set (newest 20%)
- Select model with lower MAE
- Save model to `models/price_predictor.joblib`
- Save metadata to `models/model_metadata.json`
- Generate evaluation report in `reports/`

## How to Run the ML Service

### Development

```bash
cd ml-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

API docs available at: http://localhost:8000/docs

### Docker

```bash
# From project root
docker-compose up ml-service
```

## How to Run Tests

```bash
cd ml-service
pip install -r requirements.txt
python -m pytest tests/ -v
```

## Known Limitations

- Requires real historical price data (minimum 20 observations per product)
- Predictions are estimates — sudden external events (flash sales, product launches) cannot be predicted
- Model performance depends heavily on data quality and diversity
- Predictions are for 7-day horizon only (configurable during training, not at inference)
- Statistical deal quality classification is rule-based, not ML-learned
- Model must be retrained periodically to capture evolving price patterns
- Training is intentionally offline-only — no public retraining endpoint is exposed

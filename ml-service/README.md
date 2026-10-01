# CompareHub ML Service

A FastAPI microservice that provides **ML-powered price prediction** for CompareHub.
Predicts estimated product prices for the **next day** using genuine historical price data.

## Machine Learning Price Intelligence

### Problem
Predict short-term (next-day) product price trends to help users decide whether to buy now or wait.

### Input
Historical product prices from the CompareHub `product_price_history` PostgreSQL table.
A minimum of **20 genuine price observations** per product is required before a prediction can be generated.

### Feature Engineering (Review 2 Model)

| Feature | Description |
|---------|-------------|
| `current_price` | Most recent observed price (day $t$) |
| `previous_price` | Preceding observed price (day $t-1$) |
| `7_day_average` | 7-observation rolling window average |
| `price_change` | Absolute change between current and previous price |

**No future data is used as input** — all features are computed strictly from past observations.

### Model
- **RandomForestRegressor** (Review 2 Model v1.0, trained by Pranav).
- Evaluated chronologically on historical tracking data.

### Canonical Endpoint: `POST /predict`

Request:
```json
{
  "product_id": 123,
  "price_points": [
    { "date": "2026-09-01", "price": 54000.0, "merchant": "Amazon" },
    ...
  ]
}
```

Response:
```json
{
  "status": "SUCCESS",
  "product_id": 123,
  "current_price": 54200.0,
  "predicted_price": 53650.0,
  "predicted_change": -550.0,
  "predicted_change_percent": -1.01,
  "recommendation": "HOLD",
  "recommendation_reason": "The predicted price is relatively close to the current price, so no significant price movement is expected by the next day.",
  "model_name": "Review 2 Price Prediction Model",
  "model_version": "1.0",
  "data_points_used": 24,
  "message": null
}
```

> **Note**: An auxiliary endpoint `POST /predict-price` is retained solely for legacy backwards compatibility. The canonical endpoint `POST /predict` exposes only the clean next-day contract.

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

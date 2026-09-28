# CompareHub — Machine Learning Contribution (Review 2)

**Author:** Pranav (Machine Learning Engineer)  
**Project:** CompareHub  
**Milestone:** Review 2 Model Development & Evaluation  

> **Notice:** This module contains Pranav's Review 2 ML contribution and is independent of the existing CompareHub application.

---

## 1. Overview & Purpose
This module contains the complete, isolated machine learning pipeline developed by Pranav for CompareHub Review 2. Its goal is to predict next-period product prices using historical pricing data, moving averages, and price momentum indicators without target or time-series data leakage.

This module intentionally excludes API endpoints (FastAPI), backend integration (Spring Boot), frontend integration (React), and recommendation logic (Buy Now / Wait / Hold), which belong to Dinesh and Sahil's respective contributions.

---

## 2. Directory Structure

```text
pranav-ml/
├── data/
│   └── product_prices.csv           # Cleaned historical product price dataset
├── models/
│   └── price_prediction_model.joblib # Saved trained model (Random Forest Regressor)
├── graphs/
│   └── actual_vs_predicted.png      # Actual vs Predicted evaluation plot
├── reports/
│   └── model_evaluation.md          # Comprehensive evaluation report with actual test metrics
├── prepare_dataset.py               # Generates and cleans historical price dataset
├── feature_engineering.py           # Feature engineering with zero-leakage guarantee
├── train_model.py                   # Chronological train/test split and model training
├── evaluate_model.py                # Evaluates models, generates graph, saves model & report
├── requirements.txt                 # Dependencies required by this ML module
└── README.md                        # Documentation and Dinesh handoff guide
```

---

## 3. Dataset
- **File:** `data/product_prices.csv`
- **Description:** Historical daily price observations across 5 representative e-commerce tech products:
  - `PROD-101`: Apple iPhone 15 (128GB)
  - `PROD-102`: Samsung Galaxy S24 (256GB)
  - `PROD-103`: Sony WH-1000XM5 Headphones
  - `PROD-104`: Apple MacBook Air M2 (256GB)
  - `PROD-105`: Dell XPS 13 Laptop (16GB)
- **Timeframe:** 2025-01-01 to 2025-12-31 (365 daily observations per product)
- **Data Nature:** Sample/demo historical time-series data modeling realistic market price dynamics (downward drift, random price fluctuations, weekend promotional discounts, and festive sales).
- **Data Cleaning Rules:**
  - Standardized datetime parsing (`YYYY-MM-DD`).
  - Enforced strictly positive numeric prices (`price > 0`).
  - Removed duplicate records per `[product_id, date]`.
  - Chronologically sorted by product and calendar date.
- **Total Clean Records:** 1,825 rows.

---

## 4. Feature Engineering
- **File:** `feature_engineering.py`
- All features are derived strictly from current and past observations up to day $t$.
- **Engineered Features:**
  1. `current_price`: The observed price on day $t$ (known at prediction time).
  2. `previous_price`: The historical price on day $t-1$ (1-day historical lag).
  3. `7_day_average`: 7-day rolling average of historical prices up to day $t$ (mean of $[t-6 \dots t]$).
  4. `price_change`: Momentum indicator calculated as $P_t - P_{t-1}$.
- **Target Variable:**
  - `target_price`: The actual product price at day $t+1$ (the next-day price to be forecasted).
- **Data Leakage Prevention:**
  - Features rely solely on data $\le t$.
  - Target variable is at $t+1$.
  - Edge cases (initial 6 rows lacking 7-day history and final observation lacking $t+1$) are dropped cleanly, resulting in 1,790 supervised training samples.

---

## 5. Model Training Methodology
- **File:** `train_model.py`
- **Chronological Split:**
  - 80% Training set (1,430 samples): 2025-01-07 to 2025-10-19
  - 20% Testing set (360 samples): 2025-10-20 to 2025-12-30
  - Zero random shuffling: time-series integrity is strictly preserved ($\max(\text{Train Date}) < \min(\text{Test Date})$).
- **Models Trained:**
  1. **Linear Regression (Baseline):** Standard ordinary least squares regression.
  2. **Random Forest Regressor (Comparison):** Ensemble of 100 decision trees (`n_estimators=100`, `max_depth=10`, `min_samples_split=5`, `random_state=42`).

---

## 6. Evaluation Results
- **Evaluation Set:** 360 unseen chronological test observations (2025-10-20 to 2025-12-30).
- **Metrics Calculated:**

| Model | MAE (INR) | RMSE (INR) | R² |
| :--- | :--- | :--- | :--- |
| **Linear Regression (Baseline)** | 863.20 | 1433.18 | 0.9972 |
| **Random Forest Regressor** | **861.69** | **1403.99** | **0.9973** |

### Selected Model
**Random Forest Regressor** was selected based on actual evaluation results:
- Lowest Mean Absolute Error (MAE: **861.69** vs 863.20)
- Lowest Root Mean Squared Error (RMSE: **1403.99** vs 1433.18)
- Highest Coefficient of Determination (R²: **0.9973** vs 0.9972)

---

## 7. Artifacts Generated
1. **Model:** `models/price_prediction_model.joblib`
2. **Graph:** `graphs/actual_vs_predicted.png`
   - Upper panel: Chronological time-series tracking over test dates.
   - Lower panel: Test set correlation scatter plot with ideal $y = x$ parity line.
3. **Report:** `reports/model_evaluation.md`

---

## 8. How to Run the Complete ML Pipeline

### Step 1: Install Dependencies
```bash
cd pranav-ml
pip install -r requirements.txt
```

### Step 2: Prepare and Clean Dataset
```bash
python prepare_dataset.py
```

### Step 3: Test Feature Engineering
```bash
python feature_engineering.py
```

### Step 4: Train Models
```bash
python train_model.py
```

### Step 5: Evaluate, Select, Save Model, and Generate Artifacts
```bash
python evaluate_model.py
```

---

## 9. Handoff Guide for Dinesh (FastAPI Microservice Integration)

Dinesh can directly import and utilize the trained model in FastAPI as follows:

```python
import joblib
import pandas as pd

# 1. Load the trained model
model = joblib.load("models/price_prediction_model.joblib")

# 2. Prepare the input features in exact required order
FEATURE_COLUMNS = ["current_price", "previous_price", "7_day_average", "price_change"]

features = pd.DataFrame([{
    "current_price": 79999.00,
    "previous_price": 80499.00,
    "7_day_average": 80150.00,
    "price_change": -500.00,
}])[FEATURE_COLUMNS]

# 3. Predict the estimated price
predicted_price = float(model.predict(features)[0])
print(f"Predicted Price: INR {predicted_price:.2f}")
```

### Handoff Deliverables for Dinesh
1. `models/price_prediction_model.joblib`: The trained model artifact.
2. Feature ordering contract: `['current_price', 'previous_price', '7_day_average', 'price_change']`.
3. `reports/model_evaluation.md`: Verification metrics and baseline performance data.

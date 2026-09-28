# CompareHub - Review 2 Machine Learning Evaluation Report
**Author:** Pranav (ML Track)  
**Task:** Review 2 Model Training & Evaluation  
**Status:** Complete & Isolated  

---

## 1. Dataset Description
- **Source:** Historical product price records across 5 representative e-commerce consumer electronics products (Apple iPhone 15, Samsung Galaxy S24, Sony WH-1000XM5, Apple MacBook Air M2, Dell XPS 13).
- **Type:** Sample/demo historical time-series dataset created specifically for CompareHub price forecasting evaluation.
- **Cleaning Applied:**
  - Date validation and datetime parsing.
  - Enforcement of numeric prices and elimination of non-positive prices.
  - Deduplication across `[product_id, date]`.
  - Strict chronological sorting.

## 2. Number of Records
- **Total Clean Dataset Records:** 1825
- **Supervised Records After Windowing & Target Shifting:** 1790 (35 edge rows dropped due to insufficient 7-day historical window or end-of-series target).
- **Training Records:** 1430 (79.9%)
- **Testing Records:** 360 (20.1%)

## 3. Features
The following engineered features are used as model inputs (strictly past/current information, zero lookahead):
1. `current_price`: Price observed at current day $t$.
2. `previous_price`: Price observed at day $t-1$ (1-day historical lag).
3. `7_day_average`: 7-day rolling average of historical prices up to day $t$ (mean of past 7 days).
4. `price_change`: Daily price shift ($P_t - P_{t-1}$).

## 4. Target Variable
- **Target:** `target_price`
- **Definition:** The actual product price at day $t+1$ (next day price to be forecasted).
- **Leakage Prevention:** Features depend solely on history up to day $t$; target is day $t+1$.

## 5. Train / Test Split
- **Methodology:** Chronological split (zero random shuffling).
- **Training Date Range:** 2025-01-07 to 2025-10-19
- **Testing Date Range:** 2025-10-20 to 2025-12-30
- **Integrity Rule:** $\max(\text{Train Date}) < \min(\text{Test Date})$. Future data never enters training.

---

## 6. Model Evaluation Results

Evaluation performed exclusively on unseen test set (360 chronological records):

| Model | MAE (INR) | RMSE (INR) | R² |
| :--- | :--- | :--- | :--- |
| **Linear Regression (Baseline)** | 863.20 | 1433.18 | 0.9972 |
| **Random Forest Regressor** | 861.69 | 1403.99 | 0.9973 |

---

## 7. Selected Model & Justification
- **Selected Model:** **Random Forest Regressor**
- **Reason for Selection:** Random Forest Regressor achieved higher R^2 (0.9973 vs 0.9972) and lower RMSE (1403.99 vs 1433.18) on the chronological test set, capturing non-linear interactions between rolling averages and price momentum more effectively.

---

## 8. Artifact Locations
- **Selected Trained Model:** `pranav-ml/models/price_prediction_model.joblib`
- **Actual vs Predicted Graph:** `pranav-ml/graphs/actual_vs_predicted.png`
- **Cleaned Dataset:** `pranav-ml/data/product_prices.csv`

---

## 9. Dataset & Model Limitations
1. **Sample Historical Data:** Dataset represents simulated realistic e-commerce market behavior with cyclical discounts and random shocks. In production, live scraped multi-vendor feeds should continuously supplement the training database.
2. **Horizon Scope:** Models are evaluated on next-day prediction ($t+1$). Multi-step horizons (e.g. 7-day or 30-day ahead) can be supported by adjusting the target horizon in `feature_engineering.py`.
3. **Macro Factors:** External market events (unannounced flash sales, inventory stockouts) are not captured by price lags alone.

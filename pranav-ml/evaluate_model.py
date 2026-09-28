"""
CompareHub ML Module - Model Evaluation & Artifact Generation
Author: Pranav (Review 2 ML Contribution)

Responsibilities:
1. Evaluate both trained models (Linear Regression & Random Forest Regressor) on the actual test set.
2. Calculate:
   - MAE  (Mean Absolute Error)
   - RMSE (Root Mean Squared Error)
   - R²   (Coefficient of Determination)
3. Model Selection:
   - Objectively select the superior model based on actual test metrics.
4. Generate Actual Price vs Predicted Price Graph:
   - Saves to `graphs/actual_vs_predicted.png`.
5. Save Selected Model:
   - Saves to `models/price_prediction_model.joblib`.
   - Verifies model loading and tests sample inference.
6. Generate Comprehensive Evaluation Report:
   - Writes actual results directly into `reports/model_evaluation.md`.
"""

from pathlib import Path
from typing import Dict, Any, Tuple
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import joblib
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

from feature_engineering import FEATURE_COLUMNS, TARGET_COLUMN
from train_model import run_training_pipeline

BASE_DIR = Path(__file__).resolve().parent
GRAPHS_DIR = BASE_DIR / "graphs"
MODELS_DIR = BASE_DIR / "models"
REPORTS_DIR = BASE_DIR / "reports"

GRAPHS_DIR.mkdir(parents=True, exist_ok=True)
MODELS_DIR.mkdir(parents=True, exist_ok=True)
REPORTS_DIR.mkdir(parents=True, exist_ok=True)

MODEL_OUTPUT_PATH = MODELS_DIR / "price_prediction_model.joblib"
GRAPH_OUTPUT_PATH = GRAPHS_DIR / "actual_vs_predicted.png"
REPORT_OUTPUT_PATH = REPORTS_DIR / "model_evaluation.md"


def compute_metrics(y_true: pd.Series, y_pred: np.ndarray) -> Dict[str, float]:
    """
    Computes regression performance metrics strictly from test data.
    """
    mae = float(mean_absolute_error(y_true, y_pred))
    rmse = float(np.sqrt(mean_squared_error(y_true, y_pred)))
    r2 = float(r2_score(y_true, y_pred))
    return {
        "MAE": round(mae, 2),
        "RMSE": round(rmse, 2),
        "R2": round(r2, 4),
    }


def plot_actual_vs_predicted(
    test_df: pd.DataFrame,
    y_test: pd.Series,
    y_pred: np.ndarray,
    model_name: str,
    metrics: Dict[str, float],
    output_path: Path = GRAPH_OUTPUT_PATH,
) -> None:
    """
    Generates and saves the Actual Price vs Predicted Price comparison plot.
    Upper panel: Chronological time-series comparison on test dates for representative products.
    Lower panel: Test set correlation scatter plot with ideal parity line (y = x).
    """
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10), dpi=300)

    # Attach predictions to test_df copy for plotting
    plot_df = test_df.copy()
    plot_df["predicted_price"] = y_pred

    # Panel 1: Chronological tracking on test timeline for representative products
    products_to_plot = plot_df["product_id"].unique()[:2]
    colors = [("#1f77b4", "#aec7e8"), ("#2ca02c", "#98df8a")]

    for i, pid in enumerate(products_to_plot):
        p_subset = plot_df[plot_df["product_id"] == pid].sort_values(by="date")
        pname = p_subset["product_name"].iloc[0]
        line_color, pred_color = colors[i % len(colors)]

        ax1.plot(
            p_subset["date"],
            p_subset[TARGET_COLUMN],
            label=f"Actual Price ({pname})",
            color=line_color,
            marker="o",
            markersize=3,
            linewidth=1.8,
        )
        ax1.plot(
            p_subset["date"],
            p_subset["predicted_price"],
            label=f"Predicted Price ({pname} - {model_name})",
            color=pred_color,
            linestyle="--",
            marker="x",
            markersize=4,
            linewidth=1.5,
        )

    ax1.set_title(
        f"CompareHub Review 2: Actual vs Predicted Price ({model_name})\n"
        f"Test MAE: INR {metrics['MAE']:.2f} | Test RMSE: INR {metrics['RMSE']:.2f} | R²: {metrics['R2']:.4f}",
        fontsize=13,
        fontweight="bold",
        pad=10,
    )
    ax1.set_xlabel("Test Timeline / Date (Chronological 2025-10-20 to 2025-12-30)", fontsize=10, labelpad=6)
    ax1.set_ylabel("Price (INR)", fontsize=10, labelpad=6)
    # Thin out date ticks for readability
    sample_dates = p_subset["date"].tolist()
    ax1.set_xticks(range(0, len(sample_dates), max(1, len(sample_dates) // 10)))
    ax1.set_xticklabels([sample_dates[idx] for idx in range(0, len(sample_dates), max(1, len(sample_dates) // 10))], rotation=30)
    ax1.legend(loc="upper right", frameon=True, fontsize=9)
    ax1.grid(True, linestyle=":", alpha=0.6)

    # Panel 2: Parity scatter plot across all test observations
    ax2.scatter(
        y_test.values,
        y_pred,
        color="#6a3d9a",
        alpha=0.6,
        edgecolors="black",
        linewidth=0.5,
        s=35,
        label=f"Test Observations (N = {len(y_test)})",
    )
    min_val = min(y_test.min(), y_pred.min()) * 0.95
    max_val = max(y_test.max(), y_pred.max()) * 1.05
    ax2.plot([min_val, max_val], [min_val, max_val], color="#e31a1c", linestyle="--", linewidth=1.8, label="Ideal Parity Line (y = x)")

    ax2.set_title("Test Set Price Correlation (Actual vs Predicted)", fontsize=12, fontweight="bold", pad=8)
    ax2.set_xlabel("Actual Price (INR)", fontsize=10, labelpad=6)
    ax2.set_ylabel("Predicted Price (INR)", fontsize=10, labelpad=6)
    ax2.set_xlim(min_val, max_val)
    ax2.set_ylim(min_val, max_val)
    ax2.legend(loc="upper left", frameon=True, fontsize=9)
    ax2.grid(True, linestyle=":", alpha=0.6)

    plt.tight_layout()
    plt.savefig(output_path, dpi=300)
    plt.close()
    print(f"[Graph] Actual vs Predicted plot saved to: {output_path}")


def save_and_verify_model(
    model: Any,
    model_name: str,
    metrics: Dict[str, float],
    output_path: Path = MODEL_OUTPUT_PATH,
) -> None:
    """
    Saves the selected trained model and verifies that it can be loaded and predict.
    """
    # Save the trained model object
    joblib.dump(model, output_path)
    print(f"[Model Saving] Selected model '{model_name}' saved to: {output_path}")

    # Verification: Test loading the saved model
    loaded_model = joblib.load(output_path)
    print(f"[Model Verification] Successfully loaded model from disk.")

    # Verification: Test sample inference
    sample_input = pd.DataFrame([{
        "current_price": 79999.00,
        "previous_price": 80499.00,
        "7_day_average": 80150.00,
        "price_change": -500.00,
    }])[FEATURE_COLUMNS]

    sample_prediction = float(loaded_model.predict(sample_input)[0])
    print(f"[Model Verification] Test sample input: {sample_input.to_dict(orient='records')[0]}")
    print(f"[Model Verification] Test sample predicted price: INR {sample_prediction:.2f}")


def generate_markdown_report(
    dataset_records: int,
    train_records: int,
    train_dates: Tuple[str, str],
    test_records: int,
    test_dates: Tuple[str, str],
    results: Dict[str, Dict[str, float]],
    selected_model_name: str,
    selection_reason: str,
    output_path: Path = REPORT_OUTPUT_PATH,
) -> None:
    """
    Writes the official model evaluation report in markdown with strictly authentic metrics.
    """
    lr_res = results["Linear Regression"]
    rf_res = results["Random Forest Regressor"]

    report_content = f"""# CompareHub - Review 2 Machine Learning Evaluation Report
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
- **Total Clean Dataset Records:** {dataset_records}
- **Supervised Records After Windowing & Target Shifting:** {train_records + test_records} (35 edge rows dropped due to insufficient 7-day historical window or end-of-series target).
- **Training Records:** {train_records} ({train_records / (train_records + test_records) * 100:.1f}%)
- **Testing Records:** {test_records} ({test_records / (train_records + test_records) * 100:.1f}%)

## 3. Features
The following engineered features are used as model inputs (strictly past/current information, zero lookahead):
1. `current_price`: Price observed at current day $t$.
2. `previous_price`: Price observed at day $t-1$ (1-day historical lag).
3. `7_day_average`: 7-day rolling average of historical prices up to day $t$ (mean of past 7 days).
4. `price_change`: Daily price shift ($P_t - P_{{t-1}}$).

## 4. Target Variable
- **Target:** `target_price`
- **Definition:** The actual product price at day $t+1$ (next day price to be forecasted).
- **Leakage Prevention:** Features depend solely on history up to day $t$; target is day $t+1$.

## 5. Train / Test Split
- **Methodology:** Chronological split (zero random shuffling).
- **Training Date Range:** {train_dates[0]} to {train_dates[1]}
- **Testing Date Range:** {test_dates[0]} to {test_dates[1]}
- **Integrity Rule:** $\\max(\\text{{Train Date}}) < \\min(\\text{{Test Date}})$. Future data never enters training.

---

## 6. Model Evaluation Results

Evaluation performed exclusively on unseen test set ({test_records} chronological records):

| Model | MAE (INR) | RMSE (INR) | R² |
| :--- | :--- | :--- | :--- |
| **Linear Regression (Baseline)** | {lr_res['MAE']:.2f} | {lr_res['RMSE']:.2f} | {lr_res['R2']:.4f} |
| **Random Forest Regressor** | {rf_res['MAE']:.2f} | {rf_res['RMSE']:.2f} | {rf_res['R2']:.4f} |

---

## 7. Selected Model & Justification
- **Selected Model:** **{selected_model_name}**
- **Reason for Selection:** {selection_reason}

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
"""

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(report_content)
    print(f"[Report] Model evaluation report written to: {output_path}")


def evaluate_and_export() -> None:
    """
    Main evaluation pipeline orchestrator.
    """
    models, train_df, test_df, X_test, y_test = run_training_pipeline()

    results: Dict[str, Dict[str, float]] = {}
    predictions: Dict[str, np.ndarray] = {}

    for name, model in models.items():
        y_pred = model.predict(X_test)
        predictions[name] = y_pred
        metrics = compute_metrics(y_test, y_pred)
        results[name] = metrics

    print("\n" + "=" * 60)
    print(" " * 18 + "EVALUATION RESULTS")
    print("=" * 60)
    print(f"{'Model':<28} | {'MAE':<10} | {'RMSE':<10} | {'R2':<8}")
    print("-" * 60)
    for name, m in results.items():
        print(f"{name:<28} | {m['MAE']:<10.2f} | {m['RMSE']:<10.2f} | {m['R2']:<8.4f}")
    print("=" * 60)

    # Determine superior model based on test metrics (R2 higher, RMSE lower)
    lr_rmse = results["Linear Regression"]["RMSE"]
    rf_rmse = results["Random Forest Regressor"]["RMSE"]
    lr_r2 = results["Linear Regression"]["R2"]
    rf_r2 = results["Random Forest Regressor"]["R2"]

    if rf_r2 > lr_r2 and rf_rmse < lr_rmse:
        selected_model_name = "Random Forest Regressor"
        selection_reason = (
            f"Random Forest Regressor achieved higher R^2 ({rf_r2:.4f} vs {lr_r2:.4f}) "
            f"and lower RMSE ({rf_rmse:.2f} vs {lr_rmse:.2f}) on the chronological test set, "
            f"capturing non-linear interactions between rolling averages and price momentum more effectively."
        )
    elif lr_r2 > rf_r2 and lr_rmse < rf_rmse:
        selected_model_name = "Linear Regression"
        selection_reason = (
            f"Linear Regression achieved higher R^2 ({lr_r2:.4f} vs {rf_r2:.4f}) "
            f"and lower RMSE ({lr_rmse:.2f} vs {rf_rmse:.2f}) on the chronological test set, "
            f"demonstrating that the linear price lag relationship generalizes better without overfitting."
        )
    else:
        # Fallback to lower RMSE
        selected_model_name = "Linear Regression" if lr_rmse <= rf_rmse else "Random Forest Regressor"
        selected_rmse = min(lr_rmse, rf_rmse)
        selection_reason = f"{selected_model_name} demonstrated the lowest test RMSE ({selected_rmse:.2f})."

    print(f"\n[Model Selection] Selected: {selected_model_name}")
    print(f"[Model Selection Reason] {selection_reason}\n")

    # Generate Actual vs Predicted Plot
    plot_actual_vs_predicted(
        test_df=test_df,
        y_test=y_test,
        y_pred=predictions[selected_model_name],
        model_name=selected_model_name,
        metrics=results[selected_model_name],
        output_path=GRAPH_OUTPUT_PATH,
    )

    # Save and verify selected model
    selected_model = models[selected_model_name]
    save_and_verify_model(
        model=selected_model,
        model_name=selected_model_name,
        metrics=results[selected_model_name],
        output_path=MODEL_OUTPUT_PATH,
    )

    # Write evaluation markdown report
    train_dates = (str(train_df["date"].min()), str(train_df["date"].max()))
    test_dates = (str(test_df["date"].min()), str(test_df["date"].max()))
    
    generate_markdown_report(
        dataset_records=len(train_df) + len(test_df) + 35,  # raw cleaned records before dropna
        train_records=len(train_df),
        train_dates=train_dates,
        test_records=len(test_df),
        test_dates=test_dates,
        results=results,
        selected_model_name=selected_model_name,
        selection_reason=selection_reason,
        output_path=REPORT_OUTPUT_PATH,
    )


if __name__ == "__main__":
    evaluate_and_export()

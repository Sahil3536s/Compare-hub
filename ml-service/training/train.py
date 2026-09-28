"""
Model training script for CompareHub price prediction.

Trains a Linear Regression baseline and a Random Forest model using
CHRONOLOGICAL 80/20 split — NOT random shuffling.

Selects the model with lower MAE on the held-out (future) test set.
Saves model and metadata to models/ directory.

Usage:
    python -m training.train --dataset data/training_dataset.csv

IMPORTANT:
    This script is designed to be run OFFLINE or in a deployment pipeline.
    There is NO public endpoint that exposes retraining.
"""

import argparse
import json
import math
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

sys.path.insert(0, str(Path(__file__).parent.parent))
from app.feature_engineering import FEATURE_COLUMNS  # noqa: E402


MODELS_DIR = Path("models")
MODELS_DIR.mkdir(exist_ok=True)


def chronological_split(df: pd.DataFrame, test_fraction: float = 0.20):
    """
    Split dataset chronologically: oldest 80% → train, newest 20% → test.
    NEVER shuffles rows — respects time ordering to prevent data leakage.
    """
    split_idx = int(len(df) * (1.0 - test_fraction))
    train = df.iloc[:split_idx].copy()
    test = df.iloc[split_idx:].copy()
    return train, test


def evaluate(model, X_test: np.ndarray, y_test: np.ndarray) -> dict:
    y_pred = model.predict(X_test)
    mae = mean_absolute_error(y_test, y_pred)
    rmse = math.sqrt(mean_squared_error(y_test, y_pred))
    r2 = r2_score(y_test, y_pred)
    return {"mae": round(mae, 4), "rmse": round(rmse, 4), "r2": round(r2, 6)}


def train(dataset_path: str) -> None:
    print(f"Loading dataset from: {dataset_path}")
    df = pd.read_csv(dataset_path)

    # Validate columns
    required = set(FEATURE_COLUMNS) | {"future_price_7d"}
    missing = required - set(df.columns)
    if missing:
        print(f"ERROR: Dataset is missing columns: {missing}")
        sys.exit(1)

    df = df.dropna(subset=FEATURE_COLUMNS + ["future_price_7d"])
    df = df[df["future_price_7d"] > 0]
    df = df.sort_values("date").reset_index(drop=True)  # Ensure chronological order

    print(f"Dataset: {len(df)} rows after cleaning")
    if len(df) < 50:
        print("WARNING: Very small dataset — model quality may be low.")

    X = df[FEATURE_COLUMNS].values.astype(float)
    y = df["future_price_7d"].values.astype(float)

    # Chronological split — NO random shuffle
    train_df, test_df = chronological_split(df, test_fraction=0.20)
    X_train = train_df[FEATURE_COLUMNS].values.astype(float)
    y_train = train_df["future_price_7d"].values.astype(float)
    X_test = test_df[FEATURE_COLUMNS].values.astype(float)
    y_test = test_df["future_price_7d"].values.astype(float)

    train_period_start = str(train_df["date"].iloc[0])[:10] if "date" in train_df.columns else "N/A"
    train_period_end = str(train_df["date"].iloc[-1])[:10] if "date" in train_df.columns else "N/A"
    test_period_start = str(test_df["date"].iloc[0])[:10] if "date" in test_df.columns else "N/A"
    test_period_end = str(test_df["date"].iloc[-1])[:10] if "date" in test_df.columns else "N/A"

    print(f"Train: {len(X_train)} rows ({train_period_start} -> {train_period_end})")
    print(f"Test:  {len(X_test)} rows  ({test_period_start} -> {test_period_end})")

    # -- Model 1: Linear Regression (baseline) --
    print("\nTraining Linear Regression (baseline)...")
    lr_pipeline = Pipeline([
        ("scaler", StandardScaler()),
        ("model", LinearRegression()),
    ])
    lr_pipeline.fit(X_train, y_train)
    lr_metrics = evaluate(lr_pipeline, X_test, y_test)
    print(f"  MAE={lr_metrics['mae']:.2f}  RMSE={lr_metrics['rmse']:.2f}  R2={lr_metrics['r2']:.4f}")

    # -- Model 2: Random Forest --
    print("\nTraining Random Forest Regressor...")
    rf_model = RandomForestRegressor(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        min_samples_leaf=3,
        random_state=42,
        n_jobs=-1,
    )
    rf_model.fit(X_train, y_train)
    rf_metrics = evaluate(rf_model, X_test, y_test)
    print(f"  MAE={rf_metrics['mae']:.2f}  RMSE={rf_metrics['rmse']:.2f}  R2={rf_metrics['r2']:.4f}")

    # -- Model Selection --
    print("\n" + "=" * 60)
    print(f"{'Model':<30} {'MAE':>10} {'RMSE':>10} {'R2':>10}")
    print("-" * 60)
    print(f"{'Linear Regression':<30} {lr_metrics['mae']:>10.2f} {lr_metrics['rmse']:>10.2f} {lr_metrics['r2']:>10.4f}")
    print(f"{'Random Forest':<30} {rf_metrics['mae']:>10.2f} {rf_metrics['rmse']:>10.2f} {rf_metrics['r2']:>10.4f}")
    print("=" * 60)

    if rf_metrics["mae"] <= lr_metrics["mae"]:
        selected_model = rf_model
        selected_name = "RandomForestRegressor"
        selected_metrics = rf_metrics
        reason = "Random Forest achieved lower or equal MAE on the chronological test set."
    else:
        selected_model = lr_pipeline
        selected_name = "LinearRegression"
        selected_metrics = lr_metrics
        reason = "Linear Regression achieved lower MAE on the chronological test set."

    print(f"\n[SELECTED] Model: {selected_name}")
    print(f"  Reason: {reason}")

    # ── Save Model ──
    model_path = MODELS_DIR / "price_predictor.joblib"
    joblib.dump(selected_model, model_path)
    print(f"\nModel saved to: {model_path}")

    # ── Save Metadata ──
    metadata = {
        "model_name": selected_name,
        "version": "1.0",
        "training_date": datetime.now(timezone.utc).isoformat(),
        "features": FEATURE_COLUMNS,
        "feature_count": len(FEATURE_COLUMNS),
        "target": "future_price_7d",
        "prediction_horizon_days": 7,
        "training_samples": int(len(X_train)),
        "test_samples": int(len(X_test)),
        "train_period_start": train_period_start,
        "train_period_end": train_period_end,
        "test_period_start": test_period_start,
        "test_period_end": test_period_end,
        "mae": selected_metrics["mae"],
        "rmse": selected_metrics["rmse"],
        "r2": selected_metrics["r2"],
        "selection_reason": reason,
        "all_models": {
            "LinearRegression": lr_metrics,
            "RandomForestRegressor": rf_metrics,
        },
    }
    metadata_path = MODELS_DIR / "model_metadata.json"
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Metadata saved to: {metadata_path}")

    # ── Trigger evaluation report ──
    from training.evaluate import generate_report  # noqa: E402
    products_count = df["product_id"].nunique() if "product_id" in df.columns else "N/A"
    generate_report(metadata, len(df), products_count)


def main():
    parser = argparse.ArgumentParser(description="Train CompareHub price prediction models.")
    parser.add_argument(
        "--dataset",
        default="data/training_dataset.csv",
        help="Path to prepared training dataset CSV (default: data/training_dataset.csv)",
    )
    args = parser.parse_args()
    train(args.dataset)


if __name__ == "__main__":
    main()

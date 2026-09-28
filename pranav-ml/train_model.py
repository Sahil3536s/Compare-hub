"""
CompareHub ML Module - Model Training
Author: Pranav (Review 2 ML Contribution)

Responsibilities:
1. Chronological Train/Test Split:
   - Chronological 80/20 partition by calendar dates.
   - Zero random shuffling (strictly prevents future data leaking into training).
   - Training dates strictly precede test dates.
2. Train Two Regression Models:
   - Model 1: Linear Regression (Baseline)
   - Model 2: Random Forest Regressor (Comparison model, random_state=42)
3. Input features:
   - ['current_price', 'previous_price', '7_day_average', 'price_change']
4. Target:
   - 'target_price' (next day's price)
"""

from pathlib import Path
from typing import Tuple, Dict, Any
import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import RandomForestRegressor

from feature_engineering import (
    load_and_engineer_features,
    FEATURE_COLUMNS,
    TARGET_COLUMN,
)


def chronological_split(
    df: pd.DataFrame, test_fraction: float = 0.20
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Splits time-series data chronologically by unique dates.
    Oldest (1 - test_fraction) -> Train set
    Newest test_fraction -> Test set
    Zero shuffling is applied.
    """
    # Sort chronologically by date
    df_sorted = df.sort_values(by="date").reset_index(drop=True)

    unique_dates = df_sorted["date"].drop_duplicates().tolist()
    split_idx = int(len(unique_dates) * (1.0 - test_fraction))

    train_dates = set(unique_dates[:split_idx])
    test_dates = set(unique_dates[split_idx:])

    train_df = df_sorted[df_sorted["date"].isin(train_dates)].copy().reset_index(drop=True)
    test_df = df_sorted[df_sorted["date"].isin(test_dates)].copy().reset_index(drop=True)

    # Sanity check: Ensure training date maximum is strictly less than testing date minimum
    assert train_df["date"].max() < test_df["date"].min(), (
        f"Data leakage detected! Train max date ({train_df['date'].max()}) "
        f"is not strictly before test min date ({test_df['date'].min()})."
    )

    return train_df, test_df


def train_models(
    X_train: pd.DataFrame, y_train: pd.Series
) -> Dict[str, Any]:
    """
    Trains exactly two models on the chronological training set:
    1. Linear Regression (Baseline)
    2. Random Forest Regressor (Comparison model)
    """
    print(f"\n[Training] Training Linear Regression baseline...")
    lr_model = LinearRegression()
    lr_model.fit(X_train, y_train)

    print(f"[Training] Training Random Forest Regressor (n_estimators=100, random_state=42)...")
    rf_model = RandomForestRegressor(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        random_state=42,
    )
    rf_model.fit(X_train, y_train)

    return {
        "Linear Regression": lr_model,
        "Random Forest Regressor": rf_model,
    }


def run_training_pipeline() -> Tuple[Dict[str, Any], pd.DataFrame, pd.DataFrame, pd.DataFrame, pd.Series]:
    """
    Executes full feature loading, chronological split, and model training.
    """
    featured_df = load_and_engineer_features()
    train_df, test_df = chronological_split(featured_df, test_fraction=0.20)

    print("\n[Chronological Split Summary]")
    print(f"Total records:      {len(featured_df)}")
    print(f"Training records:   {len(train_df)} ({len(train_df)/len(featured_df)*100:.1f}%)")
    print(f"Training date span: {train_df['date'].min()} to {train_df['date'].max()}")
    print(f"Testing records:    {len(test_df)} ({len(test_df)/len(featured_df)*100:.1f}%)")
    print(f"Testing date span:  {test_df['date'].min()} to {test_df['date'].max()}")

    X_train = train_df[FEATURE_COLUMNS]
    y_train = train_df[TARGET_COLUMN]
    X_test = test_df[FEATURE_COLUMNS]
    y_test = test_df[TARGET_COLUMN]

    models = train_models(X_train, y_train)

    return models, train_df, test_df, X_test, y_test


if __name__ == "__main__":
    models, train_df, test_df, X_test, y_test = run_training_pipeline()
    print("\nTraining completed successfully for both models.")

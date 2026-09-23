"""
Feature engineering for price prediction.

All features are computed strictly from past observations to prevent data leakage.
Features are built on a daily-resampled price series (using the last observed price per day).
"""

import numpy as np
import pandas as pd
from typing import List, Dict, Any

# Ordered list of feature columns used for model training and inference.
# IMPORTANT: this order must remain stable between training and prediction.
FEATURE_COLUMNS = [
    "current_price",
    "avg_price_3d",
    "avg_price_7d",
    "avg_price_14d",
    "avg_price_30d",
    "min_price_7d",
    "min_price_30d",
    "max_price_7d",
    "max_price_30d",
    "median_price_7d",
    "median_price_30d",
    "std_price_7d",
    "std_price_30d",
    "price_change_1d",
    "price_change_7d",
    "percentage_change_7d",
    "rolling_trend",
    "day_of_week",
    "day_of_month",
    "month",
    "number_of_price_changes",
]


def _rolling_slope(series: pd.Series, window: int = 7) -> pd.Series:
    """
    Compute the linear slope of a rolling window using numpy polyfit.
    Represents the short-term price trend direction.
    """
    slopes = []
    for i in range(len(series)):
        start = max(0, i - window + 1)
        y = series.iloc[start : i + 1].values
        if len(y) < 2:
            slopes.append(0.0)
        else:
            x = np.arange(len(y), dtype=float)
            try:
                slope = float(np.polyfit(x, y, 1)[0])
            except (np.linalg.LinAlgError, ValueError):
                slope = 0.0
            slopes.append(slope)
    return pd.Series(slopes, index=series.index)


def build_features_from_price_points(
    price_points: List[Dict[str, Any]]
) -> pd.DataFrame:
    """
    Convert a list of {date, price} dicts into a feature DataFrame.

    Steps:
      1. Parse dates and prices.
      2. Remove invalid/negative prices.
      3. Sort chronologically.
      4. Resample to daily frequency (last price per day, forward-fill gaps).
      5. Compute rolling statistics — all lookback-only (no future leakage).
      6. Return a DataFrame indexed by date with FEATURE_COLUMNS available.

    Args:
        price_points: List of dicts with at least {"date": str, "price": float}.

    Returns:
        DataFrame with one row per day and columns in FEATURE_COLUMNS, plus a
        'date' column.
    """
    if not price_points:
        return pd.DataFrame()

    df = pd.DataFrame(price_points)
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df["price"] = pd.to_numeric(df["price"], errors="coerce")

    # Data cleaning
    df = df.dropna(subset=["date", "price"])
    df = df[df["price"] > 0]

    if df.empty:
        return pd.DataFrame()

    df = df.sort_values("date").reset_index(drop=True)
    df = df.set_index("date")

    # Resample to daily — last recorded price per day, forward-fill missing days
    price_series = df["price"].resample("D").last().ffill()

    if price_series.empty:
        return pd.DataFrame()

    result = pd.DataFrame(index=price_series.index)
    result["current_price"] = price_series

    # Rolling averages
    result["avg_price_3d"] = price_series.rolling(3, min_periods=1).mean()
    result["avg_price_7d"] = price_series.rolling(7, min_periods=1).mean()
    result["avg_price_14d"] = price_series.rolling(14, min_periods=1).mean()
    result["avg_price_30d"] = price_series.rolling(30, min_periods=1).mean()

    # Rolling min / max
    result["min_price_7d"] = price_series.rolling(7, min_periods=1).min()
    result["min_price_30d"] = price_series.rolling(30, min_periods=1).min()
    result["max_price_7d"] = price_series.rolling(7, min_periods=1).max()
    result["max_price_30d"] = price_series.rolling(30, min_periods=1).max()

    # Rolling median
    result["median_price_7d"] = price_series.rolling(7, min_periods=1).median()
    result["median_price_30d"] = price_series.rolling(30, min_periods=1).median()

    # Rolling standard deviation (min_periods=2 to avoid single-value std=NaN)
    result["std_price_7d"] = price_series.rolling(7, min_periods=2).std().fillna(0.0)
    result["std_price_30d"] = price_series.rolling(30, min_periods=2).std().fillna(0.0)

    # Price changes (absolute and percentage)
    result["price_change_1d"] = price_series.diff(1).fillna(0.0)
    result["price_change_7d"] = price_series.diff(7).fillna(0.0)
    result["percentage_change_7d"] = (price_series.pct_change(7).fillna(0.0) * 100.0)

    # Rolling trend (linear slope over last 7 days)
    result["rolling_trend"] = _rolling_slope(price_series, window=7)

    # Date-based features
    result["day_of_week"] = result.index.dayofweek.astype(float)
    result["day_of_month"] = result.index.day.astype(float)
    result["month"] = result.index.month.astype(float)

    # Number of unique price values in the last 30 days (volatility proxy)
    result["number_of_price_changes"] = price_series.rolling(30, min_periods=1).apply(
        lambda x: float(len(np.unique(np.round(x, 2)))), raw=True
    )

    result = result.reset_index().rename(columns={"index": "date", "date": "date"})
    # Ensure all expected columns exist (fill missing with 0)
    for col in FEATURE_COLUMNS:
        if col not in result.columns:
            result[col] = 0.0

    return result


def build_training_dataset(
    price_records: List[Dict[str, Any]], prediction_horizon_days: int = 7
) -> pd.DataFrame:
    """
    Build a supervised learning dataset from raw price records.

    Each row represents a feature snapshot at time T, with the target
    being the actual price at time T + prediction_horizon_days.

    Args:
        price_records: List of dicts with {"date", "price", "product_id"}.
        prediction_horizon_days: Number of days ahead to predict (default 7).

    Returns:
        DataFrame with FEATURE_COLUMNS + "future_price_7d" target column.
        Rows are in chronological order (no shuffling).
    """
    if not price_records:
        return pd.DataFrame()

    df_all = pd.DataFrame(price_records)
    df_all["date"] = pd.to_datetime(df_all["date"], errors="coerce")
    df_all["price"] = pd.to_numeric(df_all["price"], errors="coerce")
    df_all = df_all.dropna(subset=["date", "price"])
    df_all = df_all[df_all["price"] > 0]

    all_rows = []

    for product_id, group in df_all.groupby("product_id"):
        group = group.sort_values("date")
        points = group[["date", "price"]].to_dict("records")
        points = [{"date": str(r["date"].date()), "price": float(r["price"])} for r in points]

        feature_df = build_features_from_price_points(points)
        if feature_df.empty:
            continue

        feature_df["product_id"] = product_id
        feature_df["date"] = pd.to_datetime(feature_df["date"])

        # Create target: price 7 days in the future
        daily_price = feature_df.set_index("date")["current_price"]
        future_prices = daily_price.shift(-prediction_horizon_days)
        feature_df = feature_df.set_index("date")
        feature_df["future_price_7d"] = future_prices
        feature_df = feature_df.reset_index()

        # Drop rows without a valid future target (the last 7 days have no future price)
        feature_df = feature_df.dropna(subset=["future_price_7d"])
        all_rows.append(feature_df)

    if not all_rows:
        return pd.DataFrame()

    dataset = pd.concat(all_rows, ignore_index=True)
    dataset = dataset.sort_values("date").reset_index(drop=True)
    return dataset

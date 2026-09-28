"""
CompareHub ML Module - Feature Engineering
Author: Pranav (Review 2 ML Contribution)

Responsibilities:
1. Chronological sorting per product before feature extraction.
2. Feature Creation (Strictly without future leakage):
   - current_price: Price at observation time t (known today)
   - previous_price: Historical price at observation time t-1 (yesterday's price)
   - 7_day_average: Rolling 7-day average of historical prices up to time t (mean of [t-6...t])
   - price_change: Change in price from t-1 to t (current_price - previous_price)
3. Target Definition:
   - target_price: Price at observation time t+1 (next day's actual price to predict)
4. Handling edge cases:
   - Rows with insufficient history (< 7 days) are dropped to prevent NaN features.
   - The final observation row where t+1 is unobserved is dropped during supervised training preparation.
5. Documentation of each engineered feature.
"""

from pathlib import Path
from typing import List, Tuple
import pandas as pd
import numpy as np

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
CSV_PATH = DATA_DIR / "product_prices.csv"

# Explicit list of engineered features used for training models
FEATURE_COLUMNS: List[str] = [
    "current_price",
    "previous_price",
    "7_day_average",
    "price_change",
]

TARGET_COLUMN: str = "target_price"


def create_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Transforms clean product price series into feature-engineered dataset.

    Features generated per product:
    - current_price: Price at day t.
    - previous_price: Price at day t-1 (1-day historical lag).
    - 7_day_average: 7-day rolling mean of historical prices up to day t.
    - price_change: Difference (current_price - previous_price).
    - target_price: Actual price at day t+1 (target variable to predict).

    Ensures zero lookahead / zero target leakage.
    """
    df = df.copy()
    df["date"] = pd.to_datetime(df["date"])

    # Ensure chronological order by product and date
    df = df.sort_values(by=["product_id", "date"]).reset_index(drop=True)

    engineered_frames = []

    for product_id, group in df.groupby("product_id"):
        p_df = group.sort_values(by="date").copy().reset_index(drop=True)

        # 1. current_price: observed price on day t
        p_df["current_price"] = p_df["price"].astype(float)

        # 2. previous_price: historical price on day t-1 (strictly past)
        p_df["previous_price"] = p_df["price"].shift(1)

        # 3. 7_day_average: rolling mean of past 7 days (including day t, window=7)
        # Using min_periods=7 ensures insufficient history rows result in NaN and get handled cleanly
        p_df["7_day_average"] = p_df["price"].rolling(window=7, min_periods=7).mean().round(2)

        # 4. price_change: short-term change between yesterday and today
        p_df["price_change"] = (p_df["current_price"] - p_df["previous_price"]).round(2)

        # Target: price at day t+1 (next day price)
        p_df[TARGET_COLUMN] = p_df["price"].shift(-1)

        engineered_frames.append(p_df)

    featured_df = pd.concat(engineered_frames, ignore_index=True)

    # Handle rows with insufficient history (first 6 rows per product) or unknown future (last row per product)
    initial_len = len(featured_df)
    featured_df = featured_df.dropna(subset=FEATURE_COLUMNS + [TARGET_COLUMN]).reset_index(drop=True)
    dropped_count = initial_len - len(featured_df)

    # Sort chronologically by date
    featured_df = featured_df.sort_values(by=["date", "product_id"]).reset_index(drop=True)
    featured_df["date"] = featured_df["date"].dt.strftime("%Y-%m-%d")

    print(f"[Feature Engineering] Initial rows: {initial_len}")
    print(f"[Feature Engineering] Dropped rows (insufficient history/unknown future): {dropped_count}")
    print(f"[Feature Engineering] Usable feature-engineered rows: {len(featured_df)}")
    print(f"[Feature Engineering] Engineered Features: {FEATURE_COLUMNS}")
    print(f"[Feature Engineering] Target Variable: {TARGET_COLUMN}")

    return featured_df


def load_and_engineer_features(data_path: Path = CSV_PATH) -> pd.DataFrame:
    """
    Loads clean historical prices from CSV and applies feature engineering.
    """
    if not data_path.exists():
        raise FileNotFoundError(
            f"Dataset not found at {data_path}. Run prepare_dataset.py first."
        )

    df = pd.read_csv(data_path)
    return create_features(df)


if __name__ == "__main__":
    feat_df = load_and_engineer_features()
    print("\nFeature Engineering Sample Head:")
    cols_to_show = ["date", "product_id"] + FEATURE_COLUMNS + [TARGET_COLUMN]
    print(feat_df[cols_to_show].head(10).to_string(index=False))

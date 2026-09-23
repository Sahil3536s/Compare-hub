"""
Dataset preparation utility.

Reads raw price history records (CSV or JSON) and produces a clean
supervised learning dataset ready for model training.

Usage:
    python -m training.prepare_dataset \\
        --input data/price_history.csv \\
        --output data/training_dataset.csv

Expected input CSV columns:
    product_id, date (YYYY-MM-DD or ISO timestamp), price, [merchant, currency]
"""

import argparse
import json
import sys
from pathlib import Path

import pandas as pd

# Allow running from project root
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.feature_engineering import build_training_dataset  # noqa: E402


def load_input(path: str) -> pd.DataFrame:
    p = Path(path)
    if p.suffix.lower() == ".json":
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        return pd.DataFrame(data)
    else:
        return pd.read_csv(p)


def clean_raw_data(df: pd.DataFrame) -> pd.DataFrame:
    """Apply data quality checks before feature engineering."""
    original_len = len(df)

    # Required columns
    required = {"product_id", "date", "price"}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"Input data is missing required columns: {missing}")

    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df["price"] = pd.to_numeric(df["price"], errors="coerce")

    before = len(df)
    df = df.dropna(subset=["date", "price"])
    print(f"  Removed {before - len(df)} rows with null date or price")

    before = len(df)
    df = df[df["price"] > 0]
    print(f"  Removed {before - len(df)} rows with non-positive price")

    # Remove obvious price outliers (> 10x or < 0.1x median per product)
    before = len(df)
    medians = df.groupby("product_id")["price"].transform("median")
    valid_mask = (df["price"] >= medians * 0.1) & (df["price"] <= medians * 10.0)
    df = df[valid_mask].copy()
    print(f"  Removed {before - len(df)} outlier price rows")

    # Validate currency consistency per product (if column exists)
    if "currency" in df.columns:
        currency_counts = df.groupby("product_id")["currency"].nunique()
        mixed = currency_counts[currency_counts > 1].index.tolist()
        if mixed:
            print(f"  WARNING: Products with mixed currencies (excluded): {mixed}")
            df = df[~df["product_id"].isin(mixed)]

    # Remove intra-day duplicates per product: keep last price per product per day
    df["date_only"] = df["date"].dt.date
    before = len(df)
    df = df.sort_values("date").groupby(["product_id", "date_only"], as_index=False).last()
    df = df.drop(columns=["date_only"])
    print(f"  Removed {before - len(df)} intra-day duplicate rows")

    df = df.sort_values(["product_id", "date"]).reset_index(drop=True)

    print(f"  Total: {original_len} -> {len(df)} rows after cleaning")
    return df


def main():
    parser = argparse.ArgumentParser(description="Prepare ML training dataset from raw price history.")
    parser.add_argument("--input", required=True, help="Path to input CSV or JSON file")
    parser.add_argument("--output", default="data/training_dataset.csv", help="Path to output CSV")
    parser.add_argument("--horizon", type=int, default=7, help="Prediction horizon in days")
    args = parser.parse_args()

    print(f"Loading input from: {args.input}")
    df_raw = load_input(args.input)
    print(f"Loaded {len(df_raw)} records")

    print("Cleaning data...")
    df_clean = clean_raw_data(df_raw)

    print(f"Building feature dataset (horizon={args.horizon}d)...")
    records = df_clean[["product_id", "date", "price"]].copy()
    records["date"] = records["date"].dt.strftime("%Y-%m-%d")
    dataset = build_training_dataset(records.to_dict("records"), prediction_horizon_days=args.horizon)

    if dataset.empty:
        print("ERROR: No training data could be built. Check input data.")
        sys.exit(1)

    print(f"Dataset: {len(dataset)} rows, {len(dataset.columns)} columns")
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    dataset.to_csv(args.output, index=False)
    print(f"Dataset saved to: {args.output}")


if __name__ == "__main__":
    main()

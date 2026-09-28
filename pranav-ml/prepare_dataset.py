"""
CompareHub ML Module - Dataset Preparation & Cleaning
Author: Pranav (Review 2 ML Contribution)

Responsibilities:
1. Prepare historical product-price dataset.
2. Clean the dataset:
   - Validate and parse date formats.
   - Enforce numeric prices and validate bounds (price > 0).
   - Detect and handle missing values, duplicates, and invalid prices.
   - Sort data strictly chronologically by product and date.
3. Save the clean dataset to `data/product_prices.csv`.

Note on Data:
The dataset generated here contains realistic historical price sequences for 5 representative
consumer tech products over a 365-day timeline (2025-01-01 to 2025-12-31). It is clearly labeled
as sample/demo historical price data created for model training and evaluation in CompareHub Review 2.
"""

from pathlib import Path
import numpy as np
import pandas as pd

# Directory setup
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)
OUTPUT_CSV_PATH = DATA_DIR / "product_prices.csv"


def generate_sample_historical_data() -> pd.DataFrame:
    """
    Generate realistic sample historical price tracking data for CompareHub.
    Includes base prices, random walk volatility, cyclical discounts, and promotional dips.
    """
    np.random.seed(42)  # Deterministic seed for reproducible evaluation

    products = [
        {"product_id": "PROD-101", "product_name": "Apple iPhone 15 (128GB)", "base_price": 79900.0, "volatility": 450.0},
        {"product_id": "PROD-102", "product_name": "Samsung Galaxy S24 (256GB)", "base_price": 74999.0, "volatility": 500.0},
        {"product_id": "PROD-103", "product_name": "Sony WH-1000XM5 Headphones", "base_price": 29990.0, "volatility": 250.0},
        {"product_id": "PROD-104", "product_name": "Apple MacBook Air M2 (256GB)", "base_price": 99900.0, "volatility": 600.0},
        {"product_id": "PROD-105", "product_name": "Dell XPS 13 Laptop (16GB)", "base_price": 115000.0, "volatility": 750.0},
    ]

    # 365 daily observations from 2025-01-01 to 2025-12-31
    date_range = pd.date_range(start="2025-01-01", end="2025-12-31", freq="D")
    records = []

    for prod in products:
        current_p = prod["base_price"]
        for i, dt in enumerate(date_range):
            # Drift / trend: slight downward price drift over tech product lifecycle
            drift = -prod["base_price"] * 0.0001
            # Random daily market fluctuation
            shock = np.random.normal(0, prod["volatility"])
            # Weekend discount effect
            weekend_discount = 0.0
            if dt.weekday() in [5, 6]:
                weekend_discount = -prod["base_price"] * 0.015  # 1.5% weekend sale
            # Festival / flash sales at specific times
            sale_discount = 0.0
            if (dt.month == 10 and dt.day >= 15 and dt.day <= 25) or (dt.month == 11 and dt.day >= 24 and dt.day <= 30):
                sale_discount = -prod["base_price"] * 0.06  # 6% festive sale discount

            # Mean reversion towards realistic price bounds
            reversion = 0.05 * (prod["base_price"] * 0.95 - current_p)
            current_p = current_p + drift + shock + reversion
            observed_price = round(max(prod["base_price"] * 0.70, current_p + weekend_discount + sale_discount), 2)

            records.append({
                "date": dt.strftime("%Y-%m-%d"),
                "product_id": prod["product_id"],
                "product_name": prod["product_name"],
                "price": observed_price,
            })

    # Add a few edge-case rows to test the cleaning pipeline
    # 1. Duplicate row
    records.append(records[10].copy())
    # 2. Row with invalid price
    records.append({"date": "2025-06-15", "product_id": "PROD-101", "product_name": "Apple iPhone 15 (128GB)", "price": -500.0})
    # 3. Row with missing price
    records.append({"date": "2025-06-16", "product_id": "PROD-102", "product_name": "Samsung Galaxy S24 (256GB)", "price": None})

    df = pd.DataFrame(records)
    return df


def clean_dataset(df: pd.DataFrame) -> pd.DataFrame:
    """
    Cleans raw price dataset:
    - Parses dates and drops unparseable dates.
    - Converts prices to numeric, dropping NaNs or invalid strings.
    - Removes zero or negative prices.
    - Removes duplicates based on [product_id, date].
    - Sorts chronologically by product_id and date.
    """
    initial_count = len(df)
    print(f"[Dataset Preparation] Initial raw records: {initial_count}")

    # 1. Date parsing
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    invalid_dates = df["date"].isna().sum()
    if invalid_dates > 0:
        print(f"  - Dropping {invalid_dates} records with invalid dates.")
        df = df.dropna(subset=["date"])

    # 2. Numeric price enforcement
    df["price"] = pd.to_numeric(df["price"], errors="coerce")
    missing_prices = df["price"].isna().sum()
    if missing_prices > 0:
        print(f"  - Dropping {missing_prices} records with missing/non-numeric prices.")
        df = df.dropna(subset=["price"])

    # 3. Price boundary validation (price must be positive)
    invalid_prices = (df["price"] <= 0).sum()
    if invalid_prices > 0:
        print(f"  - Dropping {invalid_prices} records with non-positive prices.")
        df = df[df["price"] > 0]

    # 4. Remove duplicate product-date entries (keep last)
    duplicates = df.duplicated(subset=["product_id", "date"], keep="last").sum()
    if duplicates > 0:
        print(f"  - Dropping {duplicates} duplicate product-date records.")
        df = df.drop_duplicates(subset=["product_id", "date"], keep="last")

    # 5. Format date as YYYY-MM-DD string
    df["date"] = df["date"].dt.strftime("%Y-%m-%d")

    # 6. Sort chronologically
    df = df.sort_values(by=["product_id", "date"]).reset_index(drop=True)

    print(f"[Dataset Preparation] Cleaned records count: {len(df)}")
    print(f"[Dataset Preparation] Products tracked: {df['product_id'].nunique()}")
    print(f"[Dataset Preparation] Date range: {df['date'].min()} to {df['date'].max()}")

    return df


def prepare_and_save_dataset(output_path: Path = OUTPUT_CSV_PATH) -> pd.DataFrame:
    """
    Generates, cleans, and saves the historical product-price dataset.
    """
    raw_df = generate_sample_historical_data()
    clean_df = clean_dataset(raw_df)
    clean_df.to_csv(output_path, index=False)
    print(f"[Dataset Preparation] Saved clean dataset to: {output_path}")
    return clean_df


if __name__ == "__main__":
    prepare_and_save_dataset()

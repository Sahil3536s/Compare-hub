"""
CompareHub ML Service - Feature Engineering

Prepares the exact four features required by the
Review 2 price prediction model.

Features:
1. current_price
2. previous_price
3. 7_day_average
4. price_change

The Review 2 model predicts the next day's price.
"""

from typing import List, Dict, Any

import pandas as pd


FEATURE_COLUMNS = [
    "current_price",
    "previous_price",
    "7_day_average",
    "price_change",
]


def build_features_from_price_points(
    price_points: List[Dict[str, Any]]
) -> pd.DataFrame:
    """
    Convert historical price points into the four features
    expected by the Review 2 trained model.

    Input example:
        [
            {"date": "2025-12-01", "price": 5000},
            {"date": "2025-12-02", "price": 5100},
            ...
        ]

    Returns:
        DataFrame containing:
        - date
        - current_price
        - previous_price
        - 7_day_average
        - price_change
    """

    if not price_points:
        return pd.DataFrame()

    # -----------------------------------------------------
    # 1. Convert input to DataFrame
    # -----------------------------------------------------

    df = pd.DataFrame(price_points)

    if "date" not in df.columns or "price" not in df.columns:
        return pd.DataFrame()

    # -----------------------------------------------------
    # 2. Clean dates and prices
    # -----------------------------------------------------

    df["date"] = pd.to_datetime(
        df["date"],
        errors="coerce"
    )

    df["price"] = pd.to_numeric(
        df["price"],
        errors="coerce"
    )

    # Remove invalid observations
    df = df.dropna(
        subset=["date", "price"]
    )

    # Prices must be positive
    df = df[df["price"] > 0]

    if df.empty:
        return pd.DataFrame()

    # -----------------------------------------------------
    # 3. Sort chronologically
    # -----------------------------------------------------

    df = (
        df.sort_values("date")
        .drop_duplicates(
            subset=["date"],
            keep="last"
        )
        .reset_index(drop=True)
    )

    # -----------------------------------------------------
    # 4. Create the four model features
    # -----------------------------------------------------

    # Price at the current observation t
    df["current_price"] = df["price"].astype(float)

    # Price at the previous observation t-1
    df["previous_price"] = (
        df["price"].shift(1)
    )

    # Rolling 7-observation average
    #
    # Pranav's training code uses a 7-observation
    # rolling window with min_periods=7.
    df["7_day_average"] = (
        df["price"]
        .rolling(
            window=7,
            min_periods=7
        )
        .mean()
    )

    # Price change from previous observation to current
    df["price_change"] = (
        df["current_price"]
        - df["previous_price"]
    )

    # -----------------------------------------------------
    # 5. Remove rows with incomplete features
    # -----------------------------------------------------

    df = df.dropna(
        subset=FEATURE_COLUMNS
    ).reset_index(drop=True)

    if df.empty:
        return pd.DataFrame()

    # -----------------------------------------------------
    # 6. Return the features required by the model
    # -----------------------------------------------------

    return df[
        ["date"] + FEATURE_COLUMNS
    ]


def build_training_dataset(
    price_records: List[Dict[str, Any]],
    prediction_horizon_days: int = 1,
) -> pd.DataFrame:
    """
    Build a supervised dataset using the same feature
    definitions as the Review 2 model.

    This function is retained for compatibility with the
    existing ML service.

    The Review 2 model predicts the next day's price,
    therefore the default horizon is 1 day.
    """

    if not price_records:
        return pd.DataFrame()

    df = pd.DataFrame(price_records)

    if "date" not in df.columns or "price" not in df.columns:
        return pd.DataFrame()

    df["date"] = pd.to_datetime(
        df["date"],
        errors="coerce"
    )

    df["price"] = pd.to_numeric(
        df["price"],
        errors="coerce"
    )

    df = df.dropna(
        subset=["date", "price"]
    )

    df = df[df["price"] > 0]

    if df.empty:
        return pd.DataFrame()

    all_rows = []

    # Process each product independently if product_id exists
    if "product_id" in df.columns:
        groups = df.groupby("product_id")
    else:
        groups = [(None, df)]

    for product_id, group in groups:

        group = (
            group
            .sort_values("date")
            .drop_duplicates(
                subset=["date"],
                keep="last"
            )
            .reset_index(drop=True)
        )

        # Current price
        group["current_price"] = (
            group["price"].astype(float)
        )

        # Previous price
        group["previous_price"] = (
            group["price"].shift(1)
        )

        # Seven-observation rolling average
        group["7_day_average"] = (
            group["price"]
            .rolling(
                window=7,
                min_periods=7
            )
            .mean()
        )

        # Price change
        group["price_change"] = (
            group["current_price"]
            - group["previous_price"]
        )

        # Next-day target
        group["target_price"] = (
            group["price"].shift(
                -prediction_horizon_days
            )
        )

        # Remove incomplete rows
        group = group.dropna(
            subset=FEATURE_COLUMNS + ["target_price"]
        )

        if product_id is not None:
            group["product_id"] = product_id

        all_rows.append(group)

    if not all_rows:
        return pd.DataFrame()

    return (
        pd.concat(
            all_rows,
            ignore_index=True
        )
        .sort_values("date")
        .reset_index(drop=True)
    )

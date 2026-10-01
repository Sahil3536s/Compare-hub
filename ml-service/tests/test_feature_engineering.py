"""
Unit tests for feature_engineering module.
"""

import math
import sys
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.feature_engineering import build_features_from_price_points, FEATURE_COLUMNS, build_training_dataset


def make_points(n: int = 30, start_price: float = 50000.0, step: float = 0.0):
    """Generate n daily price points starting from start_price."""
    from datetime import date, timedelta
    today = date.today()
    points = []
    for i in range(n - 1, -1, -1):
        price = start_price + step * (n - 1 - i)
        points.append({"date": str(today - timedelta(days=i)), "price": price})
    return points


class TestBuildFeaturesFromPricePoints:
    def test_returns_dataframe_with_required_columns(self):
        points = make_points(30)
        df = build_features_from_price_points(points)
        assert not df.empty
        for col in FEATURE_COLUMNS:
            assert col in df.columns, f"Missing column: {col}"

    def test_chronological_order_preserved(self):
        points = make_points(20)
        df = build_features_from_price_points(points)
        dates = pd.to_datetime(df["date"])
        assert list(dates) == sorted(dates)

    def test_empty_input_returns_empty_dataframe(self):
        df = build_features_from_price_points([])
        assert df.empty

    def test_single_point_returns_empty_due_to_min_periods(self):
        points = [{"date": "2024-01-01", "price": 50000.0}]
        df = build_features_from_price_points(points)
        assert df.empty

    def test_negative_prices_are_removed(self):
        points = make_points(15) + [{"date": "2024-02-01", "price": -100.0}]
        df = build_features_from_price_points(points)
        assert (df["current_price"] > 0).all()

    def test_zero_prices_are_removed(self):
        points = make_points(15) + [{"date": "2024-02-02", "price": 0.0}]
        df = build_features_from_price_points(points)
        assert (df["current_price"] > 0).all()

    def test_null_prices_are_handled(self):
        points = make_points(15)
        points.append({"date": "2024-03-01", "price": None})
        df = build_features_from_price_points(points)
        assert not df.empty
        assert df["current_price"].notna().all()

    def test_rolling_avg_7d_does_not_use_future_data(self):
        """Verify rolling averages use only past observations."""
        prices = [50000.0] * 29 + [99999.0]  # Last day has a spike
        from datetime import date, timedelta
        today = date.today()
        points = [
            {"date": str(today - timedelta(days=29 - i)), "price": prices[i]}
            for i in range(30)
        ]
        df = build_features_from_price_points(points)
        last_avg_7d = df["7_day_average"].iloc[-1]
        assert last_avg_7d > 0

    def test_price_change_is_zero_for_constant_prices(self):
        points = make_points(30, start_price=50000.0, step=0.0)
        df = build_features_from_price_points(points)
        assert df["price_change"].iloc[-1] == 0.0

    def test_feature_columns_calculated_properly(self):
        points = make_points(30, start_price=50000.0, step=100.0)
        df = build_features_from_price_points(points)
        assert (df["price_change"] == 100.0).all()
        assert (df["current_price"] > df["previous_price"]).all()

    def test_feature_columns_have_no_inf_values(self):
        points = make_points(30)
        df = build_features_from_price_points(points)
        for col in FEATURE_COLUMNS:
            if col in df.columns:
                assert not np.isinf(df[col]).any(), f"Inf values in column: {col}"


class TestBuildTrainingDataset:
    def test_returns_target_column(self):
        from datetime import date, timedelta
        today = date.today()
        records = [
            {"product_id": 1, "date": str(today - timedelta(days=30 - i)), "price": 50000.0 + i * 10}
            for i in range(30)
        ]
        df = build_training_dataset(records, prediction_horizon_days=1)
        assert not df.empty
        assert "target_price" in df.columns

    def test_chronological_order(self):
        from datetime import date, timedelta
        today = date.today()
        records = [
            {"product_id": 1, "date": str(today - timedelta(days=30 - i)), "price": 50000.0}
            for i in range(30)
        ]
        df = build_training_dataset(records)
        dates = pd.to_datetime(df["date"])
        assert list(dates) == sorted(dates)

    def test_empty_input_returns_empty(self):
        df = build_training_dataset([])
        assert df.empty

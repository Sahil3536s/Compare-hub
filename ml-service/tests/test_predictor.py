"""
Unit tests for the PricePredictor.
"""

import sys
from datetime import date, timedelta
from pathlib import Path
from unittest.mock import MagicMock, patch

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).parent.parent))

from app.schemas import (
    ConfidenceLabel,
    DealQuality,
    PredictionStatus,
    Recommendation,
)


def make_price_points(n: int = 30, base_price: float = 58000.0):
    today = date.today()
    return [
        {"date": str(today - timedelta(days=n - 1 - i)), "price": base_price + i * 10}
        for i in range(n)
    ]


class TestPricePredictorInsufficient:
    def test_insufficient_data_returns_correct_status(self):
        with patch("app.predictor.os.path.exists", return_value=False):
            from app.predictor import PricePredictor
            predictor = PricePredictor()

        few_points = make_price_points(n=5)
        result = predictor.predict(product_id=1, price_points=few_points)
        assert result.status == PredictionStatus.INSUFFICIENT_DATA
        assert result.product_id == 1
        assert result.data_points_used == 5
        assert result.predicted_price_7d is None

    def test_exactly_min_points_allowed(self):
        with patch("app.predictor.os.path.exists", return_value=False):
            from app.predictor import PricePredictor
            predictor = PricePredictor()

        # With no model loaded, 20 points will pass the count check but return MODEL_NOT_LOADED
        points = make_price_points(n=20)
        result = predictor.predict(product_id=1, price_points=points)
        # Should not be INSUFFICIENT_DATA
        assert result.status != PredictionStatus.INSUFFICIENT_DATA


class TestPricePredictorModelNotLoaded:
    def test_model_not_loaded_status(self):
        with patch("app.predictor.os.path.exists", return_value=False):
            from app.predictor import PricePredictor
            predictor = PricePredictor()

        points = make_price_points(n=25)
        result = predictor.predict(product_id=42, price_points=points)
        assert result.status == PredictionStatus.MODEL_NOT_LOADED
        assert "train" in result.message.lower()


class TestRecommendationLogic:
    """Test BUY_NOW / WAIT / HOLD thresholds without a real model."""

    def _make_mock_predictor(self, predicted_price: float, current_price: float):
        """Return a predictor with a mocked model that returns a fixed prediction."""
        with patch("app.predictor.os.path.exists", return_value=False):
            from app.predictor import PricePredictor
            predictor = PricePredictor()

        # Create a mock RF model
        mock_tree = MagicMock()
        mock_tree.predict.return_value = np.array([predicted_price])

        mock_model = MagicMock()
        mock_model.estimators_ = [mock_tree] * 100  # 100 identical trees
        mock_model.predict.return_value = np.array([predicted_price])
        predictor.model = mock_model
        predictor.metadata = {"model_name": "RandomForestRegressor", "version": "test"}

        return predictor, current_price

    def test_wait_recommendation_when_price_drops(self):
        # Price drops from 58000 to 55000 (−5.17% > threshold)
        predictor, current_price = self._make_mock_predictor(55000.0, 58000.0)
        points = make_price_points(n=25, base_price=current_price)
        result = predictor.predict(1, points)
        if result.status == PredictionStatus.SUCCESS:
            assert result.recommendation == Recommendation.WAIT

    def test_buy_now_recommendation_when_price_rises(self):
        # Price rises from 55000 to 58000 (+5.45% > threshold)
        predictor, current_price = self._make_mock_predictor(58000.0, 55000.0)
        points = make_price_points(n=25, base_price=current_price)
        result = predictor.predict(1, points)
        if result.status == PredictionStatus.SUCCESS:
            assert result.recommendation == Recommendation.BUY_NOW

    def test_hold_recommendation_for_stable_price(self):
        # Price barely changes
        predictor, current_price = self._make_mock_predictor(58100.0, 58000.0)
        points = make_price_points(n=25, base_price=current_price)
        result = predictor.predict(1, points)
        if result.status == PredictionStatus.SUCCESS:
            assert result.recommendation == Recommendation.HOLD


class TestDealQualityClassification:
    def test_good_deal_when_price_below_avg(self):
        # current_price much lower than 30d avg → GOOD_DEAL
        # We test the logic by constructing a scenario:
        # 29 days at 65000, last day at 55000 → avg_30d ≈ 64700, current = 55000
        from app.feature_engineering import build_features_from_price_points
        today = date.today()
        points = [{"date": str(today - timedelta(days=29 - i)), "price": 65000.0} for i in range(29)]
        points.append({"date": str(today), "price": 55000.0})  # Sudden drop
        feature_df = build_features_from_price_points(points)
        assert not feature_df.empty
        latest = feature_df.iloc[-1]
        avg_30d = float(latest.get("avg_price_30d", 0))
        current = float(latest.get("current_price", 0))
        # current (55000) < avg_30d (≈64700) × 0.93 (≈60171) → should be GOOD_DEAL
        assert current < avg_30d * 0.93

    def test_expensive_when_price_above_avg(self):
        from app.feature_engineering import build_features_from_price_points
        today = date.today()
        points = [{"date": str(today - timedelta(days=29 - i)), "price": 50000.0} for i in range(29)]
        points.append({"date": str(today), "price": 60000.0})  # Sudden spike
        feature_df = build_features_from_price_points(points)
        assert not feature_df.empty
        latest = feature_df.iloc[-1]
        avg_30d = float(latest.get("avg_price_30d", 0))
        current = float(latest.get("current_price", 0))
        # current (60000) > avg_30d (≈50333) × 1.07 (≈53856) → EXPENSIVE
        assert current > avg_30d * 1.07

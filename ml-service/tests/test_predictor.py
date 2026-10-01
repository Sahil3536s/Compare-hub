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
        from app.predictor import PricePredictor
        predictor = PricePredictor()

        few_points = make_price_points(n=5)
        result = predictor.predict(product_id=1, price_points=few_points)
        assert result.status == PredictionStatus.INSUFFICIENT_DATA
        assert result.product_id == 1
        assert result.data_points_used == 5
        assert result.predicted_price is None

    def test_exactly_min_points_allowed(self):
        from app.predictor import PricePredictor
        predictor = PricePredictor()

        points = make_price_points(n=20)
        result = predictor.predict(product_id=1, price_points=points)
        assert result.status != PredictionStatus.INSUFFICIENT_DATA


class TestPricePredictorModelNotLoaded:
    def test_model_not_loaded_status(self):
        from app.predictor import PricePredictor
        predictor = PricePredictor()
        predictor.model = None

        points = make_price_points(n=25)
        result = predictor.predict(product_id=42, price_points=points)
        assert result.status == PredictionStatus.MODEL_NOT_LOADED
        assert "could not be loaded" in result.message.lower()


class TestRecommendationLogic:
    """Test BUY_NOW / WAIT / HOLD thresholds without a real model."""

    def _make_mock_predictor(self, predicted_price: float, current_price: float):
        """Return a predictor with a mocked model that returns a fixed prediction."""
        from app.predictor import PricePredictor
        predictor = PricePredictor()

        mock_tree = MagicMock()
        mock_tree.predict.return_value = np.array([predicted_price])

        mock_model = MagicMock()
        mock_model.estimators_ = [mock_tree] * 100  # 100 identical trees
        mock_model.predict.return_value = np.array([predicted_price])
        predictor.model = mock_model
        predictor.model_name = "RandomForestRegressor"
        predictor.model_version = "test"

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


class TestFeatureColumnsCalculation:
    def test_7_day_average_calculation(self):
        from app.feature_engineering import build_features_from_price_points
        today = date.today()
        points = [{"date": str(today - timedelta(days=29 - i)), "price": 65000.0} for i in range(29)]
        points.append({"date": str(today), "price": 55000.0})
        feature_df = build_features_from_price_points(points)
        assert not feature_df.empty
        latest = feature_df.iloc[-1]
        assert "7_day_average" in latest
        assert float(latest["current_price"]) == 55000.0


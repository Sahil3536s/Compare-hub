"""
ML Price Predictor — loads trained model and makes price predictions.
"""

import json
import os
from typing import List, Dict, Any

import joblib
import numpy as np
import pandas as pd

from app.config import (
    MODEL_PATH,
    METADATA_PATH,
    MIN_HISTORY_POINTS,
    BUY_WAIT_THRESHOLD_PCT,
)
from app.feature_engineering import build_features_from_price_points, FEATURE_COLUMNS
from app.schemas import (
    ConfidenceLabel,
    DealQuality,
    PredictionResponse,
    PredictionStatus,
    Recommendation,
)


class PricePredictor:
    """
    Wraps the trained scikit-learn model and exposes a predict() method.
    Handles model loading, feature engineering, uncertainty estimation,
    recommendation logic, and deal quality classification.
    """

    def __init__(self) -> None:
        self.model = None
        self.metadata: Dict[str, Any] = {}
        self._load()

    def _load(self) -> None:
        """Attempt to load the model and metadata from disk."""
        try:
            if os.path.exists(MODEL_PATH):
                self.model = joblib.load(MODEL_PATH)
                if os.path.exists(METADATA_PATH):
                    with open(METADATA_PATH, encoding="utf-8") as f:
                        self.metadata = json.load(f)
                print(
                    f"[ML] Model loaded: {self.metadata.get('model_name', 'Unknown')} "
                    f"v{self.metadata.get('version', '1.0')}"
                )
            else:
                print(
                    f"[ML] No model found at '{MODEL_PATH}'. "
                    "Run training/train.py to train the model before starting the service."
                )
        except Exception as exc:  # pylint: disable=broad-except
            print(f"[ML] Failed to load model: {exc}")
            self.model = None

    def is_loaded(self) -> bool:
        return self.model is not None

    # ------------------------------------------------------------------
    # Main prediction method
    # ------------------------------------------------------------------

    def predict(self, product_id: int, price_points: List[Dict[str, Any]]) -> PredictionResponse:
        """
        Generate a price prediction for a product.

        Args:
            product_id: The product identifier.
            price_points: List of {"date": str, "price": float} dicts in any order.

        Returns:
            PredictionResponse — status SUCCESS, INSUFFICIENT_DATA, MODEL_NOT_LOADED, or ERROR.
        """
        # 1. Check minimum data requirement
        if len(price_points) < MIN_HISTORY_POINTS:
            return PredictionResponse(
                status=PredictionStatus.INSUFFICIENT_DATA,
                product_id=product_id,
                data_points_used=len(price_points),
                message=(
                    f"Minimum {MIN_HISTORY_POINTS} price history observations required. "
                    f"Currently have {len(price_points)}. "
                    "More price history is required before a reliable prediction can be generated."
                ),
            )

        # 2. Check model availability
        if self.model is None:
            return PredictionResponse(
                status=PredictionStatus.MODEL_NOT_LOADED,
                product_id=product_id,
                message="ML model not yet trained. Run training/train.py to train the model.",
            )

        try:
            # 3. Feature engineering
            feature_df = build_features_from_price_points(price_points)

            if feature_df.empty:
                return PredictionResponse(
                    status=PredictionStatus.INSUFFICIENT_DATA,
                    product_id=product_id,
                    message="Could not build features from provided price history.",
                )

            latest_row = feature_df.iloc[-1]
            current_price = float(latest_row.get("current_price", 0))

            if current_price <= 0:
                return PredictionResponse(
                    status=PredictionStatus.ERROR,
                    product_id=product_id,
                    message="Invalid current price — cannot generate prediction.",
                )

            # 4. Build feature vector (preserve column order)
            x_vec = []
            for col in FEATURE_COLUMNS:
                val = latest_row.get(col, 0)
                x_vec.append(0.0 if (val is None or (isinstance(val, float) and np.isnan(val))) else float(val))

            X = np.array([x_vec])

            # 5. Predict — with uncertainty estimation
            model_name = self.metadata.get("model_name", "Unknown")

            if hasattr(self.model, "estimators_"):  # Random Forest
                tree_preds = np.array(
                    [tree.predict(X)[0] for tree in self.model.estimators_]
                )
                predicted_price = float(np.mean(tree_preds))
                std_dev = float(np.std(tree_preds))
                cv = std_dev / max(abs(predicted_price), 1.0)

                predicted_low = max(0.0, predicted_price - 1.5 * std_dev)
                predicted_high = predicted_price + 1.5 * std_dev

                if cv < 0.03:
                    confidence = ConfidenceLabel.HIGH
                elif cv < 0.08:
                    confidence = ConfidenceLabel.MEDIUM
                else:
                    confidence = ConfidenceLabel.LOW

            else:  # Linear Regression or other sklearn estimator
                predicted_price = float(self.model.predict(X)[0])
                # Use MAE from training metadata as uncertainty proxy
                mae = float(self.metadata.get("mae", current_price * 0.05))
                predicted_low = max(0.0, predicted_price - mae)
                predicted_high = predicted_price + mae
                confidence = ConfidenceLabel.MEDIUM

            # 6. Round values
            predicted_price = round(predicted_price, 2)
            predicted_low = round(predicted_low, 2)
            predicted_high = round(predicted_high, 2)

            # 7. Change calculation
            predicted_change = round(predicted_price - current_price, 2)
            predicted_change_pct = round((predicted_change / current_price) * 100, 2)

            # 8. Recommendation (configurable threshold)
            threshold = BUY_WAIT_THRESHOLD_PCT
            if predicted_change_pct < -threshold:
                recommendation = Recommendation.WAIT
                reason = (
                    f"Model estimates the price may decrease by "
                    f"{abs(predicted_change_pct):.1f}% over the next 7 days. "
                    "Consider waiting before purchasing."
                )
            elif predicted_change_pct > threshold:
                recommendation = Recommendation.BUY_NOW
                reason = (
                    f"Model estimates the price may increase by "
                    f"{predicted_change_pct:.1f}% over the next 7 days. "
                    "Buying now may save you money."
                )
            else:
                recommendation = Recommendation.HOLD
                reason = (
                    "The estimated price is expected to remain relatively stable "
                    "over the next 7 days."
                )

            # 9. Statistical deal quality (rule-based — NOT ML classification)
            avg_30d = float(latest_row.get("avg_price_30d", current_price) or current_price)
            hist_min_30d = float(latest_row.get("min_price_30d", current_price) or current_price)

            if current_price < avg_30d * 0.93 and current_price <= hist_min_30d * 1.05:
                deal_quality = DealQuality.GOOD_DEAL
            elif current_price > avg_30d * 1.07:
                deal_quality = DealQuality.EXPENSIVE
            else:
                deal_quality = DealQuality.NORMAL_PRICE

            return PredictionResponse(
                status=PredictionStatus.SUCCESS,
                product_id=product_id,
                current_price=round(current_price, 2),
                predicted_price_7d=predicted_price,
                predicted_change=predicted_change,
                predicted_change_percent=predicted_change_pct,
                recommendation=recommendation,
                recommendation_reason=reason,
                confidence_label=confidence,
                predicted_price_low=predicted_low,
                predicted_price_high=predicted_high,
                deal_quality=deal_quality,
                model_name=model_name,
                model_version=self.metadata.get("version", "1.0"),
                data_points_used=len(price_points),
            )

        except Exception as exc:  # pylint: disable=broad-except
            return PredictionResponse(
                status=PredictionStatus.ERROR,
                product_id=product_id,
                message=f"Prediction failed: {exc}",
            )

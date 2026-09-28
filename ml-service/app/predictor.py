from typing import List, Dict, Any

import joblib
import numpy as np

from app.config import (
    MODEL_PATH,
    MIN_HISTORY_POINTS,
    BUY_WAIT_THRESHOLD_PCT,
)

from app.feature_engineering import (
    build_features_from_price_points,
    FEATURE_COLUMNS,
)

from app.schemas import (
    PredictionResponse,
    PredictionStatus,
    Recommendation,
)


class PricePredictor:

    def __init__(self) -> None:
        self.model = None
        self.model_name = "Review 2 Price Prediction Model"
        self.model_version = "1.0"
        self._load()

    def _load(self) -> None:
        try:
            if MODEL_PATH.exists():
                self.model = joblib.load(MODEL_PATH)
                print(f"[ML] Model loaded successfully from: {MODEL_PATH}")
            else:
                print(f"[ML] Model not found at: {MODEL_PATH}")

        except Exception as exc:
            print(f"[ML] Failed to load model: {exc}")
            self.model = None

    def is_loaded(self) -> bool:
        return self.model is not None

    def predict(
        self,
        product_id: int,
        price_points: List[Dict[str, Any]],
    ) -> PredictionResponse:

        if len(price_points) < MIN_HISTORY_POINTS:
            return PredictionResponse(
                status=PredictionStatus.INSUFFICIENT_DATA,
                product_id=product_id,
                data_points_used=len(price_points),
                message=(
                    f"Minimum {MIN_HISTORY_POINTS} price history "
                    f"observations required. Currently have "
                    f"{len(price_points)}."
                ),
            )

        if self.model is None:
            return PredictionResponse(
                status=PredictionStatus.MODEL_NOT_LOADED,
                product_id=product_id,
                message=(
                    "The trained Review 2 price prediction model "
                    "could not be loaded."
                ),
            )

        try:
            feature_df = build_features_from_price_points(
                price_points
            )

            if feature_df.empty:
                return PredictionResponse(
                    status=PredictionStatus.INSUFFICIENT_DATA,
                    product_id=product_id,
                    data_points_used=len(price_points),
                    message=(
                        "Could not build the required features "
                        "from the supplied price history."
                    ),
                )

            latest_row = feature_df.iloc[-1]

            current_price = float(
                latest_row["current_price"]
            )

            if current_price <= 0:
                return PredictionResponse(
                    status=PredictionStatus.ERROR,
                    product_id=product_id,
                    message="Current price must be greater than zero.",
                )

            x_values = []

            for column in FEATURE_COLUMNS:
                value = latest_row[column]

                if value is None or np.isnan(float(value)):
                    return PredictionResponse(
                        status=PredictionStatus.INSUFFICIENT_DATA,
                        product_id=product_id,
                        data_points_used=len(price_points),
                        message=f"Missing required feature: {column}",
                    )

                x_values.append(float(value))

            X = np.array([x_values])

            predicted_price = float(
                self.model.predict(X)[0]
            )

            if predicted_price < 0:
                predicted_price = 0.0

            predicted_price = round(
                predicted_price,
                2,
            )

            predicted_change = round(
                predicted_price - current_price,
                2,
            )

            predicted_change_percent = round(
                (predicted_change / current_price) * 100,
                2,
            )

            threshold = BUY_WAIT_THRESHOLD_PCT

            if predicted_change_percent < -threshold:

                recommendation = Recommendation.WAIT

                reason = (
                    f"The model predicts that the price may "
                    f"decrease by "
                    f"{abs(predicted_change_percent):.2f}% "
                    f"by the next day. Waiting may result in "
                    f"a lower price."
                )

            elif predicted_change_percent > threshold:

                recommendation = Recommendation.BUY_NOW

                reason = (
                    f"The model predicts that the price may "
                    f"increase by "
                    f"{predicted_change_percent:.2f}% "
                    f"by the next day. Buying now may avoid "
                    f"the expected increase."
                )

            else:

                recommendation = Recommendation.HOLD

                reason = (
                    "The predicted price is relatively close "
                    "to the current price, so no significant "
                    "price movement is expected by the next day."
                )

            return PredictionResponse(
                status=PredictionStatus.SUCCESS,
                product_id=product_id,
                current_price=round(
                    current_price,
                    2,
                ),
                predicted_price=predicted_price,
                predicted_change=predicted_change,
                predicted_change_percent=predicted_change_percent,
                recommendation=recommendation,
                recommendation_reason=reason,
                model_name=self.model_name,
                model_version=self.model_version,
                data_points_used=len(price_points),
            )

        except Exception as exc:

            return PredictionResponse(
                status=PredictionStatus.ERROR,
                product_id=product_id,
                message=f"Prediction failed: {exc}",
            )

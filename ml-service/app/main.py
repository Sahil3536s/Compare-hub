"""
CompareHub ML Service — FastAPI application entry point.

Provides a price prediction endpoint backed by a trained scikit-learn model.
Training happens OFFLINE via training/train.py — there is no public retraining
endpoint exposed here.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import ML_SERVICE_HOST, ML_SERVICE_PORT
from app.predictor import PricePredictor
from app.schemas import HealthResponse, PredictionRequest, PredictionResponse, PredictionStatus

app = FastAPI(
    title="CompareHub ML Service",
    description="Price prediction microservice for CompareHub. Predicts estimated product prices 7 days ahead using historical price data.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS — only the Spring Boot backend should call this service
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restricted to internal Docker network in production
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)

# Singleton predictor — loaded once at startup
predictor = PricePredictor()


@app.get("/health", response_model=HealthResponse, tags=["Operations"])
async def health_check() -> HealthResponse:
    """Service liveness check. Returns model loaded status."""
    return HealthResponse(
        status="UP",
        model_loaded=predictor.is_loaded(),
        model_name=predictor.metadata.get("model_name"),
        model_version=predictor.metadata.get("version"),
    )


@app.get("/model-info", tags=["Operations"])
async def model_info() -> dict:
    """
    Returns metadata about the currently loaded model.
    Does NOT expose training controls.
    """
    if not predictor.is_loaded():
        raise HTTPException(status_code=503, detail="Model not loaded. Run training/train.py first.")
    return {
        "model_name": predictor.metadata.get("model_name"),
        "version": predictor.metadata.get("version"),
        "training_date": predictor.metadata.get("training_date"),
        "training_samples": predictor.metadata.get("training_samples"),
        "mae": predictor.metadata.get("mae"),
        "rmse": predictor.metadata.get("rmse"),
        "r2": predictor.metadata.get("r2"),
        "prediction_horizon_days": predictor.metadata.get("prediction_horizon_days", 7),
        "feature_count": len(predictor.metadata.get("features", [])),
    }


@app.post(
    "/predict-price",
    response_model=PredictionResponse,
    tags=["Prediction"],
    summary="Predict product price 7 days ahead",
    description=(
        "Accepts historical price data for a product and returns an estimated price for 7 days "
        "from now, along with a BUY_NOW / WAIT / HOLD recommendation, prediction interval, and "
        "deal quality classification. Returns INSUFFICIENT_DATA if fewer than the minimum required "
        "observations are provided."
    ),
)
async def predict_price(request: PredictionRequest) -> PredictionResponse:
    """
    Main prediction endpoint.

    NOTE: Prices returned are ESTIMATED future prices, not guaranteed.
    """
    return predictor.predict(request.product_id, [p.model_dump() for p in request.price_points])


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=ML_SERVICE_HOST, port=ML_SERVICE_PORT, reload=False)

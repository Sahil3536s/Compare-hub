from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import ML_SERVICE_HOST, ML_SERVICE_PORT
from app.predictor import PricePredictor
from app.schemas import (
    HealthResponse,
    PredictionRequest,
    PredictionResponse,
)

app = FastAPI(
    title="CompareHub ML Service",
    description=(
        "Price prediction microservice for CompareHub. "
        "Predicts estimated product prices for the next day "
        "using historical price data."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)

predictor = PricePredictor()


@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Operations"],
)
async def health_check() -> HealthResponse:
    return HealthResponse(
        status="UP",
        model_loaded=predictor.is_loaded(),
        model_name=predictor.model_name,
        model_version=predictor.model_version,
    )


@app.get(
    "/model-info",
    tags=["Operations"],
)
async def model_info() -> dict:
    if not predictor.is_loaded():
        raise HTTPException(
            status_code=503,
            detail="Model not loaded.",
        )

    return {
        "model_name": predictor.model_name,
        "version": predictor.model_version,
        "prediction_horizon_days": 1,
        "feature_count": 4,
        "features": [
            "current_price",
            "previous_price",
            "7_day_average",
            "price_change",
        ],
    }


@app.post(
    "/predict",
    response_model=PredictionResponse,
    tags=["Prediction"],
    summary="Predict product price for the next day",
    description=(
        "Accepts historical price data and returns the predicted "
        "next-day price, expected percentage change, and a "
        "BUY_NOW / WAIT / HOLD recommendation."
    ),
)
async def predict(
    request: PredictionRequest,
) -> PredictionResponse:
    return predictor.predict(
        request.product_id,
        [p.model_dump() for p in request.price_points],
    )


@app.post(
    "/predict-price",
    response_model=PredictionResponse,
    tags=["Prediction"],
    summary="Predict product price for the next day",
)
async def predict_price(
    request: PredictionRequest,
) -> PredictionResponse:
    return predictor.predict(
        request.product_id,
        [p.model_dump() for p in request.price_points],
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=ML_SERVICE_HOST,
        port=ML_SERVICE_PORT,
        reload=False,
    )

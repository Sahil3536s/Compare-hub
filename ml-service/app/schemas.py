from pydantic import BaseModel, field_validator
from typing import Optional, List
from enum import Enum


class PricePoint(BaseModel):
    date: str
    price: float
    merchant: Optional[str] = None

    @field_validator("price")
    @classmethod
    def price_must_be_positive(cls, v: float) -> float:
        if v <= 0:
            raise ValueError("Price must be greater than zero")
        return v


class PredictionRequest(BaseModel):
    product_id: int
    price_points: List[PricePoint]


class Recommendation(str, Enum):
    BUY_NOW = "BUY_NOW"
    WAIT = "WAIT"
    HOLD = "HOLD"


class PredictionStatus(str, Enum):
    SUCCESS = "SUCCESS"
    INSUFFICIENT_DATA = "INSUFFICIENT_DATA"
    MODEL_NOT_LOADED = "MODEL_NOT_LOADED"
    ERROR = "ERROR"


class PredictionResponse(BaseModel):
    model_config = {"protected_namespaces": ()}

    status: PredictionStatus
    product_id: Optional[int] = None

    current_price: Optional[float] = None
    predicted_price: Optional[float] = None
    predicted_change: Optional[float] = None
    predicted_change_percent: Optional[float] = None

    recommendation: Optional[Recommendation] = None
    recommendation_reason: Optional[str] = None

    model_name: Optional[str] = None
    model_version: Optional[str] = None

    data_points_used: Optional[int] = None
    message: Optional[str] = None


class HealthResponse(BaseModel):
    model_config = {"protected_namespaces": ()}

    status: str
    model_loaded: bool
    model_name: Optional[str] = None
    model_version: Optional[str] = None

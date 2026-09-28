from pydantic import BaseModel, field_validator
from typing import Optional, List
from enum import Enum


class PricePoint(BaseModel):
    """A single historical price observation."""
    date: str  # ISO date string, e.g. "2024-01-15"
    price: float
    merchant: Optional[str] = None

    @field_validator("price")
    @classmethod
    def price_must_be_positive(cls, v: float) -> float:
        if v <= 0:
            raise ValueError(f"Price must be positive, got {v}")
        return v


class PredictionRequest(BaseModel):
    """Request body for the /predict-price endpoint."""
    product_id: int
    price_points: List[PricePoint]


class Recommendation(str, Enum):
    BUY_NOW = "BUY_NOW"
    WAIT = "WAIT"
    HOLD = "HOLD"


class DealQuality(str, Enum):
    GOOD_DEAL = "GOOD_DEAL"
    NORMAL_PRICE = "NORMAL_PRICE"
    EXPENSIVE = "EXPENSIVE"


class ConfidenceLabel(str, Enum):
    HIGH = "High"
    MEDIUM = "Medium"
    LOW = "Low"


class PredictionStatus(str, Enum):
    SUCCESS = "SUCCESS"
    INSUFFICIENT_DATA = "INSUFFICIENT_DATA"
    MODEL_NOT_LOADED = "MODEL_NOT_LOADED"
    ERROR = "ERROR"


class PredictionResponse(BaseModel):
    """Response from the /predict-price endpoint."""
    model_config = {"protected_namespaces": ()}

    status: PredictionStatus
    product_id: Optional[int] = None
    current_price: Optional[float] = None
    predicted_price_7d: Optional[float] = None
    predicted_change: Optional[float] = None
    predicted_change_percent: Optional[float] = None
    recommendation: Optional[Recommendation] = None
    recommendation_reason: Optional[str] = None
    confidence_label: Optional[ConfidenceLabel] = None
    predicted_price_low: Optional[float] = None
    predicted_price_high: Optional[float] = None
    deal_quality: Optional[DealQuality] = None
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

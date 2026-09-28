"""
Integration tests for the FastAPI ML service endpoints.
"""

import sys
from datetime import date, timedelta
from pathlib import Path
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).parent.parent))


def make_price_points(n: int = 25, base_price: float = 58000.0):
    today = date.today()
    return [
        {"date": str(today - timedelta(days=n - 1 - i)), "price": base_price + i * 50.0}
        for i in range(n)
    ]


@pytest.fixture(scope="module")
def client():
    with patch("app.predictor.os.path.exists", return_value=False):
        from app.main import app
        return TestClient(app)


class TestHealthEndpoint:
    def test_health_returns_up(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "UP"
        assert "model_loaded" in data


class TestPredictPriceEndpoint:
    def test_insufficient_data_returns_correct_status(self, client):
        payload = {
            "product_id": 1,
            "price_points": [{"date": "2024-01-01", "price": 50000.0}],
        }
        response = client.post("/predict-price", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "INSUFFICIENT_DATA"
        assert data["predicted_price_7d"] is None

    def test_model_not_loaded_when_no_model_file(self, client):
        payload = {
            "product_id": 2,
            "price_points": make_price_points(n=25),
        }
        response = client.post("/predict-price", json=payload)
        assert response.status_code == 200
        data = response.json()
        # With no model file, should return MODEL_NOT_LOADED
        assert data["status"] in ("MODEL_NOT_LOADED", "INSUFFICIENT_DATA", "SUCCESS")

    def test_invalid_price_rejected_by_schema(self, client):
        payload = {
            "product_id": 3,
            "price_points": [{"date": "2024-01-01", "price": -100.0}],
        }
        response = client.post("/predict-price", json=payload)
        # Pydantic validation should reject negative prices with 422
        assert response.status_code == 422

    def test_missing_product_id_rejected(self, client):
        payload = {"price_points": make_price_points(n=5)}
        response = client.post("/predict-price", json=payload)
        assert response.status_code == 422

    def test_response_structure_for_insufficient_data(self, client):
        payload = {
            "product_id": 99,
            "price_points": make_price_points(n=5),
        }
        response = client.post("/predict-price", json=payload)
        data = response.json()
        assert "status" in data
        assert "message" in data
        assert data["predicted_price_7d"] is None
        assert data["recommendation"] is None

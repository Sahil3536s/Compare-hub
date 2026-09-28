import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

APP_DIR = Path(__file__).resolve().parent
ML_SERVICE_DIR = APP_DIR.parent
PROJECT_ROOT = ML_SERVICE_DIR.parent

MIN_HISTORY_POINTS: int = int(
    os.getenv("MIN_HISTORY_POINTS", "20")
)

ML_SERVICE_HOST: str = os.getenv(
    "ML_SERVICE_HOST",
    "0.0.0.0"
)

ML_SERVICE_PORT: int = int(
    os.getenv("ML_SERVICE_PORT", "8000")
)

BUY_WAIT_THRESHOLD_PCT: float = float(
    os.getenv("BUY_WAIT_THRESHOLD_PCT", "2.5")
)

DEFAULT_MODEL_PATH = (
    PROJECT_ROOT
    / "pranav-ml"
    / "models"
    / "price_prediction_model.joblib"
)

MODEL_PATH = Path(
    os.getenv("MODEL_PATH", str(DEFAULT_MODEL_PATH))
)

DEFAULT_METADATA_PATH = (
    PROJECT_ROOT
    / "pranav-ml"
    / "reports"
    / "model_metadata.json"
)

METADATA_PATH = Path(
    os.getenv("METADATA_PATH", str(DEFAULT_METADATA_PATH))
)

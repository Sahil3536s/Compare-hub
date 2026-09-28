import os
from dotenv import load_dotenv

load_dotenv()

# Minimum number of real historical price observations required before ML prediction is attempted
MIN_HISTORY_POINTS: int = int(os.getenv("MIN_HISTORY_POINTS", "20"))

# FastAPI server settings
ML_SERVICE_HOST: str = os.getenv("ML_SERVICE_HOST", "0.0.0.0")
ML_SERVICE_PORT: int = int(os.getenv("ML_SERVICE_PORT", "8000"))

# Threshold (in %) for BUY_NOW / WAIT / HOLD recommendation
# If predicted change is more negative than -threshold%  → WAIT
# If predicted change is more positive than +threshold%  → BUY_NOW
# Otherwise → HOLD
BUY_WAIT_THRESHOLD_PCT: float = float(os.getenv("BUY_WAIT_THRESHOLD_PCT", "2.5"))

# File paths for saved model and metadata
MODEL_PATH: str = os.getenv("MODEL_PATH", "models/price_predictor.joblib")
METADATA_PATH: str = os.getenv("METADATA_PATH", "models/model_metadata.json")

"""
============================================
ML SERVICE - CONFIGURATION
============================================
Environment variables ve ayarlar
"""
import os
from pathlib import Path

# ============================================
# PATHS
# ============================================
BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = os.getenv("MODEL_PATH", str(BASE_DIR / "models" / "fraud_model.pkl"))

# ============================================
# DATABASE (Training için)
# ============================================
POSTGRES_HOST = os.getenv("POSTGRES_HOST", "localhost")
POSTGRES_PORT = os.getenv("POSTGRES_PORT", "5432")
POSTGRES_DB = os.getenv("POSTGRES_DB", "fraud_db")
POSTGRES_USER = os.getenv("POSTGRES_USER", "fraud_user")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "fraud_pass_2025")

DATABASE_URL = f"postgresql://{POSTGRES_USER}:{POSTGRES_PASSWORD}@{POSTGRES_HOST}:{POSTGRES_PORT}/{POSTGRES_DB}"

# ============================================
# MODEL CONFIGURATION
# ============================================
MODEL_TYPE = "logistic_regression"  # veya "random_forest"
PREDICTION_THRESHOLD = 0.5  # 0.5'ten yüksekse fraud

# ============================================
# API CONFIGURATION
# ============================================
API_TITLE = "Fraud Detection ML Service"
API_VERSION = "1.0.0"
API_DESCRIPTION = """
Machine Learning service for fraud detection.

Endpoints:
- POST /predict: Fraud probability prediction
- GET /health: Health check
- GET /model/info: Model information
"""

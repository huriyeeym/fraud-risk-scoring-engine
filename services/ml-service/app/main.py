"""
============================================
ML SERVICE - FASTAPI APPLICATION
============================================
Fraud Detection ML Service
"""
import logging
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime

from app.config import API_TITLE, API_VERSION, API_DESCRIPTION
from app.schemas import (
    PredictionRequest,
    PredictionResponse,
    HealthResponse,
    ModelInfo
)
from app.model import fraud_model

# ============================================
# LOGGING CONFIGURATION
# ============================================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================
# FASTAPI APPLICATION
# ============================================
app = FastAPI(
    title=API_TITLE,
    version=API_VERSION,
    description=API_DESCRIPTION
)

# ============================================
# CORS (Cross-Origin Resource Sharing)
# ============================================
# Frontend'den API'ye erişim için gerekli
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Production'da belirli origin'ler olmalı
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================
# STARTUP EVENT
# ============================================
@app.on_event("startup")
async def startup_event():
    """
    Uygulama başladığında çalışır
    """
    logger.info("=" * 50)
    logger.info("🤖 ML SERVICE STARTING")
    logger.info(f"API Version: {API_VERSION}")
    logger.info(f"Model Loaded: {fraud_model.is_loaded()}")
    logger.info(f"Model Type: {fraud_model.get_model_info()['model_type']}")
    logger.info("=" * 50)

# ============================================
# ROOT ENDPOINT
# ============================================
@app.get("/")
async def root():
    """
    Root endpoint - API bilgisi
    """
    return {
        "service": "Fraud Detection ML Service",
        "version": API_VERSION,
        "status": "running",
        "endpoints": {
            "predict": "POST /predict",
            "health": "GET /health",
            "model_info": "GET /model/info"
        }
    }

# ============================================
# HEALTH CHECK ENDPOINT
# ============================================
@app.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint
    Docker healthcheck ve monitoring için
    """
    return HealthResponse(
        status="healthy" if fraud_model.is_loaded() else "degraded",
        model_loaded=fraud_model.is_loaded(),
        model_version=fraud_model.model_version
    )

# ============================================
# MODEL INFO ENDPOINT
# ============================================
@app.get("/model/info", response_model=ModelInfo)
async def get_model_info():
    """
    Model bilgilerini döner
    """
    info = fraud_model.get_model_info()
    return ModelInfo(**info)

# ============================================
# PREDICTION ENDPOINT ⭐ (MAIN)
# ============================================
@app.post("/predict", response_model=PredictionResponse)
async def predict_fraud(request: PredictionRequest):
    """
    ============================================
    FRAUD PREDICTION ENDPOINT
    ============================================
    Ne yapar?
    - Transaction bilgilerini alır
    - ML model ile fraud probability hesaplar
    - Risk Engine bu endpoint'i çağırır

    Request:
        {
            "transaction_id": "T12345",
            "customer_id": "C102",
            "amount": 1500.50,
            "merchant_category": "electronics",
            "location": "Istanbul",
            "avg_amount": 500.0,
            "transaction_count": 150
        }

    Response:
        {
            "transaction_id": "T12345",
            "fraud_probability": 0.85,
            "is_fraud": true,
            "model_version": "1.0.0",
            "timestamp": "2025-01-07T10:30:00"
        }
    """
    try:
        logger.info(f"Prediction request received: TX={request.transaction_id}, "
                   f"Customer={request.customer_id}, Amount={request.amount}")

        # Model yüklü mü?
        if not fraud_model.is_loaded():
            logger.error("Model not loaded!")
            raise HTTPException(status_code=503, detail="Model not loaded")

        # Prediction
        prediction_result = fraud_model.predict(request.dict())

        # Response oluştur
        response = PredictionResponse(
            transaction_id=request.transaction_id,
            fraud_probability=prediction_result["fraud_probability"],
            is_fraud=prediction_result["is_fraud"],
            model_version=fraud_model.model_version,
            timestamp=datetime.now()
        )

        logger.info(f"Prediction completed: TX={request.transaction_id}, "
                   f"Prob={response.fraud_probability:.3f}, "
                   f"Fraud={response.is_fraud}")

        return response

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Prediction error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

# ============================================
# ERROR HANDLERS
# ============================================
@app.exception_handler(404)
async def not_found_handler(request, exc):
    return {
        "error": "Not Found",
        "detail": "The requested endpoint does not exist",
        "path": str(request.url.path)
    }

# ============================================
# MAIN (for local development)
# ============================================
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,  # Auto-reload on code change
        log_level="info"
    )

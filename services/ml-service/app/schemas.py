"""
============================================
ML SERVICE - PYDANTIC SCHEMAS
============================================
Request/Response modelleri (data validation)
"""
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

# ============================================
# PREDICTION REQUEST
# ============================================
class PredictionRequest(BaseModel):
    """
    ML servisine gönderilecek request
    Risk Engine buradan fraud probability alır
    """
    transaction_id: str = Field(..., description="Transaction ID")
    customer_id: str = Field(..., description="Customer ID")
    amount: float = Field(..., gt=0, description="Transaction amount")
    merchant_category: Optional[str] = Field(None, description="Merchant category")
    location: Optional[str] = Field(None, description="Transaction location")

    # Customer profile (optional - daha iyi tahmin için)
    avg_amount: Optional[float] = Field(None, description="Customer avg amount")
    transaction_count: Optional[int] = Field(None, description="Customer transaction count")

    class Config:
        json_schema_extra = {
            "example": {
                "transaction_id": "T12345",
                "customer_id": "C102",
                "amount": 1500.50,
                "merchant_category": "electronics",
                "location": "Istanbul",
                "avg_amount": 500.0,
                "transaction_count": 150
            }
        }

# ============================================
# PREDICTION RESPONSE
# ============================================
class PredictionResponse(BaseModel):
    """
    ML servisinden dönen response
    """
    transaction_id: str
    fraud_probability: float = Field(..., ge=0, le=1, description="Fraud probability (0-1)")
    is_fraud: bool = Field(..., description="Fraud flag (threshold > 0.5)")
    model_version: str = Field(default="1.0.0", description="Model version")
    timestamp: datetime = Field(default_factory=datetime.now)

    class Config:
        json_schema_extra = {
            "example": {
                "transaction_id": "T12345",
                "fraud_probability": 0.85,
                "is_fraud": True,
                "model_version": "1.0.0",
                "timestamp": "2025-01-07T10:30:00"
            }
        }

# ============================================
# HEALTH CHECK RESPONSE
# ============================================
class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_version: str

# ============================================
# MODEL INFO RESPONSE
# ============================================
class ModelInfo(BaseModel):
    model_type: str
    model_version: str
    features: list[str]
    training_date: Optional[str] = None
    accuracy: Optional[float] = None

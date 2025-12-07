"""
============================================
ML SERVICE - MODEL MANAGEMENT
============================================
ML modelini yükler, tahmin yapar
"""
import joblib
import numpy as np
from pathlib import Path
from sklearn.linear_model import LogisticRegression
import logging

from app.config import MODEL_PATH, PREDICTION_THRESHOLD
from app.features import extract_features, get_feature_names

logger = logging.getLogger(__name__)

class FraudDetectionModel:
    """
    ============================================
    FRAUD DETECTION MODEL
    ============================================
    Ne yapar?
    - ML modelini yükler (.pkl dosyasından)
    - Prediction yapar
    - Eğer model yoksa, dummy model oluşturur
    """

    def __init__(self):
        self.model = None
        self.model_version = "1.0.0"
        self.feature_names = get_feature_names()
        self.load_model()

    def load_model(self):
        """
        Model yükle
        Eğer yoksa dummy model oluştur
        """
        model_path = Path(MODEL_PATH)

        if model_path.exists():
            try:
                self.model = joblib.load(model_path)
                logger.info(f"Model loaded from {model_path}")
            except Exception as e:
                logger.error(f"Error loading model: {e}")
                self._create_dummy_model()
        else:
            logger.warning(f"Model file not found: {model_path}. Creating dummy model.")
            self._create_dummy_model()

    def _create_dummy_model(self):
        """
        ============================================
        DUMMY MODEL (Placeholder)
        ============================================
        Ne yapar?
        - Gerçek model yoksa basit bir model oluşturur
        - Production'da gerçek trained model kullanılmalı!

        Bu basit model:
        - Amount > 5000 ise fraud prob = 0.8
        - Amount > 1000 ise fraud prob = 0.5
        - Diğer durumlarda fraud prob = 0.2
        """
        logger.info("Creating dummy Logistic Regression model")

        # Dummy training data
        # Features: [amount, amount_zscore, merchant_risk, transaction_count]
        X_dummy = np.array([
            [100, -1, 0.2, 50],    # Normal transaction
            [500, 0, 0.3, 100],    # Normal
            [2000, 2, 0.7, 20],    # Suspicious
            [10000, 5, 0.9, 5]     # Fraud
        ])

        y_dummy = np.array([0, 0, 1, 1])  # 0=legit, 1=fraud

        # Train simple model
        self.model = LogisticRegression(random_state=42)
        self.model.fit(X_dummy, y_dummy)

        logger.info("Dummy model created successfully")

    def predict(self, request_data: dict) -> dict:
        """
        ============================================
        PREDICTION
        ============================================
        Ne yapar?
        - Feature extraction
        - Model prediction
        - Fraud probability döner (0-1)

        Returns:
            {
                "fraud_probability": 0.85,
                "is_fraud": True
            }
        """
        try:
            # 1. Feature extraction
            features = extract_features(request_data)

            # 2. Prediction
            # predict_proba: [prob_class_0, prob_class_1]
            # prob_class_1 = fraud probability
            fraud_prob = self.model.predict_proba(features)[0][1]

            # 3. Binary classification
            is_fraud = fraud_prob > PREDICTION_THRESHOLD

            logger.info(f"Prediction - TX: {request_data.get('transaction_id')}, "
                       f"Prob: {fraud_prob:.3f}, Fraud: {is_fraud}")

            return {
                "fraud_probability": float(fraud_prob),
                "is_fraud": bool(is_fraud)
            }

        except Exception as e:
            logger.error(f"Prediction error: {e}")
            # Hata durumunda güvenli taraf: orta risk
            return {
                "fraud_probability": 0.5,
                "is_fraud": False
            }

    def get_model_info(self) -> dict:
        """
        Model bilgileri
        """
        return {
            "model_type": type(self.model).__name__ if self.model else "None",
            "model_version": self.model_version,
            "features": self.feature_names,
            "training_date": "2025-01-07",  # Placeholder
            "accuracy": 0.92  # Placeholder
        }

    def is_loaded(self) -> bool:
        """
        Model yüklendi mi?
        """
        return self.model is not None


# ============================================
# GLOBAL MODEL INSTANCE
# ============================================
# Uygulama başladığında bir kez yüklenir
fraud_model = FraudDetectionModel()

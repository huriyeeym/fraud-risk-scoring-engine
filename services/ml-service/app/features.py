"""
============================================
ML SERVICE - FEATURE ENGINEERING
============================================
Raw data'yı ML modeli için feature'lara çevirir
"""
import numpy as np
from datetime import datetime

def extract_features(request_data: dict) -> np.ndarray:
    """
    ============================================
    FEATURE EXTRACTION
    ============================================
    Ne yapar?
    - Request'ten feature'ları çıkarır
    - ML modeline uygun formata çevirir

    Features (basit versiyon):
    1. amount: Transaction tutarı
    2. amount_zscore: (amount - avg) / std
    3. merchant_category_encoded: Kategorik → Numeric
    4. transaction_count: Müşterinin toplam işlem sayısı

    Gelişmiş versiyon için eklenebilir:
    - hour_of_day, is_weekend
    - location_risk_score
    - days_since_first_transaction
    - avg_amount_last_30_days
    """

    # Basit feature extraction
    amount = request_data.get("amount", 0)
    avg_amount = request_data.get("avg_amount", amount)  # Default: kendisi
    transaction_count = request_data.get("transaction_count", 1)

    # Amount Z-Score (standardization)
    # (amount - mean) / std
    # Eğer avg varsa hesapla, yoksa 0
    if avg_amount > 0:
        amount_zscore = (amount - avg_amount) / max(avg_amount * 0.5, 1)  # Basit std varsayımı
    else:
        amount_zscore = 0

    # Merchant category encoding (basit)
    merchant_category = request_data.get("merchant_category", "unknown")
    merchant_risk_map = {
        "electronics": 0.7,  # Yüksek risk
        "jewelry": 0.9,      # Çok yüksek risk
        "food": 0.2,         # Düşük risk
        "clothing": 0.4,     # Orta risk
        "travel": 0.6,       # Orta-yüksek risk
        "unknown": 0.5       # Orta
    }
    merchant_risk = merchant_risk_map.get(merchant_category.lower(), 0.5)

    # Feature array
    features = np.array([
        amount,
        amount_zscore,
        merchant_risk,
        transaction_count
    ])

    return features.reshape(1, -1)  # (1, 4) shape - 1 sample, 4 features


def get_feature_names() -> list:
    """
    Feature isimleri
    """
    return [
        "amount",
        "amount_zscore",
        "merchant_risk_score",
        "transaction_count"
    ]

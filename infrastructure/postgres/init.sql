-- ============================================
-- FRAUD & RISK SCORING ENGINE
-- Database Schema Initialization
-- ============================================
-- Bu script PostgreSQL container'ı ilk başladığında
-- otomatik çalışır ve tüm tabloları oluşturur.

-- Database encoding
SET client_encoding = 'UTF8';

-- ============================================
-- 1. TRANSACTIONS TABLE
-- ============================================
-- Ne yapar? Tüm işlemleri saklar
-- Kim kullanır? Transaction Service (yazar), Risk Engine (okur)
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    merchant_category VARCHAR(100),
    location VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index'ler (Neden? Sorguları hızlandırır)
CREATE INDEX IF NOT EXISTS idx_transactions_customer_id
    ON transactions(customer_id);

CREATE INDEX IF NOT EXISTS idx_transactions_timestamp
    ON transactions(timestamp DESC);

-- Composite index (Velocity rule için önemli!)
-- "Son 10 dakikada bu müşterinin işlemleri" sorgusu çok hızlanır
CREATE INDEX IF NOT EXISTS idx_transactions_customer_timestamp
    ON transactions(customer_id, timestamp DESC);

COMMENT ON TABLE transactions IS 'Tüm bankacılık işlemleri';
COMMENT ON INDEX idx_transactions_customer_timestamp IS 'Velocity rule için kritik - customer + time bazlı sorgular';

-- ============================================
-- 2. RISK_SCORES TABLE
-- ============================================
-- Ne yapar? Her işlemin risk skorunu saklar
-- Kim kullanır? Risk Engine (yazar), Dashboard (okur)
CREATE TABLE IF NOT EXISTS risk_scores (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) REFERENCES transactions(transaction_id) ON DELETE CASCADE,

    -- Skorlar
    rule_score INT CHECK (rule_score BETWEEN 0 AND 100),
    ml_score DECIMAL(5, 2) CHECK (ml_score BETWEEN 0 AND 1),
    final_score DECIMAL(5, 2) CHECK (final_score BETWEEN 0 AND 100),

    -- Açıklama (JSONB = JSON Binary, çok hızlı sorgulanır)
    -- Örnek: {"high_amount": 30, "velocity": 40, "location_anomaly": 25}
    reasons JSONB,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index'ler
CREATE INDEX IF NOT EXISTS idx_risk_scores_transaction_id
    ON risk_scores(transaction_id);

CREATE INDEX IF NOT EXISTS idx_risk_scores_final_score
    ON risk_scores(final_score DESC);

CREATE INDEX IF NOT EXISTS idx_risk_scores_created_at
    ON risk_scores(created_at DESC);

-- JSONB index (reasons içinde arama yapmak için)
CREATE INDEX IF NOT EXISTS idx_risk_scores_reasons
    ON risk_scores USING GIN (reasons);

COMMENT ON TABLE risk_scores IS 'Her transaction için hesaplanan risk skorları';
COMMENT ON COLUMN risk_scores.reasons IS 'JSON format - hangi kurallar tetiklendi ve ne kadar skor ekledi';

-- ============================================
-- 3. ALERTS TABLE
-- ============================================
-- Ne yapar? Yüksek riskli işlemler için alert'leri saklar
-- Kim kullanır? Alert Service (yazar), Dashboard (okur)
CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    risk_score DECIMAL(5, 2) NOT NULL,

    -- Alert durumu
    status VARCHAR(20) DEFAULT 'NEW' CHECK (status IN ('NEW', 'REVIEWED', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE')),

    -- Kim inceledi? (Fraud analyst)
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP,

    -- Notlar
    notes TEXT,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index'ler
CREATE INDEX IF NOT EXISTS idx_alerts_transaction_id
    ON alerts(transaction_id);

CREATE INDEX IF NOT EXISTS idx_alerts_status
    ON alerts(status);

CREATE INDEX IF NOT EXISTS idx_alerts_created_at
    ON alerts(created_at DESC);

-- Sadece NEW alert'leri getir (dashboard için)
CREATE INDEX IF NOT EXISTS idx_alerts_new_desc
    ON alerts(created_at DESC) WHERE status = 'NEW';

COMMENT ON TABLE alerts IS 'Yüksek riskli işlemler için oluşturulan alert\'ler';
COMMENT ON COLUMN alerts.status IS 'NEW: Yeni | REVIEWED: İncelendi | CONFIRMED_FRAUD: Gerçek fraud | FALSE_POSITIVE: Hatalı alarm';

-- ============================================
-- 4. CUSTOMER_PROFILES TABLE
-- ============================================
-- Ne yapar? Her müşterinin davranış profilini saklar
-- Kim kullanır? Risk Engine (okur), Batch job (yazar/günceller)
CREATE TABLE IF NOT EXISTS customer_profiles (
    customer_id VARCHAR(50) PRIMARY KEY,

    -- İstatistikler
    avg_amount DECIMAL(10, 2),
    median_amount DECIMAL(10, 2),
    std_amount DECIMAL(10, 2),  -- Standard deviation (sapma)

    -- Sık kullanılan lokasyonlar (JSON array)
    -- Örnek: ["Istanbul", "Ankara", "Izmir"]
    frequent_locations JSONB,

    -- Merchant kategori dağılımı (JSON object)
    -- Örnek: {"electronics": 0.4, "food": 0.5, "clothing": 0.1}
    merchant_categories JSONB,

    -- Zaman bazlı davranış
    -- Örnek: {"morning": 0.2, "afternoon": 0.5, "evening": 0.3}
    time_distribution JSONB,

    -- Genel bilgiler
    transaction_count INT DEFAULT 0,
    first_transaction_date DATE,
    last_transaction_date DATE,

    -- Metadata
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index'ler
CREATE INDEX IF NOT EXISTS idx_customer_profiles_last_updated
    ON customer_profiles(last_updated DESC);

-- JSONB indexes (hızlı JSON sorguları için)
CREATE INDEX IF NOT EXISTS idx_customer_profiles_locations
    ON customer_profiles USING GIN (frequent_locations);

CREATE INDEX IF NOT EXISTS idx_customer_profiles_merchants
    ON customer_profiles USING GIN (merchant_categories);

COMMENT ON TABLE customer_profiles IS 'Her müşterinin davranış profili - batch job tarafından güncellenir';
COMMENT ON COLUMN customer_profiles.frequent_locations IS 'JSON array - müşterinin en sık işlem yaptığı lokasyonlar';

-- ============================================
-- 5. ML_MODEL_METRICS TABLE (Opsiyonel)
-- ============================================
-- Ne yapar? ML modelinin performans metriklerini saklar
-- Kim kullanır? ML Service, Model monitoring
CREATE TABLE IF NOT EXISTS ml_model_metrics (
    id SERIAL PRIMARY KEY,
    model_version VARCHAR(50) NOT NULL,

    -- Metrikler
    accuracy DECIMAL(5, 4),
    precision_score DECIMAL(5, 4),
    recall DECIMAL(5, 4),
    f1_score DECIMAL(5, 4),

    -- Training bilgileri
    training_samples INT,
    training_date TIMESTAMP,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ml_model_metrics_version
    ON ml_model_metrics(model_version);

COMMENT ON TABLE ml_model_metrics IS 'ML modelinin performans metrikleri - model monitoring için';

-- ============================================
-- 6. FRAUD_LABELS TABLE (ML Training için)
-- ============================================
-- Ne yapar? Hangi işlemler gerçekten fraud'du? (labelled data)
-- Kim kullanır? ML training pipeline
CREATE TABLE IF NOT EXISTS fraud_labels (
    transaction_id VARCHAR(50) PRIMARY KEY REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    is_fraud BOOLEAN NOT NULL,

    -- Kim etiketledi?
    labelled_by VARCHAR(100),
    labelled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Fraud tipi (opsiyonel)
    fraud_type VARCHAR(50),  -- "stolen_card", "account_takeover", "synthetic_identity", etc.

    -- Notlar
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_fraud_labels_is_fraud
    ON fraud_labels(is_fraud);

COMMENT ON TABLE fraud_labels IS 'Labelled data - ML model training için kullanılır';

-- ============================================
-- SAMPLE DATA (Test için)
-- ============================================
-- Development ortamında test verileri ekle

-- Sample customers
INSERT INTO customer_profiles (customer_id, avg_amount, median_amount, std_amount, frequent_locations, merchant_categories, time_distribution, transaction_count, first_transaction_date, last_transaction_date)
VALUES
    ('C101', 150.00, 120.00, 45.00, '["Istanbul", "Ankara"]'::jsonb, '{"food": 0.6, "electronics": 0.4}'::jsonb, '{"morning": 0.3, "afternoon": 0.5, "evening": 0.2}'::jsonb, 150, '2024-01-01', '2025-01-05'),
    ('C102', 500.00, 450.00, 200.00, '["Izmir"]'::jsonb, '{"electronics": 0.7, "clothing": 0.3}'::jsonb, '{"afternoon": 0.6, "evening": 0.4}'::jsonb, 75, '2024-02-15', '2025-01-06')
ON CONFLICT (customer_id) DO NOTHING;

-- ============================================
-- UTILITY FUNCTIONS
-- ============================================

-- Function: Transaction count by date
CREATE OR REPLACE FUNCTION get_daily_transaction_count(target_date DATE)
RETURNS INT AS $$
BEGIN
    RETURN (
        SELECT COUNT(*)
        FROM transactions
        WHERE DATE(timestamp) = target_date
    );
END;
$$ LANGUAGE plpgsql;

-- Function: High risk transactions (score > threshold)
CREATE OR REPLACE FUNCTION get_high_risk_transactions(threshold DECIMAL)
RETURNS TABLE (
    transaction_id VARCHAR,
    customer_id VARCHAR,
    amount DECIMAL,
    final_score DECIMAL,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.transaction_id,
        t.customer_id,
        t.amount,
        r.final_score,
        r.created_at
    FROM transactions t
    JOIN risk_scores r ON t.transaction_id = r.transaction_id
    WHERE r.final_score > threshold
    ORDER BY r.final_score DESC, r.created_at DESC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_daily_transaction_count IS 'Belirli bir günde kaç transaction olduğunu döner';
COMMENT ON FUNCTION get_high_risk_transactions IS 'Threshold üzeri risk skoruna sahip transaction\'ları döner';

-- ============================================
-- INITIALIZATION COMPLETE
-- ============================================
-- Log
DO $$
BEGIN
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Fraud & Risk Scoring Engine DB initialized';
    RAISE NOTICE 'Tables: transactions, risk_scores, alerts, customer_profiles, ml_model_metrics, fraud_labels';
    RAISE NOTICE 'Sample data: 2 customers added';
    RAISE NOTICE '===========================================';
END $$;

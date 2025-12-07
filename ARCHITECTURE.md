# 🏛️ System Architecture - Fraud & Risk Scoring Engine

## 📋 Table of Contents
- [Design Principles](#design-principles)
- [Microservices Architecture](#microservices-architecture)
- [Data Flow](#data-flow)
- [Technology Decisions](#technology-decisions)
- [Scalability Considerations](#scalability-considerations)

---

## 🎯 Design Principles

### 1. **Event-Driven Architecture**
**Why?**
- Banking transactions happen in **bursts** (rush hours)
- Need to **decouple** services for resilience
- **Async processing** prevents bottlenecks

**How?**
- Kafka as message broker
- Services communicate via events
- No direct HTTP calls between services

### 2. **Hybrid Fraud Detection**
**Why Pure Rules Don't Work:**
- Too rigid → Many false positives
- Hard to maintain → 100+ rules become chaos

**Why Pure ML Doesn't Work:**
- "Black box" → No explanation
- Regulators demand transparency
- Need instant updates (no retraining delay)

**Our Solution: Hybrid**
```
Final Score = (Rule Score × 0.6) + (ML Score × 0.4)
            ↓                      ↓
      Explainable            Adaptive Learning
```

### 3. **Microservices (Not Monolith)**
**Why?**
- **Independent scaling** → ML service needs GPU, others don't
- **Technology diversity** → Java for business logic, Python for ML
- **Team autonomy** → Different teams own different services
- **Fault isolation** → If dashboard crashes, fraud detection still works

---

## 🧩 Microservices Architecture

### **Service Breakdown**

| Service | Responsibility | Technology | Why This Tech? |
|---------|---------------|------------|----------------|
| **Transaction Service** | Receive transactions, publish to Kafka | Spring Boot | Excellent Kafka integration, high throughput |
| **Risk Engine Service** | Apply rules, calculate risk score | Spring Boot | Complex business logic, need strong typing |
| **ML Service** | Fraud probability prediction | Python (FastAPI) | ML libraries (scikit-learn, TensorFlow) |
| **Alert Service** | Manage high-risk alerts | Spring Boot | Integration with email/SMS providers |
| **Customer Profile Service** | Build behavioral profiles | Spring Boot | Aggregate data, complex queries |
| **Dashboard** | Real-time visualization | React | Modern UI, WebSocket support |

---

## 🔄 Data Flow

### **Transaction Processing Flow**

```
1. Transaction Arrives
   ↓
[Transaction Service]
   │
   │ Validation
   │ ✓ Required fields exist?
   │ ✓ Amount > 0?
   │ ✓ Valid customer_id?
   │
   ├─→ [PostgreSQL] (store original transaction)
   └─→ [Kafka Topic: transactions-raw]
          ↓
      ┌───────────────────────────────┐
      │  Risk Engine Service          │
      │  (Consumes from Kafka)        │
      └───────────────────────────────┘
          │
          ├─→ Apply Rules (Parallel)
          │   ├─ Velocity Check
          │   ├─ Amount Threshold
          │   ├─ Location Anomaly
          │   └─ Merchant Category Risk
          │
          ├─→ Call ML Service (HTTP)
          │   └─ Get fraud probability (0-1)
          │
          ├─→ Fetch Customer Profile (Redis Cache)
          │   └─ Compare with historical behavior
          │
          └─→ Calculate Final Score
              │
              ├─→ [PostgreSQL] (store risk_scores)
              │
              └─→ If score > 70:
                  [Kafka Topic: fraud-alerts]
                      ↓
                  ┌─────────────────┐
                  │  Alert Service  │
                  └─────────────────┘
                      │
                      ├─→ Send to Dashboard (WebSocket)
                      ├─→ Store in alerts table
                      └─→ (Future: Email/SMS)
```

### **Why This Flow?**

**Step 1: Kafka (Not Direct HTTP)**
- ❌ `API Gateway → Risk Engine` = Tight coupling
- ✅ `API Gateway → Kafka → Risk Engine` = Loose coupling
- If Risk Engine is down, transactions still get queued

**Step 2: Parallel Rule Execution**
- Rules run concurrently (Java Streams API)
- 10 rules in 50ms instead of 500ms

**Step 3: Redis Cache for Profiles**
- ❌ `PostgreSQL lookup every time` = 100ms
- ✅ `Redis cache` = 2ms
- Profiles updated async (hourly batch job)

---

## 🧠 Rule Engine Design

### **Rule Interface**
```java
public interface FraudRule {
    int calculateScore(Transaction transaction, CustomerProfile profile);
    String getReason();
    int getPriority(); // Higher priority rules run first
}
```

### **Example Rules**

#### **1. High Amount Rule**
```java
if (transaction.amount > profile.avgAmount * 3) {
    score += 30;
    reason = "Amount is 3x customer average";
}
```

#### **2. Velocity Rule**
```java
int transactionsLast10Min = countRecentTransactions(customerId, 10_MINUTES);
if (transactionsLast10Min > 5) {
    score += 40;
    reason = "5+ transactions in 10 minutes";
}
```

#### **3. Location Anomaly**
```java
if (!profile.frequentLocations.contains(transaction.location)) {
    score += 25;
    reason = "Unusual location: " + transaction.location;
}
```

### **Why This Design?**
- ✅ **Extensible**: Add new rules without changing core engine
- ✅ **Testable**: Each rule is isolated, easy to unit test
- ✅ **Explainable**: Each rule provides a reason
- ✅ **Prioritizable**: Critical rules run first

---

## 🤖 ML Model Architecture

### **Model Choice: Logistic Regression (v1)**

**Why Not Deep Learning?**
- Logistic Regression:
  - ✅ Fast inference (<10ms)
  - ✅ Explainable (feature weights)
  - ✅ Works well with small datasets
- Deep Learning:
  - ❌ Needs millions of samples
  - ❌ Black box
  - ❌ Overkill for this problem

**Future**: Upgrade to Random Forest if accuracy isn't enough

### **Feature Engineering**
```python
features = [
    'amount',
    'amount_zscore',  # (amount - mean) / std
    'hour_of_day',
    'is_weekend',
    'merchant_category_encoded',
    'location_risk_score',
    'days_since_first_transaction',
    'avg_amount_last_30_days',
    'transaction_count_last_hour'
]
```

### **Training Pipeline**
```python
1. Fetch labeled data (PostgreSQL)
   └─ fraud_transactions table (is_fraud: true/false)

2. Feature engineering
   └─ Calculate derived features

3. Train model
   └─ 80% train, 20% test

4. Evaluate
   └─ Precision, Recall, F1-Score

5. Save model
   └─ model.pkl → loaded by ML service

6. (Future) A/B Testing
   └─ Deploy v2 model to 10% of traffic
```

---

## 📊 Database Schema Design

### **Why PostgreSQL?**
- ✅ ACID transactions (consistency is critical)
- ✅ JSONB support (flexible `reasons` field)
- ✅ Excellent query performance with indexes

### **Why Redis?**
- ✅ Sub-millisecond reads
- ✅ Perfect for customer profiles (read-heavy)
- ✅ TTL support (auto-expire old data)

### **Schema**

#### **transactions**
```sql
CREATE TABLE transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    merchant_category VARCHAR(100),
    location VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_customer_timestamp (customer_id, timestamp),
    INDEX idx_timestamp (timestamp)
);
```
**Why these indexes?**
- `idx_customer_timestamp`: Velocity rules (recent transactions per customer)
- `idx_timestamp`: Daily trend queries

#### **risk_scores**
```sql
CREATE TABLE risk_scores (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) REFERENCES transactions(transaction_id),
    rule_score INT,
    ml_score DECIMAL(5,2),
    final_score DECIMAL(5,2),
    reasons JSONB,  -- {"high_amount": 30, "velocity": 40}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_final_score (final_score DESC),
    INDEX idx_created_at (created_at)
);
```

#### **customer_profiles**
```sql
CREATE TABLE customer_profiles (
    customer_id VARCHAR(50) PRIMARY KEY,
    avg_amount DECIMAL(10,2),
    frequent_locations JSONB,  -- ["Istanbul", "Ankara"]
    merchant_categories JSONB,  -- {"electronics": 0.4, "food": 0.6}
    transaction_count INT,
    first_transaction_date DATE,
    last_updated TIMESTAMP
);
```

---

## ⚡ Scalability Considerations

### **Current (MVP) Throughput**
- ~1,000 transactions/second

### **Future Scaling Strategies**

#### **1. Horizontal Scaling**
```yaml
# docker-compose.yml
risk-engine-service:
  deploy:
    replicas: 5  # Run 5 instances
```
Kafka will distribute load across instances

#### **2. Database Partitioning**
```sql
-- Partition transactions by date
CREATE TABLE transactions_2025_01 PARTITION OF transactions
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

#### **3. Caching Strategy**
```
Redis (hot data, 1-hour TTL)
  ↓ (cache miss)
PostgreSQL (all data)
```

#### **4. Kafka Tuning**
```properties
# More partitions = more parallelism
num.partitions=12

# Batch processing
batch.size=16384
```

---

## 🔒 Security Considerations

### **1. Sensitive Data**
- ❌ NEVER log full transaction details
- ✅ Log only `transaction_id` + `risk_score`

### **2. Environment Variables**
```bash
# .env (NEVER commit to Git!)
DB_PASSWORD=super_secret
KAFKA_PASSWORD=another_secret
```

### **3. API Authentication**
```java
@PreAuthorize("hasRole('FRAUD_ANALYST')")
public List<Alert> getHighRiskAlerts() { ... }
```

---

## 🧪 Testing Strategy

### **Unit Tests**
- Each fraud rule tested independently
- Mock customer profiles
- Example:
  ```java
  @Test
  void testHighAmountRule() {
      Transaction tx = new Transaction(amount: 10000);
      CustomerProfile profile = new CustomerProfile(avgAmount: 1000);

      int score = highAmountRule.calculate(tx, profile);

      assertEquals(30, score);
  }
  ```

### **Integration Tests**
- End-to-end flow
- Real Kafka (Testcontainers)
- Example:
  ```java
  @Test
  void testFraudDetectionFlow() {
      // Send transaction to Kafka
      kafkaTemplate.send("transactions-raw", transaction);

      // Wait for processing
      await().atMost(5, SECONDS)
             .until(() -> alertRepository.findByTransactionId(txId) != null);

      // Verify alert was created
      Alert alert = alertRepository.findByTransactionId(txId);
      assertTrue(alert.getScore() > 70);
  }
  ```

---

## 📈 Monitoring & Observability

### **Metrics to Track**
```
- Transactions processed/second
- Average processing time per transaction
- Fraud detection rate (% of transactions flagged)
- False positive rate
- ML model accuracy (weekly)
- Kafka lag (messages waiting to be processed)
```

### **Tools**
- **Prometheus**: Metrics collection
- **Grafana**: Dashboards
- **ELK Stack**: Log aggregation

---

## 🔮 Future Enhancements

1. **Graph Database** (Neo4j)
   - Detect fraud rings (connected accounts)

2. **Stream Processing** (Kafka Streams)
   - Real-time aggregations

3. **Feature Store** (Feast)
   - Centralized feature management

4. **Model Monitoring**
   - Detect model drift
   - Auto-retrain when accuracy drops

---

**Last Updated**: 2025-01-07
**Version**: 1.0

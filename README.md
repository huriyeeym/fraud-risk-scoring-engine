# 🛡️ Fraud & Risk Scoring Engine

Real-time fraud detection system for banking transactions using **Event-Driven Architecture**, **Hybrid Rule Engine + Machine Learning**, and **Customer Behavior Profiling**.

---

## 🎯 What Does This Do?

Detects fraudulent banking transactions in **real-time** using:
- ⚡ Kafka streaming
- 🧠 Rule Engine + ML (Hybrid)
- 📊 Customer behavior profiling
- 🚨 Instant alerts
- 📈 Live dashboard

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java Spring Boot |
| **Streaming** | Apache Kafka |
| **ML Service** | Python (FastAPI) |
| **Frontend** | React + Tailwind CSS |
| **Database** | PostgreSQL |
| **Cache** | Redis |
| **DevOps** | Docker Compose |

---

## 🚀 Quick Start

```bash
# Clone & run
git clone https://github.com/huriyeeym/fraud-risk-scoring-engine.git
cd fraud-risk-scoring-engine
docker-compose up -d

# Access dashboard
http://localhost:3000
```

---

## 📦 Project Structure

```
fraud-risk-scoring-engine/
├── services/               # Microservices
│   ├── transaction-service/
│   ├── risk-engine-service/
│   ├── ml-service/
│   └── alert-service/
├── frontend/               # React dashboard
├── infrastructure/         # Kafka, PostgreSQL, Redis
└── docker-compose.yml
```

---

## 📈 Roadmap

- [x] Project setup
- [ ] Transaction service
- [ ] Rule engine
- [ ] ML model
- [ ] Customer profiling
- [ ] Dashboard
- [ ] Production deployment

---

## 📚 Documentation

- [Architecture Details](./ARCHITECTURE.md) - Deep dive into design decisions
- API Docs (coming soon)

---

## 👨‍💻 Author

**Hüriye** - [GitHub](https://github.com/huriyeeym)

*Personal learning project to understand enterprise fraud detection*

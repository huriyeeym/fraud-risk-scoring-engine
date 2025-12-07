# ============================================
# ML SERVICE - DOCKERFILE
# ============================================
# Python + FastAPI + scikit-learn

FROM python:3.10-slim

# ============================================
# SYSTEM DEPENDENCIES
# ============================================
# PostgreSQL için gerekli sistem paketleri
RUN apt-get update && apt-get install -y \
    gcc \
    postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# ============================================
# WORKING DIRECTORY
# ============================================
WORKDIR /app

# ============================================
# INSTALL PYTHON DEPENDENCIES
# ============================================
# requirements.txt'yi önce kopyala (cache layer)
COPY requirements.txt .

# Pip upgrade ve install
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# ============================================
# COPY APPLICATION CODE
# ============================================
COPY app/ ./app/
COPY models/ ./models/

# ============================================
# NON-ROOT USER (Security)
# ============================================
RUN useradd -m -u 1000 mluser && chown -R mluser:mluser /app
USER mluser

# ============================================
# EXPOSE PORT
# ============================================
EXPOSE 8000

# ============================================
# HEALTHCHECK
# ============================================
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:8000/health')" || exit 1

# ============================================
# START APPLICATION
# ============================================
# Uvicorn: ASGI server
# --host 0.0.0.0: Tüm network interface'lerden dinle
# --port 8000: Port
# --reload: Development mode (production'da kullanma!)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

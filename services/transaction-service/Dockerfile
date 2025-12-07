# ============================================
# TRANSACTION SERVICE - DOCKERFILE
# ============================================
# Multi-stage build: Küçük ve hızlı image

# ============================================
# STAGE 1: BUILD (Derleme)
# ============================================
# Ne yapar? Maven ile projeyi derler, JAR oluşturur
# Neden? Source code'u compile etmek gerekli
FROM maven:3.9-eclipse-temurin-17 AS builder

# Çalışma dizini
WORKDIR /app

# pom.xml'i kopyala (dependency caching için önce bu)
# Neden önce? Dependencies nadiren değişir, cache kullanılır
COPY pom.xml .

# Dependencies'i indir (cache layer)
RUN mvn dependency:go-offline -B

# Tüm kaynak kodları kopyala
COPY src ./src

# Projeyi derle (JAR oluştur)
# -DskipTests: Testleri şimdilik atla (hızlı build)
RUN mvn clean package -DskipTests

# ============================================
# STAGE 2: RUNTIME (Çalıştırma)
# ============================================
# Ne yapar? Sadece JRE + JAR dosyası (küçük image)
# Neden? Maven, source code gereksiz (sadece çalıştırmak için)
FROM eclipse-temurin:17-jre-alpine

# Non-root user (güvenlik)
# Neden? Root olarak çalıştırmak güvenlik riski
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Çalışma dizini
WORKDIR /app

# JAR dosyasını builder stage'den kopyala
# --from=builder: Önceki stage'den al
COPY --from=builder /app/target/transaction-service.jar app.jar

# Port expose (bilgilendirme amaçlı)
# Not: Gerçek port docker-compose.yml'de belirtilir
EXPOSE 8081

# Healthcheck (Docker'a "servis hazır mı?" bilgisi verir)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Uygulama başlat
# -Djava.security.egd: Daha hızlı rastgele sayı üretimi
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

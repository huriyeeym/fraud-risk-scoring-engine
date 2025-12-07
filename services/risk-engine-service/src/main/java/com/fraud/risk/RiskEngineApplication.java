package com.fraud.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * ============================================
 * RISK ENGINE APPLICATION
 * ============================================
 * Core fraud detection engine
 * - Kafka Consumer (Transaction'ları dinler)
 * - Rule Engine (Fraud kuralları)
 * - ML Service Client
 * - Kafka Producer (Alert gönderir)
 */
@SpringBootApplication
@EnableKafka  // Kafka'yı aktifleştir
public class RiskEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskEngineApplication.java, args);

        System.out.println("""

                ============================================
                🧠  RISK ENGINE SERVICE STARTED
                ============================================
                Port: 8082
                Kafka Consumer: transactions-topic
                Kafka Producer: fraud-alerts-topic
                ============================================
                """);
    }
}

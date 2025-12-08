package com.fraud.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class AlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.java, args);

        System.out.println("""

                ============================================
                🚨  ALERT SERVICE STARTED
                ============================================
                Port: 8083
                Kafka Consumer: fraud-alerts-topic
                REST API: /api/alerts
                ============================================
                """);
    }
}

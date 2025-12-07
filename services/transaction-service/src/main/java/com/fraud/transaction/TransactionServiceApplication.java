package com.fraud.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================
 * TRANSACTION SERVICE APPLICATION
 * ============================================
 * Ne yapar?
 * - Spring Boot uygulamasının başlangıç noktası
 * - main() metodu burada
 *
 * @SpringBootApplication nedir?
 * - 3 annotation'ı birleştirir:
 *   1. @Configuration: Bean configuration
 *   2. @EnableAutoConfiguration: Spring Boot auto-config
 *   3. @ComponentScan: Component taraması (com.fraud.transaction package'ı altında)
 *
 * Neden böyle?
 * - Boilerplate kod azalır
 * - Convention over configuration (Spring felsefesi)
 *
 * Nasıl çalışır?
 * 1. main() çalışır
 * 2. SpringApplication.run() Spring container'ı başlatır
 * 3. Component scan yapılır (@Controller, @Service, @Repository)
 * 4. Auto-configuration çalışır (Kafka, JPA, vb.)
 * 5. Tomcat embedded server başlar (port 8081)
 * 6. Uygulama hazır!
 */
@SpringBootApplication
public class TransactionServiceApplication {

    /**
     * ============================================
     * MAIN METHOD
     * ============================================
     * Ne yapar?
     * - JVM tarafından çalıştırılır
     * - Spring Boot application context'i başlatır
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Spring Boot uygulamasını başlat
        // Bu satır:
        // 1. ApplicationContext oluşturur
        // 2. Tüm Bean'leri yükler
        // 3. Embedded Tomcat başlatır
        // 4. Application çalışmaya başlar
        SpringApplication.run(TransactionServiceApplication.java, args);

        System.out.println("""

                ============================================
                🛡️  TRANSACTION SERVICE STARTED
                ============================================
                Port: 8081
                API: http://localhost:8081/api/transactions
                Health: http://localhost:8081/actuator/health
                ============================================
                """);
    }
}

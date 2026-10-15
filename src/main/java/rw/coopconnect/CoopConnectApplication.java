package rw.coopconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * CoopConnect — Farmers' cooperative and produce marketplace for Rwanda.
 * FR-01: Application entry point with role-based security and JPA auditing.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class CoopConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoopConnectApplication.class, args);
    }
}

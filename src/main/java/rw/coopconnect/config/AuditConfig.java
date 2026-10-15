package rw.coopconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import rw.coopconnect.security.SecurityUtils;

/**
 * Configuration for Spring Data JPA auditing.
 */
@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getEmail());
    }
}

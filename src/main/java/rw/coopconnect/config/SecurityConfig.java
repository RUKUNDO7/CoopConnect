package rw.coopconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.security.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * FR-01: Spring Security configuration with role-based access control,
 * BCrypt password hashing, and role-based login landing routing.
 * NFR-01: OWASP mitigations, CSRF protection, secure session handling.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                Role role = userDetails.getRole();
                String targetUrl = switch (role) {
                    case FARMER -> "/farmer/dashboard";
                    case COOPERATIVE_MANAGER -> "/manager/dashboard";
                    case BUYER -> "/buyer/dashboard";
                    case TRANSPORTER -> "/transporter/dashboard";
                    case ADMIN -> "/admin/dashboard";
                };
                response.sendRedirect(targetUrl);
                return;
            }
            response.sendRedirect("/dashboard");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/auth/**",
                    "/login",
                    "/register",
                    "/reset-password",
                    "/products",
                    "/products/{id:[0-9]+}",
                    "/price-dashboard",
                    "/api/price-dashboard/**",
                    "/api/payment/callback/**",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/uploads/**",
                    "/h2-console/**",
                    "/error"
                ).permitAll()
                .requestMatchers("/farmer/**").hasRole("FARMER")
                .requestMatchers("/manager/**").hasRole("COOPERATIVE_MANAGER")
                .requestMatchers("/buyer/**", "/cart/**", "/orders/**", "/checkout/**", "/ratings/**").hasRole("BUYER")
                .requestMatchers("/transporter/**").hasRole("TRANSPORTER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/reports/**").hasAnyRole("COOPERATIVE_MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleBasedAuthenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/payment/callback/**")
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }
}

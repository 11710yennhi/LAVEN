package iuh.fit.laven.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Chỉ mở đúng POST webhook cho SePay; API key được kiểm tra trong BankPaymentService. */
@Configuration
public class PaymentWebhookSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain sepayWebhookChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/payments/webhook")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}

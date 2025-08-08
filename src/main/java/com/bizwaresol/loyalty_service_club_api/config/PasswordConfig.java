package com.bizwaresol.loyalty_service_club_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password encoding and password-related security beans.
 * This configuration is separated from SecurityConfig to avoid circular dependencies:
 * SecurityConfig -> CustomAuthenticationProvider -> AuthenticationService
 * -> CustomerAccountService -> PasswordEncoder
 * By keeping password encoding configuration separate, we ensure clean dependency resolution.
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt password encoder for the application.
     * BCrypt is chosen because:
     * - Adaptive hashing (can increase cost factor over time)
     * - Built-in salt generation
     * - Resistant to timing attacks
     * - Industry standard for password hashing
     *
     * @return PasswordEncoder instance using BCrypt algorithm
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Future password-related beans can be added here:
    // - Password strength validators
    // - Password history checkers
    // - Password policy enforcement
    // - Password reset token generators
}
package com.bizwaresol.loyalty_service_club_api.config;

import com.bizwaresol.loyalty_service_club_api.security.auth.CustomAuthenticationProvider;
import com.bizwaresol.loyalty_service_club_api.security.auth.CustomerUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final CustomerUserDetailsService customerUserDetailsService;
    private final DataSource dataSource;

    public SecurityConfig(
            CustomAuthenticationProvider customAuthenticationProvider,
            CustomerUserDetailsService customerUserDetailsService,
            DataSource dataSource) {
        this.customAuthenticationProvider = customAuthenticationProvider;
        this.customerUserDetailsService = customerUserDetailsService;
        this.dataSource = dataSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        // Uses your existing persistent_logins table
        return tokenRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(customAuthenticationProvider)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .permitAll()
                        .disable() // Disable default form login since you're using API
                )
                .httpBasic(AbstractHttpConfigurer::disable) // Disable basic auth
                .rememberMe(remember -> remember
                        .key("loyaltyClubRememberMeKey") // Secret key for signing tokens
                        .tokenValiditySeconds(14 * 24 * 60 * 60) // 14 days as requested
                        .userDetailsService(customerUserDetailsService)
                        .tokenRepository(persistentTokenRepository())
                        .rememberMeParameter("rememberMe") // Form parameter name
                        .rememberMeCookieName("LOYALTY_REMEMBER_ME") // Cookie name
                )
                .sessionManagement(session -> session
                        .maximumSessions(1) // One session per user
                        .maxSessionsPreventsLogin(false) // Allow new login to kick out old session
                )
                .csrf(CsrfConfigurer::disable); // Disable CSRF for API development

        return http.build();
    }
}
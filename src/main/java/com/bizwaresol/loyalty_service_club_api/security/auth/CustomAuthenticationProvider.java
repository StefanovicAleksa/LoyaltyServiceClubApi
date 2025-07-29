package com.bizwaresol.loyalty_service_club_api.security.auth;

import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.LoginRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.LoginResult;
import com.bizwaresol.loyalty_service_club_api.service.auth.AuthenticationService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Custom authentication provider that integrates your AuthenticationService with Spring Security
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationService authenticationService;
    private final CustomerUserDetailsService userDetailsService;

    public CustomAuthenticationProvider(
            AuthenticationService authenticationService,
            CustomerUserDetailsService userDetailsService) {
        this.authenticationService = authenticationService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identifier = authentication.getName();
        String password = authentication.getCredentials().toString();

        try {
            // Use your custom authentication service
            LoginRequest loginRequest = new LoginRequest(identifier, password, false);
            LoginResult result = authenticationService.authenticate(loginRequest);

            if (!result.success()) {
                throw new BadCredentialsException("Authentication failed");
            }

            // Load user details for Spring Security
            CustomerUserDetails userDetails = (CustomerUserDetails) userDetailsService.loadUserByUsername(
                    result.account().getUsername()
            );

            // Check account status
            if (!userDetails.isEnabled()) {
                throw new DisabledException("Account is disabled");
            }

            // Create authenticated token
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, // Don't store credentials
                    userDetails.getAuthorities()
            );

        } catch (com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.InvalidLoginCredentialsException e) {
            throw new BadCredentialsException("Invalid credentials", e);
        } catch (com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.AccountSuspendedException e) {
            throw new DisabledException("Account is suspended", e);
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication error", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
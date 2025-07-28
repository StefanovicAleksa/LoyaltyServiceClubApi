package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login;

public class InvalidLoginCredentialsException extends AuthenticationException {

    public InvalidLoginCredentialsException(String identifier) {
        super("Invalid login credentials for identifier: " + identifier, "INVALID_LOGIN_CREDENTIALS");
    }

    public InvalidLoginCredentialsException(String identifier, Throwable cause) {
        super("Invalid login credentials for identifier: " + identifier, "INVALID_LOGIN_CREDENTIALS", cause);
    }

    public InvalidLoginCredentialsException() {
        super("Invalid login credentials provided", "INVALID_LOGIN_CREDENTIALS");
    }
}
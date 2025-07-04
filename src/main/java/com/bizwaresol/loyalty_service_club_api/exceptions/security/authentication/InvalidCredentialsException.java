package com.bizwaresol.loyalty_service_club_api.exceptions.security.authentication;

public class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException(String message) {
        super("Invalid credentials: " + message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super("Invalid credentials: " + message, cause);
    }
}
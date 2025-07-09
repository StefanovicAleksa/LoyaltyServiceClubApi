package com.bizwaresol.loyalty_service_club_api.exception.security.authentication;

import com.bizwaresol.loyalty_service_club_api.exception.security.SecurityException;

public class AuthenticationException extends SecurityException {

    public AuthenticationException(String message) {
        super(message, "AUTHENTICATION_FAILED", 401);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, "AUTHENTICATION_FAILED", 401, cause);
    }
}
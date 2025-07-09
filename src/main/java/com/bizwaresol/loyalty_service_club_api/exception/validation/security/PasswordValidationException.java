package com.bizwaresol.loyalty_service_club_api.exception.validation.security;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public class PasswordValidationException extends ValidationException {
    public PasswordValidationException(String passwordRequirements) {
        super("Password validation failed: " + passwordRequirements, "PASSWORD_VALIDATION_FAILED");
    }

    public PasswordValidationException(String passwordRequirements, Throwable cause) {
        super("Password validation failed: " + passwordRequirements, "PASSWORD_VALIDATION_FAILED", cause);
    }
}

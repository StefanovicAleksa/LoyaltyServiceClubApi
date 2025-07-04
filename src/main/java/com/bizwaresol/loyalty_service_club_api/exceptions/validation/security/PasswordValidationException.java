package com.bizwaresol.loyalty_service_club_api.exceptions.validation.security;

import com.bizwaresol.loyalty_service_club_api.exceptions.validation.ValidationException;

public class PasswordValidationException extends ValidationException {
    public PasswordValidationException(String passwordRequirements) {
        super("Password validation failed: " + passwordRequirements, "PASSWORD_VALIDATION_FAILED");
    }

    public PasswordValidationException(String passwordRequirements, Throwable cause) {
        super("Password validation failed: " + passwordRequirements, "PASSWORD_VALIDATION_FAILED", cause);
    }
}

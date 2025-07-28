package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration;

public class RegistrationFailedException extends RegistrationException {

    public RegistrationFailedException(String reason) {
        super("Registration failed: " + reason, "REGISTRATION_FAILED");
    }

    public RegistrationFailedException(String reason, Throwable cause) {
        super("Registration failed: " + reason, "REGISTRATION_FAILED", cause);
    }

    public RegistrationFailedException(Throwable cause) {
        super("Registration failed due to system error", "REGISTRATION_FAILED", cause);
    }
}
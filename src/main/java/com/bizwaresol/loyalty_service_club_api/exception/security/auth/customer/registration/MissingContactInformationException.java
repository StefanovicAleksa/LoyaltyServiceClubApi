package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration;

public class MissingContactInformationException extends RegistrationException {

    public MissingContactInformationException() {
        super("Registration requires at least email or phone contact information",
                "MISSING_CONTACT_INFORMATION");
    }

    public MissingContactInformationException(Throwable cause) {
        super("Registration requires at least email or phone contact information",
                "MISSING_CONTACT_INFORMATION", cause);
    }
}
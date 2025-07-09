package com.bizwaresol.loyalty_service_club_api.exception.business.duplicate;

public class DuplicateEmailException extends DuplicateResourceException {

    public DuplicateEmailException(String email) {
        super("Email already exists: " + email, "DUPLICATE_EMAIL");
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super("Email already exists: " + email, "DUPLICATE_EMAIL", cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.exceptions.business.duplicate;

public class DuplicateUsernameException extends DuplicateResourceException {

    public DuplicateUsernameException(String username) {
        super("Username already exists: " + username, "DUPLICATE_USERNAME");
    }

    public DuplicateUsernameException(String username, Throwable cause) {
        super("Username already exists: " + username, "DUPLICATE_USERNAME", cause);
    }
}
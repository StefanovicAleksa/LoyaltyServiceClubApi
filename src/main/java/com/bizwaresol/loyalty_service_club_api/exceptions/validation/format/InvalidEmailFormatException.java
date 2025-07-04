package com.bizwaresol.loyalty_service_club_api.exceptions.validation.format;

import com.bizwaresol.loyalty_service_club_api.exceptions.validation.ValidationException;

public class InvalidEmailFormatException extends ValidationException {
    public InvalidEmailFormatException(String email) {
        super("Invalid email format: " + email, "INVALID_EMAIL_FORMAT");
    }

    public InvalidEmailFormatException(String email, Throwable cause) {
        super("Invalid email format: " + email, "INVALID_EMAIL_FORMAT", cause);
    }
}

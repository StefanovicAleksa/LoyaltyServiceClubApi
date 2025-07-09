package com.bizwaresol.loyalty_service_club_api.exception.validation.field;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public class InvalidCharacterException extends ValidationException {

    public InvalidCharacterException(String fieldName, String reason) {
        super("Field '" + fieldName + "' contains invalid characters. " + reason, "INVALID_CHARACTER");
    }

    public InvalidCharacterException(String fieldName, String reason, Throwable cause) {
        super("Field '" + fieldName + "' contains invalid characters. " + reason, "INVALID_CHARACTER", cause);
    }
}
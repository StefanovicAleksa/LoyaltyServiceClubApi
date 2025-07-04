package com.bizwaresol.loyalty_service_club_api.exceptions.validation.field;

import com.bizwaresol.loyalty_service_club_api.exceptions.validation.ValidationException;

public class FieldTooLongException extends ValidationException {
    public FieldTooLongException(String fieldName, int maxLength, int actualLength) {
        super("Field " + fieldName + " too long: " + actualLength + " characters. Max: " + maxLength + " characters", "FIELD_TOO_LONG");
    }

    public FieldTooLongException(String fieldName, int maxLength, int actualLength, Throwable cause) {
        super("Field " + fieldName + " too long: " + actualLength + " characters. Max: " + maxLength + " characters", "FIELD_TOO_LONG", cause);
    }
}

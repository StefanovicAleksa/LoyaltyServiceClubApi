package com.bizwaresol.loyalty_service_club_api.exception.validation.field;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public class FieldTooShortException extends ValidationException {
    public FieldTooShortException(String fieldName, int minLength, int actualLength) {
        super("Field " + fieldName + " too short: " + actualLength + " characters. Min: " + minLength + " characters", "FIELD_TOO_SHORT");
    }

    public FieldTooShortException(String fieldName, int minLength, int actualLength, Throwable cause) {
        super("Field " + fieldName + " too short: " + actualLength + " characters. Min: " + minLength + " characters", "FIELD_TOO_SHORT", cause);
    }
}

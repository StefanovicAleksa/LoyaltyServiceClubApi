package com.bizwaresol.loyalty_service_club_api.exceptions.validation.field;

import com.bizwaresol.loyalty_service_club_api.exceptions.validation.ValidationException;

public class EmptyFieldException extends ValidationException {
    public EmptyFieldException(String fieldName) {
        super("Field '" + fieldName + "' cannot be empty", "EMPTY_FIELD");
    }

    public EmptyFieldException(String fieldName, Throwable cause) {
        super("Field '" + fieldName + "' cannot be empty", "EMPTY_FIELD", cause);
    }
}

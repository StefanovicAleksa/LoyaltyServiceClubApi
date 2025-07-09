package com.bizwaresol.loyalty_service_club_api.exception.validation.field;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public class NullFieldException extends ValidationException {
    public NullFieldException(String fieldName) {
        super("Field '" + fieldName + "' cannot be null", "NULL_FIELD");
    }

    public NullFieldException(String fieldName, Throwable cause) {
        super("Field '" + fieldName + "' cannot be null", "NULL_FIELD", cause);
    }
}

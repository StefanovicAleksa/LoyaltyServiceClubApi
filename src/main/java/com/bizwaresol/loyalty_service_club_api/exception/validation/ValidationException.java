package com.bizwaresol.loyalty_service_club_api.exception.validation;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class ValidationException extends ServiceException {
    public ValidationException(String message, String errorCode) {
        super(message, errorCode, 400);
    }

    public ValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 400, cause);
    }
}

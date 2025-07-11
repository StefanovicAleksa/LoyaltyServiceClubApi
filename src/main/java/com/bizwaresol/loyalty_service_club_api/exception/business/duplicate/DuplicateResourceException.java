package com.bizwaresol.loyalty_service_club_api.exception.business.duplicate;

import com.bizwaresol.loyalty_service_club_api.exception.business.BusinessLogicException;

public abstract class DuplicateResourceException extends BusinessLogicException {

    public DuplicateResourceException(String message, String errorCode) {
        super(message, errorCode, 409);
    }

    public DuplicateResourceException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 409, cause);
    }
}
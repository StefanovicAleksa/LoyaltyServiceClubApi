package com.bizwaresol.loyalty_service_club_api.exceptions.business.resource;

import com.bizwaresol.loyalty_service_club_api.exceptions.business.BusinessLogicException;

public abstract class ResourceNotFoundException extends BusinessLogicException {

    public ResourceNotFoundException(String message, String errorCode) {
        super(message, errorCode, 404);
    }

    public ResourceNotFoundException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 404, cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class RegistrationException extends ServiceException {

    public RegistrationException(String message, String errorCode) {
        super(message, errorCode, 400);
    }

    public RegistrationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 400, cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.exception.security;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class SecurityException extends ServiceException {

    public SecurityException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, httpStatus);
    }

    public SecurityException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
}
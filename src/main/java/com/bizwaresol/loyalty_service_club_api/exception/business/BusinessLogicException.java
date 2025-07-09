package com.bizwaresol.loyalty_service_club_api.exception.business;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class BusinessLogicException extends ServiceException {

    public BusinessLogicException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, httpStatus);
    }

    public BusinessLogicException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
}
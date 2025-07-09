package com.bizwaresol.loyalty_service_club_api.exception.system;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class SystemException extends ServiceException {

    public SystemException(String message, String errorCode) {
        super(message, errorCode, 500);
    }

    public SystemException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 500, cause);
    }
}
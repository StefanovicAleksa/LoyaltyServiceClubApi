package com.bizwaresol.loyalty_service_club_api.exception.system.client;

import com.bizwaresol.loyalty_service_club_api.exception.system.SystemException;

public abstract class ClientSystemException extends SystemException {

    public ClientSystemException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ClientSystemException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
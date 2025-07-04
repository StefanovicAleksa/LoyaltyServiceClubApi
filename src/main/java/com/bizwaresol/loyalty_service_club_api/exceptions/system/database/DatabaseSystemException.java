package com.bizwaresol.loyalty_service_club_api.exceptions.system.database;

import com.bizwaresol.loyalty_service_club_api.exceptions.system.SystemException;

public class DatabaseSystemException extends SystemException {

    public DatabaseSystemException(String message) {
        super(message, "DATABASE_SYSTEM_ERROR");
    }

    public DatabaseSystemException(String message, Throwable cause) {
        super(message, "DATABASE_SYSTEM_ERROR", cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.exception.system.database;

public class DatabaseTimeoutException extends DatabaseSystemException {

    public DatabaseTimeoutException(String message) {
        super("Database operation timeout: " + message);
    }

    public DatabaseTimeoutException(String message, Throwable cause) {
        super("Database operation timeout: " + message, cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.exception.system.database;

public class DatabaseConnectionException extends DatabaseSystemException {

    public DatabaseConnectionException(String message) {
        super("Database connection failed: " + message);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super("Database connection failed: " + message, cause);
    }
}
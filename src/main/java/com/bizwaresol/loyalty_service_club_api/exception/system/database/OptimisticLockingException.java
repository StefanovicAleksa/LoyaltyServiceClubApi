package com.bizwaresol.loyalty_service_club_api.exception.system.database;

public class OptimisticLockingException extends DatabaseSystemException {

    public OptimisticLockingException(String message) {
        super("Optimistic locking conflict: " + message);
    }

    public OptimisticLockingException(String message, Throwable cause) {
        super("Optimistic locking conflict: " + message, cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.exceptions.system.database;

public class TransactionRollbackException extends DatabaseSystemException {

    public TransactionRollbackException(String message) {
        super("Transaction rollback occurred: " + message);
    }

    public TransactionRollbackException(String message, Throwable cause) {
        super("Transaction rollback occurred: " + message, cause);
    }
}
package com.bizwaresol.loyalty_service_club_api.util.mappers;

import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.*;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.*;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.*;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

import org.springframework.dao.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionTimedOutException;

import jakarta.persistence.*;
import java.sql.SQLException;

public final class RepositoryErrorMapper {

    private RepositoryErrorMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Maps repository layer exceptions to service layer exceptions
     * Usage: catch(Exception e) { throw RepositoryErrorMapper.mapException(e); }
     */
    public static ServiceException mapException(Exception e) {
        // Spring Data Access Exceptions (highest priority)
        if (e instanceof DataAccessException) {
            return mapDataAccessException((DataAccessException) e);
        }

        // JPA Persistence Exceptions
        if (e instanceof PersistenceException) {
            return mapPersistenceException((PersistenceException) e);
        }

        // Spring Transaction Exceptions
        if (e instanceof TransactionException) {
            return mapTransactionException((TransactionException) e);
        }

        // Raw SQL Exceptions
        if (e instanceof SQLException) {
            return mapSqlException((SQLException) e);
        }

        // Unknown exception
        return new DatabaseSystemException("Unexpected repository error: " + e.getMessage(), e);
    }

    private static ServiceException mapDataAccessException(DataAccessException e) {
        // Constraint violations and duplicates
        if (e instanceof DuplicateKeyException) {
            return mapConstraintViolation(e.getMessage(), e);
        }

        if (e instanceof DataIntegrityViolationException) {
            return mapConstraintViolation(e.getMessage(), e);
        }

        // Not found scenarios
        if (e instanceof EmptyResultDataAccessException) {
            return new CustomerNotFoundException("No result found for query");
        }

        if (e instanceof IncorrectResultSizeDataAccessException) {
            return new CustomerNotFoundException("Query returned unexpected number of results");
        }

        // Optimistic locking (check specific type first)
        if (e instanceof ObjectOptimisticLockingFailureException) {
            return new OptimisticLockingException("Concurrent modification detected", e);
        }

        if (e instanceof OptimisticLockingFailureException) {
            return new OptimisticLockingException("Optimistic locking conflict", e);
        }

        // Connection issues
        if (e instanceof DataAccessResourceFailureException) {
            return new DatabaseConnectionException("Database resource failure", e);
        }

        if (e instanceof TransientDataAccessResourceException) {
            return new DatabaseConnectionException("Transient database connection issue", e);
        }

        // Timeout issues
        if (e instanceof org.springframework.dao.QueryTimeoutException) {
            return new DatabaseTimeoutException("Query execution timeout", e);
        }

        if (e instanceof TransientDataAccessException) {
            return new DatabaseTimeoutException("Transient database access timeout", e);
        }

        // Permission and SQL issues
        if (e instanceof PermissionDeniedDataAccessException) {
            return new DatabaseSystemException("Database permission denied", e);
        }

        if (e instanceof InvalidDataAccessApiUsageException) {
            return new DatabaseSystemException("Invalid data access API usage", e);
        }

        // Check for underlying constraint violations in wrapped exceptions
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && (causeMessage.contains("unique") || causeMessage.contains("duplicate"))) {
                return mapConstraintViolation(causeMessage, e);
            }
        }

        return new DatabaseSystemException("Data access error: " + e.getMessage(), e);
    }

    private static ServiceException mapPersistenceException(PersistenceException e) {
        if (e instanceof EntityNotFoundException) {
            return mapEntityNotFound(e.getMessage());
        }

        if (e instanceof EntityExistsException) {
            return mapConstraintViolation(e.getMessage(), e);
        }

        if (e instanceof OptimisticLockException) {
            return new OptimisticLockingException("JPA optimistic lock conflict", e);
        }

        if (e instanceof LockTimeoutException) {
            return new DatabaseTimeoutException("JPA lock timeout", e);
        }

        if (e instanceof jakarta.persistence.QueryTimeoutException) {
            return new DatabaseTimeoutException("JPA query timeout", e);
        }

        if (e instanceof TransactionRequiredException) {
            return new TransactionRollbackException("Transaction required but not active", e);
        }

        if (e instanceof RollbackException) {
            return new TransactionRollbackException("Transaction rolled back", e);
        }

        return new DatabaseSystemException("JPA persistence error: " + e.getMessage(), e);
    }

    private static ServiceException mapTransactionException(TransactionException e) {
        if (e instanceof TransactionTimedOutException) {
            return new DatabaseTimeoutException("Transaction timeout", e);
        }

        return new TransactionRollbackException("Transaction error: " + e.getMessage(), e);
    }

    private static ServiceException mapSqlException(SQLException e) {
        String sqlState = e.getSQLState();

        if (sqlState == null) {
            return new DatabaseSystemException("SQL error: " + e.getMessage(), e);
        }

        return switch (sqlState) {
            case "23505" -> mapConstraintViolation(e.getMessage(), e); // Unique violation
            case "23503", "23502" -> new DatabaseSystemException("Data integrity violation", e);
            case "08006", "08000", "53300" -> new DatabaseConnectionException("Database connection error", e);
            case "25P01", "25P02" -> new TransactionRollbackException("Transaction state error", e);
            case "57014" -> new DatabaseTimeoutException("Query timeout", e);
            case "42601", "42P01", "42703" -> new DatabaseSystemException("SQL syntax error", e);
            default -> new DatabaseSystemException("SQL error (state: " + sqlState + "): " + e.getMessage(), e);
        };
    }

    private static ServiceException mapConstraintViolation(String message, Exception cause) {
        if (message == null) {
            return new DatabaseSystemException("Constraint violation", cause);
        }

        String lowerMessage = message.toLowerCase();

        // Check for your actual table unique constraints based on your schema

        // customer_emails table: email VARCHAR(50) NOT NULL UNIQUE
        if (lowerMessage.contains("customer_emails") && lowerMessage.contains("email")) {
            String email = extractValueFromParentheses(message);
            return new DuplicateEmailException(email != null ? email : "unknown", cause);
        }

        // customer_phones table: phone VARCHAR(13) NOT NULL UNIQUE
        if (lowerMessage.contains("customer_phones") && lowerMessage.contains("phone")) {
            String phone = extractValueFromParentheses(message);
            return new DuplicatePhoneException(phone != null ? phone : "unknown", cause);
        }

        // customer_accounts table: username VARCHAR(60) UNIQUE NOT NULL
        if (lowerMessage.contains("customer_accounts") && lowerMessage.contains("username")) {
            String username = extractUsernameFromMessage(message);
            return new DuplicateUsernameException(username != null ? username : "unknown", cause);
        }

        // NEW: password_reset_tokens table: uq_active_reset_token_per_user
        if (lowerMessage.contains("uq_active_reset_token_per_user")) {
            String accountId = extractValueFromParentheses(message);
            return new DuplicateActivePasswordResetTokenException(accountId != null ? "account_id: " + accountId : "unknown account", cause);
        }

        // otp_tokens table: check for any constraints (otp_code is not unique by design)
        if (lowerMessage.contains("otp_tokens")) {
            return new DatabaseSystemException("OTP token constraint violation: " + message, cause);
        }

        // Foreign key constraints
        if (lowerMessage.contains("fk_customer_email") || lowerMessage.contains("fk_customer_phone") ||
                lowerMessage.contains("fk_customer_account_customer") || lowerMessage.contains("fk_otp_customer")) {
            return new DatabaseSystemException("Foreign key constraint violation", cause);
        }

        // Generic unique constraint (fallback)
        if (lowerMessage.contains("unique") || lowerMessage.contains("duplicate")) {
            return new DatabaseSystemException("Unique constraint violation: " + message, cause);
        }

        return new DatabaseSystemException("Constraint violation: " + message, cause);
    }

    private static ServiceException mapEntityNotFound(String message) {
        if (message == null) {
            return new CustomerNotFoundException("Entity not found");
        }

        String lowerMessage = message.toLowerCase();

        // Map based on your actual entity types
        if (lowerMessage.contains("passwordresettoken")) {
            return new PasswordResetTokenNotFoundException(message);
        }
        if (lowerMessage.contains("customer") && !lowerMessage.contains("account")) {
            return new CustomerNotFoundException(message);
        }
        if (lowerMessage.contains("customer") && lowerMessage.contains("account")) {
            return new CustomerAccountNotFoundException(message);
        }
        if (lowerMessage.contains("email")) {
            return new EmailNotFoundException(message);
        }
        if (lowerMessage.contains("phone")) {
            return new PhoneNotFoundException(message);
        }
        if (lowerMessage.contains("otp") || lowerMessage.contains("token")) {
            return new OtpTokenNotFoundException(message);
        }

        return new CustomerNotFoundException("Entity not found: " + message);
    }

    private static String extractUsernameFromMessage(String message) {
        return extractValueFromParentheses(message);
    }

    private static String extractValueFromParentheses(String message) {
        // PostgreSQL typically formats unique violations as:
        // "duplicate key value violates unique constraint ... Key (column)=(value) already exists."
        if (message.contains("(") && message.contains(")")) {
            int start = message.lastIndexOf('(') + 1;
            int end = message.indexOf(')', start);
            if (end > start) {
                String value = message.substring(start, end).trim();
                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }
}

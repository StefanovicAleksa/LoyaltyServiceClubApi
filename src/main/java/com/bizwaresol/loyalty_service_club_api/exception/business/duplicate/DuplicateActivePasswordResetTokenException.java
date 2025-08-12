package com.bizwaresol.loyalty_service_club_api.exception.business.duplicate;

/**
 * A data-access-level exception thrown when the 'uq_active_reset_token_per_user'
 * unique constraint is violated in the database.
 * This indicates an attempt to create a new active password reset token for an
 * account that already has one.
 */
public class DuplicateActivePasswordResetTokenException extends DuplicateResourceException {

    public DuplicateActivePasswordResetTokenException(String accountIdentifier) {
        super("Attempted to create a new active password reset token for an account that already has one: " + accountIdentifier, "DUPLICATE_ACTIVE_PASSWORD_RESET_TOKEN");
    }

    public DuplicateActivePasswordResetTokenException(String accountIdentifier, Throwable cause) {
        super("Attempted to create a new active password reset token for an account that already has one: " + accountIdentifier, "DUPLICATE_ACTIVE_PASSWORD_RESET_TOKEN", cause);
    }
}

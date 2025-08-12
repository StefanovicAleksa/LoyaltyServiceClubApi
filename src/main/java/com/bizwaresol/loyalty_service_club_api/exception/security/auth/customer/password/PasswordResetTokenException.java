package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password;

import com.bizwaresol.loyalty_service_club_api.exception.business.BusinessLogicException;

/**
 * Abstract base class for exceptions related to the password reset process.
 * This allows for catching a general category of password reset errors.
 */
public abstract class PasswordResetTokenException extends BusinessLogicException {

    public PasswordResetTokenException(String message, String errorCode) {
        // Password reset token issues are client errors (e.g., bad token), so we use HTTP 400.
        super(message, errorCode, 400);
    }

    public PasswordResetTokenException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 400, cause);
    }
}

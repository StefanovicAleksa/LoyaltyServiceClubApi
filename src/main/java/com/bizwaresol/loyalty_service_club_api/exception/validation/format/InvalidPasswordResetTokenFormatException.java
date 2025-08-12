package com.bizwaresol.loyalty_service_club_api.exception.validation.format;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

/**
 * Thrown when a password reset token does not conform to the expected format (e.g., UUID).
 */
public class InvalidPasswordResetTokenFormatException extends ValidationException {

    public InvalidPasswordResetTokenFormatException() {
        super("Invalid password reset token format. Expected a UUID.", "INVALID_PASSWORD_RESET_TOKEN_FORMAT");
    }

    public InvalidPasswordResetTokenFormatException(Throwable cause) {
        super("Invalid password reset token format. Expected a UUID.", "INVALID_PASSWORD_RESET_TOKEN_FORMAT", cause);
    }
}

package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password;

/**
 * Thrown when an operation is attempted with a password reset token that has expired.
 */
public class PasswordResetTokenExpiredException extends PasswordResetTokenException {

    public PasswordResetTokenExpiredException() {
        super("The password reset token has expired. Please request a new one.", "PASSWORD_RESET_TOKEN_EXPIRED");
    }

    public PasswordResetTokenExpiredException(Throwable cause) {
        super("The password reset token has expired. Please request a new one.", "PASSWORD_RESET_TOKEN_EXPIRED", cause);
    }
}

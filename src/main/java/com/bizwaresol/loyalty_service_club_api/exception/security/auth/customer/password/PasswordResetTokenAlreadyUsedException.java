package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password;

/**
 * Thrown when an operation is attempted with a password reset token that has already been used.
 */
public class PasswordResetTokenAlreadyUsedException extends PasswordResetTokenException {

    public PasswordResetTokenAlreadyUsedException() {
        super("This password reset token has already been used.", "PASSWORD_RESET_TOKEN_ALREADY_USED");
    }

    public PasswordResetTokenAlreadyUsedException(Throwable cause) {
        super("This password reset token has already been used.", "PASSWORD_RESET_TOKEN_ALREADY_USED", cause);
    }
}

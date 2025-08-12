package com.bizwaresol.loyalty_service_club_api.exception.business.resource;

/**
 * Thrown when a specific PasswordResetToken cannot be found in the database.
 */
public class PasswordResetTokenNotFoundException extends ResourceNotFoundException {

    public PasswordResetTokenNotFoundException(Long tokenId) {
        super("Password reset token not found with ID: " + tokenId, "PASSWORD_RESET_TOKEN_NOT_FOUND");
    }

    public PasswordResetTokenNotFoundException(String token) {
        super("Password reset token not found for the provided value.", "PASSWORD_RESET_TOKEN_NOT_FOUND");
    }

    public PasswordResetTokenNotFoundException(Long tokenId, Throwable cause) {
        super("Password reset token not found with ID: " + tokenId, "PASSWORD_RESET_TOKEN_NOT_FOUND", cause);
    }

    public PasswordResetTokenNotFoundException(String token, Throwable cause) {
        super("Password reset token not found for the provided value.", "PASSWORD_RESET_TOKEN_NOT_FOUND", cause);
    }
}

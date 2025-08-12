package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password;

/**
 * Thrown when a user attempts to request a new password reset token
 * while they already have an active (unused) one. This is a business-level exception.
 */
public class ActivePasswordResetTokenExistsException extends PasswordResetTokenException {

  public ActivePasswordResetTokenExistsException() {
    // HTTP 409 Conflict is appropriate for this business rule violation.
    super("An active password reset request already exists for this account. Please check your contact method or wait for the previous token to expire.", "ACTIVE_PASSWORD_RESET_TOKEN_EXISTS");
  }

  public ActivePasswordResetTokenExistsException(Throwable cause) {
    super("An active password reset request already exists for this account. Please check your contact method or wait for the previous token to expire.", "ACTIVE_PASSWORD_RESET_TOKEN_EXISTS", cause);
  }
}

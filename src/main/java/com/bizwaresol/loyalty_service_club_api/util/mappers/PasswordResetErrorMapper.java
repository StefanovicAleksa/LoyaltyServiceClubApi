package com.bizwaresol.loyalty_service_club_api.util.mappers;

import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateActivePasswordResetTokenException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.ActivePasswordResetTokenExistsException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.PasswordResetTokenAlreadyUsedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.PasswordResetTokenExpiredException;

/**
 * A utility class to map exceptions and validate states related to the password reset process.
 */
public final class PasswordResetErrorMapper {

    private PasswordResetErrorMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Validates a PasswordResetToken's state (used or expired) and throws a specific
     * business exception if it is invalid for use.
     *
     * @param token The token to validate.
     * @throws PasswordResetTokenExpiredException if the token's expiration timestamp is in the past.
     * @throws PasswordResetTokenAlreadyUsedException if the token's used timestamp is not null.
     */
    public static void validateTokenState(PasswordResetToken token) {
        if (token.isExpired()) {
            throw new PasswordResetTokenExpiredException();
        }
        if (token.isUsed()) {
            throw new PasswordResetTokenAlreadyUsedException();
        }
    }

    /**
     * Maps the data-access-level DuplicateActivePasswordResetTokenException to the
     * business-level ActivePasswordResetTokenExistsException.
     *
     * @param e The original data-access exception.
     * @return The business-level exception to be thrown by the service layer.
     */
    public static ActivePasswordResetTokenExistsException mapDuplicateTokenException(DuplicateActivePasswordResetTokenException e) {
        return new ActivePasswordResetTokenExistsException(e);
    }
}

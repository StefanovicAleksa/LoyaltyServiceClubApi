// =====================================================================================
// FILE: src/main/java/com/bizwaresol/loyalty_service_club_api/util/mappers/AuthErrorMapper.java
// =====================================================================================
package com.bizwaresol.loyalty_service_club_api.util.mappers;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateEmailException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicatePhoneException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateUsernameException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.InvalidLoginCredentialsException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.ContactAlreadyRegisteredException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.RegistrationFailedException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public final class AuthErrorMapper {

    private AuthErrorMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Maps repository exceptions specifically for login operations
     */
    public static ServiceException mapToLoginException(Exception e, String identifier) {
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        if (e instanceof CustomerAccountNotFoundException) {
            return new InvalidLoginCredentialsException(identifier);
        }

        return new InvalidLoginCredentialsException(identifier, e);
    }

    /**
     * Maps repository exceptions specifically for registration operations
     */
    public static ServiceException mapToRegistrationException(Exception e) {
        // Let specific, intentional exceptions pass through without modification.
        if (e instanceof ValidationException || e instanceof RegistrationFailedException || e instanceof ContactAlreadyRegisteredException) {
            return (ServiceException) e;
        }

        // Translate specific duplicate exceptions into a business-friendly registration error.
        if (e instanceof DuplicateEmailException dupEmail) {
            String email = extractValueAfterColon(dupEmail.getMessage(), "unknown email");
            return new ContactAlreadyRegisteredException(email, "email");
        }
        if (e instanceof DuplicatePhoneException dupPhone) {
            String phone = extractValueAfterColon(dupPhone.getMessage(), "unknown phone");
            return new ContactAlreadyRegisteredException(phone, "phone");
        }
        if (e instanceof DuplicateUsernameException dupUsername) {
            String username = extractValueAfterColon(dupUsername.getMessage(), "unknown contact");
            return new ContactAlreadyRegisteredException(username, "contact information");
        }

        // For database or other unexpected errors, provide a generic registration failure message.
        if (e instanceof DatabaseSystemException) {
            return new RegistrationFailedException("Registration temporarily unavailable", e);
        }

        // Fallback for any other unknown error.
        return new RegistrationFailedException(e);
    }

    // ===== VALUE EXTRACTION HELPER =====

    private static String extractValueAfterColon(String message, String defaultValue) {
        if (message != null && message.contains(": ")) {
            String[] parts = message.split(": ", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        return defaultValue;
    }
}

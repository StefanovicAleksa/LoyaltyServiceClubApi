package com.bizwaresol.loyalty_service_club_api.util.mappers;

import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.*;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.*;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.*;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.*;
import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;

public final class AuthErrorMapper {

    private AuthErrorMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Maps repository and validation exceptions to authentication-specific exceptions
     * Usage: catch(Exception e) { throw CustomerAuthErrorMapper.mapToAuthException(e, context); }
     */
    public static ServiceException mapToAuthException(Exception e, String context) {
        // Handle validation exceptions first
        if (e instanceof ValidationException) {
            return mapValidationException((ValidationException) e, context);
        }

        // Handle duplicate resource exceptions (registration conflicts)
        if (e instanceof DuplicateResourceException) {
            return mapDuplicateException((DuplicateResourceException) e);
        }

        // Handle resource not found exceptions (login failures)
        if (e instanceof ResourceNotFoundException) {
            return mapNotFoundToAuthException((ResourceNotFoundException) e, context);
        }

        // Handle database system exceptions
        if (e instanceof DatabaseSystemException) {
            return mapDatabaseException((DatabaseSystemException) e, context);
        }

        // Handle already mapped service exceptions
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        // Unknown exception - wrap as authentication failure
        return new RegistrationFailedException("Unexpected error during " + context, e);
    }

    /**
     * Maps repository exceptions specifically for login operations
     */
    public static ServiceException mapToLoginException(Exception e, String identifier) {
        // Customer account not found -> invalid credentials (don't reveal account existence)
        if (e instanceof CustomerAccountNotFoundException) {
            return new InvalidLoginCredentialsException(identifier);
        }

        // Email/phone not found -> invalid credentials
        if (e instanceof EmailNotFoundException || e instanceof PhoneNotFoundException) {
            return new InvalidLoginCredentialsException(identifier);
        }

        // Customer not found -> invalid credentials
        if (e instanceof CustomerNotFoundException) {
            return new InvalidLoginCredentialsException(identifier);
        }

        // Validation errors during login -> invalid credentials
        if (e instanceof ValidationException) {
            return new InvalidLoginCredentialsException(identifier);
        }

        // Generic database error during login
        if (e instanceof DatabaseSystemException) {
            return new InvalidLoginCredentialsException("Login temporarily unavailable");
        }

        // Already mapped service exception
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        // Unknown login error
        return new InvalidLoginCredentialsException(identifier, e);
    }

    /**
     * Maps repository exceptions specifically for registration operations
     */
    public static ServiceException mapToRegistrationException(Exception e) {
        // Duplicate email/phone/username -> contact already registered
        if (e instanceof DuplicateEmailException dupEmail) {
            String email = extractEmailFromMessage(dupEmail.getMessage());
            return new ContactAlreadyRegisteredException(email, "email");
        }

        if (e instanceof DuplicatePhoneException dupPhone) {
            String phone = extractPhoneFromMessage(dupPhone.getMessage());
            return new ContactAlreadyRegisteredException(phone, "phone");
        }

        if (e instanceof DuplicateUsernameException dupUsername) {
            String username = extractUsernameFromMessage(dupUsername.getMessage());
            return new ContactAlreadyRegisteredException(username, "contact information");
        }

        // Validation errors during registration
        if (e instanceof ValidationException) {
            return new RegistrationFailedException("Invalid registration data: " + e.getMessage());
        }

        // Database errors during registration
        if (e instanceof DatabaseSystemException) {
            return new RegistrationFailedException("Registration temporarily unavailable", e);
        }

        // Already mapped service exception
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        // Unknown registration error
        return new RegistrationFailedException(e);
    }

    // ===== PRIVATE HELPER METHODS =====

    private static ServiceException mapValidationException(ValidationException e, String context) {
        if ("login".equals(context)) {
            return new InvalidLoginCredentialsException();
        } else if ("registration".equals(context)) {
            return new RegistrationFailedException("Invalid input: " + e.getMessage());
        }
        return e; // Return original validation exception
    }

    private static ServiceException mapDuplicateException(DuplicateResourceException e) {
        if (e instanceof DuplicateEmailException) {
            String email = extractEmailFromMessage(e.getMessage());
            return new ContactAlreadyRegisteredException(email, "email");
        } else if (e instanceof DuplicatePhoneException) {
            String phone = extractPhoneFromMessage(e.getMessage());
            return new ContactAlreadyRegisteredException(phone, "phone");
        } else if (e instanceof DuplicateUsernameException) {
            String username = extractUsernameFromMessage(e.getMessage());
            return new ContactAlreadyRegisteredException(username, "contact information");
        }
        return new RegistrationFailedException("Contact information already registered", e);
    }

    private static ServiceException mapNotFoundToAuthException(ResourceNotFoundException e, String context) {
        if ("login".equals(context)) {
            return new InvalidLoginCredentialsException();
        }
        return e; // Return original for other contexts
    }

    private static ServiceException mapDatabaseException(DatabaseSystemException e, String context) {
        if ("login".equals(context)) {
            return new InvalidLoginCredentialsException("Login temporarily unavailable");
        } else if ("registration".equals(context)) {
            return new RegistrationFailedException("Registration temporarily unavailable", e);
        }
        return e; // Return original for other contexts
    }

    // ===== VALUE EXTRACTION HELPERS =====

    private static String extractEmailFromMessage(String message) {
        return extractValueAfterColon(message, "unknown email");
    }

    private static String extractPhoneFromMessage(String message) {
        return extractValueAfterColon(message, "unknown phone");
    }

    private static String extractUsernameFromMessage(String message) {
        return extractValueAfterColon(message, "unknown contact");
    }

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
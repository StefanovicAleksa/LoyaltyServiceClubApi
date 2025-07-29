package com.bizwaresol.loyalty_service_club_api.util.validators;

import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.LoginRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.RegistrationRequest;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.MissingContactInformationException;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.InvalidCharacterException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.BusinessEmailNotAllowedException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.security.PasswordValidationException;

public final class AuthValidator {

    private AuthValidator() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    // ===== LOGIN VALIDATION =====

    /**
     * Validates login request data
     * @param request the login request to validate
     * @throws NullFieldException if request or required fields are null
     * @throws EmptyFieldException if required fields are empty
     */
    public static void validateLoginRequest(LoginRequest request) {
        DataValidator.checkNotNull(request, "loginRequest");
        DataValidator.checkNotNull(request.identifier(), "identifier");
        DataValidator.checkNotEmptyString(request.identifier(), "identifier");
        DataValidator.checkNotNull(request.password(), "password");
        DataValidator.checkNotEmptyString(request.password(), "password");
    }

    // ===== REGISTRATION VALIDATION =====

    /**
     * Validates registration request data including contact information requirements
     * @param request the registration request to validate
     * @throws NullFieldException if request or required fields are null
     * @throws EmptyFieldException if required fields are empty
     * @throws FieldTooShortException if fields are too short
     * @throws FieldTooLongException if fields are too long
     * @throws InvalidCharacterException if names contain invalid characters
     * @throws InvalidEmailFormatException if email format is invalid
     * @throws BusinessEmailNotAllowedException if email domain is not allowed
     * @throws InvalidPhoneFormatException if phone format is invalid
     * @throws PasswordValidationException if password doesn't meet requirements
     * @throws MissingContactInformationException if no contact information provided
     */
    public static void validateRegistrationRequest(RegistrationRequest request) {
        DataValidator.checkNotNull(request, "registrationRequest");

        // Validate names using existing DataValidator
        DataValidator.validateName(request.firstName(), "firstName");
        DataValidator.validateName(request.lastName(), "lastName");

        // Validate password using existing DataValidator
        DataValidator.validatePassword(request.password(), "password");

        // Validate contact information (authentication-specific logic)
        validateContactInformation(request);
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Validates that at least one contact method is provided and validates format if present
     * @param request the registration request containing contact information
     * @throws MissingContactInformationException if no email or phone provided
     * @throws InvalidEmailFormatException if email format is invalid
     * @throws BusinessEmailNotAllowedException if email domain is not allowed
     * @throws InvalidPhoneFormatException if phone format is invalid
     */
    private static void validateContactInformation(RegistrationRequest request) {
        boolean hasEmail = hasValidString(request.email());
        boolean hasPhone = hasValidString(request.phone());

        // Check that at least one contact method is provided
        if (!hasEmail && !hasPhone) {
            throw new MissingContactInformationException();
        }

        // Validate email format if provided (using existing DataValidator)
        if (hasEmail) {
            DataValidator.validatePersonalEmail(request.email(), "email");
        }

        // Validate phone format if provided (using existing DataValidator)
        if (hasPhone) {
            DataValidator.validatePhone(request.phone(), "phone");
        }
    }

    /**
     * Checks if a string is not null and not empty after trimming
     * @param value the string to check
     * @return true if string has content, false otherwise
     */
    private static boolean hasValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
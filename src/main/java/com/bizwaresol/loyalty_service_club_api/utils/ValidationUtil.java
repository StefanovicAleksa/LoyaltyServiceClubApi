package com.bizwaresol.loyalty_service_club_api.utils;

import com.bizwaresol.loyalty_service_club_api.constants.EmailConstants;
import com.bizwaresol.loyalty_service_club_api.constants.ValidationConstants;
import com.bizwaresol.loyalty_service_club_api.exceptions.validation.field.*;
import com.bizwaresol.loyalty_service_club_api.exceptions.validation.format.*;
import com.bizwaresol.loyalty_service_club_api.exceptions.validation.security.*;

public final class ValidationUtil {

    private ValidationUtil() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    // ===== ATOMIC CHECKS (NO DEPENDENCIES) =====

    public static void checkNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new NullFieldException(fieldName);
        }
    }

    public static void checkNotEmptyString(String value, String fieldName) {
        if (value.trim().isEmpty()) {
            throw new EmptyFieldException(fieldName);
        }
    }

    public static void checkMinLength(String value, String fieldName, int minLength) {
        if (value.trim().length() < minLength) {
            throw new FieldTooShortException(fieldName, minLength, value.trim().length());
        }
    }

    public static void checkMaxLength(String value, String fieldName, int maxLength) {
        if (value.trim().length() > maxLength) {
            throw new FieldTooLongException(fieldName, maxLength, value.trim().length());
        }
    }

    public static void checkEmailPattern(String email) {
        if (!ValidationConstants.EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new InvalidEmailFormatException(email);
        }
    }

    public static void checkPhonePattern(String phone) {
        if (!ValidationConstants.PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new InvalidPhoneFormatException(phone + " (Expected format: +381xxxxxxxx)");
        }
    }

    public static void checkNamePattern(String name, String fieldName) {
        if (!ValidationConstants.NAME_PATTERN.matcher(name.trim()).matches()) {
            throw new InvalidCharacterException(fieldName, "Only letters, spaces, hyphens, and apostrophes are allowed");
        }
    }

    public static void checkPasswordPattern(String password) {
        if (!ValidationConstants.PASSWORD_DIGIT_PATTERN.matcher(password).matches()) {
            throw new PasswordValidationException(ValidationConstants.PASSWORD_REQUIREMENTS);
        }
    }

    public static void checkPersonalEmailDomain(String email) {
        String domain = extractDomain(email);
        if (!EmailConstants.ALLOWED_PERSONAL_EMAIL_DOMAINS.contains(domain.toLowerCase())) {
            throw new BusinessEmailNotAllowedException(domain, EmailConstants.ALLOWED_DOMAINS_MESSAGE);
        }
    }

    private static String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex > 0 && atIndex < email.length() - 1) {
            return email.substring(atIndex + 1);
        }
        return "";
    }

    // ===== MODULAR FIELD VALIDATION =====

    public static void checkField(String value, String fieldName, int minLength, int maxLength) {
        checkNotNull(value, fieldName);
        checkNotEmptyString(value, fieldName);
        checkMinLength(value, fieldName, minLength);
        checkMaxLength(value, fieldName, maxLength);
    }

    public static void checkEmailDomain(String email) {
        checkEmailPattern(email);
        checkPersonalEmailDomain(email);
    }

    // ===== COMPOSITE VALIDATION (CONTROLLED CALL ORDER) =====

    public static void validateEmail(String email, String fieldName) {
        checkField(email, fieldName, ValidationConstants.MIN_EMAIL_LENGTH, ValidationConstants.MAX_EMAIL_LENGTH);
        checkEmailPattern(email);
    }

    public static void validatePersonalEmail(String email, String fieldName) {
        checkField(email, fieldName, ValidationConstants.MIN_EMAIL_LENGTH, ValidationConstants.MAX_EMAIL_LENGTH);
        checkEmailDomain(email);
    }

    public static void validatePhone(String phone, String fieldName) {
        checkField(phone, fieldName, ValidationConstants.MIN_PHONE_LENGTH, ValidationConstants.MAX_PHONE_LENGTH);
        checkPhonePattern(phone);
    }

    public static void validatePassword(String password, String fieldName) {
        checkField(password, fieldName, ValidationConstants.MIN_PASSWORD_LENGTH, ValidationConstants.MAX_PASSWORD_LENGTH);
        checkPasswordPattern(password);
    }

    public static void validateName(String name, String fieldName) {
        checkField(name, fieldName, ValidationConstants.MIN_NAME_LENGTH, ValidationConstants.MAX_NAME_LENGTH);
        checkNamePattern(name, fieldName);
    }

    public static void validateUsername(String username, String fieldName) {
        checkField(username, fieldName, ValidationConstants.MIN_USERNAME_LENGTH, ValidationConstants.MAX_USERNAME_LENGTH);
    }
}
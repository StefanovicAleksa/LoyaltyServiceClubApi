package com.bizwaresol.loyalty_service_club_api.domain.enums;

import com.bizwaresol.loyalty_service_club_api.constant.ValidationConstants;

public enum CustomerAccountIdentifierType {
    EMAIL,
    PHONE,
    USERNAME;

    /**
     * Determines the identifier type based on format patterns
     * @param identifier the identifier string to analyze
     * @return the determined identifier type
     */
    public static CustomerAccountIdentifierType fromIdentifier(String identifier) {
        if (identifier == null) {
            return USERNAME; // Default fallback
        }

        String trimmed = identifier.trim();

        if (ValidationConstants.EMAIL_PATTERN.matcher(trimmed).matches()) {
            return EMAIL;
        } else if (ValidationConstants.PHONE_PATTERN.matcher(trimmed).matches()) {
            return PHONE;
        }

        // For invalid format, we'll still try username lookup and let it fail naturally
        return USERNAME;
    }
}
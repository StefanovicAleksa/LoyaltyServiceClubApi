package com.bizwaresol.loyalty_service_club_api.exception.validation.format;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public class BusinessEmailNotAllowedException extends ValidationException {
    public BusinessEmailNotAllowedException(String domain, String allowedDomains) {
        super("Business email domain '" + domain + "' not allowed. Please use a personal email from: " + allowedDomains,
                "BUSINESS_EMAIL_NOT_ALLOWED");
    }

    public BusinessEmailNotAllowedException(String domain, String allowedDomains, Throwable cause) {
        super("Business email domain '" + domain + "' not allowed. Please use a personal email from: " + allowedDomains,
                "BUSINESS_EMAIL_NOT_ALLOWED", cause);
    }
}
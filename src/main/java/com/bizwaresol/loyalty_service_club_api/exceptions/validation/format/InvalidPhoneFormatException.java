package com.bizwaresol.loyalty_service_club_api.exceptions.validation.format;

import com.bizwaresol.loyalty_service_club_api.exceptions.validation.ValidationException;

public class InvalidPhoneFormatException extends ValidationException {
    public InvalidPhoneFormatException(String phone) {
        super("Invalid phone format: " + phone, "INVALID_PHONE_FORMAT");
    }

    public InvalidPhoneFormatException(String phone, Throwable cause) {
        super("Invalid phone format: " + phone, "INVALID_PHONE_FORMAT", cause);
    }
}

package com.bizwaresol.loyalty_service_club_api.exceptions.business.duplicate;

public class DuplicatePhoneException extends DuplicateResourceException {

    public DuplicatePhoneException(String phone) {
        super("Phone already exists: " + phone, "DUPLICATE_PHONE");
    }

    public DuplicatePhoneException(String phone, Throwable cause) {
        super("Phone already exists: " + phone, "DUPLICATE_PHONE", cause);
    }
}
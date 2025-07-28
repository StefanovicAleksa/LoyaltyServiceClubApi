package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration;

public class ContactAlreadyRegisteredException extends RegistrationException {

    public ContactAlreadyRegisteredException(String contactInfo, String contactType) {
        super("Contact already registered: " + contactInfo + " (" + contactType + ")",
                "CONTACT_ALREADY_REGISTERED");
    }

    public ContactAlreadyRegisteredException(String contactInfo, String contactType, Throwable cause) {
        super("Contact already registered: " + contactInfo + " (" + contactType + ")",
                "CONTACT_ALREADY_REGISTERED", cause);
    }
}
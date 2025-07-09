package com.bizwaresol.loyalty_service_club_api.exception.business.resource;

public class EmailNotFoundException extends ResourceNotFoundException {

    public EmailNotFoundException(Long emailId) {
        super("Email not found with ID: " + emailId, "EMAIL_NOT_FOUND");
    }

    public EmailNotFoundException(String email) {
        super("Email not found: " + email, "EMAIL_NOT_FOUND");
    }

    public EmailNotFoundException(Long emailId, Throwable cause) {
        super("Email not found with ID: " + emailId, "EMAIL_NOT_FOUND", cause);
    }

    public EmailNotFoundException(String email, Throwable cause) {
        super("Email not found: " + email, "EMAIL_NOT_FOUND", cause);
    }
}
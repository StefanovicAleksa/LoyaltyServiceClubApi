package com.bizwaresol.loyalty_service_club_api.exceptions.business.resource;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(Long customerId) {
        super("Customer not found with ID: " + customerId, "CUSTOMER_NOT_FOUND");
    }

    public CustomerNotFoundException(String identifier) {
        super("Customer not found: " + identifier, "CUSTOMER_NOT_FOUND");
    }

    public CustomerNotFoundException(Long customerId, Throwable cause) {
        super("Customer not found with ID: " + customerId, "CUSTOMER_NOT_FOUND", cause);
    }

    public CustomerNotFoundException(String identifier, Throwable cause) {
        super("Customer not found: " + identifier, "CUSTOMER_NOT_FOUND", cause);
    }
}
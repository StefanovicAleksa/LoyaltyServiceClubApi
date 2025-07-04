package com.bizwaresol.loyalty_service_club_api.exceptions.business.resource;

public class CustomerAccountNotFoundException extends ResourceNotFoundException {

  public CustomerAccountNotFoundException(Long accountId) {
    super("Customer account not found with ID: " + accountId, "CUSTOMER_ACCOUNT_NOT_FOUND");
  }

  public CustomerAccountNotFoundException(String identifier) {
    super("Customer account not found: " + identifier, "CUSTOMER_ACCOUNT_NOT_FOUND");
  }

  public CustomerAccountNotFoundException(Long accountId, Throwable cause) {
    super("Customer account not found with ID: " + accountId, "CUSTOMER_ACCOUNT_NOT_FOUND", cause);
  }

  public CustomerAccountNotFoundException(String identifier, Throwable cause) {
    super("Customer account not found: " + identifier, "CUSTOMER_ACCOUNT_NOT_FOUND", cause);
  }
}
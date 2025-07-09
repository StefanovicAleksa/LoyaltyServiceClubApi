package com.bizwaresol.loyalty_service_club_api.exception.business.resource;

public class PhoneNotFoundException extends ResourceNotFoundException {

  public PhoneNotFoundException(Long phoneId) {
    super("Phone not found with ID: " + phoneId, "PHONE_NOT_FOUND");
  }

  public PhoneNotFoundException(String phone) {
    super("Phone not found: " + phone, "PHONE_NOT_FOUND");
  }

  public PhoneNotFoundException(Long phoneId, Throwable cause) {
    super("Phone not found with ID: " + phoneId, "PHONE_NOT_FOUND", cause);
  }

  public PhoneNotFoundException(String phone, Throwable cause) {
    super("Phone not found: " + phone, "PHONE_NOT_FOUND", cause);
  }
}
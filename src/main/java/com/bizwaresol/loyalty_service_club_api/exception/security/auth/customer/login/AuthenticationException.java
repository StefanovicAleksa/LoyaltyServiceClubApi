package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class AuthenticationException extends ServiceException {

  public AuthenticationException(String message, String errorCode) {
    super(message, errorCode, 401);
  }

  public AuthenticationException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, 401, cause);
  }
}
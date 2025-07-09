package com.bizwaresol.loyalty_service_club_api.exception.base;

public abstract class ServiceException extends RuntimeException {

  private final String errorCode;
  private final int httpStatus;

  public ServiceException(String message, String errorCode, int httpStatus) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  public ServiceException(String message, String errorCode, int httpStatus, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String toString() {
    return "ServiceException{" +
            "message='" + getMessage() + '\'' +
            ", errorCode='" + errorCode + '\'' +
            ", httpStatus=" + httpStatus +
            '}';
  }
}
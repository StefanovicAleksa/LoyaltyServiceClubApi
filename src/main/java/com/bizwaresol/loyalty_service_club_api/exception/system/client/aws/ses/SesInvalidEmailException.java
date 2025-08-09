package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses;

public class SesInvalidEmailException extends SesClientException {

  public SesInvalidEmailException(String email, String awsErrorCode, String awsRequestId) {
    super("Invalid email address: " + email, "SES_INVALID_EMAIL", awsErrorCode, awsRequestId);
  }

  public SesInvalidEmailException(String email, String awsErrorCode, String awsRequestId, Throwable cause) {
    super("Invalid email address: " + email, "SES_INVALID_EMAIL", awsErrorCode, awsRequestId, cause);
  }
}
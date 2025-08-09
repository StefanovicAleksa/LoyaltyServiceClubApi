package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns;

public class SnsOptedOutException extends SnsClientException {

  public SnsOptedOutException(String phoneNumber, String awsErrorCode, String awsRequestId) {
    super("Phone number has opted out of SMS: " + phoneNumber, "SNS_OPTED_OUT", awsErrorCode, awsRequestId);
  }

  public SnsOptedOutException(String phoneNumber, String awsErrorCode, String awsRequestId, Throwable cause) {
    super("Phone number has opted out of SMS: " + phoneNumber, "SNS_OPTED_OUT", awsErrorCode, awsRequestId, cause);
  }
}
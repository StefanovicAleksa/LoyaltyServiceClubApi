package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns;

public class SnsThrottlingException extends SnsClientException {

  public SnsThrottlingException(String awsErrorCode, String awsRequestId) {
    super("SNS request was throttled. Please retry after some time.", "SNS_THROTTLING", awsErrorCode, awsRequestId);
  }

  public SnsThrottlingException(String awsErrorCode, String awsRequestId, Throwable cause) {
    super("SNS request was throttled. Please retry after some time.", "SNS_THROTTLING", awsErrorCode, awsRequestId, cause);
  }
}
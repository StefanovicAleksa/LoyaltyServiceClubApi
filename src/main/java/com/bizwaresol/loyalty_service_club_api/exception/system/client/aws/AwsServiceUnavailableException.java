package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws;

public class AwsServiceUnavailableException extends AwsClientException {

  public AwsServiceUnavailableException(String serviceName, String awsErrorCode, String awsRequestId) {
    super("AWS " + serviceName + " service is temporarily unavailable. Please try again later.",
            "AWS_SERVICE_UNAVAILABLE", awsErrorCode, awsRequestId);
  }

  public AwsServiceUnavailableException(String serviceName, String awsErrorCode, String awsRequestId, Throwable cause) {
    super("AWS " + serviceName + " service is temporarily unavailable. Please try again later.",
            "AWS_SERVICE_UNAVAILABLE", awsErrorCode, awsRequestId, cause);
  }
}
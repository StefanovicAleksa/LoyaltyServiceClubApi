package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws;

import com.bizwaresol.loyalty_service_club_api.exception.system.client.ClientSystemException;

public abstract class AwsClientException extends ClientSystemException {

  private final String awsErrorCode;
  private final String awsRequestId;

  public AwsClientException(String message, String errorCode, String awsErrorCode, String awsRequestId) {
    super(message, errorCode);
    this.awsErrorCode = awsErrorCode;
    this.awsRequestId = awsRequestId;
  }

  public AwsClientException(String message, String errorCode, String awsErrorCode, String awsRequestId, Throwable cause) {
    super(message, errorCode, cause);
    this.awsErrorCode = awsErrorCode;
    this.awsRequestId = awsRequestId;
  }

  public String getAwsErrorCode() {
    return awsErrorCode;
  }

  public String getAwsRequestId() {
    return awsRequestId;
  }

  @Override
  public String toString() {
    return super.toString() +
            ", awsErrorCode='" + awsErrorCode + '\'' +
            ", awsRequestId='" + awsRequestId + '\'';
  }
}
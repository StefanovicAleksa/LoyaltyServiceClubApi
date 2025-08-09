package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws;

public class AwsAuthenticationException extends AwsClientException {

    public AwsAuthenticationException(String awsErrorCode, String awsRequestId) {
        super("AWS authentication failed. Please check credentials.", "AWS_AUTHENTICATION_FAILED", awsErrorCode, awsRequestId);
    }

    public AwsAuthenticationException(String awsErrorCode, String awsRequestId, Throwable cause) {
        super("AWS authentication failed. Please check credentials.", "AWS_AUTHENTICATION_FAILED", awsErrorCode, awsRequestId, cause);
    }
}
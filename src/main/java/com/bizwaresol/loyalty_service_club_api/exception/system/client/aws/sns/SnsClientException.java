package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns;

import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.AwsClientException;

public abstract class SnsClientException extends AwsClientException {

    public SnsClientException(String message, String errorCode, String awsErrorCode, String awsRequestId) {
        super(message, errorCode, awsErrorCode, awsRequestId);
    }

    public SnsClientException(String message, String errorCode, String awsErrorCode, String awsRequestId, Throwable cause) {
        super(message, errorCode, awsErrorCode, awsRequestId, cause);
    }
}
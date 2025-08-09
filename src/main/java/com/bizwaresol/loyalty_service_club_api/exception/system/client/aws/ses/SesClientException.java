package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses;

import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.AwsClientException;

public abstract class SesClientException extends AwsClientException {

    public SesClientException(String message, String errorCode, String awsErrorCode, String awsRequestId) {
        super(message, errorCode, awsErrorCode, awsRequestId);
    }

    public SesClientException(String message, String errorCode, String awsErrorCode, String awsRequestId, Throwable cause) {
        super(message, errorCode, awsErrorCode, awsRequestId, cause);
    }
}
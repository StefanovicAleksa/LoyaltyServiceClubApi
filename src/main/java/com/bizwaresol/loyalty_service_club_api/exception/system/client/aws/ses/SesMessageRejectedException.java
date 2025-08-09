package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses;

public class SesMessageRejectedException extends SesClientException {

    public SesMessageRejectedException(String reason, String awsErrorCode, String awsRequestId) {
        super("SES message rejected: " + reason, "SES_MESSAGE_REJECTED", awsErrorCode, awsRequestId);
    }

    public SesMessageRejectedException(String reason, String awsErrorCode, String awsRequestId, Throwable cause) {
        super("SES message rejected: " + reason, "SES_MESSAGE_REJECTED", awsErrorCode, awsRequestId, cause);
    }
}
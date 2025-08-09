package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses;

public class SesQuotaExceededException extends SesClientException {

    public SesQuotaExceededException(String awsErrorCode, String awsRequestId) {
        super("SES sending quota exceeded. Please try again later.", "SES_QUOTA_EXCEEDED", awsErrorCode, awsRequestId);
    }

    public SesQuotaExceededException(String awsErrorCode, String awsRequestId, Throwable cause) {
        super("SES sending quota exceeded. Please try again later.", "SES_QUOTA_EXCEEDED", awsErrorCode, awsRequestId, cause);
    }
}
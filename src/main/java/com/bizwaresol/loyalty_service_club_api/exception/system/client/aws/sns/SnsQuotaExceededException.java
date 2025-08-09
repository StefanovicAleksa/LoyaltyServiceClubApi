package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns;

public class SnsQuotaExceededException extends SnsClientException {

    public SnsQuotaExceededException(String awsErrorCode, String awsRequestId) {
        super("SNS SMS spending limit exceeded. Please try again later.", "SNS_QUOTA_EXCEEDED", awsErrorCode, awsRequestId);
    }

    public SnsQuotaExceededException(String awsErrorCode, String awsRequestId, Throwable cause) {
        super("SNS SMS spending limit exceeded. Please try again later.", "SNS_QUOTA_EXCEEDED", awsErrorCode, awsRequestId, cause);
    }
}
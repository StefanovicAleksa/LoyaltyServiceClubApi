package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns;

public class SnsInvalidPhoneException extends SnsClientException {

    public SnsInvalidPhoneException(String phoneNumber, String awsErrorCode, String awsRequestId) {
        super("Invalid phone number: " + phoneNumber, "SNS_INVALID_PHONE", awsErrorCode, awsRequestId);
    }

    public SnsInvalidPhoneException(String phoneNumber, String awsErrorCode, String awsRequestId, Throwable cause) {
        super("Invalid phone number: " + phoneNumber, "SNS_INVALID_PHONE", awsErrorCode, awsRequestId, cause);
    }
}
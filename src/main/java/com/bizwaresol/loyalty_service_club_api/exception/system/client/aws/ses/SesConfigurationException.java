package com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses;

public class SesConfigurationException extends SesClientException {

    public SesConfigurationException(String message, String awsErrorCode, String awsRequestId) {
        super("SES configuration error: " + message, "SES_CONFIGURATION_ERROR", awsErrorCode, awsRequestId);
    }

    public SesConfigurationException(String message, String awsErrorCode, String awsRequestId, Throwable cause) {
        super("SES configuration error: " + message, "SES_CONFIGURATION_ERROR", awsErrorCode, awsRequestId, cause);
    }
}
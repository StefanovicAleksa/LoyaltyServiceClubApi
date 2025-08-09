package com.bizwaresol.loyalty_service_club_api.util.mappers;

import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.*;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses.*;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.*;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.model.SesException;
import software.amazon.awssdk.services.sns.model.SnsException;

public final class AwsErrorMapper {

    private AwsErrorMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Maps AWS exceptions to service layer exceptions
     * Usage: catch(Exception e) { throw AwsErrorMapper.mapException(e); }
     */
    public static ServiceException mapException(Exception e) {
        // SES specific exceptions
        if (e instanceof SesException) {
            return mapSesException((SesException) e);
        }

        // SNS specific exceptions
        if (e instanceof SnsException) {
            return mapSnsException((SnsException) e);
        }

        // Common AWS SDK exceptions
        if (e instanceof AwsServiceException) {
            return mapAwsServiceException((AwsServiceException) e);
        }

        if (e instanceof SdkClientException) {
            return mapSdkClientException((SdkClientException) e);
        }

        // Unknown exception - wrap as generic AWS client error
        return new AwsServiceUnavailableException("Unknown", null, null, e);
    }

    private static ServiceException mapSesException(SesException e) {
        String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
        String requestId = e.requestId();

        if (errorCode == null) {
            return new SesConfigurationException(e.getMessage(), null, requestId, e);
        }

        return switch (errorCode) {
            case "InvalidParameterValue", "InvalidParameter" -> {
                if (e.getMessage().toLowerCase().contains("email")) {
                    yield new SesInvalidEmailException("unknown", errorCode, requestId, e);
                }
                yield new SesConfigurationException(e.getMessage(), errorCode, requestId, e);
            }
            case "SendingPausedException", "MaxSendRateExceeded", "DailyQuotaExceeded" ->
                    new SesQuotaExceededException(errorCode, requestId, e);
            case "MessageRejected", "MailFromDomainNotVerifiedException" ->
                    new SesMessageRejectedException(e.getMessage(), errorCode, requestId, e);
            case "ConfigurationSetDoesNotExistException", "TemplateDoesNotExistException" ->
                    new SesConfigurationException(e.getMessage(), errorCode, requestId, e);
            default -> new SesConfigurationException("Unhandled SES error: " + e.getMessage(), errorCode, requestId, e);
        };
    }

    private static ServiceException mapSnsException(SnsException e) {
        String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
        String requestId = e.requestId();

        if (errorCode == null) {
            return new AwsServiceUnavailableException("SNS", null, requestId, e);
        }

        return switch (errorCode) {
            case "InvalidParameter", "InvalidParameterValue" -> {
                if (e.getMessage().toLowerCase().contains("phone")) {
                    yield new SnsInvalidPhoneException("unknown", errorCode, requestId, e);
                }
                yield new AwsServiceUnavailableException("SNS", errorCode, requestId, e);
            }
            case "OptedOut" -> new SnsOptedOutException("unknown", errorCode, requestId, e);
            case "Throttling", "TooManyRequestsException" -> new SnsThrottlingException(errorCode, requestId, e);
            case "MessageTooLong" -> new AwsServiceUnavailableException("SNS", errorCode, requestId, e);
            default -> new AwsServiceUnavailableException("SNS", errorCode, requestId, e);
        };
    }

    private static ServiceException mapAwsServiceException(AwsServiceException e) {
        String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
        String requestId = e.requestId();

        if (errorCode == null) {
            return new AwsServiceUnavailableException("AWS", null, requestId, e);
        }

        return switch (errorCode) {
            case "UnauthorizedOperation", "InvalidUserID.NotFound", "AuthFailure", "SignatureDoesNotMatch" ->
                    new AwsAuthenticationException(errorCode, requestId, e);
            case "RequestLimitExceeded", "Throttling", "TooManyRequestsException",
                 "ServiceUnavailable", "InternalError", "ServiceFailure" ->
                    new AwsServiceUnavailableException("AWS", errorCode, requestId, e);
            default -> new AwsServiceUnavailableException("AWS", errorCode, requestId, e);
        };
    }

    private static ServiceException mapSdkClientException(SdkClientException e) {
        // Client-side errors (network, configuration, etc.)
        return new AwsServiceUnavailableException("AWS", "CLIENT_ERROR", null, e);
    }
}
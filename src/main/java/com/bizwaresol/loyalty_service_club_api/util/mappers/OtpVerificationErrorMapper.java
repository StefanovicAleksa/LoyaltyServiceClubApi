// =====================================================================================
// FILE: src/main/java/com/bizwaresol/loyalty_service_club_api/util/mappers/OtpVerificationErrorMapper.java
// =====================================================================================
package com.bizwaresol.loyalty_service_club_api.util.mappers;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.EmailNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.OtpTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PhoneNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending.OtpDeliveryFailedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending.OtpSendingException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.InvalidOtpCodeException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.OtpNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.OtpVerificationException;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses.*;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.*;
import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public final class OtpVerificationErrorMapper {

    private OtpVerificationErrorMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Maps various exceptions to OTP verification-specific exceptions for SENDING operations
     */
    public static ServiceException mapToSendingException(Exception e, String contact, String deliveryMethod) {
        // Let our specific exceptions pass through
        if (e instanceof OtpSendingException || e instanceof ValidationException) {
            return (ServiceException) e;
        }

        // Handle resource not found for the contact method
        if (e instanceof EmailNotFoundException || e instanceof PhoneNotFoundException) {
            return new OtpDeliveryFailedException(contact, deliveryMethod, "Contact not found or not registered.", e);
        }

        // AWS client service exceptions (email/SMS delivery failures)
        if (e instanceof SesClientException) {
            return mapSesClientException((SesClientException) e, contact);
        }
        if (e instanceof SnsClientException) {
            return mapSnsClientException((SnsClientException) e, contact);
        }

        // Generic fallback for sending errors
        return new OtpDeliveryFailedException(contact, deliveryMethod, e);
    }

    /**
     * Maps various exceptions to OTP verification-specific exceptions for VERIFICATION operations
     */
    public static ServiceException mapToVerificationException(Exception e, String contact) {
        // Let our specific exceptions pass through
        if (e instanceof OtpVerificationException || e instanceof ValidationException) {
            return (ServiceException) e;
        }

        // OTP token not found from database -> specific OtpNotFoundException
        if (e instanceof OtpTokenNotFoundException) {
            return new OtpNotFoundException(contact, e);
        }

        // Generic fallback - treat as invalid OTP
        return new InvalidOtpCodeException(contact, e);
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Maps SES client exceptions to OTP delivery failures with specific reasons.
     */
    private static ServiceException mapSesClientException(SesClientException e, String contact) {
        String reason = "Email delivery service failed.";
        if (e instanceof SesInvalidEmailException) {
            reason = "Invalid email address provided.";
        } else if (e instanceof SesQuotaExceededException) {
            reason = "Email service quota exceeded. Please try again later.";
        } else if (e instanceof SesMessageRejectedException) {
            reason = "Email was rejected by the provider.";
        } else if (e instanceof SesConfigurationException) {
            reason = "Email service is currently misconfigured.";
        }
        return new OtpDeliveryFailedException(contact, "email", reason, e);
    }

    /**
     * Maps SNS client exceptions to OTP delivery failures with specific reasons.
     */
    private static ServiceException mapSnsClientException(SnsClientException e, String contact) {
        String reason = "SMS delivery service failed.";
        if (e instanceof SnsInvalidPhoneException) {
            reason = "Invalid phone number provided.";
        } else if (e instanceof SnsOptedOutException) {
            reason = "This phone number has opted out of receiving SMS messages.";
        } else if (e instanceof SnsQuotaExceededException) {
            reason = "SMS service quota exceeded. Please try again later.";
        } else if (e instanceof SnsThrottlingException) {
            reason = "SMS service is temporarily unavailable due to high traffic.";
        }
        return new OtpDeliveryFailedException(contact, "SMS", reason, e);
    }
}

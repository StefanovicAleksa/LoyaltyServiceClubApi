package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending;

import java.time.OffsetDateTime;

public class OtpRateLimitExceededException extends OtpSendingException {

    public OtpRateLimitExceededException(String contact, OffsetDateTime nextAllowedTime) {
        super("Rate limit exceeded for: " + contact + ". Next attempt allowed at: " + nextAllowedTime, "OTP_RATE_LIMIT_EXCEEDED");
    }

    public OtpRateLimitExceededException(String contact, int waitMinutes) {
        super("Rate limit exceeded for: " + contact + ". Please wait " + waitMinutes + " minutes before next attempt", "OTP_RATE_LIMIT_EXCEEDED");
    }

    public OtpRateLimitExceededException(String contact, OffsetDateTime nextAllowedTime, Throwable cause) {
        super("Rate limit exceeded for: " + contact + ". Next attempt allowed at: " + nextAllowedTime, "OTP_RATE_LIMIT_EXCEEDED", cause);
    }
}
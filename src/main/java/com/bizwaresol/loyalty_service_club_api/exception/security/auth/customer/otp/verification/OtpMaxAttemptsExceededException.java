package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification;

public class OtpMaxAttemptsExceededException extends OtpVerificationException {

    public OtpMaxAttemptsExceededException(String contact, int maxAttempts) {
        super("Maximum verification attempts exceeded for: " + contact + " (max: " + maxAttempts + ")", "OTP_MAX_ATTEMPTS_EXCEEDED");
    }

    public OtpMaxAttemptsExceededException(String contact, int maxAttempts, Throwable cause) {
        super("Maximum verification attempts exceeded for: " + contact + " (max: " + maxAttempts + ")", "OTP_MAX_ATTEMPTS_EXCEEDED", cause);
    }
}
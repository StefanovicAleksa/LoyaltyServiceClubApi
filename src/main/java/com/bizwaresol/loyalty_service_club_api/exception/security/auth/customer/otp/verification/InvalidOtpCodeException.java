package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification;

public class InvalidOtpCodeException extends OtpVerificationException {

    public InvalidOtpCodeException(String contact) {
        super("Invalid OTP code provided for: " + contact, "INVALID_OTP_CODE");
    }

    public InvalidOtpCodeException(String contact, Throwable cause) {
        super("Invalid OTP code provided for: " + contact, "INVALID_OTP_CODE", cause);
    }
}
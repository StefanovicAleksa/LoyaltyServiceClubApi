package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification;

public class OtpNotFoundException extends OtpVerificationException {

    public OtpNotFoundException(String contact) {
        super("No valid OTP found for: " + contact, "OTP_NOT_FOUND");
    }

    public OtpNotFoundException(String contact, Throwable cause) {
        super("No valid OTP found for: " + contact, "OTP_NOT_FOUND", cause);
    }
}
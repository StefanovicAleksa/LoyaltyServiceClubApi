package com.bizwaresol.loyalty_service_club_api.exception.business.resource;

public class OtpTokenNotFoundException extends ResourceNotFoundException {

    public OtpTokenNotFoundException(Long otpTokenId) {
        super("OTP token not found with ID: " + otpTokenId, "OTP_TOKEN_NOT_FOUND");
    }

    public OtpTokenNotFoundException(String identifier) {
        super("OTP token not found: " + identifier, "OTP_TOKEN_NOT_FOUND");
    }

    public OtpTokenNotFoundException(Long otpTokenId, Throwable cause) {
        super("OTP token not found with ID: " + otpTokenId, "OTP_TOKEN_NOT_FOUND", cause);
    }

    public OtpTokenNotFoundException(String identifier, Throwable cause) {
        super("OTP token not found: " + identifier, "OTP_TOKEN_NOT_FOUND", cause);
    }
}
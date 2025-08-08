package com.bizwaresol.loyalty_service_club_api.exception.validation.format;

import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;

public class InvalidOtpFormatException extends ValidationException {

    public InvalidOtpFormatException(String otpCode) {
        super("Invalid OTP format: " + otpCode + " (Expected: 6 digits)", "INVALID_OTP_FORMAT");
    }

    public InvalidOtpFormatException(String otpCode, Throwable cause) {
        super("Invalid OTP format: " + otpCode + " (Expected: 6 digits)", "INVALID_OTP_FORMAT", cause);
    }
}
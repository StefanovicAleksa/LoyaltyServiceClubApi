package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification;

import java.time.OffsetDateTime;

public class OtpAlreadyUsedException extends OtpVerificationException {

    public OtpAlreadyUsedException(String contact, OffsetDateTime usedAt) {
        super("OTP code has already been used for: " + contact + " (used at: " + usedAt + ")", "OTP_ALREADY_USED");
    }

    public OtpAlreadyUsedException(String contact, OffsetDateTime usedAt, Throwable cause) {
        super("OTP code has already been used for: " + contact + " (used at: " + usedAt + ")", "OTP_ALREADY_USED", cause);
    }
}
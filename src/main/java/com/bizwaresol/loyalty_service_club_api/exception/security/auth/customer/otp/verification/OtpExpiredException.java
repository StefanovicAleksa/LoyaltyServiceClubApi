package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification;

import java.time.OffsetDateTime;

public class OtpExpiredException extends OtpVerificationException {

    public OtpExpiredException(String contact, OffsetDateTime expiredAt) {
        super("OTP code has expired for: " + contact + " (expired at: " + expiredAt + ")", "OTP_EXPIRED");
    }

    public OtpExpiredException(String contact, OffsetDateTime expiredAt, Throwable cause) {
        super("OTP code has expired for: " + contact + " (expired at: " + expiredAt + ")", "OTP_EXPIRED", cause);
    }
}
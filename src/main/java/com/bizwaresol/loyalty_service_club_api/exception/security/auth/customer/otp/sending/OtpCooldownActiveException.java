package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending;

import java.time.OffsetDateTime;

public class OtpCooldownActiveException extends OtpSendingException {

    public OtpCooldownActiveException(String contact, OffsetDateTime nextAllowedResend) {
        super("OTP resend cooldown active for: " + contact + ". Next resend allowed at: " + nextAllowedResend, "OTP_COOLDOWN_ACTIVE");
    }

    public OtpCooldownActiveException(String contact, int cooldownMinutes) {
        super("OTP resend cooldown active for: " + contact + ". Please wait " + cooldownMinutes + " minutes before resending", "OTP_COOLDOWN_ACTIVE");
    }

    public OtpCooldownActiveException(String contact, OffsetDateTime nextAllowedResend, Throwable cause) {
        super("OTP resend cooldown active for: " + contact + ". Next resend allowed at: " + nextAllowedResend, "OTP_COOLDOWN_ACTIVE", cause);
    }
}
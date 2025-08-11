package com.bizwaresol.loyalty_service_club_api.data.dto.verification.request;

import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;

public record VerifyCodeRequest(
        String contact,                    // email or phone number
        String otpCode,                    // 6-digit OTP code to verify
        OtpDeliveryMethod deliveryMethod   // EMAIL or SMS
) {

    // Factory methods for cleaner creation
    public static VerifyCodeRequest email(String email, String otpCode) {
        return new VerifyCodeRequest(email, otpCode, OtpDeliveryMethod.EMAIL);
    }

    public static VerifyCodeRequest sms(String phone, String otpCode) {
        return new VerifyCodeRequest(phone, otpCode, OtpDeliveryMethod.SMS);
    }
}
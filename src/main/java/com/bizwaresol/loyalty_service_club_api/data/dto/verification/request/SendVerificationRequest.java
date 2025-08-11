package com.bizwaresol.loyalty_service_club_api.data.dto.verification.request;

import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;

public record SendVerificationRequest(
        String contact,                    // email or phone number
        OtpDeliveryMethod deliveryMethod   // EMAIL or SMS
) {

    // Factory methods for cleaner creation
    public static SendVerificationRequest email(String email) {
        return new SendVerificationRequest(email, OtpDeliveryMethod.EMAIL);
    }

    public static SendVerificationRequest sms(String phone) {
        return new SendVerificationRequest(phone, OtpDeliveryMethod.SMS);
    }
}
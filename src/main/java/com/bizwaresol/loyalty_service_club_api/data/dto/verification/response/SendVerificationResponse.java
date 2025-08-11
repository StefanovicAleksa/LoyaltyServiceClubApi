package com.bizwaresol.loyalty_service_club_api.data.dto.verification.response;

import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import java.time.OffsetDateTime;

public record SendVerificationResponse(
        boolean success,
        String message,
        String contact,
        OtpDeliveryMethod deliveryMethod,
        OffsetDateTime nextAllowedSend,    // null if can send immediately
        Integer cooldownMinutes            // remaining cooldown time if rate limited
) {

    public static SendVerificationResponse success(String contact, OtpDeliveryMethod deliveryMethod) {
        return new SendVerificationResponse(
                true,
                "Verification code sent successfully",
                contact,
                deliveryMethod,
                null,
                null
        );
    }

    public static SendVerificationResponse rateLimited(String contact, OtpDeliveryMethod deliveryMethod,
                                                       OffsetDateTime nextAllowedSend, Integer cooldownMinutes) {
        return new SendVerificationResponse(
                false,
                "Rate limit exceeded. Please wait before requesting another code.",
                contact,
                deliveryMethod,
                nextAllowedSend,
                cooldownMinutes
        );
    }

    public static SendVerificationResponse cooldownActive(String contact, OtpDeliveryMethod deliveryMethod,
                                                          OffsetDateTime nextAllowedSend, Integer cooldownMinutes) {
        return new SendVerificationResponse(
                false,
                "Please wait before requesting another verification code.",
                contact,
                deliveryMethod,
                nextAllowedSend,
                cooldownMinutes
        );
    }

    public static SendVerificationResponse failure(String contact, OtpDeliveryMethod deliveryMethod, String message) {
        return new SendVerificationResponse(
                false,
                message,
                contact,
                deliveryMethod,
                null,
                null
        );
    }
}
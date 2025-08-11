package com.bizwaresol.loyalty_service_club_api.data.dto.verification.response;

import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;

public record VerifyCodeResponse(
        boolean success,
        boolean verified,
        String message,
        String contact,
        OtpDeliveryMethod deliveryMethod,
        Integer attemptsRemaining,         // remaining attempts for current OTP
        boolean maxAttemptsReached,        // true if max attempts reached
        boolean contactVerified            // true if email/phone is now marked as verified
) {

    public static VerifyCodeResponse success(String contact, OtpDeliveryMethod deliveryMethod) {
        return new VerifyCodeResponse(
                true,
                true,
                "Verification successful",
                contact,
                deliveryMethod,
                null,
                false,
                true
        );
    }

    public static VerifyCodeResponse invalidCode(String contact, OtpDeliveryMethod deliveryMethod,
                                                 Integer attemptsRemaining, boolean maxReached) {
        return new VerifyCodeResponse(
                false,
                false,
                maxReached ? "Maximum verification attempts reached. Please request a new code."
                        : "Invalid verification code. Please try again.",
                contact,
                deliveryMethod,
                attemptsRemaining,
                maxReached,
                false
        );
    }

    public static VerifyCodeResponse expired(String contact, OtpDeliveryMethod deliveryMethod) {
        return new VerifyCodeResponse(
                false,
                false,
                "Verification code has expired. Please request a new code.",
                contact,
                deliveryMethod,
                0,
                true,
                false
        );
    }

    public static VerifyCodeResponse alreadyUsed(String contact, OtpDeliveryMethod deliveryMethod) {
        return new VerifyCodeResponse(
                false,
                false,
                "Verification code has already been used. Please request a new code.",
                contact,
                deliveryMethod,
                0,
                true,
                false
        );
    }

    public static VerifyCodeResponse notFound(String contact, OtpDeliveryMethod deliveryMethod) {
        return new VerifyCodeResponse(
                false,
                false,
                "No valid verification code found. Please request a new code.",
                contact,
                deliveryMethod,
                0,
                true,
                false
        );
    }

    public static VerifyCodeResponse failure(String contact, OtpDeliveryMethod deliveryMethod, String message) {
        return new VerifyCodeResponse(
                false,
                false,
                message,
                contact,
                deliveryMethod,
                null,
                false,
                false
        );
    }
}
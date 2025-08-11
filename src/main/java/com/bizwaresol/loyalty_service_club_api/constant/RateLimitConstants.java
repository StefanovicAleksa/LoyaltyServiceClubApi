package com.bizwaresol.loyalty_service_club_api.constant;

public final class RateLimitConstants {

    private RateLimitConstants() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    // OTP RESEND Rate Limiting - Wait times in seconds for each RESEND attempt
    // This controls how often users can REQUEST new OTP codes, NOT verification attempts
    // Example: After 3rd resend request, user must wait 5 minutes before requesting 4th OTP
    public static final int[] DEFAULT_OTP_RATE_LIMIT_WAIT_SECONDS = {0, 60, 120, 300, 900}; // 0s, 1m, 2m, 5m, 15m
    public static final int DEFAULT_OTP_RATE_LIMIT_RESET_HOURS = 1;

    // Maximum resend attempts before longest wait time kicks in
    public static final int OTP_RATE_LIMIT_MAX_TIER = 5;
}
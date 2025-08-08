package com.bizwaresol.loyalty_service_club_api.constant;

public final class RateLimitConstants {

    private RateLimitConstants() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    // OTP Rate Limiting - Wait times in minutes for each attempt
    public static final int[] DEFAULT_OTP_RATE_LIMIT_WAIT_MINUTES = {0, 1, 2, 5, 15}; // attempts 1-5+
    public static final int DEFAULT_OTP_RATE_LIMIT_RESET_HOURS = 1;

    // Maximum attempts before longest wait time kicks in
    public static final int OTP_RATE_LIMIT_MAX_TIER = 5;
}
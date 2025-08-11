package com.bizwaresol.loyalty_service_club_api.config.properties;

import com.bizwaresol.loyalty_service_club_api.constant.RateLimitConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "verification")
public class VerificationProperties {
    private boolean sandboxMode;
    private int otpExpiryMinutes;
    private int maxAttempts;
    private int otpLength;
    private int resendCooldownSeconds;

    // Rate limiting properties for RESEND attempts - controls OTP code requests, not verification attempts
    private int[] otpRateLimitWaitSeconds = RateLimitConstants.DEFAULT_OTP_RATE_LIMIT_WAIT_SECONDS;
    private int otpRateLimitResetHours = RateLimitConstants.DEFAULT_OTP_RATE_LIMIT_RESET_HOURS;

    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }

    public void setOtpExpiryMinutes(int otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getOtpLength() {
        return otpLength;
    }

    public void setOtpLength(int otpLength) {
        this.otpLength = otpLength;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int[] getOtpRateLimitWaitSeconds() {
        return otpRateLimitWaitSeconds;
    }

    public void setOtpRateLimitWaitSeconds(int[] otpRateLimitWaitSeconds) {
        this.otpRateLimitWaitSeconds = otpRateLimitWaitSeconds;
    }

    public int getOtpRateLimitResetHours() {
        return otpRateLimitResetHours;
    }

    public void setOtpRateLimitResetHours(int otpRateLimitResetHours) {
        this.otpRateLimitResetHours = otpRateLimitResetHours;
    }

    /**
     * Get wait time in seconds for a specific RESEND attempt number
     * This controls how long users must wait before requesting another OTP code
     * @param attemptNumber the resend attempt number (1-based)
     * @return wait time in seconds before next OTP can be requested
     */
    public int getWaitTimeForAttempt(int attemptNumber) {
        if (attemptNumber <= 1) return 0;

        int index = attemptNumber - 2; // Convert to 0-based index for wait times
        if (index >= otpRateLimitWaitSeconds.length) {
            // Use last (highest) wait time for attempts beyond configured range
            return otpRateLimitWaitSeconds[otpRateLimitWaitSeconds.length - 1];
        }
        return otpRateLimitWaitSeconds[index];
    }

    @Override
    public String toString() {
        return "VerificationProperties{" +
                "sandboxMode=" + sandboxMode +
                ", otpExpiryMinutes=" + otpExpiryMinutes +
                ", maxAttempts=" + maxAttempts +
                ", otpLength=" + otpLength +
                ", resendCooldownSeconds=" + resendCooldownSeconds +
                ", otpRateLimitWaitSeconds=" + java.util.Arrays.toString(otpRateLimitWaitSeconds) +
                ", otpRateLimitResetHours=" + otpRateLimitResetHours +
                '}';
    }
}
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
    private int resendCooldownMinutes;

    // Rate limiting properties
    private int[] otpRateLimitWaitMinutes = RateLimitConstants.DEFAULT_OTP_RATE_LIMIT_WAIT_MINUTES;
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

    public int getResendCooldownMinutes() {
        return resendCooldownMinutes;
    }

    public void setResendCooldownMinutes(int resendCooldownMinutes) {
        this.resendCooldownMinutes = resendCooldownMinutes;
    }

    public int[] getOtpRateLimitWaitMinutes() {
        return otpRateLimitWaitMinutes;
    }

    public void setOtpRateLimitWaitMinutes(int[] otpRateLimitWaitMinutes) {
        this.otpRateLimitWaitMinutes = otpRateLimitWaitMinutes;
    }

    public int getOtpRateLimitResetHours() {
        return otpRateLimitResetHours;
    }

    public void setOtpRateLimitResetHours(int otpRateLimitResetHours) {
        this.otpRateLimitResetHours = otpRateLimitResetHours;
    }

    /**
     * Get wait time in minutes for a specific attempt number
     * @param attemptNumber the attempt number (1-based)
     * @return wait time in minutes
     */
    public int getWaitTimeForAttempt(int attemptNumber) {
        if (attemptNumber <= 1) return 0;

        int index = attemptNumber - 2; // Convert to 0-based index for wait times
        if (index >= otpRateLimitWaitMinutes.length) {
            // Use last (highest) wait time for attempts beyond configured range
            return otpRateLimitWaitMinutes[otpRateLimitWaitMinutes.length - 1];
        }
        return otpRateLimitWaitMinutes[index];
    }

    @Override
    public String toString() {
        return "VerificationProperties{" +
                "sandboxMode=" + sandboxMode +
                ", otpExpiryMinutes=" + otpExpiryMinutes +
                ", maxAttempts=" + maxAttempts +
                ", otpLength=" + otpLength +
                ", resendCooldownMinutes=" + resendCooldownMinutes +
                ", otpRateLimitWaitMinutes=" + java.util.Arrays.toString(otpRateLimitWaitMinutes) +
                ", otpRateLimitResetHours=" + otpRateLimitResetHours +
                '}';
    }
}
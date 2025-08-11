// =====================================================================================
// FILE: src/main/java/com/bizwaresol/loyalty_service_club_api/service/verification/PhoneVerificationService.java
// =====================================================================================
package com.bizwaresol.loyalty_service_club_api.service.verification;

import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationTemplateProperties;
import com.bizwaresol.loyalty_service_club_api.data.dto.verification.response.SendVerificationResponse;
import com.bizwaresol.loyalty_service_club_api.data.dto.verification.response.VerifyCodeResponse;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.domain.entity.OtpToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending.OtpSendingException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.OtpVerificationException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;
import com.bizwaresol.loyalty_service_club_api.service.client.SnsClientService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerPhoneService;
import com.bizwaresol.loyalty_service_club_api.service.data.OtpTokenService;
import com.bizwaresol.loyalty_service_club_api.util.mappers.OtpVerificationErrorMapper;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class PhoneVerificationService {

    private final OtpTokenService otpTokenService;
    private final SnsClientService snsClientService;
    private final CustomerPhoneService customerPhoneService;
    private final VerificationProperties verificationProperties;
    private final VerificationTemplateProperties templateProperties;

    public PhoneVerificationService(
            OtpTokenService otpTokenService,
            SnsClientService snsClientService,
            CustomerPhoneService customerPhoneService,
            VerificationProperties verificationProperties,
            VerificationTemplateProperties templateProperties) {
        this.otpTokenService = otpTokenService;
        this.snsClientService = snsClientService;
        this.customerPhoneService = customerPhoneService;
        this.verificationProperties = verificationProperties;
        this.templateProperties = templateProperties;
    }

    /**
     * Sends phone verification OTP code via SMS
     */
    @Transactional
    public SendVerificationResponse sendVerificationCode(String phone) {
        // 1. Validate input first to fail fast
        DataValidator.validatePhone(phone, "phone");

        try {
            // 2. Validate and find CustomerPhone entity
            CustomerPhone customerPhone = customerPhoneService.findByPhone(phone);

            // 3. Check rate limits
            SendVerificationResponse rateLimitCheck = checkRateLimits(phone);
            if (!rateLimitCheck.success()) {
                return rateLimitCheck;
            }

            // 4. Invalidate any existing active OTPs
            otpTokenService.invalidateActivePhoneVerificationOtps(phone);

            // 5. Generate new OTP
            String otpCode = OtpToken.generateOtpCode();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(verificationProperties.getOtpExpiryMinutes());

            // 6. Create OTP token in database
            otpTokenService.createPhoneVerificationOtp(
                    customerPhone, otpCode, expiresAt, verificationProperties.getMaxAttempts());

            // 7. Send SMS via SNS
            sendOtpSms(phone, otpCode);

            // 8. Return success response
            return SendVerificationResponse.success(phone, OtpDeliveryMethod.SMS);

        } catch (ValidationException | OtpSendingException e) {
            // Re-throw expected business and validation exceptions directly
            throw e;
        } catch (Exception e) {
            // Map only unexpected exceptions
            throw OtpVerificationErrorMapper.mapToSendingException(e, phone, "SMS");
        }
    }

    /**
     * Verifies phone OTP code
     */
    @Transactional
    public VerifyCodeResponse verifyCode(String phone, String otpCode) {
        // 1. Validate input first to fail fast
        DataValidator.validatePhone(phone, "phone");
        DataValidator.validateOtpCode(otpCode, "otpCode");
        String trimmedOtp = otpCode.trim();

        try {
            // 2. Find valid OTP token
            OtpToken otpToken = otpTokenService.findValidPhoneVerificationOtp(trimmedOtp, phone);

            // 3. Check if OTP is expired
            if (otpToken.isExpired()) {
                return VerifyCodeResponse.expired(phone, OtpDeliveryMethod.SMS);
            }

            // 4. Check if OTP is already used
            if (otpToken.isUsed()) {
                return VerifyCodeResponse.alreadyUsed(phone, OtpDeliveryMethod.SMS);
            }

            // 5. Check if max attempts reached
            if (otpToken.hasReachedMaxAttempts()) {
                return VerifyCodeResponse.invalidCode(phone, OtpDeliveryMethod.SMS, 0, true);
            }

            // 6. Verify OTP code
            if (!otpToken.getOtpCode().equals(trimmedOtp)) {
                // Increment attempt count
                otpTokenService.incrementAttemptCount(otpToken.getId());

                int remainingAttempts = otpToken.getMaxAttempts() - (otpToken.getAttemptsCount() + 1);
                boolean maxReached = remainingAttempts <= 0;

                return VerifyCodeResponse.invalidCode(phone, OtpDeliveryMethod.SMS, remainingAttempts, maxReached);
            }

            // 7. Mark OTP as used
            otpTokenService.markOtpAsUsed(otpToken.getId());

            // 8. Mark phone as verified
            CustomerPhone customerPhone = otpToken.getCustomerPhone();
            customerPhoneService.changeVerificationStatus(customerPhone.getId(), true);

            // 9. Return success response
            return VerifyCodeResponse.success(phone, OtpDeliveryMethod.SMS);

        } catch (ValidationException | OtpVerificationException e) {
            // Re-throw expected business and validation exceptions directly
            throw e;
        } catch (Exception e) {
            // Map only unexpected exceptions
            throw OtpVerificationErrorMapper.mapToVerificationException(e, phone);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Checks rate limits for phone verification OTP sending
     * Two types of limits:
     * 1. Resend cooldown - minimum time between any OTP requests
     * 2. Rate limiting - escalating wait times for repeated OTP requests
     */
    private SendVerificationResponse checkRateLimits(String phone) {
        try {
            // Check resend cooldown (minimum time between ANY OTP requests)
            Optional<OtpToken> latestOtp = otpTokenService.findLatestPhoneVerificationOtp(phone);
            if (latestOtp.isPresent()) {
                OffsetDateTime lastSent = latestOtp.get().getCreatedDate();
                OffsetDateTime nextAllowed = lastSent.plusSeconds(verificationProperties.getResendCooldownSeconds());

                if (OffsetDateTime.now().isBefore(nextAllowed)) {
                    long secondsRemaining = java.time.Duration.between(OffsetDateTime.now(), nextAllowed).getSeconds();
                    return SendVerificationResponse.cooldownActive(
                            phone, OtpDeliveryMethod.SMS, nextAllowed, (int) secondsRemaining);
                }
            }

            // Check rate limiting for repeated OTP RESEND requests (escalating wait times)
            OffsetDateTime rateWindowStart = OffsetDateTime.now().minusHours(verificationProperties.getOtpRateLimitResetHours());
            long otpCountInWindow = otpTokenService.countPhoneVerificationOtpsInWindow(phone, rateWindowStart);

            // Calculate wait time for next OTP REQUEST (not verification attempt)
            int nextAttemptNumber = (int) otpCountInWindow + 1;
            int waitSeconds = verificationProperties.getWaitTimeForAttempt(nextAttemptNumber);

            if (waitSeconds > 0) {
                OffsetDateTime nextAllowed = OffsetDateTime.now().plusSeconds(waitSeconds);

                return SendVerificationResponse.rateLimited(
                        phone, OtpDeliveryMethod.SMS, nextAllowed, waitSeconds);
            }

            return SendVerificationResponse.success(phone, OtpDeliveryMethod.SMS);

        } catch (Exception e) {
            // If rate limit check fails, allow the operation but log the error
            return SendVerificationResponse.success(phone, OtpDeliveryMethod.SMS);
        }
    }

    /**
     * Sends OTP SMS via SNS
     */
    private void sendOtpSms(String phone, String otpCode) {
        String smsMessage = templateProperties.formatSms(otpCode);
        String appName = getAppName();

        // Send as transactional SMS with app name as sender ID
        // Let exceptions propagate to be handled by the main method's try-catch block.
        snsClientService.sendOtpSms(phone, smsMessage, appName);
    }

    /**
     * Gets application name for SMS sender ID
     */
    private String getAppName() {
        // Use a reasonable default that works with SNS sender ID requirements
        return "Loyalty"; // Sender ID for SMS (max 11 alphanumeric characters)
    }
}

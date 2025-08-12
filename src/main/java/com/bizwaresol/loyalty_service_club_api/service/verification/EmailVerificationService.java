// =====================================================================================
// FILE: src/main/java/com/bizwaresol/loyalty_service_club_api/service/verification/EmailVerificationService.java
// =====================================================================================
package com.bizwaresol.loyalty_service_club_api.service.verification;

import com.bizwaresol.loyalty_service_club_api.config.properties.SesProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationTemplateProperties;
import com.bizwaresol.loyalty_service_club_api.data.dto.verification.response.SendVerificationResponse;
import com.bizwaresol.loyalty_service_club_api.data.dto.verification.response.VerifyCodeResponse;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.OtpToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending.OtpSendingException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.OtpVerificationException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;
import com.bizwaresol.loyalty_service_club_api.service.client.SesClientService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerEmailService;
import com.bizwaresol.loyalty_service_club_api.service.data.OtpTokenService;
import com.bizwaresol.loyalty_service_club_api.util.mappers.OtpVerificationErrorMapper;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class EmailVerificationService {

    private final OtpTokenService otpTokenService;
    private final SesClientService sesClientService;
    private final CustomerEmailService customerEmailService;
    private final VerificationProperties verificationProperties;
    private final VerificationTemplateProperties templateProperties;
    private final SesProperties sesProperties;

    public EmailVerificationService(
            OtpTokenService otpTokenService,
            SesClientService sesClientService,
            CustomerEmailService customerEmailService,
            VerificationProperties verificationProperties,
            VerificationTemplateProperties templateProperties,
            SesProperties sesProperties) {
        this.otpTokenService = otpTokenService;
        this.sesClientService = sesClientService;
        this.customerEmailService = customerEmailService;
        this.verificationProperties = verificationProperties;
        this.templateProperties = templateProperties;
        this.sesProperties = sesProperties;
    }

    /**
     * Sends email verification OTP code
     */
    @Transactional
    public SendVerificationResponse sendVerificationCode(String email) {
        // 1. Validate input first to fail fast
        DataValidator.validateEmail(email, "email");

        try {
            // 2. Validate and find CustomerEmail entity
            CustomerEmail customerEmail = customerEmailService.findByEmail(email);

            // 3. Check rate limits
            SendVerificationResponse rateLimitCheck = checkRateLimits(email);
            if (!rateLimitCheck.success()) {
                return rateLimitCheck;
            }

            // 4. Invalidate any existing active OTPs
            otpTokenService.invalidateActiveEmailVerificationOtps(email);

            // 5. Generate new OTP
            String otpCode = OtpToken.generateOtpCode();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(verificationProperties.getOtpExpiryMinutes());

            // 6. Create OTP token in database
            otpTokenService.createEmailVerificationOtp(
                    customerEmail, otpCode, expiresAt, verificationProperties.getMaxAttempts());

            // 7. Send email via SES
            sendOtpEmail(email, otpCode);

            // 8. Return success response
            return SendVerificationResponse.success(email, OtpDeliveryMethod.EMAIL);

        } catch (ValidationException | OtpSendingException e) {
            // Re-throw expected business and validation exceptions directly
            throw e;
        } catch (Exception e) {
            // Map only unexpected exceptions
            throw OtpVerificationErrorMapper.mapToSendingException(e, email, "email");
        }
    }

    /**
     * Verifies email OTP code
     */
    @Transactional
    public VerifyCodeResponse verifyCode(String email, String otpCode) {
        // 1. Validate input first to fail fast
        DataValidator.validateEmail(email, "email");
        DataValidator.validateOtpCode(otpCode, "otpCode");
        String trimmedOtp = otpCode.trim();

        try {
            // 2. Find valid OTP token
            OtpToken otpToken = otpTokenService.findValidEmailVerificationOtp(trimmedOtp, email);

            // 3. Check if OTP is expired
            if (otpToken.isExpired()) {
                return VerifyCodeResponse.expired(email, OtpDeliveryMethod.EMAIL);
            }

            // 4. Check if OTP is already used
            if (otpToken.isUsed()) {
                return VerifyCodeResponse.alreadyUsed(email, OtpDeliveryMethod.EMAIL);
            }

            // 5. Check if max attempts reached
            if (otpToken.hasReachedMaxAttempts()) {
                return VerifyCodeResponse.invalidCode(email, OtpDeliveryMethod.EMAIL, 0, true);
            }

            // 6. Verify OTP code
            if (!otpToken.getOtpCode().equals(trimmedOtp)) {
                // Increment attempt count
                otpTokenService.incrementAttemptCount(otpToken.getId());

                int remainingAttempts = otpToken.getMaxAttempts() - (otpToken.getAttemptsCount() + 1);
                boolean maxReached = remainingAttempts <= 0;

                return VerifyCodeResponse.invalidCode(email, OtpDeliveryMethod.EMAIL, remainingAttempts, maxReached);
            }

            // 7. Mark OTP as used. This will now trigger the database function
            // to update the email's verification status automatically.
            otpTokenService.markOtpAsUsed(otpToken.getId());

            // 8. Return success response
            return VerifyCodeResponse.success(email, OtpDeliveryMethod.EMAIL);

        } catch (ValidationException | OtpVerificationException e) {
            // Re-throw expected business and validation exceptions directly
            throw e;
        } catch (Exception e) {
            // Map only unexpected exceptions
            throw OtpVerificationErrorMapper.mapToVerificationException(e, email);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Checks rate limits for email verification OTP sending
     * Two types of limits:
     * 1. Resend cooldown - minimum time between any OTP requests
     * 2. Rate limiting - escalating wait times for repeated OTP requests
     */
    private SendVerificationResponse checkRateLimits(String email) {
        try {
            // Check resend cooldown (minimum time between ANY OTP requests)
            Optional<OtpToken> latestOtp = otpTokenService.findLatestEmailVerificationOtp(email);
            if (latestOtp.isPresent()) {
                OffsetDateTime lastSent = latestOtp.get().getCreatedDate();
                OffsetDateTime nextAllowed = lastSent.plusSeconds(verificationProperties.getResendCooldownSeconds());

                if (OffsetDateTime.now().isBefore(nextAllowed)) {
                    long secondsRemaining = java.time.Duration.between(OffsetDateTime.now(), nextAllowed).getSeconds();
                    return SendVerificationResponse.cooldownActive(
                            email, OtpDeliveryMethod.EMAIL, nextAllowed, (int) secondsRemaining);
                }
            }

            // Check rate limiting for repeated OTP RESEND requests (escalating wait times)
            OffsetDateTime rateWindowStart = OffsetDateTime.now().minusHours(verificationProperties.getOtpRateLimitResetHours());
            long otpCountInWindow = otpTokenService.countEmailVerificationOtpsInWindow(email, rateWindowStart);

            // Calculate wait time for next OTP REQUEST (not verification attempt)
            int nextAttemptNumber = (int) otpCountInWindow + 1;
            int waitSeconds = verificationProperties.getWaitTimeForAttempt(nextAttemptNumber);

            if (waitSeconds > 0) {
                OffsetDateTime nextAllowed = OffsetDateTime.now().plusSeconds(waitSeconds);

                return SendVerificationResponse.rateLimited(
                        email, OtpDeliveryMethod.EMAIL, nextAllowed, waitSeconds);
            }

            return SendVerificationResponse.success(email, OtpDeliveryMethod.EMAIL);

        } catch (Exception e) {
            // If rate limit check fails, allow the operation but log the error
            return SendVerificationResponse.success(email, OtpDeliveryMethod.EMAIL);
        }
    }

    /**
     * Sends OTP email via SES (HTML only - simpler and more reliable)
     */
    private void sendOtpEmail(String email, String otpCode) {
        String subject = templateProperties.formatEmailSubject();
        String htmlContent = templateProperties.formatEmailHtml(otpCode, verificationProperties.getOtpExpiryMinutes());

        // Send HTML email (most modern email clients support HTML)
        // Let exceptions propagate to be handled by the main method's try-catch block.
        sesClientService.sendHtmlEmail(
                sesProperties.getSourceEmail(), email, subject, htmlContent);
    }
}

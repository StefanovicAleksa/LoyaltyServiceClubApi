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
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpPurpose;
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
     * Sends email verification OTP code.
     */
    @Transactional
    public SendVerificationResponse sendVerificationCode(String email) {
        return sendCode(email, OtpPurpose.EMAIL_VERIFICATION);
    }

    /**
     * Sends a password reset OTP code via email.
     */
    @Transactional
    public SendVerificationResponse sendPasswordResetCode(String email) {
        return sendCode(email, OtpPurpose.PASSWORD_RESET);
    }

    /**
     * Verifies an email OTP code for account verification.
     */
    @Transactional
    public VerifyCodeResponse verifyCode(String email, String otpCode) {
        return verify(email, otpCode, OtpPurpose.EMAIL_VERIFICATION);
    }

    /**
     * Verifies an email OTP code for password reset.
     */
    @Transactional
    public VerifyCodeResponse verifyPasswordResetCode(String email, String otpCode) {
        return verify(email, otpCode, OtpPurpose.PASSWORD_RESET);
    }


    // ===== PRIVATE GENERIC METHODS =====

    private SendVerificationResponse sendCode(String email, OtpPurpose purpose) {
        DataValidator.validateEmail(email, "email");

        try {
            CustomerEmail customerEmail = customerEmailService.findByEmail(email);

            SendVerificationResponse rateLimitCheck = checkRateLimits(email, purpose);
            if (!rateLimitCheck.success()) {
                return rateLimitCheck;
            }

            // Invalidate existing OTPs for the same purpose
            if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
                otpTokenService.invalidateActiveEmailVerificationOtps(email);
            } else if (purpose == OtpPurpose.PASSWORD_RESET) {
                otpTokenService.invalidateActivePasswordResetEmailOtps(email);
            }

            String otpCode = OtpToken.generateOtpCode();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(verificationProperties.getOtpExpiryMinutes());

            // Create new OTP for the specified purpose
            if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
                otpTokenService.createEmailVerificationOtp(customerEmail, otpCode, expiresAt, verificationProperties.getMaxAttempts());
            } else if (purpose == OtpPurpose.PASSWORD_RESET) {
                otpTokenService.createPasswordResetEmailOtp(customerEmail, otpCode, expiresAt, verificationProperties.getMaxAttempts());
            }

            sendOtpEmail(email, otpCode);

            return SendVerificationResponse.success(email, OtpDeliveryMethod.EMAIL);

        } catch (ValidationException | OtpSendingException e) {
            throw e;
        } catch (Exception e) {
            throw OtpVerificationErrorMapper.mapToSendingException(e, email, "email");
        }
    }

    private VerifyCodeResponse verify(String email, String otpCode, OtpPurpose purpose) {
        DataValidator.validateEmail(email, "email");
        DataValidator.validateOtpCode(otpCode, "otpCode");
        String trimmedOtp = otpCode.trim();

        try {
            OtpToken otpToken;
            if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
                otpToken = otpTokenService.findValidEmailVerificationOtp(trimmedOtp, email);
            } else {
                otpToken = otpTokenService.findValidPasswordResetEmailOtp(trimmedOtp, email);
            }

            if (otpToken.isExpired()) {
                return VerifyCodeResponse.expired(email, OtpDeliveryMethod.EMAIL);
            }
            if (otpToken.isUsed()) {
                return VerifyCodeResponse.alreadyUsed(email, OtpDeliveryMethod.EMAIL);
            }
            if (otpToken.hasReachedMaxAttempts()) {
                return VerifyCodeResponse.invalidCode(email, OtpDeliveryMethod.EMAIL, 0, true);
            }

            if (!otpToken.getOtpCode().equals(trimmedOtp)) {
                otpTokenService.incrementAttemptCount(otpToken.getId());
                int remainingAttempts = otpToken.getMaxAttempts() - (otpToken.getAttemptsCount() + 1);
                boolean maxReached = remainingAttempts <= 0;
                return VerifyCodeResponse.invalidCode(email, OtpDeliveryMethod.EMAIL, remainingAttempts, maxReached);
            }

            // Mark OTP as used. If it's for EMAIL_VERIFICATION, a trigger will handle updating the email status.
            // If it's for PASSWORD_RESET, it's simply consumed.
            otpTokenService.markOtpAsUsed(otpToken.getId());

            return VerifyCodeResponse.success(email, OtpDeliveryMethod.EMAIL);

        } catch (ValidationException | OtpVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw OtpVerificationErrorMapper.mapToVerificationException(e, email);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private SendVerificationResponse checkRateLimits(String email, OtpPurpose purpose) {
        try {
            Optional<OtpToken> latestOtp;
            if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
                latestOtp = otpTokenService.findLatestEmailVerificationOtp(email);
            } else {
                latestOtp = otpTokenService.findLatestPasswordResetEmailOtp(email);
            }

            if (latestOtp.isPresent()) {
                OffsetDateTime lastSent = latestOtp.get().getCreatedDate();
                OffsetDateTime nextAllowed = lastSent.plusSeconds(verificationProperties.getResendCooldownSeconds());
                if (OffsetDateTime.now().isBefore(nextAllowed)) {
                    long secondsRemaining = java.time.Duration.between(OffsetDateTime.now(), nextAllowed).getSeconds();
                    return SendVerificationResponse.cooldownActive(email, OtpDeliveryMethod.EMAIL, nextAllowed, (int) secondsRemaining);
                }
            }

            OffsetDateTime rateWindowStart = OffsetDateTime.now().minusHours(verificationProperties.getOtpRateLimitResetHours());
            long otpCountInWindow;
            if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
                otpCountInWindow = otpTokenService.countEmailVerificationOtpsInWindow(email, rateWindowStart);
            } else {
                otpCountInWindow = otpTokenService.countPasswordResetEmailOtpsInWindow(email, rateWindowStart);
            }

            int nextAttemptNumber = (int) otpCountInWindow + 1;
            int waitSeconds = verificationProperties.getWaitTimeForAttempt(nextAttemptNumber);
            if (waitSeconds > 0) {
                OffsetDateTime nextAllowed = OffsetDateTime.now().plusSeconds(waitSeconds);
                return SendVerificationResponse.rateLimited(email, OtpDeliveryMethod.EMAIL, nextAllowed, waitSeconds);
            }

            return SendVerificationResponse.success(email, OtpDeliveryMethod.EMAIL);

        } catch (Exception e) {
            return SendVerificationResponse.success(email, OtpDeliveryMethod.EMAIL);
        }
    }

    private void sendOtpEmail(String email, String otpCode) {
        String subject = templateProperties.formatEmailSubject();
        String htmlContent = templateProperties.formatEmailHtml(otpCode, verificationProperties.getOtpExpiryMinutes());
        sesClientService.sendHtmlEmail(sesProperties.getSourceEmail(), email, subject, htmlContent);
    }
}

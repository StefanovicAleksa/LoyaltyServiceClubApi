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
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpPurpose;
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
     * Sends phone verification OTP code via SMS.
     */
    @Transactional
    public SendVerificationResponse sendVerificationCode(String phone) {
        return sendCode(phone, OtpPurpose.PHONE_VERIFICATION);
    }

    /**
     * Sends a password reset OTP code via SMS.
     */
    @Transactional
    public SendVerificationResponse sendPasswordResetCode(String phone) {
        return sendCode(phone, OtpPurpose.PASSWORD_RESET);
    }

    /**
     * Verifies a phone OTP code for account verification.
     */
    @Transactional
    public VerifyCodeResponse verifyCode(String phone, String otpCode) {
        return verify(phone, otpCode, OtpPurpose.PHONE_VERIFICATION);
    }

    /**
     * Verifies a phone OTP code for password reset.
     */
    @Transactional
    public VerifyCodeResponse verifyPasswordResetCode(String phone, String otpCode) {
        return verify(phone, otpCode, OtpPurpose.PASSWORD_RESET);
    }

    // ===== PRIVATE GENERIC METHODS =====

    private SendVerificationResponse sendCode(String phone, OtpPurpose purpose) {
        DataValidator.validatePhone(phone, "phone");

        try {
            CustomerPhone customerPhone = customerPhoneService.findByPhone(phone);

            SendVerificationResponse rateLimitCheck = checkRateLimits(phone, purpose);
            if (!rateLimitCheck.success()) {
                return rateLimitCheck;
            }

            if (purpose == OtpPurpose.PHONE_VERIFICATION) {
                otpTokenService.invalidateActivePhoneVerificationOtps(phone);
            } else if (purpose == OtpPurpose.PASSWORD_RESET) {
                otpTokenService.invalidateActivePasswordResetPhoneOtps(phone);
            }

            String otpCode = OtpToken.generateOtpCode();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(verificationProperties.getOtpExpiryMinutes());

            if (purpose == OtpPurpose.PHONE_VERIFICATION) {
                otpTokenService.createPhoneVerificationOtp(customerPhone, otpCode, expiresAt, verificationProperties.getMaxAttempts());
            } else if (purpose == OtpPurpose.PASSWORD_RESET) {
                otpTokenService.createPasswordResetPhoneOtp(customerPhone, otpCode, expiresAt, verificationProperties.getMaxAttempts());
            }

            sendOtpSms(phone, otpCode);

            return SendVerificationResponse.success(phone, OtpDeliveryMethod.SMS);

        } catch (ValidationException | OtpSendingException e) {
            throw e;
        } catch (Exception e) {
            throw OtpVerificationErrorMapper.mapToSendingException(e, phone, "SMS");
        }
    }

    private VerifyCodeResponse verify(String phone, String otpCode, OtpPurpose purpose) {
        DataValidator.validatePhone(phone, "phone");
        DataValidator.validateOtpCode(otpCode, "otpCode");
        String trimmedOtp = otpCode.trim();

        try {
            OtpToken otpToken;
            if (purpose == OtpPurpose.PHONE_VERIFICATION) {
                otpToken = otpTokenService.findValidPhoneVerificationOtp(trimmedOtp, phone);
            } else {
                otpToken = otpTokenService.findValidPasswordResetPhoneOtp(trimmedOtp, phone);
            }

            if (otpToken.isExpired()) {
                return VerifyCodeResponse.expired(phone, OtpDeliveryMethod.SMS);
            }
            if (otpToken.isUsed()) {
                return VerifyCodeResponse.alreadyUsed(phone, OtpDeliveryMethod.SMS);
            }
            if (otpToken.hasReachedMaxAttempts()) {
                return VerifyCodeResponse.invalidCode(phone, OtpDeliveryMethod.SMS, 0, true);
            }

            if (!otpToken.getOtpCode().equals(trimmedOtp)) {
                otpTokenService.incrementAttemptCount(otpToken.getId());
                int remainingAttempts = otpToken.getMaxAttempts() - (otpToken.getAttemptsCount() + 1);
                boolean maxReached = remainingAttempts <= 0;
                return VerifyCodeResponse.invalidCode(phone, OtpDeliveryMethod.SMS, remainingAttempts, maxReached);
            }

            otpTokenService.markOtpAsUsed(otpToken.getId());

            return VerifyCodeResponse.success(phone, OtpDeliveryMethod.SMS);

        } catch (ValidationException | OtpVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw OtpVerificationErrorMapper.mapToVerificationException(e, phone);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private SendVerificationResponse checkRateLimits(String phone, OtpPurpose purpose) {
        try {
            Optional<OtpToken> latestOtp;
            if (purpose == OtpPurpose.PHONE_VERIFICATION) {
                latestOtp = otpTokenService.findLatestPhoneVerificationOtp(phone);
            } else {
                latestOtp = otpTokenService.findLatestPasswordResetPhoneOtp(phone);
            }

            if (latestOtp.isPresent()) {
                OffsetDateTime lastSent = latestOtp.get().getCreatedDate();
                OffsetDateTime nextAllowed = lastSent.plusSeconds(verificationProperties.getResendCooldownSeconds());
                if (OffsetDateTime.now().isBefore(nextAllowed)) {
                    long secondsRemaining = java.time.Duration.between(OffsetDateTime.now(), nextAllowed).getSeconds();
                    return SendVerificationResponse.cooldownActive(phone, OtpDeliveryMethod.SMS, nextAllowed, (int) secondsRemaining);
                }
            }

            OffsetDateTime rateWindowStart = OffsetDateTime.now().minusHours(verificationProperties.getOtpRateLimitResetHours());
            long otpCountInWindow;
            if (purpose == OtpPurpose.PHONE_VERIFICATION) {
                otpCountInWindow = otpTokenService.countPhoneVerificationOtpsInWindow(phone, rateWindowStart);
            } else {
                otpCountInWindow = otpTokenService.countPasswordResetPhoneOtpsInWindow(phone, rateWindowStart);
            }

            int nextAttemptNumber = (int) otpCountInWindow + 1;
            int waitSeconds = verificationProperties.getWaitTimeForAttempt(nextAttemptNumber);
            if (waitSeconds > 0) {
                OffsetDateTime nextAllowed = OffsetDateTime.now().plusSeconds(waitSeconds);
                return SendVerificationResponse.rateLimited(phone, OtpDeliveryMethod.SMS, nextAllowed, waitSeconds);
            }

            return SendVerificationResponse.success(phone, OtpDeliveryMethod.SMS);

        } catch (Exception e) {
            return SendVerificationResponse.success(phone, OtpDeliveryMethod.SMS);
        }
    }

    private void sendOtpSms(String phone, String otpCode) {
        String smsMessage = templateProperties.formatSms(otpCode);
        String appName = "LoyaltyClub";
        snsClientService.sendOtpSms(phone, smsMessage, appName);
    }
}

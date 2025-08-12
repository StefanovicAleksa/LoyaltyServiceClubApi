// =====================================================================================
// FILE: src/test/java/com/bizwaresol/loyalty_service_club_api/service/verification/PhoneVerificationServiceTest.java
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
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.OtpTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PhoneNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending.OtpDeliveryFailedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.OtpNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.SnsInvalidPhoneException;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.SnsOptedOutException;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.SnsQuotaExceededException;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.SnsThrottlingException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;
import com.bizwaresol.loyalty_service_club_api.service.client.SnsClientService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerPhoneService;
import com.bizwaresol.loyalty_service_club_api.service.data.OtpTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhoneVerificationService Unit Tests")
class PhoneVerificationServiceTest {

    @Mock
    private OtpTokenService otpTokenService;
    @Mock
    private SnsClientService snsClientService;
    @Mock
    private CustomerPhoneService customerPhoneService;
    @Mock
    private VerificationProperties verificationProperties;
    @Mock
    private VerificationTemplateProperties templateProperties;
    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    private CustomerPhone sampleCustomerPhone;
    private OtpToken sampleOtpToken;
    private OtpToken expiredOtpToken;
    private OtpToken usedOtpToken;
    private OtpToken maxAttemptsOtpToken;

    private final String VALID_PHONE = "+381123456789";
    private final String VALID_OTP_CODE = "123456";
    private final String INVALID_OTP_CODE = "999999";
    private final String APP_NAME = "Loyalty";
    private final Long PHONE_ID = 1L;
    private final Long OTP_ID = 2L;
    private final int OTP_EXPIRY_MINUTES = 10;
    private final int MAX_ATTEMPTS = 3;
    private final int RESEND_COOLDOWN_SECONDS = 60;

    @BeforeEach
    void setUp() {
        sampleCustomerPhone = new CustomerPhone();
        sampleCustomerPhone.setId(PHONE_ID);
        sampleCustomerPhone.setPhone(VALID_PHONE);
        sampleCustomerPhone.setVerified(false);

        sampleOtpToken = new OtpToken();
        sampleOtpToken.setId(OTP_ID);
        sampleOtpToken.setCustomerPhone(sampleCustomerPhone);
        sampleOtpToken.setOtpCode(VALID_OTP_CODE);
        sampleOtpToken.setPurpose(OtpPurpose.PHONE_VERIFICATION);
        sampleOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);
        sampleOtpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        sampleOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        sampleOtpToken.setAttemptsCount(0);
        sampleOtpToken.setCreatedDate(OffsetDateTime.now());

        expiredOtpToken = new OtpToken();
        expiredOtpToken.setId(OTP_ID);
        expiredOtpToken.setCustomerPhone(sampleCustomerPhone);
        expiredOtpToken.setOtpCode(VALID_OTP_CODE);
        expiredOtpToken.setPurpose(OtpPurpose.PHONE_VERIFICATION);
        expiredOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);
        expiredOtpToken.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
        expiredOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        expiredOtpToken.setAttemptsCount(0);

        usedOtpToken = new OtpToken();
        usedOtpToken.setId(OTP_ID);
        usedOtpToken.setCustomerPhone(sampleCustomerPhone);
        usedOtpToken.setOtpCode(VALID_OTP_CODE);
        usedOtpToken.setPurpose(OtpPurpose.PHONE_VERIFICATION);
        usedOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);
        usedOtpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        usedOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        usedOtpToken.setAttemptsCount(1);
        usedOtpToken.setUsedAt(OffsetDateTime.now().minusMinutes(2));

        maxAttemptsOtpToken = new OtpToken();
        maxAttemptsOtpToken.setId(OTP_ID);
        maxAttemptsOtpToken.setCustomerPhone(sampleCustomerPhone);
        maxAttemptsOtpToken.setOtpCode(VALID_OTP_CODE);
        maxAttemptsOtpToken.setPurpose(OtpPurpose.PHONE_VERIFICATION);
        maxAttemptsOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);
        maxAttemptsOtpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        maxAttemptsOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        maxAttemptsOtpToken.setAttemptsCount(MAX_ATTEMPTS);
    }

    // ===== SEND VERIFICATION CODE TESTS =====

    @Nested
    @DisplayName("sendVerificationCode() Tests")
    class SendVerificationCodeTests {

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode(null))
                    .isInstanceOf(NullFieldException.class);
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when phone is empty")
        void shouldThrowEmptyFieldExceptionWhenPhoneIsEmpty() {
            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode("   "))
                    .isInstanceOf(EmptyFieldException.class);
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode("invalid-phone"))
                    .isInstanceOf(InvalidPhoneFormatException.class);
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when customer phone not found")
        void shouldThrowOtpDeliveryFailedExceptionWhenCustomerPhoneNotFound() {
            when(customerPhoneService.findByPhone(VALID_PHONE))
                    .thenThrow(new PhoneNotFoundException(VALID_PHONE));

            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode(VALID_PHONE))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("Contact not found or not registered.");
        }

        @Test
        @DisplayName("Should send verification code successfully")
        void shouldSendVerificationCodeSuccessfully() {
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(verificationProperties.getOtpRateLimitResetHours()).thenReturn(1);
            when(templateProperties.formatSms(anyString())).thenReturn("Your code");

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.findLatestPhoneVerificationOtp(VALID_PHONE)).thenReturn(Optional.empty());
            when(otpTokenService.countPhoneVerificationOtpsInWindow(eq(VALID_PHONE), any(OffsetDateTime.class))).thenReturn(0L);
            when(otpTokenService.createPhoneVerificationOtp(eq(sampleCustomerPhone), anyString(), any(OffsetDateTime.class), eq(MAX_ATTEMPTS)))
                    .thenReturn(sampleOtpToken);

            SendVerificationResponse result = phoneVerificationService.sendVerificationCode(VALID_PHONE);

            assertThat(result.success()).isTrue();
            assertThat(result.contact()).isEqualTo(VALID_PHONE);
            assertThat(result.deliveryMethod()).isEqualTo(OtpDeliveryMethod.SMS);

            verify(snsClientService).sendOtpSms(eq(VALID_PHONE), anyString(), eq(APP_NAME));
        }

        @Test
        @DisplayName("Should return cooldown response when resend cooldown is active")
        void shouldReturnCooldownResponseWhenResendCooldownIsActive() {
            when(verificationProperties.getResendCooldownSeconds()).thenReturn(RESEND_COOLDOWN_SECONDS);
            OtpToken recentOtp = new OtpToken();
            recentOtp.setCreatedDate(OffsetDateTime.now().minusSeconds(30));

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.findLatestPhoneVerificationOtp(VALID_PHONE)).thenReturn(Optional.of(recentOtp));

            SendVerificationResponse result = phoneVerificationService.sendVerificationCode(VALID_PHONE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Please wait before requesting another verification code.");
        }

        @Test
        @DisplayName("Should return rate limit response when rate limit exceeded")
        void shouldReturnRateLimitResponseWhenRateLimitExceeded() {
            when(verificationProperties.getOtpRateLimitResetHours()).thenReturn(1);
            when(verificationProperties.getWaitTimeForAttempt(6)).thenReturn(300);

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.findLatestPhoneVerificationOtp(VALID_PHONE)).thenReturn(Optional.empty());
            when(otpTokenService.countPhoneVerificationOtpsInWindow(eq(VALID_PHONE), any(OffsetDateTime.class))).thenReturn(5L);

            SendVerificationResponse result = phoneVerificationService.sendVerificationCode(VALID_PHONE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Rate limit exceeded. Please wait before requesting another code.");
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when SNS service fails with quota exceeded")
        void shouldThrowOtpDeliveryFailedExceptionWhenSnsServiceFailsWithQuotaExceeded() {
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(templateProperties.formatSms(anyString())).thenReturn("Your code");

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.createPhoneVerificationOtp(any(), any(), any(), any())).thenReturn(sampleOtpToken);
            doThrow(new SnsQuotaExceededException("test-error", "test-request"))
                    .when(snsClientService).sendOtpSms(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode(VALID_PHONE))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("SMS service quota exceeded");
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when phone number opted out")
        void shouldThrowOtpDeliveryFailedExceptionWhenPhoneNumberOptedOut() {
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(templateProperties.formatSms(anyString())).thenReturn("Your code");

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.createPhoneVerificationOtp(any(), any(), any(), any())).thenReturn(sampleOtpToken);
            doThrow(new SnsOptedOutException(VALID_PHONE, "test-error", "test-request"))
                    .when(snsClientService).sendOtpSms(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode(VALID_PHONE))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("This phone number has opted out");
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when SNS service is throttled")
        void shouldThrowOtpDeliveryFailedExceptionWhenSnsServiceIsThrottled() {
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(templateProperties.formatSms(anyString())).thenReturn("Your code");

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.createPhoneVerificationOtp(any(), any(), any(), any())).thenReturn(sampleOtpToken);
            doThrow(new SnsThrottlingException("test-error", "test-request"))
                    .when(snsClientService).sendOtpSms(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode(VALID_PHONE))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("SMS service is temporarily unavailable");
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when phone number is invalid")
        void shouldThrowOtpDeliveryFailedExceptionWhenPhoneNumberIsInvalid() {
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(templateProperties.formatSms(anyString())).thenReturn("Your code");

            when(customerPhoneService.findByPhone(VALID_PHONE)).thenReturn(sampleCustomerPhone);
            when(otpTokenService.createPhoneVerificationOtp(any(), any(), any(), any())).thenReturn(sampleOtpToken);
            doThrow(new SnsInvalidPhoneException(VALID_PHONE, "test-error", "test-request"))
                    .when(snsClientService).sendOtpSms(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode(VALID_PHONE))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("Invalid phone number provided");
        }
    }

    // ===== VERIFY CODE TESTS =====

    @Nested
    @DisplayName("verifyCode() Tests")
    class VerifyCodeTests {

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> phoneVerificationService.verifyCode(null, VALID_OTP_CODE))
                    .isInstanceOf(NullFieldException.class);
        }

        @Test
        @DisplayName("Should throw NullFieldException when otpCode is null")
        void shouldThrowNullFieldExceptionWhenOtpCodeIsNull() {
            assertThatThrownBy(() -> phoneVerificationService.verifyCode(VALID_PHONE, null))
                    .isInstanceOf(NullFieldException.class);
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when phone is empty")
        void shouldThrowEmptyFieldExceptionWhenPhoneIsEmpty() {
            assertThatThrownBy(() -> phoneVerificationService.verifyCode("   ", VALID_OTP_CODE))
                    .isInstanceOf(EmptyFieldException.class);
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when otpCode is empty")
        void shouldThrowEmptyFieldExceptionWhenOtpCodeIsEmpty() {
            assertThatThrownBy(() -> phoneVerificationService.verifyCode(VALID_PHONE, "   "))
                    .isInstanceOf(EmptyFieldException.class);
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> phoneVerificationService.verifyCode("invalid-phone", VALID_OTP_CODE))
                    .isInstanceOf(InvalidPhoneFormatException.class);
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when OTP format is invalid")
        void shouldThrowFieldTooShortExceptionWhenOtpFormatIsInvalid() {
            assertThatThrownBy(() -> phoneVerificationService.verifyCode(VALID_PHONE, "12345"))
                    .isInstanceOf(FieldTooShortException.class);
        }

        @Test
        @DisplayName("Should throw OtpNotFoundException when OTP not found")
        void shouldThrowOtpNotFoundExceptionWhenOtpNotFound() {
            when(otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .thenThrow(new OtpTokenNotFoundException(VALID_OTP_CODE));

            assertThatThrownBy(() -> phoneVerificationService.verifyCode(VALID_PHONE, VALID_OTP_CODE))
                    .isInstanceOf(OtpNotFoundException.class);
        }

        @Test
        @DisplayName("Should return expired response when OTP is expired")
        void shouldReturnExpiredResponseWhenOtpIsExpired() {
            when(otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(expiredOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, VALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Verification code has expired. Please request a new code.");
        }

        @Test
        @DisplayName("Should return already used response when OTP is already used")
        void shouldReturnAlreadyUsedResponseWhenOtpIsAlreadyUsed() {
            when(otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(usedOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, VALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Verification code has already been used. Please request a new code.");
        }

        @Test
        @DisplayName("Should return max attempts response when max attempts reached")
        void shouldReturnMaxAttemptsResponseWhenMaxAttemptsReached() {
            when(otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(maxAttemptsOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, VALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Maximum verification attempts reached. Please request a new code.");
        }

        @Test
        @DisplayName("Should return invalid code response and increment attempts when OTP code is wrong")
        void shouldReturnInvalidCodeResponseAndIncrementAttemptsWhenOtpCodeIsWrong() {
            when(otpTokenService.findValidPhoneVerificationOtp(INVALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(sampleOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, INVALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Invalid verification code. Please try again.");
            assertThat(result.attemptsRemaining()).isEqualTo(2);
            assertThat(result.maxAttemptsReached()).isFalse();

            verify(otpTokenService).incrementAttemptCount(OTP_ID);
        }

        @Test
        @DisplayName("Should verify code successfully")
        void shouldVerifyCodeSuccessfully() {
            when(otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(sampleOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, VALID_OTP_CODE);

            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Verification successful");
            assertThat(result.contactVerified()).isTrue();

            verify(otpTokenService).markOtpAsUsed(OTP_ID);
            verify(customerPhoneService, never()).changeVerificationStatus(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should handle trimmed OTP codes correctly")
        void shouldHandleTrimmedOtpCodesCorrectly() {
            String paddedOtpCode = "  " + VALID_OTP_CODE + "  ";
            when(otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(sampleOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, paddedOtpCode);

            assertThat(result.success()).isTrue();
            verify(otpTokenService).findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE);
        }

        @Test
        @DisplayName("Should return invalid code with max attempts when last attempt fails")
        void shouldReturnInvalidCodeWithMaxAttemptsWhenLastAttemptFails() {
            sampleOtpToken.setAttemptsCount(2);

            when(otpTokenService.findValidPhoneVerificationOtp(INVALID_OTP_CODE, VALID_PHONE))
                    .thenReturn(sampleOtpToken);

            VerifyCodeResponse result = phoneVerificationService.verifyCode(VALID_PHONE, INVALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Maximum verification attempts reached. Please request a new code.");
            assertThat(result.attemptsRemaining()).isEqualTo(0);
            assertThat(result.maxAttemptsReached()).isTrue();

            verify(otpTokenService).incrementAttemptCount(OTP_ID);
        }
    }
}

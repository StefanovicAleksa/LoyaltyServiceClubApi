// =====================================================================================
// FILE: src/test/java/com/bizwaresol/loyalty_service_club_api/service/verification/EmailVerificationServiceTest.java
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
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.EmailNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.OtpTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending.OtpDeliveryFailedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.OtpNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses.SesQuotaExceededException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;
import com.bizwaresol.loyalty_service_club_api.service.client.SesClientService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerEmailService;
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
@DisplayName("EmailVerificationService Unit Tests")
class EmailVerificationServiceTest {

    @Mock
    private OtpTokenService otpTokenService;
    @Mock
    private SesClientService sesClientService;
    @Mock
    private CustomerEmailService customerEmailService;
    @Mock
    private VerificationProperties verificationProperties;
    @Mock
    private VerificationTemplateProperties templateProperties;
    @Mock
    private SesProperties sesProperties;
    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private CustomerEmail sampleCustomerEmail;
    private OtpToken sampleOtpToken;
    private OtpToken expiredOtpToken;
    private OtpToken usedOtpToken;
    private OtpToken maxAttemptsOtpToken;

    private final String VALID_EMAIL = "test@gmail.com";
    private final String VALID_OTP_CODE = "123456";
    private final String INVALID_OTP_CODE = "999999";
    private final String SOURCE_EMAIL = "noreply@example.com";
    private final Long EMAIL_ID = 1L;
    private final Long OTP_ID = 2L;
    private final int OTP_EXPIRY_MINUTES = 10;
    private final int MAX_ATTEMPTS = 3;
    private final int RESEND_COOLDOWN_SECONDS = 60;

    @BeforeEach
    void setUp() {
        sampleCustomerEmail = new CustomerEmail();
        sampleCustomerEmail.setId(EMAIL_ID);
        sampleCustomerEmail.setEmail(VALID_EMAIL);
        sampleCustomerEmail.setVerified(false);

        sampleOtpToken = new OtpToken();
        sampleOtpToken.setId(OTP_ID);
        sampleOtpToken.setCustomerEmail(sampleCustomerEmail);
        sampleOtpToken.setOtpCode(VALID_OTP_CODE);
        sampleOtpToken.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        sampleOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);
        sampleOtpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        sampleOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        sampleOtpToken.setAttemptsCount(0);
        sampleOtpToken.setCreatedDate(OffsetDateTime.now());

        expiredOtpToken = new OtpToken();
        expiredOtpToken.setId(OTP_ID);
        expiredOtpToken.setCustomerEmail(sampleCustomerEmail);
        expiredOtpToken.setOtpCode(VALID_OTP_CODE);
        expiredOtpToken.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        expiredOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);
        expiredOtpToken.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
        expiredOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        expiredOtpToken.setAttemptsCount(0);

        usedOtpToken = new OtpToken();
        usedOtpToken.setId(OTP_ID);
        usedOtpToken.setCustomerEmail(sampleCustomerEmail);
        usedOtpToken.setOtpCode(VALID_OTP_CODE);
        usedOtpToken.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        usedOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);
        usedOtpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        usedOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        usedOtpToken.setAttemptsCount(1);
        usedOtpToken.setUsedAt(OffsetDateTime.now().minusMinutes(2));

        maxAttemptsOtpToken = new OtpToken();
        maxAttemptsOtpToken.setId(OTP_ID);
        maxAttemptsOtpToken.setCustomerEmail(sampleCustomerEmail);
        maxAttemptsOtpToken.setOtpCode(VALID_OTP_CODE);
        maxAttemptsOtpToken.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        maxAttemptsOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);
        maxAttemptsOtpToken.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        maxAttemptsOtpToken.setMaxAttempts(MAX_ATTEMPTS);
        maxAttemptsOtpToken.setAttemptsCount(MAX_ATTEMPTS);
    }

    // ===== SEND VERIFICATION CODE TESTS =====

    @Nested
    @DisplayName("sendVerificationCode() Tests")
    class SendVerificationCodeTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(null))
                    .isInstanceOf(NullFieldException.class);
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when email is empty")
        void shouldThrowEmptyFieldExceptionWhenEmailIsEmpty() {
            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode("   "))
                    .isInstanceOf(EmptyFieldException.class);
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when email format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenEmailFormatIsInvalid() {
            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode("invalid-email"))
                    .isInstanceOf(InvalidEmailFormatException.class);
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when customer email not found")
        void shouldThrowOtpDeliveryFailedExceptionWhenCustomerEmailNotFound() {
            when(customerEmailService.findByEmail(VALID_EMAIL))
                    .thenThrow(new EmailNotFoundException(VALID_EMAIL));

            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(VALID_EMAIL))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("Contact not found or not registered.");
        }

        @Test
        @DisplayName("Should send verification code successfully")
        void shouldSendVerificationCodeSuccessfully() {
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(verificationProperties.getOtpRateLimitResetHours()).thenReturn(1);
            when(templateProperties.formatEmailSubject()).thenReturn("Verify Your Email");
            when(templateProperties.formatEmailHtml(anyString(), anyInt())).thenReturn("<p>Your code is {otpCode}</p>");
            when(sesProperties.getSourceEmail()).thenReturn(SOURCE_EMAIL);

            when(customerEmailService.findByEmail(VALID_EMAIL)).thenReturn(sampleCustomerEmail);
            when(otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL)).thenReturn(Optional.empty());
            when(otpTokenService.countEmailVerificationOtpsInWindow(eq(VALID_EMAIL), any(OffsetDateTime.class))).thenReturn(0L);
            when(otpTokenService.createEmailVerificationOtp(eq(sampleCustomerEmail), anyString(), any(OffsetDateTime.class), eq(MAX_ATTEMPTS)))
                    .thenReturn(sampleOtpToken);

            SendVerificationResponse result = emailVerificationService.sendVerificationCode(VALID_EMAIL);

            assertThat(result.success()).isTrue();
            assertThat(result.contact()).isEqualTo(VALID_EMAIL);
            assertThat(result.deliveryMethod()).isEqualTo(OtpDeliveryMethod.EMAIL);
            assertThat(result.message()).isEqualTo("Verification code sent successfully");

            verify(customerEmailService).findByEmail(VALID_EMAIL);
            verify(otpTokenService).invalidateActiveEmailVerificationOtps(VALID_EMAIL);
            verify(otpTokenService).createEmailVerificationOtp(eq(sampleCustomerEmail), anyString(), any(OffsetDateTime.class), eq(MAX_ATTEMPTS));
            verify(sesClientService).sendHtmlEmail(eq(SOURCE_EMAIL), eq(VALID_EMAIL), anyString(), anyString());
        }

        @Test
        @DisplayName("Should return cooldown response when resend cooldown is active")
        void shouldReturnCooldownResponseWhenResendCooldownIsActive() {
            when(verificationProperties.getResendCooldownSeconds()).thenReturn(RESEND_COOLDOWN_SECONDS);
            OtpToken recentOtp = new OtpToken();
            recentOtp.setCreatedDate(OffsetDateTime.now().minusSeconds(30));

            when(customerEmailService.findByEmail(VALID_EMAIL)).thenReturn(sampleCustomerEmail);
            when(otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL)).thenReturn(Optional.of(recentOtp));

            SendVerificationResponse result = emailVerificationService.sendVerificationCode(VALID_EMAIL);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Please wait before requesting another verification code.");
            assertThat(result.nextAllowedSend()).isNotNull();

            verify(otpTokenService, never()).createEmailVerificationOtp(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return rate limit response when rate limit exceeded")
        void shouldReturnRateLimitResponseWhenRateLimitExceeded() {
            when(verificationProperties.getOtpRateLimitResetHours()).thenReturn(1);
            when(verificationProperties.getWaitTimeForAttempt(6)).thenReturn(300);

            when(customerEmailService.findByEmail(VALID_EMAIL)).thenReturn(sampleCustomerEmail);
            when(otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL)).thenReturn(Optional.empty());
            when(otpTokenService.countEmailVerificationOtpsInWindow(eq(VALID_EMAIL), any(OffsetDateTime.class))).thenReturn(5L);

            SendVerificationResponse result = emailVerificationService.sendVerificationCode(VALID_EMAIL);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Rate limit exceeded. Please wait before requesting another code.");
            assertThat(result.cooldownMinutes()).isEqualTo(300);

            verify(otpTokenService, never()).createEmailVerificationOtp(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw OtpDeliveryFailedException when SES service fails")
        void shouldThrowOtpDeliveryFailedExceptionWhenSesServiceFails() {
            // Arrange
            when(verificationProperties.getOtpExpiryMinutes()).thenReturn(OTP_EXPIRY_MINUTES);
            when(verificationProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
            when(verificationProperties.getOtpRateLimitResetHours()).thenReturn(1);
            when(templateProperties.formatEmailSubject()).thenReturn("Verify Your Email");
            when(templateProperties.formatEmailHtml(anyString(), anyInt())).thenReturn("<p>Your code is {otpCode}</p>");
            when(sesProperties.getSourceEmail()).thenReturn(SOURCE_EMAIL);

            when(customerEmailService.findByEmail(VALID_EMAIL)).thenReturn(sampleCustomerEmail);

            // Mock rate-limit checks to ensure they pass, allowing execution to proceed
            when(otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL)).thenReturn(Optional.empty());
            when(otpTokenService.countEmailVerificationOtpsInWindow(eq(VALID_EMAIL), any(OffsetDateTime.class))).thenReturn(0L);

            when(otpTokenService.createEmailVerificationOtp(any(), any(), any(), any())).thenReturn(sampleOtpToken);
            doThrow(new SesQuotaExceededException("test-error", "test-request"))
                    .when(sesClientService).sendHtmlEmail(anyString(), anyString(), anyString(), anyString());

            // Act & Assert
            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(VALID_EMAIL))
                    .isInstanceOf(OtpDeliveryFailedException.class)
                    .hasMessageContaining("Email service quota exceeded");
        }
    }

    // ===== VERIFY CODE TESTS =====

    @Nested
    @DisplayName("verifyCode() Tests")
    class VerifyCodeTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> emailVerificationService.verifyCode(null, VALID_OTP_CODE))
                    .isInstanceOf(NullFieldException.class);
        }

        @Test
        @DisplayName("Should throw NullFieldException when otpCode is null")
        void shouldThrowNullFieldExceptionWhenOtpCodeIsNull() {
            assertThatThrownBy(() -> emailVerificationService.verifyCode(VALID_EMAIL, null))
                    .isInstanceOf(NullFieldException.class);
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when email is empty")
        void shouldThrowEmptyFieldExceptionWhenEmailIsEmpty() {
            assertThatThrownBy(() -> emailVerificationService.verifyCode("   ", VALID_OTP_CODE))
                    .isInstanceOf(EmptyFieldException.class);
        }

        @Test
        @DisplayName("Should throw OtpNotFoundException when OTP not found")
        void shouldThrowOtpNotFoundExceptionWhenOtpNotFound() {
            when(otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .thenThrow(new OtpTokenNotFoundException(VALID_OTP_CODE));

            assertThatThrownBy(() -> emailVerificationService.verifyCode(VALID_EMAIL, VALID_OTP_CODE))
                    .isInstanceOf(OtpNotFoundException.class);
        }

        @Test
        @DisplayName("Should return expired response when OTP is expired")
        void shouldReturnExpiredResponseWhenOtpIsExpired() {
            when(otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(expiredOtpToken);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, VALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Verification code has expired. Please request a new code.");
        }

        @Test
        @DisplayName("Should return already used response when OTP is already used")
        void shouldReturnAlreadyUsedResponseWhenOtpIsAlreadyUsed() {
            when(otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(usedOtpToken);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, VALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Verification code has already been used. Please request a new code.");
        }

        @Test
        @DisplayName("Should return max attempts response when max attempts reached")
        void shouldReturnMaxAttemptsResponseWhenMaxAttemptsReached() {
            when(otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(maxAttemptsOtpToken);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, VALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Maximum verification attempts reached. Please request a new code.");
        }

        @Test
        @DisplayName("Should return invalid code response and increment attempts when OTP code is wrong")
        void shouldReturnInvalidCodeResponseAndIncrementAttemptsWhenOtpCodeIsWrong() {
            when(otpTokenService.findValidEmailVerificationOtp(INVALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(sampleOtpToken);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, INVALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Invalid verification code. Please try again.");
            assertThat(result.attemptsRemaining()).isEqualTo(2);
            assertThat(result.maxAttemptsReached()).isFalse();

            verify(otpTokenService).incrementAttemptCount(OTP_ID);
        }

        @Test
        @DisplayName("Should verify code successfully and mark email as verified")
        void shouldVerifyCodeSuccessfullyAndMarkEmailAsVerified() {
            when(otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(sampleOtpToken);
            when(customerEmailService.changeVerificationStatus(EMAIL_ID, true))
                    .thenReturn(sampleCustomerEmail);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, VALID_OTP_CODE);

            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Verification successful");
            assertThat(result.contactVerified()).isTrue();

            verify(otpTokenService).markOtpAsUsed(OTP_ID);
            verify(customerEmailService).changeVerificationStatus(EMAIL_ID, true);
        }

        @Test
        @DisplayName("Should handle trimmed OTP codes correctly")
        void shouldHandleTrimmedOtpCodesCorrectly() {
            String paddedOtpCode = "  " + VALID_OTP_CODE + "  ";
            when(otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(sampleOtpToken);
            when(customerEmailService.changeVerificationStatus(EMAIL_ID, true))
                    .thenReturn(sampleCustomerEmail);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, paddedOtpCode);

            assertThat(result.success()).isTrue();
            verify(otpTokenService).findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL);
        }

        @Test
        @DisplayName("Should return invalid code with max attempts when last attempt fails")
        void shouldReturnInvalidCodeWithMaxAttemptsWhenLastAttemptFails() {
            sampleOtpToken.setAttemptsCount(2);

            when(otpTokenService.findValidEmailVerificationOtp(INVALID_OTP_CODE, VALID_EMAIL))
                    .thenReturn(sampleOtpToken);

            VerifyCodeResponse result = emailVerificationService.verifyCode(VALID_EMAIL, INVALID_OTP_CODE);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Maximum verification attempts reached. Please request a new code.");
            assertThat(result.attemptsRemaining()).isEqualTo(0);
            assertThat(result.maxAttemptsReached()).isTrue();

            verify(otpTokenService).incrementAttemptCount(OTP_ID);
        }
    }
}
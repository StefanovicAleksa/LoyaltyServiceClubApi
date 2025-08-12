package com.bizwaresol.loyalty_service_club_api.service.auth;

import com.bizwaresol.loyalty_service_club_api.data.dto.verification.response.VerifyCodeResponse;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateActivePasswordResetTokenException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification.InvalidOtpCodeException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.ActivePasswordResetTokenExistsException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.PasswordResetTokenAlreadyUsedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.password.PasswordResetTokenExpiredException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerAccountService;
import com.bizwaresol.loyalty_service_club_api.service.data.PasswordResetTokenService;
import com.bizwaresol.loyalty_service_club_api.service.verification.EmailVerificationService;
import com.bizwaresol.loyalty_service_club_api.service.verification.PhoneVerificationService;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock
    private CustomerAccountService customerAccountService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private PhoneVerificationService phoneVerificationService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private CustomerAccount sampleCustomerAccount;
    private PasswordResetToken sampleToken;
    private final String VALID_EMAIL = "test@example.com";
    private final String VALID_PHONE = "+381123456789";
    private final String VALID_OTP = "123456";
    private final String INVALID_OTP = "999999";
    private final String VALID_TOKEN_STRING = UUID.randomUUID().toString();
    private final String VALID_NEW_PASSWORD = "newPassword123";
    private final Long ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleCustomerAccount = new CustomerAccount();
        sampleCustomerAccount.setId(ACCOUNT_ID);
        sampleCustomerAccount.setUsername(VALID_EMAIL);

        sampleToken = new PasswordResetToken();
        sampleToken.setId(100L);
        sampleToken.setToken(VALID_TOKEN_STRING);
        sampleToken.setCustomerAccount(sampleCustomerAccount);
        sampleToken.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        sampleToken.setUsedAt(null);
    }

    // ===== requestPasswordReset() TESTS =====
    @Nested
    @DisplayName("requestPasswordReset() Tests")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("Should successfully request password reset via EMAIL")
        void shouldSuccessfullyRequestResetViaEmail() {
            when(customerAccountService.findByUsername(VALID_EMAIL)).thenReturn(sampleCustomerAccount);
            when(passwordResetTokenService.findActiveTokenByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatCode(() -> passwordResetService.requestPasswordReset(VALID_EMAIL, OtpDeliveryMethod.EMAIL))
                    .doesNotThrowAnyException();

            verify(emailVerificationService).sendPasswordResetCode(VALID_EMAIL);
            verify(phoneVerificationService, never()).sendPasswordResetCode(anyString());
        }

        @Test
        @DisplayName("Should successfully request password reset via SMS")
        void shouldSuccessfullyRequestResetViaSms() {
            when(customerAccountService.findByUsername(VALID_PHONE)).thenReturn(sampleCustomerAccount);
            when(passwordResetTokenService.findActiveTokenByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatCode(() -> passwordResetService.requestPasswordReset(VALID_PHONE, OtpDeliveryMethod.SMS))
                    .doesNotThrowAnyException();

            verify(phoneVerificationService).sendPasswordResetCode(VALID_PHONE);
            verify(emailVerificationService, never()).sendPasswordResetCode(anyString());
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException if contact does not exist")
        void shouldThrowExceptionIfContactNotFound() {
            when(customerAccountService.findByUsername(VALID_EMAIL))
                    .thenThrow(new CustomerAccountNotFoundException(VALID_EMAIL));

            assertThatThrownBy(() -> passwordResetService.requestPasswordReset(VALID_EMAIL, OtpDeliveryMethod.EMAIL))
                    .isInstanceOf(CustomerAccountNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw ActivePasswordResetTokenExistsException if an active token already exists")
        void shouldThrowExceptionIfActiveTokenExists() {
            when(customerAccountService.findByUsername(VALID_EMAIL)).thenReturn(sampleCustomerAccount);
            when(passwordResetTokenService.findActiveTokenByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(sampleToken));

            assertThatThrownBy(() -> passwordResetService.requestPasswordReset(VALID_EMAIL, OtpDeliveryMethod.EMAIL))
                    .isInstanceOf(ActivePasswordResetTokenExistsException.class);

            verify(emailVerificationService, never()).sendPasswordResetCode(anyString());
        }
    }

    // ===== verifyOtpAndCreateResetToken() TESTS =====
    @Nested
    @DisplayName("verifyOtpAndCreateResetToken() Tests")
    class VerifyOtpAndCreateResetTokenTests {

        @Test
        @DisplayName("Should successfully verify EMAIL OTP and create token")
        void shouldVerifyEmailOtpAndCreateToken() {
            when(emailVerificationService.verifyPasswordResetCode(VALID_EMAIL, VALID_OTP))
                    .thenReturn(VerifyCodeResponse.success(VALID_EMAIL, OtpDeliveryMethod.EMAIL));
            when(customerAccountService.findByUsername(VALID_EMAIL)).thenReturn(sampleCustomerAccount);
            when(passwordResetTokenService.createToken(any(CustomerAccount.class), any(OffsetDateTime.class)))
                    .thenReturn(sampleToken);

            String resultToken = passwordResetService.verifyOtpAndCreateResetToken(VALID_EMAIL, OtpDeliveryMethod.EMAIL, VALID_OTP);

            assertThat(resultToken).isEqualTo(VALID_TOKEN_STRING);
            verify(passwordResetTokenService).createToken(eq(sampleCustomerAccount), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should successfully verify SMS OTP and create token")
        void shouldVerifySmsOtpAndCreateToken() {
            when(phoneVerificationService.verifyPasswordResetCode(VALID_PHONE, VALID_OTP))
                    .thenReturn(VerifyCodeResponse.success(VALID_PHONE, OtpDeliveryMethod.SMS));
            when(customerAccountService.findByUsername(VALID_PHONE)).thenReturn(sampleCustomerAccount);
            when(passwordResetTokenService.createToken(any(CustomerAccount.class), any(OffsetDateTime.class)))
                    .thenReturn(sampleToken);

            String resultToken = passwordResetService.verifyOtpAndCreateResetToken(VALID_PHONE, OtpDeliveryMethod.SMS, VALID_OTP);

            assertThat(resultToken).isEqualTo(VALID_TOKEN_STRING);
        }

        @Test
        @DisplayName("Should throw InvalidOtpCodeException if OTP verification fails")
        void shouldThrowExceptionIfOtpVerificationFails() {
            when(emailVerificationService.verifyPasswordResetCode(VALID_EMAIL, INVALID_OTP))
                    .thenReturn(VerifyCodeResponse.failure(VALID_EMAIL, OtpDeliveryMethod.EMAIL, "Invalid code"));

            assertThatThrownBy(() -> passwordResetService.verifyOtpAndCreateResetToken(VALID_EMAIL, OtpDeliveryMethod.EMAIL, INVALID_OTP))
                    .isInstanceOf(InvalidOtpCodeException.class);

            verify(passwordResetTokenService, never()).createToken(any(), any());
        }

        @Test
        @DisplayName("Should handle race condition where duplicate active token is created")
        void shouldHandleDuplicateTokenRaceCondition() {
            when(emailVerificationService.verifyPasswordResetCode(VALID_EMAIL, VALID_OTP))
                    .thenReturn(VerifyCodeResponse.success(VALID_EMAIL, OtpDeliveryMethod.EMAIL));
            when(customerAccountService.findByUsername(VALID_EMAIL)).thenReturn(sampleCustomerAccount);
            when(passwordResetTokenService.createToken(any(CustomerAccount.class), any(OffsetDateTime.class)))
                    .thenThrow(new DuplicateActivePasswordResetTokenException("account_id: 1"));

            assertThatThrownBy(() -> passwordResetService.verifyOtpAndCreateResetToken(VALID_EMAIL, OtpDeliveryMethod.EMAIL, VALID_OTP))
                    .isInstanceOf(ActivePasswordResetTokenExistsException.class);
        }
    }

    // ===== resetPassword() TESTS =====
    @Nested
    @DisplayName("resetPassword() Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should successfully reset password with a valid token")
        void shouldSuccessfullyResetPassword() {
            when(passwordResetTokenService.findByToken(VALID_TOKEN_STRING)).thenReturn(sampleToken);
            when(customerAccountService.updatePassword(ACCOUNT_ID, VALID_NEW_PASSWORD)).thenReturn(sampleCustomerAccount);
            doNothing().when(passwordResetTokenService).markTokenAsUsed(sampleToken);

            assertThatCode(() -> passwordResetService.resetPassword(VALID_TOKEN_STRING, VALID_NEW_PASSWORD))
                    .doesNotThrowAnyException();

            verify(customerAccountService).updatePassword(ACCOUNT_ID, VALID_NEW_PASSWORD);
            verify(passwordResetTokenService).markTokenAsUsed(sampleToken);
        }

        @Test
        @DisplayName("Should throw FieldTooShortException for a password that is too short")
        void shouldThrowExceptionForShortNewPassword() {
            String shortPassword = "123";

            assertThatThrownBy(() -> passwordResetService.resetPassword(VALID_TOKEN_STRING, shortPassword))
                    .isInstanceOf(FieldTooShortException.class);

            verify(passwordResetTokenService, never()).findByToken(anyString());
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenExpiredException for an expired token")
        void shouldThrowExceptionForExpiredToken() {
            sampleToken.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
            when(passwordResetTokenService.findByToken(VALID_TOKEN_STRING)).thenReturn(sampleToken);

            assertThatThrownBy(() -> passwordResetService.resetPassword(VALID_TOKEN_STRING, VALID_NEW_PASSWORD))
                    .isInstanceOf(PasswordResetTokenExpiredException.class);

            verify(customerAccountService, never()).updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenAlreadyUsedException for a used token")
        void shouldThrowExceptionForUsedToken() {
            sampleToken.setUsedAt(OffsetDateTime.now().minusMinutes(1));
            when(passwordResetTokenService.findByToken(VALID_TOKEN_STRING)).thenReturn(sampleToken);

            assertThatThrownBy(() -> passwordResetService.resetPassword(VALID_TOKEN_STRING, VALID_NEW_PASSWORD))
                    .isInstanceOf(PasswordResetTokenAlreadyUsedException.class);

            verify(customerAccountService, never()).updatePassword(anyLong(), anyString());
        }
    }
}
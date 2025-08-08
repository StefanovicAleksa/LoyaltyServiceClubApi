package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.OtpTokenRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.domain.entity.OtpToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpPurpose;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.OtpTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidOtpFormatException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpTokenService Unit Tests")
class OtpTokenServiceTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @InjectMocks
    private OtpTokenService otpTokenService;

    private OtpToken sampleOtpToken;
    private CustomerEmail sampleEmail;
    private CustomerPhone samplePhone;
    private final String VALID_OTP_CODE = "123456";
    private final String NEW_OTP_CODE = "654321";
    private final String VALID_EMAIL = "test@gmail.com";
    private final String VALID_PHONE = "+381123456789";
    private final Long VALID_ID = 1L;
    private final Long EMAIL_ID = 2L;
    private final Long PHONE_ID = 3L;
    private final OffsetDateTime VALID_EXPIRES_AT = OffsetDateTime.now().plusMinutes(10);
    private final Integer VALID_MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        sampleEmail = new CustomerEmail();
        sampleEmail.setId(EMAIL_ID);
        sampleEmail.setEmail(VALID_EMAIL);

        samplePhone = new CustomerPhone();
        samplePhone.setId(PHONE_ID);
        samplePhone.setPhone(VALID_PHONE);

        sampleOtpToken = new OtpToken();
        sampleOtpToken.setId(VALID_ID);
        sampleOtpToken.setCustomerEmail(sampleEmail);
        sampleOtpToken.setOtpCode(VALID_OTP_CODE);
        sampleOtpToken.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        sampleOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);
        sampleOtpToken.setExpiresAt(VALID_EXPIRES_AT);
        sampleOtpToken.setMaxAttempts(VALID_MAX_ATTEMPTS);
        sampleOtpToken.setAttemptsCount(0);
        sampleOtpToken.setCreatedDate(OffsetDateTime.now());
        sampleOtpToken.setLastModifiedDate(OffsetDateTime.now());
    }

    // ===== CREATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("createEmailVerificationOtp() Tests")
    class CreateEmailVerificationOtpTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerEmail is null")
        void shouldThrowNullFieldExceptionWhenCustomerEmailIsNull() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(null, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Exactly one of customerEmail or customerPhone must be provided");
        }

        @Test
        @DisplayName("Should throw NullFieldException when otpCode is null")
        void shouldThrowNullFieldExceptionWhenOtpCodeIsNull() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, null, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpCode' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when otpCode is empty")
        void shouldThrowEmptyFieldExceptionWhenOtpCodeIsEmpty() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, "   ", VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'otpCode' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when otpCode is too short")
        void shouldThrowFieldTooShortExceptionWhenOtpCodeIsTooShort() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, "123", VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field otpCode too short: 3 characters. Min: 6 characters");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when otpCode is too long")
        void shouldThrowFieldTooLongExceptionWhenOtpCodeIsTooLong() {
            String longOtpCode = "1234567890";
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, longOtpCode, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessage("Field otpCode too long: 10 characters. Max: 6 characters");
        }

        @Test
        @DisplayName("Should throw InvalidOtpFormatException when otpCode format is invalid")
        void shouldThrowInvalidOtpFormatExceptionWhenOtpCodeFormatIsInvalid() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, "abc123", VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(InvalidOtpFormatException.class)
                    .hasMessage("Invalid OTP format: abc123 (Expected: 6 digits)");
        }

        @Test
        @DisplayName("Should throw NullFieldException when expiresAt is null")
        void shouldThrowNullFieldExceptionWhenExpiresAtIsNull() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, VALID_OTP_CODE, null, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'expiresAt' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when maxAttempts is null")
        void shouldThrowNullFieldExceptionWhenMaxAttemptsIsNull() {
            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, VALID_OTP_CODE, VALID_EXPIRES_AT, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'maxAttempts' cannot be null");
        }

        @Test
        @DisplayName("Should create email verification OTP successfully")
        void shouldCreateEmailVerificationOtpSuccessfully() {
            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(sampleOtpToken);

            OtpToken result = otpTokenService.createEmailVerificationOtp(sampleEmail, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS);

            assertThat(result).isNotNull();
            assertThat(result.getOtpCode()).isEqualTo(VALID_OTP_CODE);
            assertThat(result.getPurpose()).isEqualTo(OtpPurpose.EMAIL_VERIFICATION);
            assertThat(result.getDeliveryMethod()).isEqualTo(OtpDeliveryMethod.EMAIL);

            verify(otpTokenRepository).save(any(OtpToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.save(any(OtpToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.createEmailVerificationOtp(sampleEmail, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("createPhoneVerificationOtp() Tests")
    class CreatePhoneVerificationOtpTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when customerPhone is null")
        void shouldThrowIllegalArgumentExceptionWhenCustomerPhoneIsNull() {
            assertThatThrownBy(() -> otpTokenService.createPhoneVerificationOtp(null, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Exactly one of customerEmail or customerPhone must be provided");
        }

        @Test
        @DisplayName("Should create phone verification OTP successfully")
        void shouldCreatePhoneVerificationOtpSuccessfully() {
            OtpToken phoneOtpToken = new OtpToken();
            phoneOtpToken.setId(VALID_ID);
            phoneOtpToken.setCustomerPhone(samplePhone);
            phoneOtpToken.setOtpCode(VALID_OTP_CODE);
            phoneOtpToken.setPurpose(OtpPurpose.PHONE_VERIFICATION);
            phoneOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);

            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(phoneOtpToken);

            OtpToken result = otpTokenService.createPhoneVerificationOtp(samplePhone, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS);

            assertThat(result).isNotNull();
            assertThat(result.getOtpCode()).isEqualTo(VALID_OTP_CODE);
            assertThat(result.getPurpose()).isEqualTo(OtpPurpose.PHONE_VERIFICATION);
            assertThat(result.getDeliveryMethod()).isEqualTo(OtpDeliveryMethod.SMS);

            verify(otpTokenRepository).save(any(OtpToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.save(any(OtpToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.createPhoneVerificationOtp(samplePhone, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("createPasswordResetEmailOtp() Tests")
    class CreatePasswordResetEmailOtpTests {

        @Test
        @DisplayName("Should create password reset email OTP successfully")
        void shouldCreatePasswordResetEmailOtpSuccessfully() {
            OtpToken passwordResetOtpToken = new OtpToken();
            passwordResetOtpToken.setId(VALID_ID);
            passwordResetOtpToken.setCustomerEmail(sampleEmail);
            passwordResetOtpToken.setOtpCode(VALID_OTP_CODE);
            passwordResetOtpToken.setPurpose(OtpPurpose.PASSWORD_RESET);
            passwordResetOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);

            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(passwordResetOtpToken);

            OtpToken result = otpTokenService.createPasswordResetEmailOtp(sampleEmail, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS);

            assertThat(result).isNotNull();
            assertThat(result.getOtpCode()).isEqualTo(VALID_OTP_CODE);
            assertThat(result.getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
            assertThat(result.getDeliveryMethod()).isEqualTo(OtpDeliveryMethod.EMAIL);

            verify(otpTokenRepository).save(any(OtpToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.save(any(OtpToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.createPasswordResetEmailOtp(sampleEmail, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("createPasswordResetPhoneOtp() Tests")
    class CreatePasswordResetPhoneOtpTests {

        @Test
        @DisplayName("Should create password reset phone OTP successfully")
        void shouldCreatePasswordResetPhoneOtpSuccessfully() {
            OtpToken passwordResetPhoneOtpToken = new OtpToken();
            passwordResetPhoneOtpToken.setId(VALID_ID);
            passwordResetPhoneOtpToken.setCustomerPhone(samplePhone);
            passwordResetPhoneOtpToken.setOtpCode(VALID_OTP_CODE);
            passwordResetPhoneOtpToken.setPurpose(OtpPurpose.PASSWORD_RESET);
            passwordResetPhoneOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);

            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(passwordResetPhoneOtpToken);

            OtpToken result = otpTokenService.createPasswordResetPhoneOtp(samplePhone, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS);

            assertThat(result).isNotNull();
            assertThat(result.getOtpCode()).isEqualTo(VALID_OTP_CODE);
            assertThat(result.getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
            assertThat(result.getDeliveryMethod()).isEqualTo(OtpDeliveryMethod.SMS);

            verify(otpTokenRepository).save(any(OtpToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.save(any(OtpToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.createPasswordResetPhoneOtp(samplePhone, VALID_OTP_CODE, VALID_EXPIRES_AT, VALID_MAX_ATTEMPTS))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== EMAIL VERIFICATION OPERATIONS TESTS =====

    @Nested
    @DisplayName("findValidEmailVerificationOtp() Tests")
    class FindValidEmailVerificationOtpTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpCode is null")
        void shouldThrowNullFieldExceptionWhenOtpCodeIsNull() {
            assertThatThrownBy(() -> otpTokenService.findValidEmailVerificationOtp(null, VALID_EMAIL))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpCode' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidOtpFormatException when otpCode format is invalid")
        void shouldThrowInvalidOtpFormatExceptionWhenOtpCodeFormatIsInvalid() {
            assertThatThrownBy(() -> otpTokenService.findValidEmailVerificationOtp("abc123", VALID_EMAIL))
                    .isInstanceOf(InvalidOtpFormatException.class)
                    .hasMessage("Invalid OTP format: abc123 (Expected: 6 digits)");
        }

        @Test
        @DisplayName("Should find valid email verification OTP successfully")
        void shouldFindValidEmailVerificationOtpSuccessfully() {
            when(otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.of(sampleOtpToken));

            OtpToken result = otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL);

            assertThat(result).isEqualTo(sampleOtpToken);
            verify(otpTokenRepository).findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), any(OffsetDateTime.class), eq(3));
        }

        @Test
        @DisplayName("Should throw OtpTokenNotFoundException when valid OTP does not exist")
        void shouldThrowOtpTokenNotFoundExceptionWhenValidOtpDoesNotExist() {
            when(otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .isInstanceOf(OtpTokenNotFoundException.class)
                    .hasMessage("OTP token not found: " + VALID_OTP_CODE);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), any(OffsetDateTime.class), eq(3)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.findValidEmailVerificationOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findLatestEmailVerificationOtp() Tests")
    class FindLatestEmailVerificationOtpTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> otpTokenService.findLatestEmailVerificationOtp(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should find latest email verification OTP successfully")
        void shouldFindLatestEmailVerificationOtpSuccessfully() {
            when(otpTokenRepository.findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                    .thenReturn(Optional.of(sampleOtpToken));

            Optional<OtpToken> result = otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleOtpToken);
            verify(otpTokenRepository).findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION));
        }

        @Test
        @DisplayName("Should return empty when no OTP exists")
        void shouldReturnEmptyWhenNoOtpExists() {
            when(otpTokenRepository.findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                    .thenReturn(Optional.empty());

            Optional<OtpToken> result = otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL);

            assertThat(result).isEmpty();
            verify(otpTokenRepository).findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.findLatestEmailVerificationOtp(VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countEmailVerificationOtpsInWindow() Tests")
    class CountEmailVerificationOtpsInWindowTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);
            assertThatThrownBy(() -> otpTokenService.countEmailVerificationOtpsInWindow(null, since))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when since is null")
        void shouldThrowNullFieldExceptionWhenSinceIsNull() {
            assertThatThrownBy(() -> otpTokenService.countEmailVerificationOtpsInWindow(VALID_EMAIL, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'since' cannot be null");
        }

        @Test
        @DisplayName("Should count email verification OTPs in window successfully")
        void shouldCountEmailVerificationOtpsInWindowSuccessfully() {
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);
            long expectedCount = 5L;
            when(otpTokenRepository.countByCustomerEmailEmailAndPurposeAndCreatedDateAfter(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), any(OffsetDateTime.class)))
                    .thenReturn(expectedCount);

            long result = otpTokenService.countEmailVerificationOtpsInWindow(VALID_EMAIL, since);

            assertThat(result).isEqualTo(expectedCount);
            verify(otpTokenRepository).countByCustomerEmailEmailAndPurposeAndCreatedDateAfter(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), eq(since));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);
            when(otpTokenRepository.countByCustomerEmailEmailAndPurposeAndCreatedDateAfter(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.countEmailVerificationOtpsInWindow(VALID_EMAIL, since))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("invalidateActiveEmailVerificationOtps() Tests")
    class InvalidateActiveEmailVerificationOtpsTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> otpTokenService.invalidateActiveEmailVerificationOtps(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should invalidate active email verification OTPs successfully")
        void shouldInvalidateActiveEmailVerificationOtpsSuccessfully() {
            List<OtpToken> activeOtps = List.of(sampleOtpToken);
            when(otpTokenRepository.findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                    .thenReturn(activeOtps);
            when(otpTokenRepository.markOtpsAsUsed(any(List.class), any(OffsetDateTime.class)))
                    .thenReturn(1);

            int result = otpTokenService.invalidateActiveEmailVerificationOtps(VALID_EMAIL);

            assertThat(result).isEqualTo(1);
            verify(otpTokenRepository).findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION));
            verify(otpTokenRepository).markOtpsAsUsed(eq(activeOtps), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should return 0 when no active OTPs exist")
        void shouldReturn0WhenNoActiveOtpsExist() {
            when(otpTokenRepository.findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                    .thenReturn(List.of());

            int result = otpTokenService.invalidateActiveEmailVerificationOtps(VALID_EMAIL);

            assertThat(result).isEqualTo(0);
            verify(otpTokenRepository).findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION));
            verify(otpTokenRepository, never()).markOtpsAsUsed(any(List.class), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(anyString(), eq(OtpPurpose.EMAIL_VERIFICATION)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.invalidateActiveEmailVerificationOtps(VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== PHONE VERIFICATION OPERATIONS TESTS =====

    @Nested
    @DisplayName("findValidPhoneVerificationOtp() Tests")
    class FindValidPhoneVerificationOtpTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpCode is null")
        void shouldThrowNullFieldExceptionWhenOtpCodeIsNull() {
            assertThatThrownBy(() -> otpTokenService.findValidPhoneVerificationOtp(null, VALID_PHONE))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpCode' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phone' cannot be null");
        }

        @Test
        @DisplayName("Should find valid phone verification OTP successfully")
        void shouldFindValidPhoneVerificationOtpSuccessfully() {
            OtpToken phoneOtpToken = new OtpToken();
            phoneOtpToken.setId(VALID_ID);
            phoneOtpToken.setCustomerPhone(samplePhone);
            phoneOtpToken.setPurpose(OtpPurpose.PHONE_VERIFICATION);

            when(otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PHONE_VERIFICATION), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.of(phoneOtpToken));

            OtpToken result = otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE);

            assertThat(result).isEqualTo(phoneOtpToken);
            verify(otpTokenRepository).findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PHONE_VERIFICATION), any(OffsetDateTime.class), eq(3));
        }

        @Test
        @DisplayName("Should throw OtpTokenNotFoundException when valid OTP does not exist")
        void shouldThrowOtpTokenNotFoundExceptionWhenValidOtpDoesNotExist() {
            when(otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PHONE_VERIFICATION), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .isInstanceOf(OtpTokenNotFoundException.class)
                    .hasMessage("OTP token not found: " + VALID_OTP_CODE);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PHONE_VERIFICATION), any(OffsetDateTime.class), eq(3)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.findValidPhoneVerificationOtp(VALID_OTP_CODE, VALID_PHONE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== BASIC READ OPERATIONS TESTS =====

    @Nested
    @DisplayName("getAllOtpTokens() Tests")
    class GetAllOtpTokensTests {

        @Test
        @DisplayName("Should return all OTP tokens successfully")
        void shouldReturnAllOtpTokensSuccessfully() {
            List<OtpToken> expectedTokens = List.of(sampleOtpToken);
            when(otpTokenRepository.findAll()).thenReturn(expectedTokens);

            List<OtpToken> result = otpTokenService.getAllOtpTokens();

            assertThat(result).isEqualTo(expectedTokens);
            verify(otpTokenRepository).findAll();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.getAllOtpTokens())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpTokenId is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIdIsNull() {
            assertThatThrownBy(() -> otpTokenService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpTokenId' cannot be null");
        }

        @Test
        @DisplayName("Should find OTP token by ID successfully")
        void shouldFindOtpTokenByIdSuccessfully() {
            when(otpTokenRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleOtpToken));

            OtpToken result = otpTokenService.findById(VALID_ID);

            assertThat(result).isEqualTo(sampleOtpToken);
            verify(otpTokenRepository).findById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw OtpTokenNotFoundException when OTP token does not exist")
        void shouldThrowOtpTokenNotFoundExceptionWhenOtpTokenDoesNotExist() {
            when(otpTokenRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpTokenService.findById(VALID_ID))
                    .isInstanceOf(OtpTokenNotFoundException.class)
                    .hasMessage("OTP token not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.findById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UPDATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("incrementAttemptCount() Tests")
    class IncrementAttemptCountTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpTokenId is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIdIsNull() {
            assertThatThrownBy(() -> otpTokenService.incrementAttemptCount(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpTokenId' cannot be null");
        }

        @Test
        @DisplayName("Should increment attempt count successfully")
        void shouldIncrementAttemptCountSuccessfully() {
            int expectedRowsAffected = 1;
            when(otpTokenRepository.incrementAttemptCount(eq(VALID_ID), any(OffsetDateTime.class)))
                    .thenReturn(expectedRowsAffected);

            int result = otpTokenService.incrementAttemptCount(VALID_ID);

            assertThat(result).isEqualTo(expectedRowsAffected);
            verify(otpTokenRepository).incrementAttemptCount(eq(VALID_ID), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.incrementAttemptCount(eq(VALID_ID), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.incrementAttemptCount(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("markOtpAsUsed() Tests")
    class MarkOtpAsUsedTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpTokenId is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIdIsNull() {
            assertThatThrownBy(() -> otpTokenService.markOtpAsUsed(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpTokenId' cannot be null");
        }

        @Test
        @DisplayName("Should mark OTP as used successfully")
        void shouldMarkOtpAsUsedSuccessfully() {
            int expectedRowsAffected = 1;
            when(otpTokenRepository.markOtpAsUsed(eq(VALID_ID), any(OffsetDateTime.class)))
                    .thenReturn(expectedRowsAffected);

            int result = otpTokenService.markOtpAsUsed(VALID_ID);

            assertThat(result).isEqualTo(expectedRowsAffected);
            verify(otpTokenRepository).markOtpAsUsed(eq(VALID_ID), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.markOtpAsUsed(eq(VALID_ID), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.markOtpAsUsed(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("saveOtpToken() Tests")
    class SaveOtpTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpToken is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIsNull() {
            assertThatThrownBy(() -> otpTokenService.saveOtpToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpToken' cannot be null");
        }

        @Test
        @DisplayName("Should save OTP token successfully")
        void shouldSaveOtpTokenSuccessfully() {
            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(sampleOtpToken);

            OtpToken result = otpTokenService.saveOtpToken(sampleOtpToken);

            assertThat(result).isNotNull();
            verify(otpTokenRepository).save(sampleOtpToken);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.save(any(OtpToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.saveOtpToken(sampleOtpToken))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteById() Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpTokenId is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIdIsNull() {
            assertThatThrownBy(() -> otpTokenService.deleteById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpTokenId' cannot be null");
        }

        @Test
        @DisplayName("Should delete OTP token by ID successfully")
        void shouldDeleteOtpTokenByIdSuccessfully() {
            when(otpTokenRepository.existsById(VALID_ID)).thenReturn(true);

            assertThatCode(() -> otpTokenService.deleteById(VALID_ID))
                    .doesNotThrowAnyException();

            verify(otpTokenRepository).deleteById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw OtpTokenNotFoundException when OTP token does not exist")
        void shouldThrowOtpTokenNotFoundExceptionWhenOtpTokenDoesNotExist() {
            when(otpTokenRepository.existsById(VALID_ID)).thenReturn(false);

            assertThatThrownBy(() -> otpTokenService.deleteById(VALID_ID))
                    .isInstanceOf(OtpTokenNotFoundException.class)
                    .hasMessage("OTP token not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.deleteById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteOtpToken() Tests")
    class DeleteOtpTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpToken is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIsNull() {
            assertThatThrownBy(() -> otpTokenService.deleteOtpToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpToken' cannot be null");
        }

        @Test
        @DisplayName("Should delete OTP token successfully")
        void shouldDeleteOtpTokenSuccessfully() {
            otpTokenService.deleteOtpToken(sampleOtpToken);

            verify(otpTokenRepository).delete(sampleOtpToken);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(otpTokenRepository).delete(any(OtpToken.class));

            assertThatThrownBy(() -> otpTokenService.deleteOtpToken(sampleOtpToken))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UTILITY OPERATIONS TESTS =====

    @Nested
    @DisplayName("otpTokenExists() Tests")
    class OtpTokenExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when otpTokenId is null")
        void shouldThrowNullFieldExceptionWhenOtpTokenIdIsNull() {
            assertThatThrownBy(() -> otpTokenService.otpTokenExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'otpTokenId' cannot be null");
        }

        @Test
        @DisplayName("Should return true when OTP token exists")
        void shouldReturnTrueWhenOtpTokenExists() {
            when(otpTokenRepository.existsById(VALID_ID)).thenReturn(true);

            boolean result = otpTokenService.otpTokenExists(VALID_ID);

            assertThat(result).isTrue();
            verify(otpTokenRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should return false when OTP token does not exist")
        void shouldReturnFalseWhenOtpTokenDoesNotExist() {
            when(otpTokenRepository.existsById(VALID_ID)).thenReturn(false);

            boolean result = otpTokenService.otpTokenExists(VALID_ID);

            assertThat(result).isFalse();
            verify(otpTokenRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.otpTokenExists(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countAllOtpTokens() Tests")
    class CountAllOtpTokensTests {

        @Test
        @DisplayName("Should return total OTP tokens count successfully")
        void shouldReturnTotalOtpTokensCountSuccessfully() {
            long expectedCount = 25L;
            when(otpTokenRepository.count()).thenReturn(expectedCount);

            long result = otpTokenService.countAllOtpTokens();

            assertThat(result).isEqualTo(expectedCount);
            verify(otpTokenRepository).count();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(otpTokenRepository.count())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> otpTokenService.countAllOtpTokens())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== PASSWORD RESET OPERATIONS TESTS (Sample) =====

    @Nested
    @DisplayName("findValidPasswordResetEmailOtp() Tests")
    class FindValidPasswordResetEmailOtpTests {

        @Test
        @DisplayName("Should find valid password reset email OTP successfully")
        void shouldFindValidPasswordResetEmailOtpSuccessfully() {
            OtpToken passwordResetOtpToken = new OtpToken();
            passwordResetOtpToken.setId(VALID_ID);
            passwordResetOtpToken.setCustomerEmail(sampleEmail);
            passwordResetOtpToken.setPurpose(OtpPurpose.PASSWORD_RESET);
            passwordResetOtpToken.setDeliveryMethod(OtpDeliveryMethod.EMAIL);

            when(otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PASSWORD_RESET), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.of(passwordResetOtpToken));

            OtpToken result = otpTokenService.findValidPasswordResetEmailOtp(VALID_OTP_CODE, VALID_EMAIL);

            assertThat(result).isEqualTo(passwordResetOtpToken);
            verify(otpTokenRepository).findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PASSWORD_RESET), any(OffsetDateTime.class), eq(3));
        }

        @Test
        @DisplayName("Should throw OtpTokenNotFoundException when valid password reset OTP does not exist")
        void shouldThrowOtpTokenNotFoundExceptionWhenValidPasswordResetOtpDoesNotExist() {
            when(otpTokenRepository.findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PASSWORD_RESET), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpTokenService.findValidPasswordResetEmailOtp(VALID_OTP_CODE, VALID_EMAIL))
                    .isInstanceOf(OtpTokenNotFoundException.class)
                    .hasMessage("OTP token not found: " + VALID_OTP_CODE);
        }
    }

    @Nested
    @DisplayName("findValidPasswordResetPhoneOtp() Tests")
    class FindValidPasswordResetPhoneOtpTests {

        @Test
        @DisplayName("Should find valid password reset phone OTP successfully")
        void shouldFindValidPasswordResetPhoneOtpSuccessfully() {
            OtpToken passwordResetPhoneOtpToken = new OtpToken();
            passwordResetPhoneOtpToken.setId(VALID_ID);
            passwordResetPhoneOtpToken.setCustomerPhone(samplePhone);
            passwordResetPhoneOtpToken.setPurpose(OtpPurpose.PASSWORD_RESET);
            passwordResetPhoneOtpToken.setDeliveryMethod(OtpDeliveryMethod.SMS);

            when(otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PASSWORD_RESET), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.of(passwordResetPhoneOtpToken));

            OtpToken result = otpTokenService.findValidPasswordResetPhoneOtp(VALID_OTP_CODE, VALID_PHONE);

            assertThat(result).isEqualTo(passwordResetPhoneOtpToken);
            verify(otpTokenRepository).findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PASSWORD_RESET), any(OffsetDateTime.class), eq(3));
        }

        @Test
        @DisplayName("Should throw OtpTokenNotFoundException when valid password reset phone OTP does not exist")
        void shouldThrowOtpTokenNotFoundExceptionWhenValidPasswordResetPhoneOtpDoesNotExist() {
            when(otpTokenRepository.findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
                    anyString(), anyString(), eq(OtpPurpose.PASSWORD_RESET), any(OffsetDateTime.class), eq(3)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpTokenService.findValidPasswordResetPhoneOtp(VALID_OTP_CODE, VALID_PHONE))
                    .isInstanceOf(OtpTokenNotFoundException.class)
                    .hasMessage("OTP token not found: " + VALID_OTP_CODE);
        }
    }
}
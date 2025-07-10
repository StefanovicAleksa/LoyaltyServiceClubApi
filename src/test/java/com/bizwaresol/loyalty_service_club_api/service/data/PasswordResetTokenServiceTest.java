package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.PasswordResetTokenRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PasswordResetTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenService Unit Tests")
class PasswordResetTokenServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetTokenService passwordResetTokenService;

    private PasswordResetToken sampleToken;
    private CustomerAccount sampleCustomerAccount;
    private final String VALID_TOKEN = "test-token-string-123";
    private final Long VALID_TOKEN_ID = 1L;
    private final Long VALID_CUSTOMER_ACCOUNT_ID = 2L;
    private final Duration CUSTOM_EXPIRY = Duration.ofHours(12);

    @BeforeEach
    void setUp() {
        sampleCustomerAccount = new CustomerAccount();
        sampleCustomerAccount.setId(VALID_CUSTOMER_ACCOUNT_ID);
        sampleCustomerAccount.setUsername("testuser");

        sampleToken = new PasswordResetToken();
        sampleToken.setId(VALID_TOKEN_ID);
        sampleToken.setToken(VALID_TOKEN);
        sampleToken.setCustomerAccount(sampleCustomerAccount);
        sampleToken.setExpiresAt(OffsetDateTime.now().plusHours(24));
        sampleToken.setUsed(false);
        sampleToken.setCreatedDate(OffsetDateTime.now());
        sampleToken.setLastModifiedDate(OffsetDateTime.now());
    }

    // ===== CREATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("createToken() Tests")
    class CreateTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccount is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.createToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccount' cannot be null");
        }

        @Test
        @DisplayName("Should create token successfully with default expiry")
        void shouldCreateTokenSuccessfullyWithDefaultExpiry() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(sampleToken);

            PasswordResetToken result = passwordResetTokenService.createToken(sampleCustomerAccount);

            assertThat(result).isNotNull();
            assertThat(result.getCustomerAccount()).isEqualTo(sampleCustomerAccount);
            assertThat(result.isUsed()).isFalse();

            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.createToken(sampleCustomerAccount))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("createTokenWithExpiry() Tests")
    class CreateTokenWithExpiryTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccount is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.createTokenWithExpiry(null, CUSTOM_EXPIRY))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccount' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when expiry is null")
        void shouldThrowNullFieldExceptionWhenExpiryIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.createTokenWithExpiry(sampleCustomerAccount, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'expiry' cannot be null");
        }

        @Test
        @DisplayName("Should create token successfully with custom expiry")
        void shouldCreateTokenSuccessfullyWithCustomExpiry() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(sampleToken);

            PasswordResetToken result = passwordResetTokenService.createTokenWithExpiry(sampleCustomerAccount, CUSTOM_EXPIRY);

            assertThat(result).isNotNull();
            assertThat(result.getCustomerAccount()).isEqualTo(sampleCustomerAccount);
            assertThat(result.isUsed()).isFalse();

            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.createTokenWithExpiry(sampleCustomerAccount, CUSTOM_EXPIRY))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== READ OPERATIONS TESTS =====

    @Nested
    @DisplayName("findByToken() Tests")
    class FindByTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when token is null")
        void shouldThrowNullFieldExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.findByToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'token' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when token is empty")
        void shouldThrowEmptyFieldExceptionWhenTokenIsEmpty() {
            assertThatThrownBy(() -> passwordResetTokenService.findByToken("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'token' cannot be empty");
        }

        @Test
        @DisplayName("Should find token by token string successfully")
        void shouldFindTokenByTokenStringSuccessfully() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(sampleToken));

            PasswordResetToken result = passwordResetTokenService.findByToken(VALID_TOKEN);

            assertThat(result).isEqualTo(sampleToken);
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token does not exist")
        void shouldThrowPasswordResetTokenNotFoundExceptionWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.findByToken(VALID_TOKEN))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class)
                    .hasMessage("Password reset token not found: " + VALID_TOKEN);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.findByToken(VALID_TOKEN))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when tokenId is null")
        void shouldThrowNullFieldExceptionWhenTokenIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'tokenId' cannot be null");
        }

        @Test
        @DisplayName("Should find token by ID successfully")
        void shouldFindTokenByIdSuccessfully() {
            when(passwordResetTokenRepository.findById(VALID_TOKEN_ID)).thenReturn(Optional.of(sampleToken));

            PasswordResetToken result = passwordResetTokenService.findById(VALID_TOKEN_ID);

            assertThat(result).isEqualTo(sampleToken);
            verify(passwordResetTokenRepository).findById(VALID_TOKEN_ID);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token does not exist")
        void shouldThrowPasswordResetTokenNotFoundExceptionWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findById(VALID_TOKEN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.findById(VALID_TOKEN_ID))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class)
                    .hasMessage("Password reset token not found with ID: " + VALID_TOKEN_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findById(VALID_TOKEN_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.findById(VALID_TOKEN_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findActiveTokensForAccount() Tests")
    class FindActiveTokensForAccountTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccountId is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.findActiveTokensForAccount(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccountId' cannot be null");
        }

        @Test
        @DisplayName("Should find active tokens for account successfully")
        void shouldFindActiveTokensForAccountSuccessfully() {
            List<PasswordResetToken> expectedTokens = List.of(sampleToken);
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class))).thenReturn(expectedTokens);

            List<PasswordResetToken> result = passwordResetTokenService.findActiveTokensForAccount(VALID_CUSTOMER_ACCOUNT_ID);

            assertThat(result).isEqualTo(expectedTokens);
            verify(passwordResetTokenRepository).findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.findActiveTokensForAccount(VALID_CUSTOMER_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findExpiredTokens() Tests")
    class FindExpiredTokensTests {

        @Test
        @DisplayName("Should find expired tokens successfully")
        void shouldFindExpiredTokensSuccessfully() {
            List<PasswordResetToken> expectedTokens = List.of(sampleToken);
            when(passwordResetTokenRepository.findByExpiresAtBefore(any(OffsetDateTime.class))).thenReturn(expectedTokens);

            List<PasswordResetToken> result = passwordResetTokenService.findExpiredTokens();

            assertThat(result).isEqualTo(expectedTokens);
            verify(passwordResetTokenRepository).findByExpiresAtBefore(any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByExpiresAtBefore(any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.findExpiredTokens())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("tokenExists() Tests")
    class TokenExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when token is null")
        void shouldThrowNullFieldExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.tokenExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'token' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when token is empty")
        void shouldThrowEmptyFieldExceptionWhenTokenIsEmpty() {
            assertThatThrownBy(() -> passwordResetTokenService.tokenExists("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'token' cannot be empty");
        }

        @Test
        @DisplayName("Should return true when token exists")
        void shouldReturnTrueWhenTokenExists() {
            when(passwordResetTokenRepository.existsByToken(VALID_TOKEN)).thenReturn(true);

            boolean result = passwordResetTokenService.tokenExists(VALID_TOKEN);

            assertThat(result).isTrue();
            verify(passwordResetTokenRepository).existsByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should return false when token does not exist")
        void shouldReturnFalseWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.existsByToken(VALID_TOKEN)).thenReturn(false);

            boolean result = passwordResetTokenService.tokenExists(VALID_TOKEN);

            assertThat(result).isFalse();
            verify(passwordResetTokenRepository).existsByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.existsByToken(VALID_TOKEN))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.tokenExists(VALID_TOKEN))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("isTokenValid() Tests")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should throw NullFieldException when token is null")
        void shouldThrowNullFieldExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.isTokenValid(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'token' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when token is empty")
        void shouldThrowEmptyFieldExceptionWhenTokenIsEmpty() {
            assertThatThrownBy(() -> passwordResetTokenService.isTokenValid("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'token' cannot be empty");
        }

        @Test
        @DisplayName("Should return true when token is valid")
        void shouldReturnTrueWhenTokenIsValid() {
            // Setup valid token (not used, not expired)
            sampleToken.setUsed(false);
            sampleToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(sampleToken));

            boolean result = passwordResetTokenService.isTokenValid(VALID_TOKEN);

            assertThat(result).isTrue();
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should return false when token is used")
        void shouldReturnFalseWhenTokenIsUsed() {
            // Setup used token
            sampleToken.setUsed(true);
            sampleToken.setExpiresAt(OffsetDateTime.now().plusHours(1));
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(sampleToken));

            boolean result = passwordResetTokenService.isTokenValid(VALID_TOKEN);

            assertThat(result).isFalse();
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should return false when token is expired")
        void shouldReturnFalseWhenTokenIsExpired() {
            // Setup expired token
            sampleToken.setUsed(false);
            sampleToken.setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(sampleToken));

            boolean result = passwordResetTokenService.isTokenValid(VALID_TOKEN);

            assertThat(result).isFalse();
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should return false when token does not exist")
        void shouldReturnFalseWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.empty());

            boolean result = passwordResetTokenService.isTokenValid(VALID_TOKEN);

            assertThat(result).isFalse();
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.isTokenValid(VALID_TOKEN))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countActiveTokensForAccount() Tests")
    class CountActiveTokensForAccountTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccountId is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.countActiveTokensForAccount(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccountId' cannot be null");
        }

        @Test
        @DisplayName("Should return active tokens count successfully")
        void shouldReturnActiveTokensCountSuccessfully() {
            List<PasswordResetToken> tokens = List.of(sampleToken, sampleToken);
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class))).thenReturn(tokens);

            long result = passwordResetTokenService.countActiveTokensForAccount(VALID_CUSTOMER_ACCOUNT_ID);

            assertThat(result).isEqualTo(2L);
            verify(passwordResetTokenRepository).findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.countActiveTokensForAccount(VALID_CUSTOMER_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UPDATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("markTokenAsUsed() Tests")
    class MarkTokenAsUsedTests {

        @Test
        @DisplayName("Should throw NullFieldException when token is null")
        void shouldThrowNullFieldExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.markTokenAsUsed(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'token' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when token is empty")
        void shouldThrowEmptyFieldExceptionWhenTokenIsEmpty() {
            assertThatThrownBy(() -> passwordResetTokenService.markTokenAsUsed("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'token' cannot be empty");
        }

        @Test
        @DisplayName("Should mark token as used successfully")
        void shouldMarkTokenAsUsedSuccessfully() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(sampleToken));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(sampleToken);

            PasswordResetToken result = passwordResetTokenService.markTokenAsUsed(VALID_TOKEN);

            assertThat(result).isNotNull();
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token does not exist")
        void shouldThrowPasswordResetTokenNotFoundExceptionWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.markTokenAsUsed(VALID_TOKEN))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class)
                    .hasMessage("Password reset token not found: " + VALID_TOKEN);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.markTokenAsUsed(VALID_TOKEN))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("saveToken() Tests")
    class SaveTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when passwordResetToken is null")
        void shouldThrowNullFieldExceptionWhenPasswordResetTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.saveToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'passwordResetToken' cannot be null");
        }

        @Test
        @DisplayName("Should save token successfully")
        void shouldSaveTokenSuccessfully() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(sampleToken);

            PasswordResetToken result = passwordResetTokenService.saveToken(sampleToken);

            assertThat(result).isNotNull();
            verify(passwordResetTokenRepository).save(sampleToken);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.saveToken(sampleToken))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteToken() Tests")
    class DeleteTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when passwordResetToken is null")
        void shouldThrowNullFieldExceptionWhenPasswordResetTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.deleteToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'passwordResetToken' cannot be null");
        }

        @Test
        @DisplayName("Should delete token successfully")
        void shouldDeleteTokenSuccessfully() {
            passwordResetTokenService.deleteToken(sampleToken);

            verify(passwordResetTokenRepository).delete(sampleToken);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(passwordResetTokenRepository).delete(any(PasswordResetToken.class));

            assertThatThrownBy(() -> passwordResetTokenService.deleteToken(sampleToken))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteByToken() Tests")
    class DeleteByTokenTests {

        @Test
        @DisplayName("Should throw NullFieldException when token is null")
        void shouldThrowNullFieldExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.deleteByToken(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'token' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when token is empty")
        void shouldThrowEmptyFieldExceptionWhenTokenIsEmpty() {
            assertThatThrownBy(() -> passwordResetTokenService.deleteByToken("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'token' cannot be empty");
        }

        @Test
        @DisplayName("Should delete token by token string successfully")
        void shouldDeleteTokenByTokenStringSuccessfully() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(sampleToken));

            passwordResetTokenService.deleteByToken(VALID_TOKEN);

            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN);
            verify(passwordResetTokenRepository).delete(sampleToken);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token does not exist")
        void shouldThrowPasswordResetTokenNotFoundExceptionWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.deleteByToken(VALID_TOKEN))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class)
                    .hasMessage("Password reset token not found: " + VALID_TOKEN);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.deleteByToken(VALID_TOKEN))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteExpiredTokens() Tests")
    class DeleteExpiredTokensTests {

        @Test
        @DisplayName("Should delete expired tokens successfully")
        void shouldDeleteExpiredTokensSuccessfully() {
            passwordResetTokenService.deleteExpiredTokens();

            verify(passwordResetTokenRepository).deleteByExpiresAtBefore(any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(passwordResetTokenRepository).deleteByExpiresAtBefore(any(OffsetDateTime.class));

            assertThatThrownBy(() -> passwordResetTokenService.deleteExpiredTokens())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteAllTokensForAccount() Tests")
    class DeleteAllTokensForAccountTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccountId is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.deleteAllTokensForAccount(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccountId' cannot be null");
        }

        @Test
        @DisplayName("Should delete all tokens for account successfully")
        void shouldDeleteAllTokensForAccountSuccessfully() {
            List<PasswordResetToken> tokens = List.of(sampleToken);
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class))).thenReturn(tokens);

            passwordResetTokenService.deleteAllTokensForAccount(VALID_CUSTOMER_ACCOUNT_ID);

            verify(passwordResetTokenRepository).findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class));
            verify(passwordResetTokenRepository).deleteAll(tokens);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.deleteAllTokensForAccount(VALID_CUSTOMER_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== BUSINESS LOGIC OPERATIONS TESTS =====

    @Nested
    @DisplayName("invalidateAllAccountTokens() Tests")
    class InvalidateAllAccountTokensTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccountId is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.invalidateAllAccountTokens(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccountId' cannot be null");
        }

        @Test
        @DisplayName("Should invalidate all account tokens successfully")
        void shouldInvalidateAllAccountTokensSuccessfully() {
            List<PasswordResetToken> tokens = List.of(sampleToken);
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class))).thenReturn(tokens);
            when(passwordResetTokenRepository.saveAll(any())).thenReturn(tokens);

            passwordResetTokenService.invalidateAllAccountTokens(VALID_CUSTOMER_ACCOUNT_ID);

            verify(passwordResetTokenRepository).findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class));
            verify(passwordResetTokenRepository).saveAll(tokens);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(
                    eq(VALID_CUSTOMER_ACCOUNT_ID), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.invalidateAllAccountTokens(VALID_CUSTOMER_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens() Tests")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("Should cleanup expired tokens successfully")
        void shouldCleanupExpiredTokensSuccessfully() {
            passwordResetTokenService.cleanupExpiredTokens();

            verify(passwordResetTokenRepository).deleteByExpiresAtBefore(any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(passwordResetTokenRepository).deleteByExpiresAtBefore(any(OffsetDateTime.class));

            assertThatThrownBy(() -> passwordResetTokenService.cleanupExpiredTokens())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }
}
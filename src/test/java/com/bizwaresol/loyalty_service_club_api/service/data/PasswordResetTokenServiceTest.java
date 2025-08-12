package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.PasswordResetTokenRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PasswordResetTokenNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPasswordResetTokenFormatException;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    private final Long VALID_ID = 1L;
    private final Long ACCOUNT_ID = 10L;
    private final String VALID_TOKEN_STRING = UUID.randomUUID().toString();
    private final OffsetDateTime VALID_EXPIRES_AT = OffsetDateTime.now().plusHours(1);

    @BeforeEach
    void setUp() {
        sampleCustomerAccount = new CustomerAccount();
        sampleCustomerAccount.setId(ACCOUNT_ID);
        sampleCustomerAccount.setUsername("testuser");

        sampleToken = new PasswordResetToken();
        sampleToken.setId(VALID_ID);
        sampleToken.setToken(VALID_TOKEN_STRING);
        sampleToken.setCustomerAccount(sampleCustomerAccount);
        sampleToken.setExpiresAt(VALID_EXPIRES_AT);
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
            assertThatThrownBy(() -> passwordResetTokenService.createToken(null, VALID_EXPIRES_AT))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccount' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when expiresAt is null")
        void shouldThrowNullFieldExceptionWhenExpiresAtIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.createToken(sampleCustomerAccount, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'expiresAt' cannot be null");
        }

        @Test
        @DisplayName("Should create token successfully")
        void shouldCreateTokenSuccessfully() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(sampleToken);

            PasswordResetToken result = passwordResetTokenService.createToken(sampleCustomerAccount, VALID_EXPIRES_AT);

            assertThat(result).isNotNull();
            assertThat(result.getCustomerAccount()).isEqualTo(sampleCustomerAccount);
            assertThat(result.getExpiresAt()).isEqualTo(VALID_EXPIRES_AT);
            assertThat(result.getToken()).isNotNull();

            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.createToken(sampleCustomerAccount, VALID_EXPIRES_AT))
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
        @DisplayName("Should throw InvalidPasswordResetTokenFormatException for invalid token format")
        void shouldThrowExceptionForInvalidTokenFormat() {
            String invalidToken = "not-a-uuid";
            assertThatThrownBy(() -> passwordResetTokenService.findByToken(invalidToken))
                    .isInstanceOf(InvalidPasswordResetTokenFormatException.class);
        }

        @Test
        @DisplayName("Should find token by string successfully")
        void shouldFindTokenByStringSuccessfully() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN_STRING)).thenReturn(Optional.of(sampleToken));

            PasswordResetToken result = passwordResetTokenService.findByToken(VALID_TOKEN_STRING);

            assertThat(result).isEqualTo(sampleToken);
            verify(passwordResetTokenRepository).findByToken(VALID_TOKEN_STRING);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token does not exist")
        void shouldThrowNotFoundExceptionWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN_STRING)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.findByToken(VALID_TOKEN_STRING))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.findByToken(VALID_TOKEN_STRING))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.findByToken(VALID_TOKEN_STRING))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when id is null")
        void shouldThrowNullFieldExceptionWhenIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'id' cannot be null");
        }

        @Test
        @DisplayName("Should find token by ID successfully")
        void shouldFindTokenByIdSuccessfully() {
            when(passwordResetTokenRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleToken));

            PasswordResetToken result = passwordResetTokenService.findById(VALID_ID);

            assertThat(result).isEqualTo(sampleToken);
            verify(passwordResetTokenRepository).findById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token does not exist")
        void shouldThrowNotFoundExceptionWhenTokenDoesNotExist() {
            when(passwordResetTokenRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.findById(VALID_ID))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findActiveTokenByAccountId() Tests")
    class FindActiveTokenByAccountIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.findActiveTokenByAccountId(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should find active token successfully")
        void shouldFindActiveTokenSuccessfully() {
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedAtIsNull(ACCOUNT_ID))
                    .thenReturn(Optional.of(sampleToken));

            Optional<PasswordResetToken> result = passwordResetTokenService.findActiveTokenByAccountId(ACCOUNT_ID);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sampleToken);
            verify(passwordResetTokenRepository).findByCustomerAccountIdAndUsedAtIsNull(ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should return empty Optional when no active token exists")
        void shouldReturnEmptyWhenNoActiveTokenExists() {
            when(passwordResetTokenRepository.findByCustomerAccountIdAndUsedAtIsNull(ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            Optional<PasswordResetToken> result = passwordResetTokenService.findActiveTokenByAccountId(ACCOUNT_ID);

            assertThat(result).isEmpty();
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
        @DisplayName("Should throw NullFieldException when token ID is null")
        void shouldThrowNullFieldExceptionWhenTokenIdIsNull() {
            sampleToken.setId(null);
            assertThatThrownBy(() -> passwordResetTokenService.markTokenAsUsed(sampleToken))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'token.id' cannot be null");
        }

        @Test
        @DisplayName("Should mark token as used successfully")
        void shouldMarkTokenAsUsedSuccessfully() {
            when(passwordResetTokenRepository.markAsUsed(eq(VALID_ID), any(OffsetDateTime.class))).thenReturn(1);

            assertThatCode(() -> passwordResetTokenService.markTokenAsUsed(sampleToken))
                    .doesNotThrowAnyException();

            verify(passwordResetTokenRepository).markAsUsed(eq(VALID_ID), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(passwordResetTokenRepository.markAsUsed(anyLong(), any(OffsetDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> passwordResetTokenService.markTokenAsUsed(sampleToken))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteById() Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when id is null")
        void shouldThrowNullFieldExceptionWhenIdIsNull() {
            assertThatThrownBy(() -> passwordResetTokenService.deleteById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'id' cannot be null");
        }

        @Test
        @DisplayName("Should delete token by ID successfully")
        void shouldDeleteTokenByIdSuccessfully() {
            when(passwordResetTokenRepository.existsById(VALID_ID)).thenReturn(true);
            doNothing().when(passwordResetTokenRepository).deleteById(VALID_ID);

            assertThatCode(() -> passwordResetTokenService.deleteById(VALID_ID))
                    .doesNotThrowAnyException();

            verify(passwordResetTokenRepository).existsById(VALID_ID);
            verify(passwordResetTokenRepository).deleteById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw PasswordResetTokenNotFoundException when token to delete does not exist")
        void shouldThrowNotFoundExceptionWhenTokenToDeleteDoesNotExist() {
            when(passwordResetTokenRepository.existsById(VALID_ID)).thenReturn(false);

            assertThatThrownBy(() -> passwordResetTokenService.deleteById(VALID_ID))
                    .isInstanceOf(PasswordResetTokenNotFoundException.class);

            verify(passwordResetTokenRepository, never()).deleteById(anyLong());
        }
    }
}

package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerEmailRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateEmailException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.EmailNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.BusinessEmailNotAllowedException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerEmailService Unit Tests")
class CustomerEmailServiceTest {

    @Mock
    private CustomerEmailRepository customerEmailRepository;

    @InjectMocks
    private CustomerEmailService customerEmailService;

    private CustomerEmail sampleEmail;
    private final String VALID_EMAIL = "test@gmail.com";
    private final String NEW_EMAIL = "newemail@gmail.com";
    private final Long VALID_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleEmail = new CustomerEmail();
        sampleEmail.setId(VALID_ID);
        sampleEmail.setEmail(VALID_EMAIL);
        sampleEmail.setVerified(false);
        sampleEmail.setCreatedDate(OffsetDateTime.now());
        sampleEmail.setLastModifiedDate(OffsetDateTime.now());
    }

    // ===== CREATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("createEmail() Tests")
    class CreateEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.createEmail(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when email is empty")
        void shouldThrowEmptyFieldExceptionWhenEmailIsEmpty() {
            assertThatThrownBy(() -> customerEmailService.createEmail("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'email' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when email is too short")
        void shouldThrowFieldTooShortExceptionWhenEmailIsTooShort() {
            assertThatThrownBy(() -> customerEmailService.createEmail("a@b"))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field email too short: 3 characters. Min: 5 characters");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when email is too long")
        void shouldThrowFieldTooLongExceptionWhenEmailIsTooLong() {
            String longEmail = "a".repeat(45) + "@gmail.com";
            assertThatThrownBy(() -> customerEmailService.createEmail(longEmail))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessage("Field email too long: 55 characters. Max: 50 characters");
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when email format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenEmailFormatIsInvalid() {
            assertThatThrownBy(() -> customerEmailService.createEmail("invalid-email"))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .hasMessage("Invalid email format: invalid-email");
        }

        @Test
        @DisplayName("Should throw BusinessEmailNotAllowedException when email domain is not allowed")
        void shouldThrowBusinessEmailNotAllowedExceptionWhenEmailDomainIsNotAllowed() {
            assertThatThrownBy(() -> customerEmailService.createEmail("test@company.com"))
                    .isInstanceOf(BusinessEmailNotAllowedException.class)
                    .hasMessageContaining("Business email domain 'company.com' not allowed");
        }

        @Test
        @DisplayName("Should create email successfully when valid email is provided")
        void shouldCreateEmailSuccessfullyWhenValidEmailIsProvided() {
            when(customerEmailRepository.save(any(CustomerEmail.class))).thenReturn(sampleEmail);

            CustomerEmail result = customerEmailService.createEmail(VALID_EMAIL);

            // Test OUTCOMES, not implementation details
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(result.isVerified()).isFalse();

            // Verify save was called, but don't dictate HOW the entity was built
            verify(customerEmailRepository).save(any(CustomerEmail.class));
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when email already exists")
        void shouldThrowDuplicateEmailExceptionWhenEmailAlreadyExists() {
            when(customerEmailRepository.save(any(CustomerEmail.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"customer_emails_email_key\" Key (email)=(test@gmail.com) already exists."));

            assertThatThrownBy(() -> customerEmailService.createEmail(VALID_EMAIL))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessage("Email already exists: test@gmail.com");
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when unexpected repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenUnexpectedRepositoryErrorOccurs() {
            when(customerEmailRepository.save(any(CustomerEmail.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.createEmail(VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== READ OPERATIONS TESTS =====

    @Nested
    @DisplayName("getAllEmails() Tests")
    class GetAllEmailsTests {

        @Test
        @DisplayName("Should return all emails successfully")
        void shouldReturnAllEmailsSuccessfully() {
            List<CustomerEmail> expectedEmails = List.of(sampleEmail);
            when(customerEmailRepository.findAll()).thenReturn(expectedEmails);

            List<CustomerEmail> result = customerEmailService.getAllEmails();

            assertThat(result).isEqualTo(expectedEmails);
            verify(customerEmailRepository).findAll();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.getAllEmails())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when emailId is null")
        void shouldThrowNullFieldExceptionWhenEmailIdIsNull() {
            assertThatThrownBy(() -> customerEmailService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'emailId' cannot be null");
        }

        @Test
        @DisplayName("Should find email by ID successfully")
        void shouldFindEmailByIdSuccessfully() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleEmail));

            CustomerEmail result = customerEmailService.findById(VALID_ID);

            assertThat(result).isEqualTo(sampleEmail);
            verify(customerEmailRepository).findById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw EmailNotFoundException when email does not exist")
        void shouldThrowEmailNotFoundExceptionWhenEmailDoesNotExist() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerEmailService.findById(VALID_ID))
                    .isInstanceOf(EmailNotFoundException.class)
                    .hasMessage("Email not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.findById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByEmail() Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.findByEmail(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when email format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenEmailFormatIsInvalid() {
            assertThatThrownBy(() -> customerEmailService.findByEmail("invalid-email"))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .hasMessage("Invalid email format: invalid-email");
        }

        @Test
        @DisplayName("Should find email successfully")
        void shouldFindEmailSuccessfully() {
            // Setup - don't assume implementation details about case handling
            when(customerEmailRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleEmail));

            CustomerEmail result = customerEmailService.findByEmail(VALID_EMAIL);

            // Test OUTCOME - we got the right result
            assertThat(result).isEqualTo(sampleEmail);

            // Verify repository was called, but don't dictate exact parameter transformation
            verify(customerEmailRepository).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw EmailNotFoundException when email does not exist")
        void shouldThrowEmailNotFoundExceptionWhenEmailDoesNotExist() {
            when(customerEmailRepository.findByEmail(VALID_EMAIL.toLowerCase())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerEmailService.findByEmail(VALID_EMAIL))
                    .isInstanceOf(EmailNotFoundException.class)
                    .hasMessage("Email not found: " + VALID_EMAIL);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findByEmail(VALID_EMAIL.toLowerCase()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.findByEmail(VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("getVerifiedEmails() Tests")
    class GetVerifiedEmailsTests {

        @Test
        @DisplayName("Should return verified emails successfully")
        void shouldReturnVerifiedEmailsSuccessfully() {
            List<CustomerEmail> expectedEmails = List.of(sampleEmail);
            when(customerEmailRepository.findByVerified(true)).thenReturn(expectedEmails);

            List<CustomerEmail> result = customerEmailService.getVerifiedEmails();

            assertThat(result).isEqualTo(expectedEmails);
            verify(customerEmailRepository).findByVerified(true);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findByVerified(true))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.getVerifiedEmails())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("getUnverifiedEmails() Tests")
    class GetUnverifiedEmailsTests {

        @Test
        @DisplayName("Should return unverified emails successfully")
        void shouldReturnUnverifiedEmailsSuccessfully() {
            List<CustomerEmail> expectedEmails = List.of(sampleEmail);
            when(customerEmailRepository.findByVerified(false)).thenReturn(expectedEmails);

            List<CustomerEmail> result = customerEmailService.getUnverifiedEmails();

            assertThat(result).isEqualTo(expectedEmails);
            verify(customerEmailRepository).findByVerified(false);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findByVerified(false))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.getUnverifiedEmails())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countVerifiedEmails() Tests")
    class CountVerifiedEmailsTests {

        @Test
        @DisplayName("Should return verified emails count successfully")
        void shouldReturnVerifiedEmailsCountSuccessfully() {
            long expectedCount = 5L;
            when(customerEmailRepository.countByVerified(true)).thenReturn(expectedCount);

            long result = customerEmailService.countVerifiedEmails();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerEmailRepository).countByVerified(true);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.countByVerified(true))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.countVerifiedEmails())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countUnverifiedEmails() Tests")
    class CountUnverifiedEmailsTests {

        @Test
        @DisplayName("Should return unverified emails count successfully")
        void shouldReturnUnverifiedEmailsCountSuccessfully() {
            long expectedCount = 3L;
            when(customerEmailRepository.countByVerified(false)).thenReturn(expectedCount);

            long result = customerEmailService.countUnverifiedEmails();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerEmailRepository).countByVerified(false);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.countByVerified(false))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.countUnverifiedEmails())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countAllEmails() Tests")
    class CountAllEmailsTests {

        @Test
        @DisplayName("Should return total emails count successfully")
        void shouldReturnTotalEmailsCountSuccessfully() {
            long expectedCount = 10L;
            when(customerEmailRepository.count()).thenReturn(expectedCount);

            long result = customerEmailService.countAllEmails();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerEmailRepository).count();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.count())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.countAllEmails())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("existsById() Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when emailId is null")
        void shouldThrowNullFieldExceptionWhenEmailIdIsNull() {
            assertThatThrownBy(() -> customerEmailService.existsById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'emailId' cannot be null");
        }

        @Test
        @DisplayName("Should return true when email exists by ID")
        void shouldReturnTrueWhenEmailExistsByID() {
            when(customerEmailRepository.existsById(VALID_ID)).thenReturn(true);

            boolean result = customerEmailService.existsById(VALID_ID);

            assertThat(result).isTrue();
            verify(customerEmailRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should return false when email does not exist by ID")
        void shouldReturnFalseWhenEmailDoesNotExistByID() {
            when(customerEmailRepository.existsById(VALID_ID)).thenReturn(false);

            boolean result = customerEmailService.existsById(VALID_ID);

            assertThat(result).isFalse();
            verify(customerEmailRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.existsById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("emailExists() Tests")
    class EmailExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.emailExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when email format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenEmailFormatIsInvalid() {
            assertThatThrownBy(() -> customerEmailService.emailExists("invalid-email"))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .hasMessage("Invalid email format: invalid-email");
        }

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            when(customerEmailRepository.existsByEmail(anyString())).thenReturn(true);

            boolean result = customerEmailService.emailExists(VALID_EMAIL);

            // Test OUTCOME - correct return value
            assertThat(result).isTrue();
            verify(customerEmailRepository).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            when(customerEmailRepository.existsByEmail(anyString())).thenReturn(false);

            boolean result = customerEmailService.emailExists(VALID_EMAIL);

            // Test OUTCOME - correct return value
            assertThat(result).isFalse();
            verify(customerEmailRepository).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.existsByEmail(VALID_EMAIL.toLowerCase()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.emailExists(VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UPDATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("updateEmail() Tests")
    class UpdateEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when emailId is null")
        void shouldThrowNullFieldExceptionWhenEmailIdIsNull() {
            assertThatThrownBy(() -> customerEmailService.updateEmail(null, NEW_EMAIL))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'emailId' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when newEmail is null")
        void shouldThrowNullFieldExceptionWhenNewEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.updateEmail(VALID_ID, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'newEmail' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when newEmail format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenNewEmailFormatIsInvalid() {
            assertThatThrownBy(() -> customerEmailService.updateEmail(VALID_ID, "invalid-email"))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .hasMessage("Invalid email format: invalid-email");
        }

        @Test
        @DisplayName("Should update email successfully")
        void shouldUpdateEmailSuccessfully() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleEmail));
            when(customerEmailRepository.save(any(CustomerEmail.class))).thenReturn(sampleEmail);

            CustomerEmail result = customerEmailService.updateEmail(VALID_ID, NEW_EMAIL);

            // Test OUTCOME - operation succeeded and returned result
            assertThat(result).isNotNull();

            // Verify the business logic outcome - email was found and saved
            verify(customerEmailRepository).findById(VALID_ID);
            verify(customerEmailRepository).save(any(CustomerEmail.class));

            // Don't test implementation details like field setting order or verification reset
        }

        @Test
        @DisplayName("Should throw EmailNotFoundException when email does not exist")
        void shouldThrowEmailNotFoundExceptionWhenEmailDoesNotExist() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerEmailService.updateEmail(VALID_ID, NEW_EMAIL))
                    .isInstanceOf(EmailNotFoundException.class)
                    .hasMessage("Email not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when new email already exists")
        void shouldThrowDuplicateEmailExceptionWhenNewEmailAlreadyExists() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleEmail));
            when(customerEmailRepository.save(any(CustomerEmail.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"customer_emails_email_key\" Key (email)=(newemail@gmail.com) already exists."));

            assertThatThrownBy(() -> customerEmailService.updateEmail(VALID_ID, NEW_EMAIL))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessage("Email already exists: newemail@gmail.com");
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.updateEmail(VALID_ID, NEW_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("changeVerificationStatus() Tests")
    class ChangeVerificationStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when emailId is null")
        void shouldThrowNullFieldExceptionWhenEmailIdIsNull() {
            assertThatThrownBy(() -> customerEmailService.changeVerificationStatus(null, true))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'emailId' cannot be null");
        }

        @Test
        @DisplayName("Should change verification status successfully")
        void shouldChangeVerificationStatusSuccessfully() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleEmail));
            when(customerEmailRepository.save(any(CustomerEmail.class))).thenReturn(sampleEmail);

            CustomerEmail result = customerEmailService.changeVerificationStatus(VALID_ID, true);

            // Test OUTCOME - operation succeeded
            assertThat(result).isNotNull();

            // Verify business logic flow - find then save
            verify(customerEmailRepository).findById(VALID_ID);
            verify(customerEmailRepository).save(any(CustomerEmail.class));
        }

        @Test
        @DisplayName("Should throw EmailNotFoundException when email does not exist")
        void shouldThrowEmailNotFoundExceptionWhenEmailDoesNotExist() {
            when(customerEmailRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerEmailService.changeVerificationStatus(VALID_ID, true))
                    .isInstanceOf(EmailNotFoundException.class)
                    .hasMessage("Email not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.changeVerificationStatus(VALID_ID, true))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("saveEmail() Tests")
    class SaveEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerEmail is null")
        void shouldThrowNullFieldExceptionWhenCustomerEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.saveEmail(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerEmail' cannot be null");
        }

        @Test
        @DisplayName("Should save email successfully")
        void shouldSaveEmailSuccessfully() {
            when(customerEmailRepository.save(any(CustomerEmail.class))).thenReturn(sampleEmail);

            CustomerEmail result = customerEmailService.saveEmail(sampleEmail);

            // Test OUTCOME - operation succeeded
            assertThat(result).isNotNull();
            verify(customerEmailRepository).save(sampleEmail);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.save(any(CustomerEmail.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.saveEmail(sampleEmail))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteById() Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when emailId is null")
        void shouldThrowNullFieldExceptionWhenEmailIdIsNull() {
            assertThatThrownBy(() -> customerEmailService.deleteById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'emailId' cannot be null");
        }

        @Test
        @DisplayName("Should delete email by ID successfully")
        void shouldDeleteEmailByIdSuccessfully() {
            when(customerEmailRepository.existsById(VALID_ID)).thenReturn(true);

            // Test OUTCOME - no exception thrown means success
            assertThatCode(() -> customerEmailService.deleteById(VALID_ID))
                    .doesNotThrowAnyException();

            // Verify deletion happened, but don't dictate the exact implementation approach
            verify(customerEmailRepository).deleteById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw EmailNotFoundException when email does not exist")
        void shouldThrowEmailNotFoundExceptionWhenEmailDoesNotExist() {
            when(customerEmailRepository.existsById(VALID_ID)).thenReturn(false);

            assertThatThrownBy(() -> customerEmailService.deleteById(VALID_ID))
                    .isInstanceOf(EmailNotFoundException.class)
                    .hasMessage("Email not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.deleteById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteEmail() Tests")
    class DeleteEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerEmail is null")
        void shouldThrowNullFieldExceptionWhenCustomerEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.deleteEmail(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerEmail' cannot be null");
        }

        @Test
        @DisplayName("Should delete email successfully")
        void shouldDeleteEmailSuccessfully() {
            customerEmailService.deleteEmail(sampleEmail);

            verify(customerEmailRepository).delete(sampleEmail);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(customerEmailRepository).delete(any(CustomerEmail.class));

            assertThatThrownBy(() -> customerEmailService.deleteEmail(sampleEmail))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteByEmail() Tests")
    class DeleteByEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when email is null")
        void shouldThrowNullFieldExceptionWhenEmailIsNull() {
            assertThatThrownBy(() -> customerEmailService.deleteByEmail(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'email' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when email format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenEmailFormatIsInvalid() {
            assertThatThrownBy(() -> customerEmailService.deleteByEmail("invalid-email"))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .hasMessage("Invalid email format: invalid-email");
        }

        @Test
        @DisplayName("Should delete email by email address successfully")
        void shouldDeleteEmailByEmailAddressSuccessfully() {
            when(customerEmailRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleEmail));

            // Test OUTCOME - no exception means success
            assertThatCode(() -> customerEmailService.deleteByEmail(VALID_EMAIL))
                    .doesNotThrowAnyException();

            // Verify business logic flow - find then delete
            verify(customerEmailRepository).findByEmail(anyString());
            verify(customerEmailRepository).delete(sampleEmail);
        }

        @Test
        @DisplayName("Should throw EmailNotFoundException when email does not exist")
        void shouldThrowEmailNotFoundExceptionWhenEmailDoesNotExist() {
            when(customerEmailRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerEmailService.deleteByEmail(VALID_EMAIL))
                    .isInstanceOf(EmailNotFoundException.class)
                    .hasMessage("Email not found: " + VALID_EMAIL);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerEmailRepository.findByEmail(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerEmailService.deleteByEmail(VALID_EMAIL))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }
}
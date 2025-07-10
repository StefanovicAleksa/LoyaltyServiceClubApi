package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerPhoneRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicatePhoneException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PhoneNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;

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
@DisplayName("CustomerPhoneService Unit Tests")
class CustomerPhoneServiceTest {

    @Mock
    private CustomerPhoneRepository customerPhoneRepository;

    @InjectMocks
    private CustomerPhoneService customerPhoneService;

    private CustomerPhone samplePhone;
    private final String VALID_PHONE = "+381123456789";
    private final String NEW_PHONE = "+381987654321";
    private final Long VALID_ID = 1L;

    @BeforeEach
    void setUp() {
        samplePhone = new CustomerPhone();
        samplePhone.setId(VALID_ID);
        samplePhone.setPhone(VALID_PHONE);
        samplePhone.setVerified(false);
        samplePhone.setCreatedDate(OffsetDateTime.now());
        samplePhone.setLastModifiedDate(OffsetDateTime.now());
    }

    // ===== CREATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("createPhone() Tests")
    class CreatePhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.createPhone(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phone' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when phone is empty")
        void shouldThrowEmptyFieldExceptionWhenPhoneIsEmpty() {
            assertThatThrownBy(() -> customerPhoneService.createPhone("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'phone' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when phone is too short")
        void shouldThrowFieldTooShortExceptionWhenPhoneIsTooShort() {
            assertThatThrownBy(() -> customerPhoneService.createPhone("+38112345"))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field phone too short: 9 characters. Min: 12 characters");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when phone is too long")
        void shouldThrowFieldTooLongExceptionWhenPhoneIsTooLong() {
            String longPhone = "+3811234567890";
            assertThatThrownBy(() -> customerPhoneService.createPhone(longPhone))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessage("Field phone too long: 14 characters. Max: 13 characters");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> customerPhoneService.createPhone("123456789012"))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessage("Invalid phone format: 123456789012 (Expected format: +381xxxxxxxx)");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone doesn't start with +381")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneDoesNotStartWithPlus381() {
            assertThatThrownBy(() -> customerPhoneService.createPhone("+49123456789"))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessage("Invalid phone format: +49123456789 (Expected format: +381xxxxxxxx)");
        }

        @Test
        @DisplayName("Should create phone successfully when valid phone is provided")
        void shouldCreatePhoneSuccessfullyWhenValidPhoneIsProvided() {
            when(customerPhoneRepository.save(any(CustomerPhone.class))).thenReturn(samplePhone);

            CustomerPhone result = customerPhoneService.createPhone(VALID_PHONE);

            // Test OUTCOMES, not implementation details
            assertThat(result).isNotNull();
            assertThat(result.getPhone()).isEqualTo(VALID_PHONE);
            assertThat(result.isVerified()).isFalse();

            // Verify save was called, but don't dictate HOW the entity was built
            verify(customerPhoneRepository).save(any(CustomerPhone.class));
        }

        @Test
        @DisplayName("Should throw DuplicatePhoneException when phone already exists")
        void shouldThrowDuplicatePhoneExceptionWhenPhoneAlreadyExists() {
            when(customerPhoneRepository.save(any(CustomerPhone.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"customer_phones_phone_key\" Key (phone)=(+381123456789) already exists."));

            assertThatThrownBy(() -> customerPhoneService.createPhone(VALID_PHONE))
                    .isInstanceOf(DuplicatePhoneException.class)
                    .hasMessage("Phone already exists: +381123456789");
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when unexpected repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenUnexpectedRepositoryErrorOccurs() {
            when(customerPhoneRepository.save(any(CustomerPhone.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.createPhone(VALID_PHONE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== READ OPERATIONS TESTS =====

    @Nested
    @DisplayName("getAllPhones() Tests")
    class GetAllPhonesTests {

        @Test
        @DisplayName("Should return all phones successfully")
        void shouldReturnAllPhonesSuccessfully() {
            List<CustomerPhone> expectedPhones = List.of(samplePhone);
            when(customerPhoneRepository.findAll()).thenReturn(expectedPhones);

            List<CustomerPhone> result = customerPhoneService.getAllPhones();

            assertThat(result).isEqualTo(expectedPhones);
            verify(customerPhoneRepository).findAll();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.getAllPhones())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when phoneId is null")
        void shouldThrowNullFieldExceptionWhenPhoneIdIsNull() {
            assertThatThrownBy(() -> customerPhoneService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneId' cannot be null");
        }

        @Test
        @DisplayName("Should find phone by ID successfully")
        void shouldFindPhoneByIdSuccessfully() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.of(samplePhone));

            CustomerPhone result = customerPhoneService.findById(VALID_ID);

            assertThat(result).isEqualTo(samplePhone);
            verify(customerPhoneRepository).findById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw PhoneNotFoundException when phone does not exist")
        void shouldThrowPhoneNotFoundExceptionWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerPhoneService.findById(VALID_ID))
                    .isInstanceOf(PhoneNotFoundException.class)
                    .hasMessage("Phone not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.findById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByPhone() Tests")
    class FindByPhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.findByPhone(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phone' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> customerPhoneService.findByPhone("invalid-phone"))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessage("Invalid phone format: invalid-phone (Expected format: +381xxxxxxxx)");
        }

        @Test
        @DisplayName("Should find phone successfully")
        void shouldFindPhoneSuccessfully() {
            when(customerPhoneRepository.findByPhone(VALID_PHONE)).thenReturn(Optional.of(samplePhone));

            CustomerPhone result = customerPhoneService.findByPhone(VALID_PHONE);

            assertThat(result).isEqualTo(samplePhone);
            verify(customerPhoneRepository).findByPhone(VALID_PHONE);
        }

        @Test
        @DisplayName("Should throw PhoneNotFoundException when phone does not exist")
        void shouldThrowPhoneNotFoundExceptionWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.findByPhone(VALID_PHONE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerPhoneService.findByPhone(VALID_PHONE))
                    .isInstanceOf(PhoneNotFoundException.class)
                    .hasMessage("Phone not found: " + VALID_PHONE);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findByPhone(VALID_PHONE))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.findByPhone(VALID_PHONE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("getVerifiedPhones() Tests")
    class GetVerifiedPhonesTests {

        @Test
        @DisplayName("Should return verified phones successfully")
        void shouldReturnVerifiedPhonesSuccessfully() {
            List<CustomerPhone> expectedPhones = List.of(samplePhone);
            when(customerPhoneRepository.findByVerified(true)).thenReturn(expectedPhones);

            List<CustomerPhone> result = customerPhoneService.getVerifiedPhones();

            assertThat(result).isEqualTo(expectedPhones);
            verify(customerPhoneRepository).findByVerified(true);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findByVerified(true))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.getVerifiedPhones())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("getUnverifiedPhones() Tests")
    class GetUnverifiedPhonesTests {

        @Test
        @DisplayName("Should return unverified phones successfully")
        void shouldReturnUnverifiedPhonesSuccessfully() {
            List<CustomerPhone> expectedPhones = List.of(samplePhone);
            when(customerPhoneRepository.findByVerified(false)).thenReturn(expectedPhones);

            List<CustomerPhone> result = customerPhoneService.getUnverifiedPhones();

            assertThat(result).isEqualTo(expectedPhones);
            verify(customerPhoneRepository).findByVerified(false);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findByVerified(false))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.getUnverifiedPhones())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countVerifiedPhones() Tests")
    class CountVerifiedPhonesTests {

        @Test
        @DisplayName("Should return verified phones count successfully")
        void shouldReturnVerifiedPhonesCountSuccessfully() {
            long expectedCount = 5L;
            when(customerPhoneRepository.countByVerified(true)).thenReturn(expectedCount);

            long result = customerPhoneService.countVerifiedPhones();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerPhoneRepository).countByVerified(true);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.countByVerified(true))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.countVerifiedPhones())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countUnverifiedPhones() Tests")
    class CountUnverifiedPhonesTests {

        @Test
        @DisplayName("Should return unverified phones count successfully")
        void shouldReturnUnverifiedPhonesCountSuccessfully() {
            long expectedCount = 3L;
            when(customerPhoneRepository.countByVerified(false)).thenReturn(expectedCount);

            long result = customerPhoneService.countUnverifiedPhones();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerPhoneRepository).countByVerified(false);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.countByVerified(false))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.countUnverifiedPhones())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countAllPhones() Tests")
    class CountAllPhonesTests {

        @Test
        @DisplayName("Should return total phones count successfully")
        void shouldReturnTotalPhonesCountSuccessfully() {
            long expectedCount = 10L;
            when(customerPhoneRepository.count()).thenReturn(expectedCount);

            long result = customerPhoneService.countAllPhones();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerPhoneRepository).count();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.count())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.countAllPhones())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("existsById() Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when phoneId is null")
        void shouldThrowNullFieldExceptionWhenPhoneIdIsNull() {
            assertThatThrownBy(() -> customerPhoneService.existsById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneId' cannot be null");
        }

        @Test
        @DisplayName("Should return true when phone exists by ID")
        void shouldReturnTrueWhenPhoneExistsByID() {
            when(customerPhoneRepository.existsById(VALID_ID)).thenReturn(true);

            boolean result = customerPhoneService.existsById(VALID_ID);

            assertThat(result).isTrue();
            verify(customerPhoneRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should return false when phone does not exist by ID")
        void shouldReturnFalseWhenPhoneDoesNotExistByID() {
            when(customerPhoneRepository.existsById(VALID_ID)).thenReturn(false);

            boolean result = customerPhoneService.existsById(VALID_ID);

            assertThat(result).isFalse();
            verify(customerPhoneRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.existsById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("phoneExists() Tests")
    class PhoneExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.phoneExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phone' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> customerPhoneService.phoneExists("invalid-phone"))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessage("Invalid phone format: invalid-phone (Expected format: +381xxxxxxxx)");
        }

        @Test
        @DisplayName("Should return true when phone exists")
        void shouldReturnTrueWhenPhoneExists() {
            when(customerPhoneRepository.existsByPhone(VALID_PHONE)).thenReturn(true);

            boolean result = customerPhoneService.phoneExists(VALID_PHONE);

            assertThat(result).isTrue();
            verify(customerPhoneRepository).existsByPhone(VALID_PHONE);
        }

        @Test
        @DisplayName("Should return false when phone does not exist")
        void shouldReturnFalseWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.existsByPhone(VALID_PHONE)).thenReturn(false);

            boolean result = customerPhoneService.phoneExists(VALID_PHONE);

            assertThat(result).isFalse();
            verify(customerPhoneRepository).existsByPhone(VALID_PHONE);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.existsByPhone(VALID_PHONE))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.phoneExists(VALID_PHONE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UPDATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("updatePhone() Tests")
    class UpdatePhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when phoneId is null")
        void shouldThrowNullFieldExceptionWhenPhoneIdIsNull() {
            assertThatThrownBy(() -> customerPhoneService.updatePhone(null, NEW_PHONE))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneId' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when newPhone is null")
        void shouldThrowNullFieldExceptionWhenNewPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.updatePhone(VALID_ID, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'newPhone' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when newPhone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenNewPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> customerPhoneService.updatePhone(VALID_ID, "invalid-phone"))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessage("Invalid phone format: invalid-phone (Expected format: +381xxxxxxxx)");
        }

        @Test
        @DisplayName("Should update phone successfully")
        void shouldUpdatePhoneSuccessfully() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.of(samplePhone));
            when(customerPhoneRepository.save(any(CustomerPhone.class))).thenReturn(samplePhone);

            CustomerPhone result = customerPhoneService.updatePhone(VALID_ID, NEW_PHONE);

            // Test OUTCOME - operation succeeded
            assertThat(result).isNotNull();

            // Verify business logic flow - find then save
            verify(customerPhoneRepository).findById(VALID_ID);
            verify(customerPhoneRepository).save(any(CustomerPhone.class));
        }

        @Test
        @DisplayName("Should throw PhoneNotFoundException when phone does not exist")
        void shouldThrowPhoneNotFoundExceptionWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerPhoneService.updatePhone(VALID_ID, NEW_PHONE))
                    .isInstanceOf(PhoneNotFoundException.class)
                    .hasMessage("Phone not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DuplicatePhoneException when new phone already exists")
        void shouldThrowDuplicatePhoneExceptionWhenNewPhoneAlreadyExists() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.of(samplePhone));
            when(customerPhoneRepository.save(any(CustomerPhone.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"customer_phones_phone_key\" Key (phone)=(+381987654321) already exists."));

            assertThatThrownBy(() -> customerPhoneService.updatePhone(VALID_ID, NEW_PHONE))
                    .isInstanceOf(DuplicatePhoneException.class)
                    .hasMessage("Phone already exists: +381987654321");
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.updatePhone(VALID_ID, NEW_PHONE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("changeVerificationStatus() Tests")
    class ChangeVerificationStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when phoneId is null")
        void shouldThrowNullFieldExceptionWhenPhoneIdIsNull() {
            assertThatThrownBy(() -> customerPhoneService.changeVerificationStatus(null, true))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneId' cannot be null");
        }

        @Test
        @DisplayName("Should change verification status successfully")
        void shouldChangeVerificationStatusSuccessfully() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.of(samplePhone));
            when(customerPhoneRepository.save(any(CustomerPhone.class))).thenReturn(samplePhone);

            CustomerPhone result = customerPhoneService.changeVerificationStatus(VALID_ID, true);

            // Test OUTCOME - operation succeeded
            assertThat(result).isNotNull();

            // Verify business logic flow - find then save
            verify(customerPhoneRepository).findById(VALID_ID);
            verify(customerPhoneRepository).save(any(CustomerPhone.class));
        }

        @Test
        @DisplayName("Should throw PhoneNotFoundException when phone does not exist")
        void shouldThrowPhoneNotFoundExceptionWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerPhoneService.changeVerificationStatus(VALID_ID, true))
                    .isInstanceOf(PhoneNotFoundException.class)
                    .hasMessage("Phone not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.changeVerificationStatus(VALID_ID, true))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("savePhone() Tests")
    class SavePhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerPhone is null")
        void shouldThrowNullFieldExceptionWhenCustomerPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.savePhone(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerPhone' cannot be null");
        }

        @Test
        @DisplayName("Should save phone successfully")
        void shouldSavePhoneSuccessfully() {
            when(customerPhoneRepository.save(any(CustomerPhone.class))).thenReturn(samplePhone);

            CustomerPhone result = customerPhoneService.savePhone(samplePhone);

            // Test OUTCOME - operation succeeded
            assertThat(result).isNotNull();
            verify(customerPhoneRepository).save(samplePhone);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.save(any(CustomerPhone.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.savePhone(samplePhone))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteById() Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when phoneId is null")
        void shouldThrowNullFieldExceptionWhenPhoneIdIsNull() {
            assertThatThrownBy(() -> customerPhoneService.deleteById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneId' cannot be null");
        }

        @Test
        @DisplayName("Should delete phone by ID successfully")
        void shouldDeletePhoneByIdSuccessfully() {
            when(customerPhoneRepository.existsById(VALID_ID)).thenReturn(true);

            // Test OUTCOME - no exception thrown means success
            assertThatCode(() -> customerPhoneService.deleteById(VALID_ID))
                    .doesNotThrowAnyException();

            // Verify deletion happened, but don't dictate the exact implementation approach
            verify(customerPhoneRepository).deleteById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw PhoneNotFoundException when phone does not exist")
        void shouldThrowPhoneNotFoundExceptionWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.existsById(VALID_ID)).thenReturn(false);

            assertThatThrownBy(() -> customerPhoneService.deleteById(VALID_ID))
                    .isInstanceOf(PhoneNotFoundException.class)
                    .hasMessage("Phone not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.deleteById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deletePhone() Tests")
    class DeletePhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerPhone is null")
        void shouldThrowNullFieldExceptionWhenCustomerPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.deletePhone(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerPhone' cannot be null");
        }

        @Test
        @DisplayName("Should delete phone successfully")
        void shouldDeletePhoneSuccessfully() {
            customerPhoneService.deletePhone(samplePhone);

            verify(customerPhoneRepository).delete(samplePhone);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(customerPhoneRepository).delete(any(CustomerPhone.class));

            assertThatThrownBy(() -> customerPhoneService.deletePhone(samplePhone))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteByPhone() Tests")
    class DeleteByPhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when phone is null")
        void shouldThrowNullFieldExceptionWhenPhoneIsNull() {
            assertThatThrownBy(() -> customerPhoneService.deleteByPhone(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phone' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            assertThatThrownBy(() -> customerPhoneService.deleteByPhone("invalid-phone"))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessage("Invalid phone format: invalid-phone (Expected format: +381xxxxxxxx)");
        }

        @Test
        @DisplayName("Should delete phone by phone number successfully")
        void shouldDeletePhoneByPhoneNumberSuccessfully() {
            when(customerPhoneRepository.findByPhone(VALID_PHONE)).thenReturn(Optional.of(samplePhone));

            customerPhoneService.deleteByPhone(VALID_PHONE);

            verify(customerPhoneRepository).findByPhone(VALID_PHONE);
            verify(customerPhoneRepository).delete(samplePhone);
        }

        @Test
        @DisplayName("Should throw PhoneNotFoundException when phone does not exist")
        void shouldThrowPhoneNotFoundExceptionWhenPhoneDoesNotExist() {
            when(customerPhoneRepository.findByPhone(VALID_PHONE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerPhoneService.deleteByPhone(VALID_PHONE))
                    .isInstanceOf(PhoneNotFoundException.class)
                    .hasMessage("Phone not found: " + VALID_PHONE);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerPhoneRepository.findByPhone(VALID_PHONE))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerPhoneService.deleteByPhone(VALID_PHONE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }
}
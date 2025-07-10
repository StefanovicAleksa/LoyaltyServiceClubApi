package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.InvalidCharacterException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;

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
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;
    private CustomerEmail sampleEmail;
    private CustomerPhone samplePhone;
    private final String VALID_FIRST_NAME = "John";
    private final String VALID_LAST_NAME = "Doe";
    private final String NEW_FIRST_NAME = "Jane";
    private final String NEW_LAST_NAME = "Smith";
    private final Long VALID_ID = 1L;
    private final Long EMAIL_ID = 2L;
    private final Long PHONE_ID = 3L;

    @BeforeEach
    void setUp() {
        sampleEmail = new CustomerEmail();
        sampleEmail.setId(EMAIL_ID);
        sampleEmail.setEmail("test@gmail.com");

        samplePhone = new CustomerPhone();
        samplePhone.setId(PHONE_ID);
        samplePhone.setPhone("+381123456789");

        sampleCustomer = new Customer();
        sampleCustomer.setId(VALID_ID);
        sampleCustomer.setFirstName(VALID_FIRST_NAME);
        sampleCustomer.setLastName(VALID_LAST_NAME);
        sampleCustomer.setEmail(sampleEmail);
        sampleCustomer.setPhone(samplePhone);
        sampleCustomer.setCreatedDate(OffsetDateTime.now());
        sampleCustomer.setLastModifiedDate(OffsetDateTime.now());
    }

    // ===== CREATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("createCustomer(firstName, lastName, email, phone) Tests")
    class CreateCustomerFullTests {

        @Test
        @DisplayName("Should throw NullFieldException when firstName is null")
        void shouldThrowNullFieldExceptionWhenFirstNameIsNull() {
            assertThatThrownBy(() -> customerService.createCustomer(null, VALID_LAST_NAME, sampleEmail, samplePhone))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'firstName' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when lastName is null")
        void shouldThrowNullFieldExceptionWhenLastNameIsNull() {
            assertThatThrownBy(() -> customerService.createCustomer(VALID_FIRST_NAME, null, sampleEmail, samplePhone))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'lastName' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when firstName is empty")
        void shouldThrowEmptyFieldExceptionWhenFirstNameIsEmpty() {
            assertThatThrownBy(() -> customerService.createCustomer("   ", VALID_LAST_NAME, sampleEmail, samplePhone))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'firstName' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when firstName is too short")
        void shouldThrowFieldTooShortExceptionWhenFirstNameIsTooShort() {
            assertThatThrownBy(() -> customerService.createCustomer("A", VALID_LAST_NAME, sampleEmail, samplePhone))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field firstName too short: 1 characters. Min: 2 characters");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when firstName is too long")
        void shouldThrowFieldTooLongExceptionWhenFirstNameIsTooLong() {
            String longName = "A".repeat(31);
            assertThatThrownBy(() -> customerService.createCustomer(longName, VALID_LAST_NAME, sampleEmail, samplePhone))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessage("Field firstName too long: 31 characters. Max: 30 characters");
        }

        @Test
        @DisplayName("Should throw InvalidCharacterException when firstName contains invalid characters")
        void shouldThrowInvalidCharacterExceptionWhenFirstNameContainsInvalidCharacters() {
            assertThatThrownBy(() -> customerService.createCustomer("John123", VALID_LAST_NAME, sampleEmail, samplePhone))
                    .isInstanceOf(InvalidCharacterException.class)
                    .hasMessageContaining("Field 'firstName' contains invalid characters");
        }

        @Test
        @DisplayName("Should create customer successfully with all parameters")
        void shouldCreateCustomerSuccessfullyWithAllParameters() {
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone);

            // Test OUTCOMES
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(result.getEmail()).isEqualTo(sampleEmail);
            assertThat(result.getPhone()).isEqualTo(samplePhone);

            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should create customer successfully with null email and phone")
        void shouldCreateCustomerSuccessfullyWithNullEmailAndPhone() {
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(VALID_LAST_NAME);

            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.save(any(Customer.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("createCustomer(firstName, lastName) Tests")
    class CreateCustomerSimpleTests {

        @Test
        @DisplayName("Should throw NullFieldException when firstName is null")
        void shouldThrowNullFieldExceptionWhenFirstNameIsNull() {
            assertThatThrownBy(() -> customerService.createCustomer(null, VALID_LAST_NAME))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'firstName' cannot be null");
        }

        @Test
        @DisplayName("Should create customer successfully with just names")
        void shouldCreateCustomerSuccessfullyWithJustNames() {
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(VALID_LAST_NAME);

            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.save(any(Customer.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== READ OPERATIONS TESTS =====

    @Nested
    @DisplayName("getAllCustomers() Tests")
    class GetAllCustomersTests {

        @Test
        @DisplayName("Should return all customers successfully")
        void shouldReturnAllCustomersSuccessfully() {
            List<Customer> expectedCustomers = List.of(sampleCustomer);
            when(customerRepository.findAll()).thenReturn(expectedCustomers);

            List<Customer> result = customerService.getAllCustomers();

            assertThat(result).isEqualTo(expectedCustomers);
            verify(customerRepository).findAll();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.getAllCustomers())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should find customer by ID successfully")
        void shouldFindCustomerByIdSuccessfully() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleCustomer));

            Customer result = customerService.findById(VALID_ID);

            assertThat(result).isEqualTo(sampleCustomer);
            verify(customerRepository).findById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findById(VALID_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.findById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByFirstNameAndLastName() Tests")
    class FindByFirstNameAndLastNameTests {

        @Test
        @DisplayName("Should throw NullFieldException when firstName is null")
        void shouldThrowNullFieldExceptionWhenFirstNameIsNull() {
            assertThatThrownBy(() -> customerService.findByFirstNameAndLastName(null, VALID_LAST_NAME))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'firstName' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidCharacterException when firstName contains invalid characters")
        void shouldThrowInvalidCharacterExceptionWhenFirstNameContainsInvalidCharacters() {
            assertThatThrownBy(() -> customerService.findByFirstNameAndLastName("John123", VALID_LAST_NAME))
                    .isInstanceOf(InvalidCharacterException.class)
                    .hasMessageContaining("Field 'firstName' contains invalid characters");
        }

        @Test
        @DisplayName("Should find customers by first and last name successfully")
        void shouldFindCustomersByFirstAndLastNameSuccessfully() {
            List<Customer> expectedCustomers = List.of(sampleCustomer);
            when(customerRepository.findByFirstNameAndLastNameIgnoreCase(anyString(), anyString())).thenReturn(expectedCustomers);

            List<Customer> result = customerService.findByFirstNameAndLastName(VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(result).isEqualTo(expectedCustomers);
            verify(customerRepository).findByFirstNameAndLastNameIgnoreCase(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findByFirstNameAndLastNameIgnoreCase(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.findByFirstNameAndLastName(VALID_FIRST_NAME, VALID_LAST_NAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByFirstName() Tests")
    class FindByFirstNameTests {

        @Test
        @DisplayName("Should throw NullFieldException when firstName is null")
        void shouldThrowNullFieldExceptionWhenFirstNameIsNull() {
            assertThatThrownBy(() -> customerService.findByFirstName(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'firstName' cannot be null");
        }

        @Test
        @DisplayName("Should find customers by first name successfully")
        void shouldFindCustomersByFirstNameSuccessfully() {
            List<Customer> expectedCustomers = List.of(sampleCustomer);
            when(customerRepository.findByFirstNameIgnoreCase(anyString())).thenReturn(expectedCustomers);

            List<Customer> result = customerService.findByFirstName(VALID_FIRST_NAME);

            assertThat(result).isEqualTo(expectedCustomers);
            verify(customerRepository).findByFirstNameIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findByFirstNameIgnoreCase(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.findByFirstName(VALID_FIRST_NAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByLastName() Tests")
    class FindByLastNameTests {

        @Test
        @DisplayName("Should throw NullFieldException when lastName is null")
        void shouldThrowNullFieldExceptionWhenLastNameIsNull() {
            assertThatThrownBy(() -> customerService.findByLastName(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'lastName' cannot be null");
        }

        @Test
        @DisplayName("Should find customers by last name successfully")
        void shouldFindCustomersByLastNameSuccessfully() {
            List<Customer> expectedCustomers = List.of(sampleCustomer);
            when(customerRepository.findByLastNameIgnoreCase(anyString())).thenReturn(expectedCustomers);

            List<Customer> result = customerService.findByLastName(VALID_LAST_NAME);

            assertThat(result).isEqualTo(expectedCustomers);
            verify(customerRepository).findByLastNameIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findByLastNameIgnoreCase(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.findByLastName(VALID_LAST_NAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByEmailId() Tests")
    class FindByEmailIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when emailId is null")
        void shouldThrowNullFieldExceptionWhenEmailIdIsNull() {
            assertThatThrownBy(() -> customerService.findByEmailId(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'emailId' cannot be null");
        }

        @Test
        @DisplayName("Should find customer by email ID successfully")
        void shouldFindCustomerByEmailIdSuccessfully() {
            when(customerRepository.findByEmailId(EMAIL_ID)).thenReturn(Optional.of(sampleCustomer));

            Customer result = customerService.findByEmailId(EMAIL_ID);

            assertThat(result).isEqualTo(sampleCustomer);
            verify(customerRepository).findByEmailId(EMAIL_ID);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.findByEmailId(EMAIL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findByEmailId(EMAIL_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found: No customer found with email ID: " + EMAIL_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findByEmailId(EMAIL_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.findByEmailId(EMAIL_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByPhoneId() Tests")
    class FindByPhoneIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when phoneId is null")
        void shouldThrowNullFieldExceptionWhenPhoneIdIsNull() {
            assertThatThrownBy(() -> customerService.findByPhoneId(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneId' cannot be null");
        }

        @Test
        @DisplayName("Should find customer by phone ID successfully")
        void shouldFindCustomerByPhoneIdSuccessfully() {
            when(customerRepository.findByPhoneId(PHONE_ID)).thenReturn(Optional.of(sampleCustomer));

            Customer result = customerService.findByPhoneId(PHONE_ID);

            assertThat(result).isEqualTo(sampleCustomer);
            verify(customerRepository).findByPhoneId(PHONE_ID);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.findByPhoneId(PHONE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findByPhoneId(PHONE_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found: No customer found with phone ID: " + PHONE_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findByPhoneId(PHONE_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.findByPhoneId(PHONE_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("customerExists() Tests")
    class CustomerExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.customerExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should return true when customer exists")
        void shouldReturnTrueWhenCustomerExists() {
            when(customerRepository.existsById(VALID_ID)).thenReturn(true);

            boolean result = customerService.customerExists(VALID_ID);

            assertThat(result).isTrue();
            verify(customerRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should return false when customer does not exist")
        void shouldReturnFalseWhenCustomerDoesNotExist() {
            when(customerRepository.existsById(VALID_ID)).thenReturn(false);

            boolean result = customerService.customerExists(VALID_ID);

            assertThat(result).isFalse();
            verify(customerRepository).existsById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.customerExists(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countAllCustomers() Tests")
    class CountAllCustomersTests {

        @Test
        @DisplayName("Should return total customers count successfully")
        void shouldReturnTotalCustomersCountSuccessfully() {
            long expectedCount = 15L;
            when(customerRepository.count()).thenReturn(expectedCount);

            long result = customerService.countAllCustomers();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerRepository).count();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.count())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.countAllCustomers())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UPDATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("updateCustomerNames() Tests")
    class UpdateCustomerNamesTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.updateCustomerNames(null, NEW_FIRST_NAME, NEW_LAST_NAME))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when firstName is null")
        void shouldThrowNullFieldExceptionWhenFirstNameIsNull() {
            assertThatThrownBy(() -> customerService.updateCustomerNames(VALID_ID, null, NEW_LAST_NAME))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'firstName' cannot be null");
        }

        @Test
        @DisplayName("Should throw InvalidCharacterException when firstName contains invalid characters")
        void shouldThrowInvalidCharacterExceptionWhenFirstNameContainsInvalidCharacters() {
            assertThatThrownBy(() -> customerService.updateCustomerNames(VALID_ID, "Jane123", NEW_LAST_NAME))
                    .isInstanceOf(InvalidCharacterException.class)
                    .hasMessageContaining("Field 'firstName' contains invalid characters");
        }

        @Test
        @DisplayName("Should update customer names successfully")
        void shouldUpdateCustomerNamesSuccessfully() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.updateCustomerNames(VALID_ID, NEW_FIRST_NAME, NEW_LAST_NAME);

            assertThat(result).isNotNull();
            verify(customerRepository).findById(VALID_ID);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomerNames(VALID_ID, NEW_FIRST_NAME, NEW_LAST_NAME))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.updateCustomerNames(VALID_ID, NEW_FIRST_NAME, NEW_LAST_NAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("updateCustomerEmail() Tests")
    class UpdateCustomerEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.updateCustomerEmail(null, sampleEmail))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should update customer email successfully")
        void shouldUpdateCustomerEmailSuccessfully() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.updateCustomerEmail(VALID_ID, sampleEmail);

            assertThat(result).isNotNull();
            verify(customerRepository).findById(VALID_ID);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should update customer email to null successfully")
        void shouldUpdateCustomerEmailToNullSuccessfully() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.updateCustomerEmail(VALID_ID, null);

            assertThat(result).isNotNull();
            verify(customerRepository).findById(VALID_ID);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomerEmail(VALID_ID, sampleEmail))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.updateCustomerEmail(VALID_ID, sampleEmail))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("updateCustomerPhone() Tests")
    class UpdateCustomerPhoneTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.updateCustomerPhone(null, samplePhone))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should update customer phone successfully")
        void shouldUpdateCustomerPhoneSuccessfully() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.updateCustomerPhone(VALID_ID, samplePhone);

            assertThat(result).isNotNull();
            verify(customerRepository).findById(VALID_ID);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should update customer phone to null successfully")
        void shouldUpdateCustomerPhoneToNullSuccessfully() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.updateCustomerPhone(VALID_ID, null);

            assertThat(result).isNotNull();
            verify(customerRepository).findById(VALID_ID);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.findById(VALID_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomerPhone(VALID_ID, samplePhone))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.findById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.updateCustomerPhone(VALID_ID, samplePhone))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("saveCustomer() Tests")
    class SaveCustomerTests {

        @Test
        @DisplayName("Should throw NullFieldException when customer is null")
        void shouldThrowNullFieldExceptionWhenCustomerIsNull() {
            assertThatThrownBy(() -> customerService.saveCustomer(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customer' cannot be null");
        }

        @Test
        @DisplayName("Should save customer successfully")
        void shouldSaveCustomerSuccessfully() {
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            Customer result = customerService.saveCustomer(sampleCustomer);

            assertThat(result).isNotNull();
            verify(customerRepository).save(sampleCustomer);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.save(any(Customer.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.saveCustomer(sampleCustomer))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteById() Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerService.deleteById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should delete customer by ID successfully")
        void shouldDeleteCustomerByIdSuccessfully() {
            when(customerRepository.existsById(VALID_ID)).thenReturn(true);

            assertThatCode(() -> customerService.deleteById(VALID_ID))
                    .doesNotThrowAnyException();

            verify(customerRepository).deleteById(VALID_ID);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            when(customerRepository.existsById(VALID_ID)).thenReturn(false);

            assertThatThrownBy(() -> customerService.deleteById(VALID_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessage("Customer not found with ID: " + VALID_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerRepository.existsById(VALID_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerService.deleteById(VALID_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteCustomer() Tests")
    class DeleteCustomerTests {

        @Test
        @DisplayName("Should throw NullFieldException when customer is null")
        void shouldThrowNullFieldExceptionWhenCustomerIsNull() {
            assertThatThrownBy(() -> customerService.deleteCustomer(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customer' cannot be null");
        }

        @Test
        @DisplayName("Should delete customer successfully")
        void shouldDeleteCustomerSuccessfully() {
            customerService.deleteCustomer(sampleCustomer);

            verify(customerRepository).delete(sampleCustomer);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(customerRepository).delete(any(Customer.class));

            assertThatThrownBy(() -> customerService.deleteCustomer(sampleCustomer))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }
}
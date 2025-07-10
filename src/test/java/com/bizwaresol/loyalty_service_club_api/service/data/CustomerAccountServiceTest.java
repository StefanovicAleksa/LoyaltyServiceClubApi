package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerAccountRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountVerificationStatus;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.system.database.DatabaseSystemException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.security.PasswordValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
@DisplayName("CustomerAccountService Unit Tests")
class CustomerAccountServiceTest {

    @Mock
    private CustomerAccountRepository customerAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerAccountService customerAccountService;

    private CustomerAccount sampleAccount;
    private Customer sampleCustomer;
    private CustomerEmail sampleEmail;
    private CustomerPhone samplePhone;
    private final String VALID_PASSWORD = "password123";
    private final String NEW_PASSWORD = "newPassword456";
    private final String VALID_USERNAME = "testuser";
    private final String NEW_USERNAME = "newuser";
    private final Long VALID_ACCOUNT_ID = 1L;
    private final Long VALID_CUSTOMER_ID = 2L;

    @BeforeEach
    void setUp() {
        sampleEmail = new CustomerEmail();
        sampleEmail.setId(3L);
        sampleEmail.setEmail("test@gmail.com");

        samplePhone = new CustomerPhone();
        samplePhone.setId(4L);
        samplePhone.setPhone("+381123456789");

        sampleCustomer = new Customer();
        sampleCustomer.setId(VALID_CUSTOMER_ID);
        sampleCustomer.setFirstName("John");
        sampleCustomer.setLastName("Doe");
        sampleCustomer.setEmail(sampleEmail);
        sampleCustomer.setPhone(samplePhone);

        sampleAccount = new CustomerAccount();
        sampleAccount.setId(VALID_ACCOUNT_ID);
        sampleAccount.setCustomer(sampleCustomer);
        sampleAccount.setUsername(VALID_USERNAME);
        sampleAccount.setPassword("$2a$10$hashedPassword");
        sampleAccount.setActivityStatus(CustomerAccountActivityStatus.ACTIVE);
        sampleAccount.setVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED);
        sampleAccount.setCreatedDate(OffsetDateTime.now());
        sampleAccount.setLastModifiedDate(OffsetDateTime.now());
    }

    // ===== CREATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("createAccount(customer, rawPassword) Tests")
    class CreateAccountSimpleTests {

        @Test
        @DisplayName("Should throw NullFieldException when customer is null")
        void shouldThrowNullFieldExceptionWhenCustomerIsNull() {
            assertThatThrownBy(() -> customerAccountService.createAccount(null, VALID_PASSWORD))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customer' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when rawPassword is null")
        void shouldThrowNullFieldExceptionWhenRawPasswordIsNull() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'rawPassword' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when rawPassword is empty")
        void shouldThrowEmptyFieldExceptionWhenRawPasswordIsEmpty() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, "   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'rawPassword' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when rawPassword is too short")
        void shouldThrowFieldTooShortExceptionWhenRawPasswordIsTooShort() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, "short"))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field rawPassword too short: 5 characters. Min: 8 characters");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when rawPassword is too long")
        void shouldThrowFieldTooLongExceptionWhenRawPasswordIsTooLong() {
            String longPassword = "a".repeat(256);
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, longPassword))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessage("Field rawPassword too long: 256 characters. Max: 255 characters");
        }

        @Test
        @DisplayName("Should throw PasswordValidationException when rawPassword has no digits")
        void shouldThrowPasswordValidationExceptionWhenRawPasswordHasNoDigits() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, "passwordwithoutdigits"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 8 characters long and contain at least one number");
        }

        @Test
        @DisplayName("Should create account successfully when valid parameters are provided")
        void shouldCreateAccountSuccessfullyWhenValidParametersAreProvided() {
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD);

            assertThat(result).isNotNull();
            assertThat(result.getCustomer()).isEqualTo(sampleCustomer);
            assertThat(result.getActivityStatus()).isEqualTo(CustomerAccountActivityStatus.ACTIVE);
            assertThat(result.getVerificationStatus()).isEqualTo(CustomerAccountVerificationStatus.UNVERIFIED);

            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(customerAccountRepository).save(any(CustomerAccount.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.save(any(CustomerAccount.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("createAccount(customer, username, rawPassword) Tests")
    class CreateAccountWithUsernameTests {

        @Test
        @DisplayName("Should throw NullFieldException when customer is null")
        void shouldThrowNullFieldExceptionWhenCustomerIsNull() {
            assertThatThrownBy(() -> customerAccountService.createAccount(null, VALID_USERNAME, VALID_PASSWORD))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customer' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when username is null")
        void shouldThrowNullFieldExceptionWhenUsernameIsNull() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, null, VALID_PASSWORD))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'username' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when username is empty")
        void shouldThrowEmptyFieldExceptionWhenUsernameIsEmpty() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, "   ", VALID_PASSWORD))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'username' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when username is too short")
        void shouldThrowFieldTooShortExceptionWhenUsernameIsTooShort() {
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, "abc", VALID_PASSWORD))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field username too short: 3 characters. Min: 5 characters");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when username is too long")
        void shouldThrowFieldTooLongExceptionWhenUsernameIsTooLong() {
            String longUsername = "a".repeat(61);
            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, longUsername, VALID_PASSWORD))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessage("Field username too long: 61 characters. Max: 60 characters");
        }

        @Test
        @DisplayName("Should create account with username successfully")
        void shouldCreateAccountWithUsernameSuccessfully() {
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.createAccount(sampleCustomer, VALID_USERNAME, VALID_PASSWORD);

            assertThat(result).isNotNull();
            assertThat(result.getCustomer()).isEqualTo(sampleCustomer);
            assertThat(result.getActivityStatus()).isEqualTo(CustomerAccountActivityStatus.ACTIVE);
            assertThat(result.getVerificationStatus()).isEqualTo(CustomerAccountVerificationStatus.UNVERIFIED);

            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(customerAccountRepository).save(any(CustomerAccount.class));
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.save(any(CustomerAccount.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.createAccount(sampleCustomer, VALID_USERNAME, VALID_PASSWORD))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== READ OPERATIONS TESTS =====

    @Nested
    @DisplayName("getAllAccounts() Tests")
    class GetAllAccountsTests {

        @Test
        @DisplayName("Should return all accounts successfully")
        void shouldReturnAllAccountsSuccessfully() {
            List<CustomerAccount> expectedAccounts = List.of(sampleAccount);
            when(customerAccountRepository.findAll()).thenReturn(expectedAccounts);

            List<CustomerAccount> result = customerAccountService.getAllAccounts();

            assertThat(result).isEqualTo(expectedAccounts);
            verify(customerAccountRepository).findAll();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.getAllAccounts())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findById() Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.findById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should find account by ID successfully")
        void shouldFindAccountByIdSuccessfully() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(sampleAccount));

            CustomerAccount result = customerAccountService.findById(VALID_ACCOUNT_ID);

            assertThat(result).isEqualTo(sampleAccount);
            verify(customerAccountRepository).findById(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.findById(VALID_ACCOUNT_ID))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found with ID: " + VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findById(VALID_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByUsername() Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should throw NullFieldException when username is null")
        void shouldThrowNullFieldExceptionWhenUsernameIsNull() {
            assertThatThrownBy(() -> customerAccountService.findByUsername(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'username' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when username is empty")
        void shouldThrowEmptyFieldExceptionWhenUsernameIsEmpty() {
            assertThatThrownBy(() -> customerAccountService.findByUsername("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'username' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when username is too short")
        void shouldThrowFieldTooShortExceptionWhenUsernameIsTooShort() {
            assertThatThrownBy(() -> customerAccountService.findByUsername("abc"))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field username too short: 3 characters. Min: 5 characters");
        }

        @Test
        @DisplayName("Should find account by username successfully")
        void shouldFindAccountByUsernameSuccessfully() {
            when(customerAccountRepository.findByUsername(anyString())).thenReturn(Optional.of(sampleAccount));

            CustomerAccount result = customerAccountService.findByUsername(VALID_USERNAME);

            assertThat(result).isEqualTo(sampleAccount);
            verify(customerAccountRepository).findByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findByUsername(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.findByUsername(VALID_USERNAME))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found: " + VALID_USERNAME);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findByUsername(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findByUsername(VALID_USERNAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByCustomerId() Tests")
    class FindByCustomerIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerId is null")
        void shouldThrowNullFieldExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.findByCustomerId(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerId' cannot be null");
        }

        @Test
        @DisplayName("Should find account by customer ID successfully")
        void shouldFindAccountByCustomerIdSuccessfully() {
            when(customerAccountRepository.findByCustomerId(VALID_CUSTOMER_ID)).thenReturn(Optional.of(sampleAccount));

            CustomerAccount result = customerAccountService.findByCustomerId(VALID_CUSTOMER_ID);

            assertThat(result).isEqualTo(sampleAccount);
            verify(customerAccountRepository).findByCustomerId(VALID_CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findByCustomerId(VALID_CUSTOMER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.findByCustomerId(VALID_CUSTOMER_ID))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found: No customer account found for customer ID: " + VALID_CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findByCustomerId(VALID_CUSTOMER_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findByCustomerId(VALID_CUSTOMER_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByActivityStatus() Tests")
    class FindByActivityStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when activityStatus is null")
        void shouldThrowNullFieldExceptionWhenActivityStatusIsNull() {
            assertThatThrownBy(() -> customerAccountService.findByActivityStatus(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'activityStatus' cannot be null");
        }

        @Test
        @DisplayName("Should find accounts by activity status successfully")
        void shouldFindAccountsByActivityStatusSuccessfully() {
            List<CustomerAccount> expectedAccounts = List.of(sampleAccount);
            when(customerAccountRepository.findByActivityStatus(CustomerAccountActivityStatus.ACTIVE)).thenReturn(expectedAccounts);

            List<CustomerAccount> result = customerAccountService.findByActivityStatus(CustomerAccountActivityStatus.ACTIVE);

            assertThat(result).isEqualTo(expectedAccounts);
            verify(customerAccountRepository).findByActivityStatus(CustomerAccountActivityStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findByActivityStatus(CustomerAccountActivityStatus.ACTIVE))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findByActivityStatus(CustomerAccountActivityStatus.ACTIVE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByVerificationStatus() Tests")
    class FindByVerificationStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when verificationStatus is null")
        void shouldThrowNullFieldExceptionWhenVerificationStatusIsNull() {
            assertThatThrownBy(() -> customerAccountService.findByVerificationStatus(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'verificationStatus' cannot be null");
        }

        @Test
        @DisplayName("Should find accounts by verification status successfully")
        void shouldFindAccountsByVerificationStatusSuccessfully() {
            List<CustomerAccount> expectedAccounts = List.of(sampleAccount);
            when(customerAccountRepository.findByVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED)).thenReturn(expectedAccounts);

            List<CustomerAccount> result = customerAccountService.findByVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED);

            assertThat(result).isEqualTo(expectedAccounts);
            verify(customerAccountRepository).findByVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findByVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findByVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findByActivityStatusAndVerificationStatus() Tests")
    class FindByActivityStatusAndVerificationStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when activityStatus is null")
        void shouldThrowNullFieldExceptionWhenActivityStatusIsNull() {
            assertThatThrownBy(() -> customerAccountService.findByActivityStatusAndVerificationStatus(null, CustomerAccountVerificationStatus.UNVERIFIED))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'activityStatus' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when verificationStatus is null")
        void shouldThrowNullFieldExceptionWhenVerificationStatusIsNull() {
            assertThatThrownBy(() -> customerAccountService.findByActivityStatusAndVerificationStatus(CustomerAccountActivityStatus.ACTIVE, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'verificationStatus' cannot be null");
        }

        @Test
        @DisplayName("Should find accounts by both statuses successfully")
        void shouldFindAccountsByBothStatusesSuccessfully() {
            List<CustomerAccount> expectedAccounts = List.of(sampleAccount);
            when(customerAccountRepository.findByActivityStatusAndVerificationStatus(
                    CustomerAccountActivityStatus.ACTIVE, CustomerAccountVerificationStatus.UNVERIFIED))
                    .thenReturn(expectedAccounts);

            List<CustomerAccount> result = customerAccountService.findByActivityStatusAndVerificationStatus(
                    CustomerAccountActivityStatus.ACTIVE, CustomerAccountVerificationStatus.UNVERIFIED);

            assertThat(result).isEqualTo(expectedAccounts);
            verify(customerAccountRepository).findByActivityStatusAndVerificationStatus(
                    CustomerAccountActivityStatus.ACTIVE, CustomerAccountVerificationStatus.UNVERIFIED);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findByActivityStatusAndVerificationStatus(
                    CustomerAccountActivityStatus.ACTIVE, CustomerAccountVerificationStatus.UNVERIFIED))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findByActivityStatusAndVerificationStatus(
                    CustomerAccountActivityStatus.ACTIVE, CustomerAccountVerificationStatus.UNVERIFIED))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findAccountsWithLastLoginBefore() Tests")
    class FindAccountsWithLastLoginBeforeTests {

        @Test
        @DisplayName("Should throw NullFieldException when date is null")
        void shouldThrowNullFieldExceptionWhenDateIsNull() {
            assertThatThrownBy(() -> customerAccountService.findAccountsWithLastLoginBefore(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'date' cannot be null");
        }

        @Test
        @DisplayName("Should find accounts with last login before date successfully")
        void shouldFindAccountsWithLastLoginBeforeDateSuccessfully() {
            OffsetDateTime testDate = OffsetDateTime.now().minusDays(30);
            List<CustomerAccount> expectedAccounts = List.of(sampleAccount);
            when(customerAccountRepository.findByLastLoginAtBefore(testDate)).thenReturn(expectedAccounts);

            List<CustomerAccount> result = customerAccountService.findAccountsWithLastLoginBefore(testDate);

            assertThat(result).isEqualTo(expectedAccounts);
            verify(customerAccountRepository).findByLastLoginAtBefore(testDate);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            OffsetDateTime testDate = OffsetDateTime.now().minusDays(30);
            when(customerAccountRepository.findByLastLoginAtBefore(testDate))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findAccountsWithLastLoginBefore(testDate))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("findAccountsWithNoLogin() Tests")
    class FindAccountsWithNoLoginTests {

        @Test
        @DisplayName("Should find accounts with no login successfully")
        void shouldFindAccountsWithNoLoginSuccessfully() {
            List<CustomerAccount> expectedAccounts = List.of(sampleAccount);
            when(customerAccountRepository.findByLastLoginAtIsNull()).thenReturn(expectedAccounts);

            List<CustomerAccount> result = customerAccountService.findAccountsWithNoLogin();

            assertThat(result).isEqualTo(expectedAccounts);
            verify(customerAccountRepository).findByLastLoginAtIsNull();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findByLastLoginAtIsNull())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.findAccountsWithNoLogin())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("accountExists() Tests")
    class AccountExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.accountExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should return true when account exists")
        void shouldReturnTrueWhenAccountExists() {
            when(customerAccountRepository.existsById(VALID_ACCOUNT_ID)).thenReturn(true);

            boolean result = customerAccountService.accountExists(VALID_ACCOUNT_ID);

            assertThat(result).isTrue();
            verify(customerAccountRepository).existsById(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should return false when account does not exist")
        void shouldReturnFalseWhenAccountDoesNotExist() {
            when(customerAccountRepository.existsById(VALID_ACCOUNT_ID)).thenReturn(false);

            boolean result = customerAccountService.accountExists(VALID_ACCOUNT_ID);

            assertThat(result).isFalse();
            verify(customerAccountRepository).existsById(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.existsById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.accountExists(VALID_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("usernameExists() Tests")
    class UsernameExistsTests {

        @Test
        @DisplayName("Should throw NullFieldException when username is null")
        void shouldThrowNullFieldExceptionWhenUsernameIsNull() {
            assertThatThrownBy(() -> customerAccountService.usernameExists(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'username' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when username is empty")
        void shouldThrowEmptyFieldExceptionWhenUsernameIsEmpty() {
            assertThatThrownBy(() -> customerAccountService.usernameExists("   "))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'username' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when username is too short")
        void shouldThrowFieldTooShortExceptionWhenUsernameIsTooShort() {
            assertThatThrownBy(() -> customerAccountService.usernameExists("abc"))
                    .isInstanceOf(FieldTooShortException.class)
                    .hasMessage("Field username too short: 3 characters. Min: 5 characters");
        }

        @Test
        @DisplayName("Should return true when username exists")
        void shouldReturnTrueWhenUsernameExists() {
            when(customerAccountRepository.existsByUsername(anyString())).thenReturn(true);

            boolean result = customerAccountService.usernameExists(VALID_USERNAME);

            assertThat(result).isTrue();
            verify(customerAccountRepository).existsByUsername(anyString());
        }

        @Test
        @DisplayName("Should return false when username does not exist")
        void shouldReturnFalseWhenUsernameDoesNotExist() {
            when(customerAccountRepository.existsByUsername(anyString())).thenReturn(false);

            boolean result = customerAccountService.usernameExists(VALID_USERNAME);

            assertThat(result).isFalse();
            verify(customerAccountRepository).existsByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.existsByUsername(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.usernameExists(VALID_USERNAME))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("countAllAccounts() Tests")
    class CountAllAccountsTests {

        @Test
        @DisplayName("Should return total accounts count successfully")
        void shouldReturnTotalAccountsCountSuccessfully() {
            long expectedCount = 25L;
            when(customerAccountRepository.count()).thenReturn(expectedCount);

            long result = customerAccountService.countAllAccounts();

            assertThat(result).isEqualTo(expectedCount);
            verify(customerAccountRepository).count();
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.count())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.countAllAccounts())
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UPDATE OPERATIONS TESTS =====

    @Nested
    @DisplayName("updatePassword() Tests")
    class UpdatePasswordTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.updatePassword(null, NEW_PASSWORD))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when newRawPassword is null")
        void shouldThrowNullFieldExceptionWhenNewRawPasswordIsNull() {
            assertThatThrownBy(() -> customerAccountService.updatePassword(VALID_ACCOUNT_ID, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'newRawPassword' cannot be null");
        }

        @Test
        @DisplayName("Should throw PasswordValidationException when newRawPassword has no digits")
        void shouldThrowPasswordValidationExceptionWhenNewRawPasswordHasNoDigits() {
            assertThatThrownBy(() -> customerAccountService.updatePassword(VALID_ACCOUNT_ID, "passwordwithoutdigits"))
                    .isInstanceOf(PasswordValidationException.class)
                    .hasMessageContaining("Password must be at least 8 characters long and contain at least one number");
        }

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePasswordSuccessfully() {
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newHashedPassword");
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(sampleAccount));
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.updatePassword(VALID_ACCOUNT_ID, NEW_PASSWORD);

            assertThat(result).isNotNull();
            verify(passwordEncoder).encode(NEW_PASSWORD);
            verify(customerAccountRepository).findById(VALID_ACCOUNT_ID);
            verify(customerAccountRepository).save(any(CustomerAccount.class));
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.updatePassword(VALID_ACCOUNT_ID, NEW_PASSWORD))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found with ID: " + VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.updatePassword(VALID_ACCOUNT_ID, NEW_PASSWORD))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("updateActivityStatus() Tests")
    class UpdateActivityStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.updateActivityStatus(null, CustomerAccountActivityStatus.INACTIVE))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when activityStatus is null")
        void shouldThrowNullFieldExceptionWhenActivityStatusIsNull() {
            assertThatThrownBy(() -> customerAccountService.updateActivityStatus(VALID_ACCOUNT_ID, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'activityStatus' cannot be null");
        }

        @Test
        @DisplayName("Should update activity status successfully")
        void shouldUpdateActivityStatusSuccessfully() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(sampleAccount));
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.updateActivityStatus(VALID_ACCOUNT_ID, CustomerAccountActivityStatus.INACTIVE);

            assertThat(result).isNotNull();
            verify(customerAccountRepository).findById(VALID_ACCOUNT_ID);
            verify(customerAccountRepository).save(any(CustomerAccount.class));
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.updateActivityStatus(VALID_ACCOUNT_ID, CustomerAccountActivityStatus.INACTIVE))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found with ID: " + VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.updateActivityStatus(VALID_ACCOUNT_ID, CustomerAccountActivityStatus.INACTIVE))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("updateVerificationStatus() Tests")
    class UpdateVerificationStatusTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.updateVerificationStatus(null, CustomerAccountVerificationStatus.EMAIL_VERIFIED))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when verificationStatus is null")
        void shouldThrowNullFieldExceptionWhenVerificationStatusIsNull() {
            assertThatThrownBy(() -> customerAccountService.updateVerificationStatus(VALID_ACCOUNT_ID, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'verificationStatus' cannot be null");
        }

        @Test
        @DisplayName("Should update verification status successfully")
        void shouldUpdateVerificationStatusSuccessfully() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(sampleAccount));
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.updateVerificationStatus(VALID_ACCOUNT_ID, CustomerAccountVerificationStatus.EMAIL_VERIFIED);

            assertThat(result).isNotNull();
            verify(customerAccountRepository).findById(VALID_ACCOUNT_ID);
            verify(customerAccountRepository).save(any(CustomerAccount.class));
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.updateVerificationStatus(VALID_ACCOUNT_ID, CustomerAccountVerificationStatus.EMAIL_VERIFIED))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found with ID: " + VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.updateVerificationStatus(VALID_ACCOUNT_ID, CustomerAccountVerificationStatus.EMAIL_VERIFIED))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("updateLastLoginTime() Tests")
    class UpdateLastLoginTimeTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.updateLastLoginTime(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should update last login time successfully")
        void shouldUpdateLastLoginTimeSuccessfully() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.of(sampleAccount));
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.updateLastLoginTime(VALID_ACCOUNT_ID);

            assertThat(result).isNotNull();
            verify(customerAccountRepository).findById(VALID_ACCOUNT_ID);
            verify(customerAccountRepository).save(any(CustomerAccount.class));
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerAccountService.updateLastLoginTime(VALID_ACCOUNT_ID))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found with ID: " + VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.findById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.updateLastLoginTime(VALID_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("saveAccount() Tests")
    class SaveAccountTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccount is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIsNull() {
            assertThatThrownBy(() -> customerAccountService.saveAccount(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccount' cannot be null");
        }

        @Test
        @DisplayName("Should save account successfully")
        void shouldSaveAccountSuccessfully() {
            when(customerAccountRepository.save(any(CustomerAccount.class))).thenReturn(sampleAccount);

            CustomerAccount result = customerAccountService.saveAccount(sampleAccount);

            assertThat(result).isNotNull();
            verify(customerAccountRepository).save(sampleAccount);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.save(any(CustomerAccount.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.saveAccount(sampleAccount))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== DELETE OPERATIONS TESTS =====

    @Nested
    @DisplayName("deleteById() Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should throw NullFieldException when accountId is null")
        void shouldThrowNullFieldExceptionWhenAccountIdIsNull() {
            assertThatThrownBy(() -> customerAccountService.deleteById(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'accountId' cannot be null");
        }

        @Test
        @DisplayName("Should delete account by ID successfully")
        void shouldDeleteAccountByIdSuccessfully() {
            when(customerAccountRepository.existsById(VALID_ACCOUNT_ID)).thenReturn(true);

            assertThatCode(() -> customerAccountService.deleteById(VALID_ACCOUNT_ID))
                    .doesNotThrowAnyException();

            verify(customerAccountRepository).deleteById(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw CustomerAccountNotFoundException when account does not exist")
        void shouldThrowCustomerAccountNotFoundExceptionWhenAccountDoesNotExist() {
            when(customerAccountRepository.existsById(VALID_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> customerAccountService.deleteById(VALID_ACCOUNT_ID))
                    .isInstanceOf(CustomerAccountNotFoundException.class)
                    .hasMessage("Customer account not found with ID: " + VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            when(customerAccountRepository.existsById(VALID_ACCOUNT_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> customerAccountService.deleteById(VALID_ACCOUNT_ID))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    @Nested
    @DisplayName("deleteAccount() Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should throw NullFieldException when customerAccount is null")
        void shouldThrowNullFieldExceptionWhenCustomerAccountIsNull() {
            assertThatThrownBy(() -> customerAccountService.deleteAccount(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'customerAccount' cannot be null");
        }

        @Test
        @DisplayName("Should delete account successfully")
        void shouldDeleteAccountSuccessfully() {
            customerAccountService.deleteAccount(sampleAccount);

            verify(customerAccountRepository).delete(sampleAccount);
        }

        @Test
        @DisplayName("Should throw DatabaseSystemException when repository error occurs")
        void shouldThrowDatabaseSystemExceptionWhenRepositoryErrorOccurs() {
            doThrow(new RuntimeException("Database connection failed"))
                    .when(customerAccountRepository).delete(any(CustomerAccount.class));

            assertThatThrownBy(() -> customerAccountService.deleteAccount(sampleAccount))
                    .isInstanceOf(DatabaseSystemException.class)
                    .hasMessageContaining("Unexpected repository error");
        }
    }

    // ===== UTILITY METHODS TESTS =====

    @Nested
    @DisplayName("verifyPassword() Tests")
    class VerifyPasswordTests {

        @Test
        @DisplayName("Should throw NullFieldException when rawPassword is null")
        void shouldThrowNullFieldExceptionWhenRawPasswordIsNull() {
            assertThatThrownBy(() -> customerAccountService.verifyPassword(null, "$2a$10$hashedPassword"))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'rawPassword' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when hashedPassword is null")
        void shouldThrowNullFieldExceptionWhenHashedPasswordIsNull() {
            assertThatThrownBy(() -> customerAccountService.verifyPassword(VALID_PASSWORD, null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'hashedPassword' cannot be null");
        }

        @Test
        @DisplayName("Should return true when passwords match")
        void shouldReturnTrueWhenPasswordsMatch() {
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            boolean result = customerAccountService.verifyPassword(VALID_PASSWORD, "$2a$10$hashedPassword");

            assertThat(result).isTrue();
            verify(passwordEncoder).matches(VALID_PASSWORD, "$2a$10$hashedPassword");
        }

        @Test
        @DisplayName("Should return false when passwords do not match")
        void shouldReturnFalseWhenPasswordsDoNotMatch() {
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            boolean result = customerAccountService.verifyPassword("wrongpassword123", "$2a$10$hashedPassword");

            assertThat(result).isFalse();
            verify(passwordEncoder).matches("wrongpassword123", "$2a$10$hashedPassword");
        }
    }
}
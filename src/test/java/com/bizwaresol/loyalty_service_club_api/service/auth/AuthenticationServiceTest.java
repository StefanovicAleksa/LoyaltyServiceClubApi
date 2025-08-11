// =====================================================================================
// FILE: src/test/java/com/bizwaresol/loyalty_service_club_api/service/auth/AuthenticationServiceTest.java
// =====================================================================================
package com.bizwaresol.loyalty_service_club_api.service.auth;

import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.LoginRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.RegistrationRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.LoginResult;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.RegistrationResult;
import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountVerificationStatus;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateEmailException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicatePhoneException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.AccountSuspendedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.InvalidLoginCredentialsException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.ContactAlreadyRegisteredException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.MissingContactInformationException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.RegistrationFailedException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.security.PasswordValidationException;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerAccountService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerEmailService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerPhoneService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private CustomerAccountService customerAccountService;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerEmailService customerEmailService;

    @Mock
    private CustomerPhoneService customerPhoneService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private CustomerAccount sampleAccount;
    private Customer sampleCustomer;
    private CustomerEmail sampleEmail;
    private CustomerPhone samplePhone;
    private LoginRequest validLoginRequest;
    private RegistrationRequest validRegistrationRequest;

    private final String VALID_EMAIL = "test@gmail.com";
    private final String VALID_PHONE = "+381123456789";
    private final String VALID_USERNAME = "testuser";
    private final String VALID_PASSWORD = "password123";
    private final String VALID_FIRST_NAME = "John";
    private final String VALID_LAST_NAME = "Doe";
    private final Long VALID_ID = 1L;
    private final Long EMAIL_ID = 2L;
    private final Long PHONE_ID = 3L;

    @BeforeEach
    void setUp() {
        sampleEmail = new CustomerEmail();
        sampleEmail.setId(EMAIL_ID);
        sampleEmail.setEmail(VALID_EMAIL);
        sampleEmail.setVerified(false);

        samplePhone = new CustomerPhone();
        samplePhone.setId(PHONE_ID);
        samplePhone.setPhone(VALID_PHONE);
        samplePhone.setVerified(false);

        sampleCustomer = new Customer();
        sampleCustomer.setId(VALID_ID);
        sampleCustomer.setFirstName(VALID_FIRST_NAME);
        sampleCustomer.setLastName(VALID_LAST_NAME);
        sampleCustomer.setEmail(sampleEmail);
        sampleCustomer.setPhone(samplePhone);

        sampleAccount = new CustomerAccount();
        sampleAccount.setId(VALID_ID);
        sampleAccount.setUsername(VALID_USERNAME);
        sampleAccount.setPassword("$2a$10$hashedPassword");
        sampleAccount.setActivityStatus(CustomerAccountActivityStatus.ACTIVE);
        sampleAccount.setVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED);
        sampleAccount.setCustomer(sampleCustomer);
        sampleAccount.setCreatedDate(OffsetDateTime.now());
        sampleAccount.setLastModifiedDate(OffsetDateTime.now());

        validLoginRequest = new LoginRequest(VALID_USERNAME, VALID_PASSWORD, false);
        validRegistrationRequest = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_EMAIL, VALID_PHONE, VALID_PASSWORD, false);
    }

    // ===== AUTHENTICATION TESTS =====

    @Nested
    @DisplayName("authenticate() Tests")
    class AuthenticateTests {

        @Test
        @DisplayName("Should throw NullFieldException when request is null")
        void shouldThrowNullFieldExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> authenticationService.authenticate(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessageContaining("loginRequest");
        }

        @Test
        @DisplayName("Should throw NullFieldException when identifier is null")
        void shouldThrowNullFieldExceptionWhenIdentifierIsNull() {
            LoginRequest request = new LoginRequest(null, VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessageContaining("identifier");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when identifier is empty")
        void shouldThrowEmptyFieldExceptionWhenIdentifierIsEmpty() {
            LoginRequest request = new LoginRequest("   ", VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessageContaining("identifier");
        }

        @Test
        @DisplayName("Should throw NullFieldException when password is null")
        void shouldThrowNullFieldExceptionWhenPasswordIsNull() {
            LoginRequest request = new LoginRequest(VALID_USERNAME, null, false);

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessageContaining("password");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when password is empty")
        void shouldThrowEmptyFieldExceptionWhenPasswordIsEmpty() {
            LoginRequest request = new LoginRequest(VALID_USERNAME, "   ", false);

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessageContaining("password");
        }

        @Test
        @DisplayName("Should authenticate successfully with username")
        void shouldAuthenticateSuccessfullyWithUsername() {
            when(customerAccountService.findByUsername(VALID_USERNAME)).thenReturn(sampleAccount);
            when(customerAccountService.verifyPassword(VALID_PASSWORD, sampleAccount.getPassword())).thenReturn(true);
            when(customerAccountService.updateLastLoginTime(VALID_ID)).thenReturn(sampleAccount);

            LoginResult result = authenticationService.authenticate(validLoginRequest);

            assertThat(result.success()).isTrue();
            assertThat(result.account()).isEqualTo(sampleAccount);
            assertThat(result.rememberMeEnabled()).isFalse();
            assertThat(result.activityStatus()).isEqualTo(CustomerAccountActivityStatus.ACTIVE);
            assertThat(result.verificationStatus()).isEqualTo(CustomerAccountVerificationStatus.UNVERIFIED);

            verify(customerAccountService).findByUsername(VALID_USERNAME);
            verify(customerAccountService).verifyPassword(VALID_PASSWORD, sampleAccount.getPassword());
            verify(customerAccountService).updateLastLoginTime(VALID_ID);
        }

        @Test
        @DisplayName("Should authenticate successfully with email")
        void shouldAuthenticateSuccessfullyWithEmail() {
            LoginRequest emailLoginRequest = new LoginRequest(VALID_EMAIL, VALID_PASSWORD, true);
            sampleAccount.setUsername(VALID_EMAIL); // Assume username is email

            when(customerAccountService.findByUsername(VALID_EMAIL)).thenReturn(sampleAccount);
            when(customerAccountService.verifyPassword(VALID_PASSWORD, sampleAccount.getPassword())).thenReturn(true);
            when(customerAccountService.updateLastLoginTime(VALID_ID)).thenReturn(sampleAccount);

            LoginResult result = authenticationService.authenticate(emailLoginRequest);

            assertThat(result.success()).isTrue();
            assertThat(result.account()).isEqualTo(sampleAccount);
            assertThat(result.rememberMeEnabled()).isTrue();

            verify(customerAccountService).findByUsername(VALID_EMAIL);
            verify(customerAccountService).verifyPassword(VALID_PASSWORD, sampleAccount.getPassword());
            verify(customerAccountService).updateLastLoginTime(VALID_ID);
        }

        @Test
        @DisplayName("Should authenticate successfully with phone number")
        void shouldAuthenticateSuccessfullyWithPhoneNumber() {
            LoginRequest phoneLoginRequest = new LoginRequest(VALID_PHONE, VALID_PASSWORD, false);
            sampleAccount.setUsername(VALID_PHONE); // Assume username is phone

            when(customerAccountService.findByUsername(VALID_PHONE)).thenReturn(sampleAccount);
            when(customerAccountService.verifyPassword(VALID_PASSWORD, sampleAccount.getPassword())).thenReturn(true);
            when(customerAccountService.updateLastLoginTime(VALID_ID)).thenReturn(sampleAccount);

            LoginResult result = authenticationService.authenticate(phoneLoginRequest);

            assertThat(result.success()).isTrue();
            assertThat(result.account()).isEqualTo(sampleAccount);
            assertThat(result.rememberMeEnabled()).isFalse();

            verify(customerAccountService).findByUsername(VALID_PHONE);
            verify(customerAccountService).verifyPassword(VALID_PASSWORD, sampleAccount.getPassword());
            verify(customerAccountService).updateLastLoginTime(VALID_ID);
        }

        @Test
        @DisplayName("Should throw InvalidLoginCredentialsException when account not found")
        void shouldThrowInvalidLoginCredentialsExceptionWhenAccountNotFound() {
            when(customerAccountService.findByUsername(VALID_USERNAME)).thenThrow(new CustomerAccountNotFoundException(VALID_USERNAME));

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(InvalidLoginCredentialsException.class)
                    .hasMessage("Invalid login credentials for identifier: " + VALID_USERNAME);

            verify(customerAccountService).findByUsername(VALID_USERNAME);
        }

        @Test
        @DisplayName("Should throw InvalidLoginCredentialsException when password is invalid")
        void shouldThrowInvalidLoginCredentialsExceptionWhenPasswordIsInvalid() {
            when(customerAccountService.findByUsername(VALID_USERNAME)).thenReturn(sampleAccount);
            when(customerAccountService.verifyPassword(VALID_PASSWORD, sampleAccount.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(InvalidLoginCredentialsException.class)
                    .hasMessage("Invalid login credentials for identifier: " + VALID_USERNAME);

            verify(customerAccountService).findByUsername(VALID_USERNAME);
            verify(customerAccountService).verifyPassword(VALID_PASSWORD, sampleAccount.getPassword());
            verify(customerAccountService, never()).updateLastLoginTime(any());
        }

        @Test
        @DisplayName("Should throw AccountSuspendedException when account is suspended")
        void shouldThrowAccountSuspendedExceptionWhenAccountIsSuspended() {
            sampleAccount.setActivityStatus(CustomerAccountActivityStatus.SUSPENDED);

            when(customerAccountService.findByUsername(VALID_USERNAME)).thenReturn(sampleAccount);
            when(customerAccountService.verifyPassword(VALID_PASSWORD, sampleAccount.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.authenticate(validLoginRequest))
                    .isInstanceOf(AccountSuspendedException.class)
                    .hasMessage("Account is suspended and cannot login: " + VALID_USERNAME);

            verify(customerAccountService).findByUsername(VALID_USERNAME);
            verify(customerAccountService).verifyPassword(VALID_PASSWORD, sampleAccount.getPassword());
            verify(customerAccountService, never()).updateLastLoginTime(any());
        }

        @Test
        @DisplayName("Should preserve previous login time")
        void shouldPreservePreviousLoginTime() {
            OffsetDateTime previousLogin = OffsetDateTime.now().minusHours(2);
            sampleAccount.setLastLoginAt(previousLogin);

            when(customerAccountService.findByUsername(VALID_USERNAME)).thenReturn(sampleAccount);
            when(customerAccountService.verifyPassword(VALID_PASSWORD, sampleAccount.getPassword())).thenReturn(true);
            when(customerAccountService.updateLastLoginTime(VALID_ID)).thenReturn(sampleAccount);

            LoginResult result = authenticationService.authenticate(validLoginRequest);

            assertThat(result.previousLoginAt()).isEqualTo(previousLogin);

            verify(customerAccountService).updateLastLoginTime(VALID_ID);
        }
    }

    // ===== REGISTRATION TESTS =====

    @Nested
    @DisplayName("register() Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should throw NullFieldException when request is null")
        void shouldThrowNullFieldExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> authenticationService.register(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessageContaining("registrationRequest");
        }

        @Test
        @DisplayName("Should throw NullFieldException when firstName is null")
        void shouldThrowNullFieldExceptionWhenFirstNameIsNull() {
            RegistrationRequest request = new RegistrationRequest(null, VALID_LAST_NAME, VALID_EMAIL, null, VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessageContaining("firstName");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when firstName is empty")
        void shouldThrowEmptyFieldExceptionWhenFirstNameIsEmpty() {
            RegistrationRequest request = new RegistrationRequest("   ", VALID_LAST_NAME, VALID_EMAIL, null, VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessageContaining("firstName");
        }

        @Test
        @DisplayName("Should throw FieldTooShortException when password is invalid")
        void shouldThrowFieldTooShortExceptionWhenPasswordIsInvalid() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_EMAIL, null, "weak", false);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(FieldTooShortException.class);
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when email format is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenEmailFormatIsInvalid() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, "invalid-email", null, VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(InvalidEmailFormatException.class);
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone format is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneFormatIsInvalid() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, null, "invalid-phone", VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(InvalidPhoneFormatException.class);
        }

        @Test
        @DisplayName("Should throw MissingContactInformationException when no contact provided")
        void shouldThrowMissingContactInformationExceptionWhenNoContactProvided() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, null, null, VALID_PASSWORD, false);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(MissingContactInformationException.class)
                    .hasMessage("Registration requires at least email or phone contact information");
        }

        @Test
        @DisplayName("Should register successfully with email only")
        void shouldRegisterSuccessfullyWithEmailOnly() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_EMAIL, null, VALID_PASSWORD, false);

            when(customerEmailService.createEmail(VALID_EMAIL)).thenReturn(sampleEmail);
            when(customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, null)).thenReturn(sampleCustomer);
            when(customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD)).thenReturn(sampleAccount);

            RegistrationResult result = authenticationService.register(request);

            assertThat(result.success()).isTrue();
            assertThat(result.account()).isEqualTo(sampleAccount);
            assertThat(result.customer()).isEqualTo(sampleCustomer);
            assertThat(result.emailProvided()).isTrue();
            assertThat(result.phoneProvided()).isFalse();
            assertThat(result.rememberMeEnabled()).isFalse();
            assertThat(result.preferredContactMethod()).isEqualTo("email");

            verify(customerEmailService).createEmail(VALID_EMAIL);
            verify(customerService).createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, null);
            verify(customerAccountService).createAccount(sampleCustomer, VALID_PASSWORD);
            verify(customerPhoneService, never()).createPhone(any());
        }

        @Test
        @DisplayName("Should register successfully with phone only")
        void shouldRegisterSuccessfullyWithPhoneOnly() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, null, VALID_PHONE, VALID_PASSWORD, false);

            when(customerPhoneService.createPhone(VALID_PHONE)).thenReturn(samplePhone);
            when(customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, null, samplePhone)).thenReturn(sampleCustomer);
            when(customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD)).thenReturn(sampleAccount);

            RegistrationResult result = authenticationService.register(request);

            assertThat(result.success()).isTrue();
            assertThat(result.account()).isEqualTo(sampleAccount);
            assertThat(result.customer()).isEqualTo(sampleCustomer);
            assertThat(result.emailProvided()).isFalse();
            assertThat(result.phoneProvided()).isTrue();
            assertThat(result.rememberMeEnabled()).isFalse();
            assertThat(result.preferredContactMethod()).isEqualTo("phone");

            verify(customerPhoneService).createPhone(VALID_PHONE);
            verify(customerService).createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, null, samplePhone);
            verify(customerAccountService).createAccount(sampleCustomer, VALID_PASSWORD);
            verify(customerEmailService, never()).createEmail(any());
        }

        @Test
        @DisplayName("Should register successfully with both email and phone")
        void shouldRegisterSuccessfullyWithBothEmailAndPhone() {
            when(customerEmailService.createEmail(VALID_EMAIL)).thenReturn(sampleEmail);
            when(customerPhoneService.createPhone(VALID_PHONE)).thenReturn(samplePhone);
            when(customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone)).thenReturn(sampleCustomer);
            when(customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD)).thenReturn(sampleAccount);

            RegistrationResult result = authenticationService.register(validRegistrationRequest);

            assertThat(result.success()).isTrue();
            assertThat(result.account()).isEqualTo(sampleAccount);
            assertThat(result.customer()).isEqualTo(sampleCustomer);
            assertThat(result.emailProvided()).isTrue();
            assertThat(result.phoneProvided()).isTrue();
            assertThat(result.rememberMeEnabled()).isFalse();
            assertThat(result.preferredContactMethod()).isEqualTo("both");

            verify(customerEmailService).createEmail(VALID_EMAIL);
            verify(customerPhoneService).createPhone(VALID_PHONE);
            verify(customerService).createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone);
            verify(customerAccountService).createAccount(sampleCustomer, VALID_PASSWORD);
        }

        @Test
        @DisplayName("Should register successfully with remember me enabled")
        void shouldRegisterSuccessfullyWithRememberMeEnabled() {
            RegistrationRequest request = new RegistrationRequest(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_EMAIL, null, VALID_PASSWORD, true);

            when(customerEmailService.createEmail(VALID_EMAIL)).thenReturn(sampleEmail);
            when(customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, null)).thenReturn(sampleCustomer);
            when(customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD)).thenReturn(sampleAccount);

            RegistrationResult result = authenticationService.register(request);

            assertThat(result.success()).isTrue();
            assertThat(result.rememberMeEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw ContactAlreadyRegisteredException when email already exists")
        void shouldThrowContactAlreadyRegisteredExceptionWhenEmailAlreadyExists() {
            when(customerEmailService.createEmail(VALID_EMAIL)).thenThrow(new DuplicateEmailException(VALID_EMAIL));

            assertThatThrownBy(() -> authenticationService.register(validRegistrationRequest))
                    .isInstanceOf(ContactAlreadyRegisteredException.class)
                    .hasMessage("Contact already registered: " + VALID_EMAIL + " (email)");

            verify(customerEmailService).createEmail(VALID_EMAIL);
            verify(customerService, never()).createCustomer(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw ContactAlreadyRegisteredException when phone already exists")
        void shouldThrowContactAlreadyRegisteredExceptionWhenPhoneAlreadyExists() {
            when(customerEmailService.createEmail(VALID_EMAIL)).thenReturn(sampleEmail);
            when(customerPhoneService.createPhone(VALID_PHONE)).thenThrow(new DuplicatePhoneException(VALID_PHONE));

            assertThatThrownBy(() -> authenticationService.register(validRegistrationRequest))
                    .isInstanceOf(ContactAlreadyRegisteredException.class)
                    .hasMessage("Contact already registered: " + VALID_PHONE + " (phone)");

            verify(customerEmailService).createEmail(VALID_EMAIL);
            verify(customerPhoneService).createPhone(VALID_PHONE);
            verify(customerService, never()).createCustomer(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw RegistrationFailedException when customer creation fails")
        void shouldThrowRegistrationFailedExceptionWhenCustomerCreationFails() {
            when(customerEmailService.createEmail(VALID_EMAIL)).thenReturn(sampleEmail);
            when(customerPhoneService.createPhone(VALID_PHONE)).thenReturn(samplePhone);
            when(customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> authenticationService.register(validRegistrationRequest))
                    .isInstanceOf(RegistrationFailedException.class);

            verify(customerEmailService).createEmail(VALID_EMAIL);
            verify(customerPhoneService).createPhone(VALID_PHONE);
            verify(customerService).createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone);
            verify(customerAccountService, never()).createAccount(any(), any());
        }

        @Test
        @DisplayName("Should throw RegistrationFailedException when account creation fails")
        void shouldThrowRegistrationFailedExceptionWhenAccountCreationFails() {
            when(customerEmailService.createEmail(VALID_EMAIL)).thenReturn(sampleEmail);
            when(customerPhoneService.createPhone(VALID_PHONE)).thenReturn(samplePhone);
            when(customerService.createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone)).thenReturn(sampleCustomer);
            when(customerAccountService.createAccount(sampleCustomer, VALID_PASSWORD))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> authenticationService.register(validRegistrationRequest))
                    .isInstanceOf(RegistrationFailedException.class);

            verify(customerEmailService).createEmail(VALID_EMAIL);
            verify(customerPhoneService).createPhone(VALID_PHONE);
            verify(customerService).createCustomer(VALID_FIRST_NAME, VALID_LAST_NAME, sampleEmail, samplePhone);
            verify(customerAccountService).createAccount(sampleCustomer, VALID_PASSWORD);
        }
    }
}

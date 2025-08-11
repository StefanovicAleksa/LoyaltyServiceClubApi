// =====================================================================================
// FILE: src/main/java/com/bizwaresol/loyalty_service_club_api/service/auth/AuthenticationService.java
// =====================================================================================
package com.bizwaresol.loyalty_service_club_api.service.auth;

import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.LoginRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.RegistrationRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.LoginResult;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.RegistrationResult;
import com.bizwaresol.loyalty_service_club_api.domain.entity.*;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.AccountSuspendedException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.InvalidLoginCredentialsException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.ContactAlreadyRegisteredException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.MissingContactInformationException;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.RegistrationFailedException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.ValidationException;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerAccountService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerEmailService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerPhoneService;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerService;
import com.bizwaresol.loyalty_service_club_api.util.mappers.AuthErrorMapper;
import com.bizwaresol.loyalty_service_club_api.util.validators.AuthValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class AuthenticationService {

    private final CustomerAccountService customerAccountService;
    private final CustomerService customerService;
    private final CustomerEmailService customerEmailService;
    private final CustomerPhoneService customerPhoneService;

    public AuthenticationService(
            CustomerAccountService customerAccountService,
            CustomerService customerService,
            CustomerEmailService customerEmailService,
            CustomerPhoneService customerPhoneService) {
        this.customerAccountService = customerAccountService;
        this.customerService = customerService;
        this.customerEmailService = customerEmailService;
        this.customerPhoneService = customerPhoneService;
    }

    // ===== AUTHENTICATION =====

    /**
     * Authenticates a user with email or phone and password
     * @param request login request containing identifier, password, and rememberMe flag
     * @return LoginResult with authentication status and account information
     * @throws InvalidLoginCredentialsException if credentials are invalid
     * @throws AccountSuspendedException if account is suspended
     */
    @Transactional
    public LoginResult authenticate(LoginRequest request) {
        // 1. Validate input first to fail fast
        AuthValidator.validateLoginRequest(request);

        try {
            // 2. Find account by identifier (which is the username field, populated by email/phone via trigger)
            CustomerAccount account = findAccountByIdentifier(request.identifier());

            // 3. Validate password
            if (!customerAccountService.verifyPassword(request.password(), account.getPassword())) {
                throw new InvalidLoginCredentialsException(request.identifier());
            }

            // 4. Check account not suspended
            if (account.getActivityStatus() == CustomerAccountActivityStatus.SUSPENDED) {
                throw new AccountSuspendedException(request.identifier());
            }

            // 5. Store previous login time and update last_login_at
            OffsetDateTime previousLoginAt = account.getLastLoginAt();
            customerAccountService.updateLastLoginTime(account.getId());

            // 6. Return success result
            return LoginResult.success(account, request.rememberMe(), previousLoginAt);
        } catch (ValidationException | InvalidLoginCredentialsException | AccountSuspendedException e) {
            // Re-throw expected business and validation exceptions directly
            throw e;
        } catch (Exception e) {
            // Map only unexpected exceptions
            throw AuthErrorMapper.mapToLoginException(e, request.identifier());
        }
    }

    // ===== REGISTRATION =====

    /**
     * Registers a new user with contact information and password
     * @param request registration request containing user details
     * @return RegistrationResult with registration status and new account information
     * @throws MissingContactInformationException if no email or phone provided
     * @throws ContactAlreadyRegisteredException if email or phone already exists
     * @throws RegistrationFailedException for other registration errors
     */
    @Transactional
    public RegistrationResult register(RegistrationRequest request) {
        // 1. Validate input first to fail fast
        AuthValidator.validateRegistrationRequest(request);

        try {
            // 2. Create contact entities (bottom-up approach)
            CustomerEmail email = createEmailIfProvided(request.email());
            CustomerPhone phone = createPhoneIfProvided(request.phone());

            // 3. Create customer entity
            Customer customer = customerService.createCustomer(
                    request.firstName(),
                    request.lastName(),
                    email,
                    phone
            );

            // 4. Create customer account (username auto-set by trigger)
            CustomerAccount account = customerAccountService.createAccount(customer, request.password());

            // 5. Return success result
            return RegistrationResult.success(
                    account,
                    customer,
                    email != null,
                    phone != null,
                    request.rememberMe()
            );
        } catch (ValidationException | MissingContactInformationException | ContactAlreadyRegisteredException | RegistrationFailedException e) {
            // Re-throw expected business and validation exceptions directly
            throw e;
        } catch (Exception e) {
            // Map only unexpected exceptions
            throw AuthErrorMapper.mapToRegistrationException(e);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private CustomerAccount findAccountByIdentifier(String identifier) {
        // Per architecture, the username field is the single source of truth for login.
        // It is populated with email or phone by a database trigger.
        try {
            return customerAccountService.findByUsername(identifier.trim());
        } catch (CustomerAccountNotFoundException e) {
            // If the account is not found by the identifier (username), it's invalid credentials.
            throw new InvalidLoginCredentialsException(identifier);
        } catch (Exception e) {
            // Let other exceptions (like validation) bubble up to be mapped
            throw AuthErrorMapper.mapToLoginException(e, identifier);
        }
    }

    private CustomerEmail createEmailIfProvided(String email) {
        if (email != null && !email.trim().isEmpty()) {
            try {
                return customerEmailService.createEmail(email);
            } catch (Exception e) {
                // Let the mapper handle specific duplicate exceptions
                throw AuthErrorMapper.mapToRegistrationException(e);
            }
        }
        return null;
    }

    private CustomerPhone createPhoneIfProvided(String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            try {
                return customerPhoneService.createPhone(phone);
            } catch (Exception e) {
                // Let the mapper handle specific duplicate exceptions
                throw AuthErrorMapper.mapToRegistrationException(e);
            }
        }
        return null;
    }
}

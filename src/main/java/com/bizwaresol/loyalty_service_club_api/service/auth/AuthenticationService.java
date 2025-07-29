package com.bizwaresol.loyalty_service_club_api.service.auth;

import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.LoginRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.request.RegistrationRequest;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.LoginResult;
import com.bizwaresol.loyalty_service_club_api.data.dto.auth.result.RegistrationResult;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.service.data.*;
import com.bizwaresol.loyalty_service_club_api.domain.entity.*;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountIdentifierType;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login.*;
import com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.registration.*;
import com.bizwaresol.loyalty_service_club_api.util.validators.AuthValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.AuthErrorMapper;

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
        try {
            // 1. Validate input
            AuthValidator.validateLoginRequest(request);

            // 2. Determine identifier type and find account
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

        } catch (InvalidLoginCredentialsException | AccountSuspendedException e) {
            throw e;
        } catch (Exception e) {
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
        try {
            // 1. Validate input
            AuthValidator.validateRegistrationRequest(request);

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

        } catch (MissingContactInformationException | ContactAlreadyRegisteredException | RegistrationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw AuthErrorMapper.mapToRegistrationException(e);
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private CustomerAccount findAccountByIdentifier(String identifier) {
        CustomerAccountIdentifierType type = CustomerAccountIdentifierType.fromIdentifier(identifier);

        try {
            return customerAccountService.findByUsername(identifier.trim());
        } catch (CustomerAccountNotFoundException e) {
            // Only try phone fallback for actual "not found" cases
            if (type == CustomerAccountIdentifierType.PHONE) {
                try {
                    return customerAccountService.findByPhoneNumber(identifier.trim());
                } catch (Exception phoneException) {
                    throw new InvalidLoginCredentialsException(identifier);
                }
            }
            throw new InvalidLoginCredentialsException(identifier);
        } catch (Exception e) {
            // Let validation errors bubble up through AuthErrorMapper
            throw AuthErrorMapper.mapToLoginException(e, identifier);
        }
    }

    private CustomerEmail createEmailIfProvided(String email) {
        if (email != null && !email.trim().isEmpty()) {
            try {
                return customerEmailService.createEmail(email);
            } catch (Exception e) {
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
                throw AuthErrorMapper.mapToRegistrationException(e);
            }
        }
        return null;
    }
}
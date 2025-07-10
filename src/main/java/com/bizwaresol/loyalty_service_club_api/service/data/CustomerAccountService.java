package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerAccountRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountVerificationStatus;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.security.PasswordValidationException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerAccountService {

    private final CustomerAccountRepository customerAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(CustomerAccountRepository customerAccountRepository, PasswordEncoder passwordEncoder) {
        this.customerAccountRepository = customerAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new customer account
     * @param customer the customer entity to associate with the account
     * @param rawPassword the raw password to hash and store
     * @return the created CustomerAccount entity
     * @throws NullFieldException if customer or rawPassword is null
     * @throws EmptyFieldException if rawPassword is empty
     * @throws FieldTooShortException if rawPassword is too short
     * @throws FieldTooLongException if rawPassword is too long
     * @throws PasswordValidationException if rawPassword doesn't meet requirements
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount createAccount(Customer customer, String rawPassword) throws ServiceException {
        DataValidator.checkNotNull(customer, "customer");
        DataValidator.validatePassword(rawPassword, "rawPassword");

        try {
            CustomerAccount customerAccount = new CustomerAccount();
            customerAccount.setCustomer(customer);
            customerAccount.setPassword(passwordEncoder.encode(rawPassword));
            customerAccount.setActivityStatus(CustomerAccountActivityStatus.ACTIVE);
            customerAccount.setVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED);
            customerAccount.setCreatedDate(OffsetDateTime.now());
            customerAccount.setLastModifiedDate(OffsetDateTime.now());

            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Creates a new customer account with username
     * @param customer the customer entity to associate with the account
     * @param username the username for the account
     * @param rawPassword the raw password to hash and store
     * @return the created CustomerAccount entity
     * @throws NullFieldException if customer, username, or rawPassword is null
     * @throws EmptyFieldException if username or rawPassword is empty
     * @throws FieldTooShortException if username or rawPassword is too short
     * @throws FieldTooLongException if username or rawPassword is too long
     * @throws PasswordValidationException if rawPassword doesn't meet requirements
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount createAccount(Customer customer, String username, String rawPassword) throws ServiceException {
        DataValidator.checkNotNull(customer, "customer");
        DataValidator.validateUsername(username, "username");
        DataValidator.validatePassword(rawPassword, "rawPassword");

        try {
            CustomerAccount customerAccount = new CustomerAccount();
            customerAccount.setCustomer(customer);
            customerAccount.setUsername(username.trim());
            customerAccount.setPassword(passwordEncoder.encode(rawPassword));
            customerAccount.setActivityStatus(CustomerAccountActivityStatus.ACTIVE);
            customerAccount.setVerificationStatus(CustomerAccountVerificationStatus.UNVERIFIED);
            customerAccount.setCreatedDate(OffsetDateTime.now());
            customerAccount.setLastModifiedDate(OffsetDateTime.now());

            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== READ OPERATIONS =====

    /**
     * Retrieves all customer accounts
     * @return list of all CustomerAccount entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerAccount> getAllAccounts() throws ServiceException {
        try {
            return customerAccountRepository.findAll();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an account by ID
     * @param accountId the ID to search for
     * @return the CustomerAccount entity
     * @throws NullFieldException if accountId is null
     * @throws CustomerAccountNotFoundException if account doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerAccount findById(Long accountId) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");

        try {
            return customerAccountRepository.findById(accountId)
                    .orElseThrow(() -> new CustomerAccountNotFoundException(accountId));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an account by username
     * @param username the username to search for
     * @return the CustomerAccount entity
     * @throws NullFieldException if username is null
     * @throws EmptyFieldException if username is empty
     * @throws FieldTooShortException if username is too short
     * @throws FieldTooLongException if username is too long
     * @throws CustomerAccountNotFoundException if account doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerAccount findByUsername(String username) throws ServiceException {
        DataValidator.validateUsername(username, "username");

        try {
            return customerAccountRepository.findByUsername(username.trim())
                    .orElseThrow(() -> new CustomerAccountNotFoundException(username));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an account by customer ID
     * @param customerId the customer ID to search for
     * @return the CustomerAccount entity
     * @throws NullFieldException if customerId is null
     * @throws CustomerAccountNotFoundException if account doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerAccount findByCustomerId(Long customerId) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");

        try {
            return customerAccountRepository.findByCustomerId(customerId)
                    .orElseThrow(() -> new CustomerAccountNotFoundException("No customer account found for customer ID: " + customerId));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves accounts by activity status
     * @param activityStatus the activity status to search for
     * @return list of CustomerAccount entities with the specified activity status
     * @throws NullFieldException if activityStatus is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerAccount> findByActivityStatus(CustomerAccountActivityStatus activityStatus) throws ServiceException {
        DataValidator.checkNotNull(activityStatus, "activityStatus");

        try {
            return customerAccountRepository.findByActivityStatus(activityStatus);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves accounts by verification status
     * @param verificationStatus the verification status to search for
     * @return list of CustomerAccount entities with the specified verification status
     * @throws NullFieldException if verificationStatus is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerAccount> findByVerificationStatus(CustomerAccountVerificationStatus verificationStatus) throws ServiceException {
        DataValidator.checkNotNull(verificationStatus, "verificationStatus");

        try {
            return customerAccountRepository.findByVerificationStatus(verificationStatus);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves accounts by both activity and verification status
     * @param activityStatus the activity status to search for
     * @param verificationStatus the verification status to search for
     * @return list of CustomerAccount entities matching both statuses
     * @throws NullFieldException if activityStatus or verificationStatus is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerAccount> findByActivityStatusAndVerificationStatus(
            CustomerAccountActivityStatus activityStatus,
            CustomerAccountVerificationStatus verificationStatus) throws ServiceException {
        DataValidator.checkNotNull(activityStatus, "activityStatus");
        DataValidator.checkNotNull(verificationStatus, "verificationStatus");

        try {
            return customerAccountRepository.findByActivityStatusAndVerificationStatus(activityStatus, verificationStatus);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves accounts with last login before specified date
     * @param date the date to compare last login against
     * @return list of CustomerAccount entities with last login before the specified date
     * @throws NullFieldException if date is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerAccount> findAccountsWithLastLoginBefore(OffsetDateTime date) throws ServiceException {
        DataValidator.checkNotNull(date, "date");

        try {
            return customerAccountRepository.findByLastLoginAtBefore(date);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves accounts that have never logged in
     * @return list of CustomerAccount entities with null last login
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerAccount> findAccountsWithNoLogin() throws ServiceException {
        try {
            return customerAccountRepository.findByLastLoginAtIsNull();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if an account exists by ID
     * @param accountId the account ID to check
     * @return true if account exists, false otherwise
     * @throws NullFieldException if accountId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean accountExists(Long accountId) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");

        try {
            return customerAccountRepository.existsById(accountId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if a username already exists
     * @param username the username to check
     * @return true if username exists, false otherwise
     * @throws NullFieldException if username is null
     * @throws EmptyFieldException if username is empty
     * @throws FieldTooShortException if username is too short
     * @throws FieldTooLongException if username is too long
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) throws ServiceException {
        DataValidator.validateUsername(username, "username");

        try {
            return customerAccountRepository.existsByUsername(username.trim());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts all accounts
     * @return total number of accounts
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countAllAccounts() throws ServiceException {
        try {
            return customerAccountRepository.count();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Updates account password
     * @param accountId the ID of the account to update
     * @param newRawPassword the new raw password to hash and store
     * @return the updated CustomerAccount entity
     * @throws NullFieldException if accountId or newRawPassword is null
     * @throws EmptyFieldException if newRawPassword is empty
     * @throws FieldTooShortException if newRawPassword is too short
     * @throws FieldTooLongException if newRawPassword is too long
     * @throws PasswordValidationException if newRawPassword doesn't meet requirements
     * @throws CustomerAccountNotFoundException if account with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount updatePassword(Long accountId, String newRawPassword) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");
        DataValidator.validatePassword(newRawPassword, "newRawPassword");

        CustomerAccount customerAccount;
        try {
            customerAccount = customerAccountRepository.findById(accountId)
                    .orElseThrow(() -> new CustomerAccountNotFoundException(accountId));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerAccount.setPassword(passwordEncoder.encode(newRawPassword));
            customerAccount.setLastModifiedDate(OffsetDateTime.now());
            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Updates account activity status
     * @param accountId the ID of the account to update
     * @param activityStatus the new activity status
     * @return the updated CustomerAccount entity
     * @throws NullFieldException if accountId or activityStatus is null
     * @throws CustomerAccountNotFoundException if account with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount updateActivityStatus(Long accountId, CustomerAccountActivityStatus activityStatus) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");
        DataValidator.checkNotNull(activityStatus, "activityStatus");

        CustomerAccount customerAccount;
        try {
            customerAccount = customerAccountRepository.findById(accountId)
                    .orElseThrow(() -> new CustomerAccountNotFoundException(accountId));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerAccount.setActivityStatus(activityStatus);
            customerAccount.setLastModifiedDate(OffsetDateTime.now());
            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Updates account verification status
     * @param accountId the ID of the account to update
     * @param verificationStatus the new verification status
     * @return the updated CustomerAccount entity
     * @throws NullFieldException if accountId or verificationStatus is null
     * @throws CustomerAccountNotFoundException if account with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount updateVerificationStatus(Long accountId, CustomerAccountVerificationStatus verificationStatus) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");
        DataValidator.checkNotNull(verificationStatus, "verificationStatus");

        CustomerAccount customerAccount;
        try {
            customerAccount = customerAccountRepository.findById(accountId)
                    .orElseThrow(() -> new CustomerAccountNotFoundException(accountId));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerAccount.setVerificationStatus(verificationStatus);
            customerAccount.setLastModifiedDate(OffsetDateTime.now());
            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Updates last login time to current time
     * @param accountId the ID of the account to update
     * @return the updated CustomerAccount entity
     * @throws NullFieldException if accountId is null
     * @throws CustomerAccountNotFoundException if account with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount updateLastLoginTime(Long accountId) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");

        CustomerAccount customerAccount;
        try {
            customerAccount = customerAccountRepository.findById(accountId)
                    .orElseThrow(() -> new CustomerAccountNotFoundException(accountId));
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerAccount.setLastLoginAt(OffsetDateTime.now());
            customerAccount.setLastModifiedDate(OffsetDateTime.now());
            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Saves an existing account entity
     * @param customerAccount the account entity to save
     * @return the saved CustomerAccount entity
     * @throws NullFieldException if customerAccount is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerAccount saveAccount(CustomerAccount customerAccount) throws ServiceException {
        DataValidator.checkNotNull(customerAccount, "customerAccount");

        try {
            customerAccount.setLastModifiedDate(OffsetDateTime.now());
            return customerAccountRepository.save(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes an account by ID
     * @param accountId the ID of the account to delete
     * @throws NullFieldException if accountId is null
     * @throws CustomerAccountNotFoundException if account with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteById(Long accountId) throws ServiceException {
        DataValidator.checkNotNull(accountId, "accountId");

        try {
            if (!customerAccountRepository.existsById(accountId)) {
                throw new CustomerAccountNotFoundException(accountId);
            }
        } catch (CustomerAccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerAccountRepository.deleteById(accountId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes an account entity
     * @param customerAccount the account entity to delete
     * @throws NullFieldException if customerAccount is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteAccount(CustomerAccount customerAccount) throws ServiceException {
        DataValidator.checkNotNull(customerAccount, "customerAccount");

        try {
            customerAccountRepository.delete(customerAccount);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UTILITY METHODS =====

    /**
     * Verifies a raw password against the stored hashed password
     * @param rawPassword the raw password to verify
     * @param hashedPassword the stored hashed password
     * @return true if passwords match, false otherwise
     * @throws NullFieldException if rawPassword or hashedPassword is null
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        DataValidator.checkNotNull(rawPassword, "rawPassword");
        DataValidator.checkNotNull(hashedPassword, "hashedPassword");

        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
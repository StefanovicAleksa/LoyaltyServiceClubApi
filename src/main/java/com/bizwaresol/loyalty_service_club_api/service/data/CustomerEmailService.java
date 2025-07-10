package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerEmailRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.EmailNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicateEmailException;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.BusinessEmailNotAllowedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerEmailService {

    private final CustomerEmailRepository customerEmailRepository;

    public CustomerEmailService(CustomerEmailRepository customerEmailRepository) {
        this.customerEmailRepository = customerEmailRepository;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new customer email
     * @param email the email address to create
     * @return the created CustomerEmail entity
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws FieldTooShortException if email is too short
     * @throws FieldTooLongException if email is too long
     * @throws InvalidEmailFormatException if email format is invalid
     * @throws BusinessEmailNotAllowedException if email domain is not allowed
     * @throws DuplicateEmailException if email already exists
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerEmail createEmail(String email) throws ServiceException {
        DataValidator.validatePersonalEmail(email, "email");

        try {
            CustomerEmail customerEmail = new CustomerEmail();
            customerEmail.setEmail(email.trim().toLowerCase());
            customerEmail.setVerified(false);
            customerEmail.setCreatedDate(OffsetDateTime.now());
            customerEmail.setLastModifiedDate(OffsetDateTime.now());

            return customerEmailRepository.save(customerEmail);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== READ OPERATIONS =====

    /**
     * Retrieves all customer emails
     * @return list of all CustomerEmail entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerEmail> getAllEmails() throws ServiceException {
        try {
            return customerEmailRepository.findAll();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an email by ID
     * @param emailId the ID to search for
     * @return the CustomerEmail entity
     * @throws NullFieldException if emailId is null
     * @throws EmailNotFoundException if email doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerEmail findById(Long emailId) throws ServiceException {
        DataValidator.checkNotNull(emailId, "emailId");

        try {
            return customerEmailRepository.findById(emailId)
                    .orElseThrow(() -> new EmailNotFoundException(emailId));
        } catch (EmailNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds an email by email address
     * @param email the email address to search for
     * @return the CustomerEmail entity
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws FieldTooShortException if email is too short
     * @throws FieldTooLongException if email is too long
     * @throws InvalidEmailFormatException if email format is invalid
     * @throws EmailNotFoundException if email doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerEmail findByEmail(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        try {
            return customerEmailRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new EmailNotFoundException(email));
        } catch (EmailNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves all verified customer emails
     * @return list of verified CustomerEmail entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerEmail> getVerifiedEmails() throws ServiceException {
        try {
            return customerEmailRepository.findByVerified(true);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves all unverified customer emails
     * @return list of unverified CustomerEmail entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerEmail> getUnverifiedEmails() throws ServiceException {
        try {
            return customerEmailRepository.findByVerified(false);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts verified emails
     * @return count of verified emails
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countVerifiedEmails() throws ServiceException {
        try {
            return customerEmailRepository.countByVerified(true);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts unverified emails
     * @return count of unverified emails
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countUnverifiedEmails() throws ServiceException {
        try {
            return customerEmailRepository.countByVerified(false);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts all emails
     * @return total number of emails
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countAllEmails() throws ServiceException {
        try {
            return customerEmailRepository.count();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if an email exists by ID
     * @param emailId the email ID to check
     * @return true if email exists, false otherwise
     * @throws NullFieldException if emailId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long emailId) throws ServiceException {
        DataValidator.checkNotNull(emailId, "emailId");

        try {
            return customerEmailRepository.existsById(emailId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if an email already exists
     * @param email the email address to check
     * @return true if email exists, false otherwise
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws FieldTooShortException if email is too short
     * @throws FieldTooLongException if email is too long
     * @throws InvalidEmailFormatException if email format is invalid
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        try {
            return customerEmailRepository.existsByEmail(email.trim().toLowerCase());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Updates an email address by ID
     * @param emailId the ID of the email to update
     * @param newEmail the new email address
     * @return the updated CustomerEmail entity
     * @throws NullFieldException if emailId is null or newEmail is null
     * @throws EmptyFieldException if newEmail is empty
     * @throws FieldTooShortException if newEmail is too short
     * @throws FieldTooLongException if newEmail is too long
     * @throws InvalidEmailFormatException if newEmail format is invalid
     * @throws BusinessEmailNotAllowedException if newEmail domain is not allowed
     * @throws EmailNotFoundException if email with given ID doesn't exist
     * @throws DuplicateEmailException if new email already exists
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerEmail updateEmail(Long emailId, String newEmail) throws ServiceException {
        DataValidator.checkNotNull(emailId, "emailId");
        DataValidator.validatePersonalEmail(newEmail, "newEmail");

        CustomerEmail customerEmail;
        try {
            customerEmail = customerEmailRepository.findById(emailId)
                    .orElseThrow(() -> new EmailNotFoundException(emailId));
        } catch (EmailNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerEmail.setEmail(newEmail.trim().toLowerCase());
            customerEmail.setVerified(false);
            customerEmail.setLastModifiedDate(OffsetDateTime.now());
            return customerEmailRepository.save(customerEmail);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Changes the verification status of an email
     * @param emailId the ID of the email to update
     * @param isVerified the new verification status
     * @return the updated CustomerEmail entity
     * @throws NullFieldException if emailId is null
     * @throws EmailNotFoundException if email with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerEmail changeVerificationStatus(Long emailId, boolean isVerified) throws ServiceException {
        DataValidator.checkNotNull(emailId, "emailId");

        CustomerEmail customerEmail;
        try {
            customerEmail = customerEmailRepository.findById(emailId)
                    .orElseThrow(() -> new EmailNotFoundException(emailId));
        } catch (EmailNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerEmail.setVerified(isVerified);
            customerEmail.setLastModifiedDate(OffsetDateTime.now());
            return customerEmailRepository.save(customerEmail);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Saves an existing email entity
     * @param customerEmail the email entity to save
     * @return the saved CustomerEmail entity
     * @throws NullFieldException if customerEmail is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerEmail saveEmail(CustomerEmail customerEmail) throws ServiceException {
        DataValidator.checkNotNull(customerEmail, "customerEmail");

        try {
            customerEmail.setLastModifiedDate(OffsetDateTime.now());
            return customerEmailRepository.save(customerEmail);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes an email by ID
     * @param emailId the ID of the email to delete
     * @throws NullFieldException if emailId is null
     * @throws EmailNotFoundException if email with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteById(Long emailId) throws ServiceException {
        DataValidator.checkNotNull(emailId, "emailId");

        // Check if email exists before deleting
        try {
            if (!customerEmailRepository.existsById(emailId)) {
                throw new EmailNotFoundException(emailId);
            }
        } catch (EmailNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerEmailRepository.deleteById(emailId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes an email entity
     * @param customerEmail the email entity to delete
     * @throws NullFieldException if customerEmail is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteEmail(CustomerEmail customerEmail) throws ServiceException {
        DataValidator.checkNotNull(customerEmail, "customerEmail");

        try {
            customerEmailRepository.delete(customerEmail);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes an email by email address
     * @param email the email address to delete
     * @throws NullFieldException if email is null
     * @throws EmptyFieldException if email is empty
     * @throws FieldTooShortException if email is too short
     * @throws FieldTooLongException if email is too long
     * @throws InvalidEmailFormatException if email format is invalid
     * @throws EmailNotFoundException if email doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteByEmail(String email) throws ServiceException {
        DataValidator.validateEmail(email, "email");

        CustomerEmail customerEmail;
        try {
            customerEmail = customerEmailRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new EmailNotFoundException(email));
        } catch (EmailNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerEmailRepository.delete(customerEmail);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }
}
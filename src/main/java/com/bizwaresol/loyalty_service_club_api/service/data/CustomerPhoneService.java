package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerPhoneRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.PhoneNotFoundException;
import com.bizwaresol.loyalty_service_club_api.exception.business.duplicate.DuplicatePhoneException;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerPhoneService {

    private final CustomerPhoneRepository customerPhoneRepository;

    public CustomerPhoneService(CustomerPhoneRepository customerPhoneRepository) {
        this.customerPhoneRepository = customerPhoneRepository;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new customer phone
     * @param phone the phone number to create
     * @return the created CustomerPhone entity
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws FieldTooShortException if phone is too short
     * @throws FieldTooLongException if phone is too long
     * @throws InvalidPhoneFormatException if phone format is invalid
     * @throws DuplicatePhoneException if phone already exists
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerPhone createPhone(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            CustomerPhone customerPhone = new CustomerPhone();
            customerPhone.setPhone(phone.trim());
            customerPhone.setVerified(false);
            customerPhone.setCreatedDate(OffsetDateTime.now());
            customerPhone.setLastModifiedDate(OffsetDateTime.now());

            return customerPhoneRepository.save(customerPhone);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== READ OPERATIONS =====

    /**
     * Retrieves all customer phones
     * @return list of all CustomerPhone entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerPhone> getAllPhones() throws ServiceException {
        try {
            return customerPhoneRepository.findAll();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds a phone by ID
     * @param phoneId the ID to search for
     * @return the CustomerPhone entity
     * @throws NullFieldException if phoneId is null
     * @throws PhoneNotFoundException if phone doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerPhone findById(Long phoneId) throws ServiceException {
        DataValidator.checkNotNull(phoneId, "phoneId");

        try {
            return customerPhoneRepository.findById(phoneId)
                    .orElseThrow(() -> new PhoneNotFoundException(phoneId));
        } catch (PhoneNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds a phone by phone number
     * @param phone the phone number to search for
     * @return the CustomerPhone entity
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws FieldTooShortException if phone is too short
     * @throws FieldTooLongException if phone is too long
     * @throws InvalidPhoneFormatException if phone format is invalid
     * @throws PhoneNotFoundException if phone doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public CustomerPhone findByPhone(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            return customerPhoneRepository.findByPhone(phone.trim())
                    .orElseThrow(() -> new PhoneNotFoundException(phone));
        } catch (PhoneNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves all verified customer phones
     * @return list of verified CustomerPhone entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerPhone> getVerifiedPhones() throws ServiceException {
        try {
            return customerPhoneRepository.findByVerified(true);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Retrieves all unverified customer phones
     * @return list of unverified CustomerPhone entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<CustomerPhone> getUnverifiedPhones() throws ServiceException {
        try {
            return customerPhoneRepository.findByVerified(false);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts verified phones
     * @return count of verified phones
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countVerifiedPhones() throws ServiceException {
        try {
            return customerPhoneRepository.countByVerified(true);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts unverified phones
     * @return count of unverified phones
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countUnverifiedPhones() throws ServiceException {
        try {
            return customerPhoneRepository.countByVerified(false);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts all phones
     * @return total number of phones
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countAllPhones() throws ServiceException {
        try {
            return customerPhoneRepository.count();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if a phone exists by ID
     * @param phoneId the phone ID to check
     * @return true if phone exists, false otherwise
     * @throws NullFieldException if phoneId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long phoneId) throws ServiceException {
        DataValidator.checkNotNull(phoneId, "phoneId");

        try {
            return customerPhoneRepository.existsById(phoneId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if a phone already exists
     * @param phone the phone number to check
     * @return true if phone exists, false otherwise
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws FieldTooShortException if phone is too short
     * @throws FieldTooLongException if phone is too long
     * @throws InvalidPhoneFormatException if phone format is invalid
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean phoneExists(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        try {
            return customerPhoneRepository.existsByPhone(phone.trim());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Updates a phone number by ID
     * @param phoneId the ID of the phone to update
     * @param newPhone the new phone number
     * @return the updated CustomerPhone entity
     * @throws NullFieldException if phoneId is null or newPhone is null
     * @throws EmptyFieldException if newPhone is empty
     * @throws FieldTooShortException if newPhone is too short
     * @throws FieldTooLongException if newPhone is too long
     * @throws InvalidPhoneFormatException if newPhone format is invalid
     * @throws PhoneNotFoundException if phone with given ID doesn't exist
     * @throws DuplicatePhoneException if new phone already exists
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerPhone updatePhone(Long phoneId, String newPhone) throws ServiceException {
        DataValidator.checkNotNull(phoneId, "phoneId");
        DataValidator.validatePhone(newPhone, "newPhone");

        CustomerPhone customerPhone;
        try {
            customerPhone = customerPhoneRepository.findById(phoneId)
                    .orElseThrow(() -> new PhoneNotFoundException(phoneId));
        } catch (PhoneNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerPhone.setPhone(newPhone.trim());
            customerPhone.setVerified(false);
            customerPhone.setLastModifiedDate(OffsetDateTime.now());
            return customerPhoneRepository.save(customerPhone);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Changes the verification status of a phone
     * @param phoneId the ID of the phone to update
     * @param isVerified the new verification status
     * @return the updated CustomerPhone entity
     * @throws NullFieldException if phoneId is null
     * @throws PhoneNotFoundException if phone with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerPhone changeVerificationStatus(Long phoneId, boolean isVerified) throws ServiceException {
        DataValidator.checkNotNull(phoneId, "phoneId");

        CustomerPhone customerPhone;
        try {
            customerPhone = customerPhoneRepository.findById(phoneId)
                    .orElseThrow(() -> new PhoneNotFoundException(phoneId));
        } catch (PhoneNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerPhone.setVerified(isVerified);
            customerPhone.setLastModifiedDate(OffsetDateTime.now());
            return customerPhoneRepository.save(customerPhone);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Saves an existing phone entity
     * @param customerPhone the phone entity to save
     * @return the saved CustomerPhone entity
     * @throws NullFieldException if customerPhone is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public CustomerPhone savePhone(CustomerPhone customerPhone) throws ServiceException {
        DataValidator.checkNotNull(customerPhone, "customerPhone");

        try {
            customerPhone.setLastModifiedDate(OffsetDateTime.now());
            return customerPhoneRepository.save(customerPhone);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes a phone by ID
     * @param phoneId the ID of the phone to delete
     * @throws NullFieldException if phoneId is null
     * @throws PhoneNotFoundException if phone with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteById(Long phoneId) throws ServiceException {
        DataValidator.checkNotNull(phoneId, "phoneId");

        // Check if phone exists before deleting
        try {
            if (!customerPhoneRepository.existsById(phoneId)) {
                throw new PhoneNotFoundException(phoneId);
            }
        } catch (PhoneNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerPhoneRepository.deleteById(phoneId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes a phone entity
     * @param customerPhone the phone entity to delete
     * @throws NullFieldException if customerPhone is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deletePhone(CustomerPhone customerPhone) throws ServiceException {
        DataValidator.checkNotNull(customerPhone, "customerPhone");

        try {
            customerPhoneRepository.delete(customerPhone);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes a phone by phone number
     * @param phone the phone number to delete
     * @throws NullFieldException if phone is null
     * @throws EmptyFieldException if phone is empty
     * @throws FieldTooShortException if phone is too short
     * @throws FieldTooLongException if phone is too long
     * @throws InvalidPhoneFormatException if phone format is invalid
     * @throws PhoneNotFoundException if phone doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteByPhone(String phone) throws ServiceException {
        DataValidator.validatePhone(phone, "phone");

        CustomerPhone customerPhone;
        try {
            customerPhone = customerPhoneRepository.findByPhone(phone.trim())
                    .orElseThrow(() -> new PhoneNotFoundException(phone));
        } catch (PhoneNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerPhoneRepository.delete(customerPhone);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }
}
package com.bizwaresol.loyalty_service_club_api.service.data;

import com.bizwaresol.loyalty_service_club_api.data.repository.CustomerRepository;
import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerNotFoundException;
import com.bizwaresol.loyalty_service_club_api.util.validators.DataValidator;
import com.bizwaresol.loyalty_service_club_api.util.mappers.RepositoryErrorMapper;

// JavaDoc exception imports
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooShortException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.InvalidCharacterException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // ===== CREATE OPERATIONS =====

    /**
     * Creates a new customer
     * @param firstName the customer's first name
     * @param lastName the customer's last name
     * @param customerEmail the customer's email entity (optional)
     * @param customerPhone the customer's phone entity (optional)
     * @return the created Customer entity
     * @throws NullFieldException if firstName or lastName is null
     * @throws EmptyFieldException if firstName or lastName is empty
     * @throws FieldTooShortException if firstName or lastName is too short
     * @throws FieldTooLongException if firstName or lastName is too long
     * @throws InvalidCharacterException if firstName or lastName contains invalid characters
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public Customer createCustomer(String firstName, String lastName, CustomerEmail customerEmail, CustomerPhone customerPhone) throws ServiceException {
        DataValidator.validateName(firstName, "firstName");
        DataValidator.validateName(lastName, "lastName");

        try {
            Customer customer = new Customer();
            customer.setFirstName(firstName.trim());
            customer.setLastName(lastName.trim());
            customer.setEmail(customerEmail);
            customer.setPhone(customerPhone);
            customer.setCreatedDate(OffsetDateTime.now());
            customer.setLastModifiedDate(OffsetDateTime.now());

            return customerRepository.save(customer);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Creates a customer with just names (no contact info)
     * @param firstName the customer's first name
     * @param lastName the customer's last name
     * @return the created Customer entity
     * @throws NullFieldException if firstName or lastName is null
     * @throws EmptyFieldException if firstName or lastName is empty
     * @throws FieldTooShortException if firstName or lastName is too short
     * @throws FieldTooLongException if firstName or lastName is too long
     * @throws InvalidCharacterException if firstName or lastName contains invalid characters
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public Customer createCustomer(String firstName, String lastName) throws ServiceException {
        return createCustomer(firstName, lastName, null, null);
    }

    // ===== READ OPERATIONS =====

    /**
     * Retrieves all customers
     * @return list of all Customer entities
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() throws ServiceException {
        try {
            return customerRepository.findAll();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds a customer by ID
     * @param customerId the ID to search for
     * @return the Customer entity
     * @throws NullFieldException if customerId is null
     * @throws CustomerNotFoundException if customer doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Customer findById(Long customerId) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");

        try {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds customers by first and last name (case-insensitive)
     * @param firstName the first name to search for
     * @param lastName the last name to search for
     * @return list of matching Customer entities
     * @throws NullFieldException if firstName or lastName is null
     * @throws EmptyFieldException if firstName or lastName is empty
     * @throws FieldTooShortException if firstName or lastName is too short
     * @throws FieldTooLongException if firstName or lastName is too long
     * @throws InvalidCharacterException if firstName or lastName contains invalid characters
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<Customer> findByFirstNameAndLastName(String firstName, String lastName) throws ServiceException {
        DataValidator.validateName(firstName, "firstName");
        DataValidator.validateName(lastName, "lastName");

        try {
            return customerRepository.findByFirstNameAndLastNameIgnoreCase(firstName.trim(), lastName.trim());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds customers by first name (case-insensitive)
     * @param firstName the first name to search for
     * @return list of matching Customer entities
     * @throws NullFieldException if firstName is null
     * @throws EmptyFieldException if firstName is empty
     * @throws FieldTooShortException if firstName is too short
     * @throws FieldTooLongException if firstName is too long
     * @throws InvalidCharacterException if firstName contains invalid characters
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<Customer> findByFirstName(String firstName) throws ServiceException {
        DataValidator.validateName(firstName, "firstName");

        try {
            return customerRepository.findByFirstNameIgnoreCase(firstName.trim());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds customers by last name (case-insensitive)
     * @param lastName the last name to search for
     * @return list of matching Customer entities
     * @throws NullFieldException if lastName is null
     * @throws EmptyFieldException if lastName is empty
     * @throws FieldTooShortException if lastName is too short
     * @throws FieldTooLongException if lastName is too long
     * @throws InvalidCharacterException if lastName contains invalid characters
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public List<Customer> findByLastName(String lastName) throws ServiceException {
        DataValidator.validateName(lastName, "lastName");

        try {
            return customerRepository.findByLastNameIgnoreCase(lastName.trim());
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds customer by email ID
     * @param emailId the email ID to search for
     * @return the Customer entity
     * @throws NullFieldException if emailId is null
     * @throws CustomerNotFoundException if customer doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Customer findByEmailId(Long emailId) throws ServiceException {
        DataValidator.checkNotNull(emailId, "emailId");

        try {
            return customerRepository.findByEmailId(emailId)
                    .orElseThrow(() -> new CustomerNotFoundException("No customer found with email ID: " + emailId));
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Finds customer by phone ID
     * @param phoneId the phone ID to search for
     * @return the Customer entity
     * @throws NullFieldException if phoneId is null
     * @throws CustomerNotFoundException if customer doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public Customer findByPhoneId(Long phoneId) throws ServiceException {
        DataValidator.checkNotNull(phoneId, "phoneId");

        try {
            return customerRepository.findByPhoneId(phoneId)
                    .orElseThrow(() -> new CustomerNotFoundException("No customer found with phone ID: " + phoneId));
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Checks if a customer exists by ID
     * @param customerId the customer ID to check
     * @return true if customer exists, false otherwise
     * @throws NullFieldException if customerId is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public boolean customerExists(Long customerId) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");

        try {
            return customerRepository.existsById(customerId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Counts all customers
     * @return total number of customers
     * @throws ServiceException if repository operation fails
     */
    @Transactional(readOnly = true)
    public long countAllCustomers() throws ServiceException {
        try {
            return customerRepository.count();
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Updates customer names
     * @param customerId the ID of the customer to update
     * @param firstName the new first name
     * @param lastName the new last name
     * @return the updated Customer entity
     * @throws NullFieldException if customerId, firstName, or lastName is null
     * @throws EmptyFieldException if firstName or lastName is empty
     * @throws FieldTooShortException if firstName or lastName is too short
     * @throws FieldTooLongException if firstName or lastName is too long
     * @throws InvalidCharacterException if firstName or lastName contains invalid characters
     * @throws CustomerNotFoundException if customer with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public Customer updateCustomerNames(Long customerId, String firstName, String lastName) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");
        DataValidator.validateName(firstName, "firstName");
        DataValidator.validateName(lastName, "lastName");

        Customer customer;
        try {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customer.setFirstName(firstName.trim());
            customer.setLastName(lastName.trim());
            customer.setLastModifiedDate(OffsetDateTime.now());
            return customerRepository.save(customer);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Updates customer email association
     * @param customerId the ID of the customer to update
     * @param customerEmail the new email entity (can be null to remove email)
     * @return the updated Customer entity
     * @throws NullFieldException if customerId is null
     * @throws CustomerNotFoundException if customer with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public Customer updateCustomerEmail(Long customerId, CustomerEmail customerEmail) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");

        Customer customer;
        try {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customer.setEmail(customerEmail);
            customer.setLastModifiedDate(OffsetDateTime.now());
            return customerRepository.save(customer);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Updates customer phone association
     * @param customerId the ID of the customer to update
     * @param customerPhone the new phone entity (can be null to remove phone)
     * @return the updated Customer entity
     * @throws NullFieldException if customerId is null
     * @throws CustomerNotFoundException if customer with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public Customer updateCustomerPhone(Long customerId, CustomerPhone customerPhone) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");

        Customer customer;
        try {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customer.setPhone(customerPhone);
            customer.setLastModifiedDate(OffsetDateTime.now());
            return customerRepository.save(customer);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Saves an existing customer entity
     * @param customer the customer entity to save
     * @return the saved Customer entity
     * @throws NullFieldException if customer is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public Customer saveCustomer(Customer customer) throws ServiceException {
        DataValidator.checkNotNull(customer, "customer");

        try {
            customer.setLastModifiedDate(OffsetDateTime.now());
            return customerRepository.save(customer);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Deletes a customer by ID
     * @param customerId the ID of the customer to delete
     * @throws NullFieldException if customerId is null
     * @throws CustomerNotFoundException if customer with given ID doesn't exist
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteById(Long customerId) throws ServiceException {
        DataValidator.checkNotNull(customerId, "customerId");

        try {
            if (!customerRepository.existsById(customerId)) {
                throw new CustomerNotFoundException(customerId);
            }
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }

        try {
            customerRepository.deleteById(customerId);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }

    /**
     * Deletes a customer entity
     * @param customer the customer entity to delete
     * @throws NullFieldException if customer is null
     * @throws ServiceException if repository operation fails
     */
    @Transactional
    public void deleteCustomer(Customer customer) throws ServiceException {
        DataValidator.checkNotNull(customer, "customer");

        try {
            customerRepository.delete(customer);
        } catch (Exception e) {
            throw RepositoryErrorMapper.mapException(e);
        }
    }
}
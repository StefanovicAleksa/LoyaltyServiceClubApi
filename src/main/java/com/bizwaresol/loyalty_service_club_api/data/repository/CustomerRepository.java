package com.bizwaresol.loyalty_service_club_api.data.repository;

import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByFirstNameAndLastNameIgnoreCase(String firstName, String lastName);
    List<Customer> findByFirstNameIgnoreCase(String firstName);
    List<Customer> findByLastNameIgnoreCase(String lastName);

    // fk lookup methods
    Optional<Customer> findByEmailId(Long emailId);
    Optional<Customer> findByPhoneId(Long phoneId);
}

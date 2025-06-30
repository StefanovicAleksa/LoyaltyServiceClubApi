package com.bizwaresol.loyalty_service_club_api.data.repositories;

import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<CustomerAccount> findByActivityStatus(CustomerAccountActivityStatus activityStatus);

    List<CustomerAccount> findByVerificationStatus(CustomerAccountVerificationStatus verificationStatus);

    List<CustomerAccount> findByActivityStatusAndVerificationStatus(CustomerAccountActivityStatus activityStatus, CustomerAccountVerificationStatus verificationStatus);

    List<CustomerAccount> findByLastLoginAtBefore(OffsetDateTime date);

    List<CustomerAccount> findByLastLoginAtIsNull();

    //fk lookup

    Optional<CustomerAccount> findByCustomerId(Long customerId);
}

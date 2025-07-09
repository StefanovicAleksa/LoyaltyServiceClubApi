package com.bizwaresol.loyalty_service_club_api.data.repository;

import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerPhone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPhoneRepository extends JpaRepository<CustomerPhone, Long> {
    Optional<CustomerPhone> findByPhone(String phoneNumber);

    boolean existsByPhone(String phoneNumber);

    List<CustomerPhone> findByVerified(boolean isVerified);

    long countByVerified(boolean isVerified);
}

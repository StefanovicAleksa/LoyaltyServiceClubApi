package com.bizwaresol.loyalty_service_club_api.data.repository;

import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerEmail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerEmailRepository extends JpaRepository<CustomerEmail, Long> {

    Optional<CustomerEmail> findByEmail(String email);

    boolean existsByEmail(String email);

    List<CustomerEmail> findByVerified(boolean isVerified);

    long countByVerified(boolean isVerified);
}

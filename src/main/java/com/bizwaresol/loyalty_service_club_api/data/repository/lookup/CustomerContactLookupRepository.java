package com.bizwaresol.loyalty_service_club_api.data.repository.lookup;

import com.bizwaresol.loyalty_service_club_api.domain.entity.lookup.CustomerContactLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerContactLookupRepository extends JpaRepository<CustomerContactLookup, Long> {

    Optional<CustomerContactLookup> findByPhone(String phone);

    Optional<CustomerContactLookup> findByEmail(String email);

    Optional<CustomerContactLookup> findByPreferredUsername(String preferredUsername);
}
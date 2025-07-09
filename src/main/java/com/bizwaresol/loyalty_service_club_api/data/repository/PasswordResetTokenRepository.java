package com.bizwaresol.loyalty_service_club_api.data.repository;

import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    boolean existsByToken(String token);

    // cleanup
    List<PasswordResetToken> findByExpiresAtBefore(OffsetDateTime date);

    void deleteByExpiresAtBefore(OffsetDateTime date);

    //lookup for active and unused account tokens
    List<PasswordResetToken> findByCustomerAccountIdAndUsedFalseAndExpiresAtAfter(Long customerAccountId, OffsetDateTime date);
}

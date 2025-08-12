package com.bizwaresol.loyalty_service_club_api.data.repository;

import com.bizwaresol.loyalty_service_club_api.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository for managing {@link PasswordResetToken} entities.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a password reset token by its unique token string.
     * This is the primary method for validating a token provided by a user.
     *
     * @param token The unique token string (UUID).
     * @return An Optional containing the {@link PasswordResetToken} if found.
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Finds an active (unused) password reset token for a specific customer account.
     * This is used to enforce the rule that a user can only have one active reset request at a time.
     *
     * @param customerAccountId The ID of the customer account.
     * @return An Optional containing the active {@link PasswordResetToken} if one exists.
     */
    Optional<PasswordResetToken> findByCustomerAccountIdAndUsedAtIsNull(Long customerAccountId);

    /**
     * Marks a specific token as used by setting its `used_at` timestamp.
     * This is a critical step to ensure a token cannot be reused.
     *
     * @param tokenId The ID of the token to mark as used.
     * @param usedAt  The timestamp to set as the used time.
     * @return The number of rows affected (should be 1 if successful).
     */
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.usedAt = :usedAt, prt.lastModifiedDate = :usedAt WHERE prt.id = :tokenId")
    int markAsUsed(@Param("tokenId") Long tokenId, @Param("usedAt") OffsetDateTime usedAt);
}

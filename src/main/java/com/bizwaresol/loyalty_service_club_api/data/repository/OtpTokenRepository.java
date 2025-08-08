package com.bizwaresol.loyalty_service_club_api.data.repository;

import com.bizwaresol.loyalty_service_club_api.domain.entity.OtpToken;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // ===== EMAIL VERIFICATION =====

    /**
     * Find valid email verification OTP by code and email
     */
    Optional<OtpToken> findByOtpCodeAndCustomerEmailEmailAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
            String otpCode, String email, OtpPurpose purpose, OffsetDateTime currentTime, Integer maxAttempts);

    /**
     * Find latest email verification OTP by email (for rate limiting)
     */
    Optional<OtpToken> findTopByCustomerEmailEmailAndPurposeOrderByCreatedDateDesc(String email, OtpPurpose purpose);

    /**
     * Count email verification OTPs in time window (for rate limiting)
     */
    long countByCustomerEmailEmailAndPurposeAndCreatedDateAfter(String email, OtpPurpose purpose, OffsetDateTime since);

    /**
     * Find active email verification OTPs to invalidate
     */
    List<OtpToken> findByCustomerEmailEmailAndPurposeAndUsedAtIsNull(String email, OtpPurpose purpose);

    // ===== PHONE VERIFICATION =====

    /**
     * Find valid phone verification OTP by code and phone
     */
    Optional<OtpToken> findByOtpCodeAndCustomerPhonePhoneAndPurposeAndUsedAtIsNullAndExpiresAtAfterAndAttemptsCountLessThan(
            String otpCode, String phone, OtpPurpose purpose, OffsetDateTime currentTime, Integer maxAttempts);

    /**
     * Find latest phone verification OTP by phone (for rate limiting)
     */
    Optional<OtpToken> findTopByCustomerPhonePhoneAndPurposeOrderByCreatedDateDesc(String phone, OtpPurpose purpose);

    /**
     * Count phone verification OTPs in time window (for rate limiting)
     */
    long countByCustomerPhonePhoneAndPurposeAndCreatedDateAfter(String phone, OtpPurpose purpose, OffsetDateTime since);

    /**
     * Find active phone verification OTPs to invalidate
     */
    List<OtpToken> findByCustomerPhonePhoneAndPurposeAndUsedAtIsNull(String phone, OtpPurpose purpose);

    // ===== SHARED OPERATIONS (These need custom queries for updates) =====

    /**
     * Increment attempt count for an OTP
     */
    @Modifying
    @Query("UPDATE OtpToken o SET o.attemptsCount = o.attemptsCount + 1, " +
            "o.lastModifiedDate = :currentTime WHERE o.id = :otpId")
    int incrementAttemptCount(@Param("otpId") Long otpId,
                              @Param("currentTime") OffsetDateTime currentTime);

    /**
     * Mark OTP as used
     */
    @Modifying
    @Query("UPDATE OtpToken o SET o.usedAt = :currentTime, o.lastModifiedDate = :currentTime " +
            "WHERE o.id = :otpId")
    int markOtpAsUsed(@Param("otpId") Long otpId,
                      @Param("currentTime") OffsetDateTime currentTime);

    /**
     * Mark multiple OTPs as used (for invalidation)
     */
    @Modifying
    @Query("UPDATE OtpToken o SET o.usedAt = :currentTime, o.lastModifiedDate = :currentTime " +
            "WHERE o IN :otpTokens")
    int markOtpsAsUsed(@Param("otpTokens") List<OtpToken> otpTokens,
                       @Param("currentTime") OffsetDateTime currentTime);
}
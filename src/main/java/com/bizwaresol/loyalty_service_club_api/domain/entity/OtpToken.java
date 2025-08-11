package com.bizwaresol.loyalty_service_club_api.domain.entity;

import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpDeliveryMethod;
import com.bizwaresol.loyalty_service_club_api.domain.enums.OtpPurpose;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "otp_tokens")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_email_id")
    private CustomerEmail customerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_phone_id")
    private CustomerPhone customerPhone;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "purpose", nullable = false)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "delivery_method", nullable = false)
    private OtpDeliveryMethod deliveryMethod;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private OffsetDateTime lastModifiedDate;

    public OtpToken() {}

    // ===== GETTERS AND SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerEmail getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(CustomerEmail customerEmail) {
        this.customerEmail = customerEmail;
    }

    public CustomerPhone getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(CustomerPhone customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public OtpDeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(OtpDeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public Integer getAttemptsCount() {
        return attemptsCount;
    }

    public void setAttemptsCount(Integer attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public OffsetDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    // ===== BUSINESS METHODS =====

    /**
     * Checks if the OTP token is currently valid (not used and not expired)
     * @return true if token is valid for use
     */
    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    /**
     * Checks if the OTP token has been used
     * @return true if token has been used
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Checks if the OTP token has expired
     * @return true if token has expired
     */
    public boolean isExpired() {
        return expiresAt.isBefore(OffsetDateTime.now());
    }

    /**
     * Checks if the maximum number of attempts has been reached
     * @return true if max attempts reached
     */
    public boolean hasReachedMaxAttempts() {
        return attemptsCount >= maxAttempts;
    }

    /**
     * Increments the attempts count
     */
    public void incrementAttempts() {
        this.attemptsCount++;
    }

    /**
     * Marks the token as used with current timestamp
     */
    public void markAsUsed() {
        this.usedAt = OffsetDateTime.now();
    }

    /**
     * Gets the contact identifier (email or phone) for this OTP
     * @return email address or phone number
     */
    public String getContactIdentifier() {
        if (customerEmail != null) {
            return customerEmail.getEmail();
        } else if (customerPhone != null) {
            return customerPhone.getPhone();
        }
        return null;
    }

    /**
     * Checks if this is an email-based OTP
     * @return true if delivered via email
     */
    public boolean isEmailOtp() {
        return deliveryMethod == OtpDeliveryMethod.EMAIL && customerEmail != null;
    }

    /**
     * Checks if this is a phone-based OTP
     * @return true if delivered via SMS
     */
    public boolean isPhoneOtp() {
        return deliveryMethod == OtpDeliveryMethod.SMS && customerPhone != null;
    }

    // ===== EQUALS, HASHCODE, TOSTRING =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OtpToken otpToken)) return false;
        return Objects.equals(id, otpToken.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "OtpToken{" +
                "id=" + id +
                ", purpose=" + purpose +
                ", deliveryMethod=" + deliveryMethod +
                ", expiresAt=" + expiresAt +
                ", usedAt=" + usedAt +
                ", attemptsCount=" + attemptsCount +
                ", maxAttempts=" + maxAttempts +
                ", contactIdentifier='" + getContactIdentifier() + "'" +
                '}';
    }

    /**
     * Generates a random 6-digit OTP code
     * @return a 6-digit numeric string (e.g., "123456", "000789")
     */
    public static String generateOtpCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
    }
}
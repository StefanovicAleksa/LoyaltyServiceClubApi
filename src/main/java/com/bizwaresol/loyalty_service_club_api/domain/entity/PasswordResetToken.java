package com.bizwaresol.loyalty_service_club_api.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single-use, short-lived token for authorizing a password reset.
 * This token is generated after a user has successfully verified their identity (e.g., via OTP)
 * and is required for the final step of changing their password.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique, high-entropy token string (UUID). This is what is sent to the client.
     */
    @Column(name = "token", nullable = false, unique = true, length = 36)
    private String token;

    /**
     * The customer account this token belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private CustomerAccount customerAccount;

    /**
     * The timestamp when this token becomes invalid.
     */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * The timestamp when this token was successfully used. A null value indicates the token is still active.
     */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /**
     * The timestamp when this entity was created. Managed by a database trigger.
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    /**
     * The timestamp when this entity was last modified. Managed by a database trigger.
     */
    @Column(name = "last_modified_date", nullable = false)
    private OffsetDateTime lastModifiedDate;

    public PasswordResetToken() {
    }

    // ===== GETTERS AND SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
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

    // ===== BUSINESS LOGIC METHODS =====

    /**
     * Checks if the token has been consumed.
     * @return true if the token has been used, false otherwise.
     */
    public boolean isUsed() {
        return this.usedAt != null;
    }

    /**
     * Checks if the token has passed its expiration date.
     * @return true if the token is expired, false otherwise.
     */
    public boolean isExpired() {
        return this.expiresAt.isBefore(OffsetDateTime.now());
    }

    /**
     * Marks the token as used with the current timestamp.
     */
    public void markAsUsed() {
        this.usedAt = OffsetDateTime.now();
    }


    // ===== FACTORY METHOD =====

    /**
     * Generates a new, secure, unique token string.
     * @return A unique token string (UUID).
     */
    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    // ===== EQUALS, HASHCODE, TOSTRING =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordResetToken that = (PasswordResetToken) o;
        return Objects.equals(id, that.id) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token);
    }

    @Override
    public String toString() {
        return "PasswordResetToken{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", customerAccountId=" + (customerAccount != null ? customerAccount.getId() : "null") +
                ", expiresAt=" + expiresAt +
                ", usedAt=" + usedAt +
                '}';
    }
}

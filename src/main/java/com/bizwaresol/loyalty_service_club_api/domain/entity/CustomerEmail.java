package com.bizwaresol.loyalty_service_club_api.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "customer_emails")
public class CustomerEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;  // ← Changed from 'isVerified' to 'verified'

    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private OffsetDateTime lastModifiedDate;

    public CustomerEmail() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerified() {  // ← Keep 'isVerified()' for getter (boolean convention)
        return verified;
    }

    public void setVerified(boolean verified) {  // ← Now matches the property name
        this.verified = verified;
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

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if (!(o instanceof CustomerEmail that)) return false;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }

    @Override
    public String toString() {
        return "CustomerEmail{" +
                ", id=" + id +
                ", email='" + email + '\'' +
                "verified=" + verified +  // ← Updated field reference
                '}';
    }
}
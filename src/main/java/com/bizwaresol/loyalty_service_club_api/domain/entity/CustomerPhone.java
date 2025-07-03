package com.bizwaresol.loyalty_service_club_api.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "customer_phones")
public class CustomerPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, unique = true, length = 13)
    private String phone;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "created_date",  nullable = false,  updatable = false)
    private OffsetDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private OffsetDateTime lastModifiedDate;

    public CustomerPhone() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
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
        if (this == o) return true;
        if (!(o instanceof CustomerPhone that)) return false;
        return Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(phone);
    }

    @Override
    public String toString() {
        return "CustomerPhone{" +
                "id=" + id +
                ", phone='" + phone + '\'' +
                ", isVerified=" + isVerified +
                '}';
    }
}

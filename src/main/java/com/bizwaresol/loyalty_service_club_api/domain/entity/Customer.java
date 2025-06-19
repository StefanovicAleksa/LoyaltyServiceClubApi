package com.bizwaresol.loyalty_service_club_api.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name",  nullable = false, length = 20)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", unique = true)
    private CustomerEmail email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", unique = true)
    private CustomerPhone phone;

    @OneToOne(mappedBy = "customer")
    private CustomerAccount account;

    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private OffsetDateTime lastModifiedDate;

    public Customer() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void setFullName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public CustomerEmail getEmail() {
        return email;
    }

    public void setEmail(CustomerEmail email) {
        this.email = email;
    }

    public CustomerPhone getPhone() {
        return phone;
    }

    public void setPhone(CustomerPhone phone) {
        this.phone = phone;
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
        if (!(o instanceof Customer customer)) return false;
        return this.id != null && Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return this.id != null ? Objects.hashCode(id) : 0;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email=" + email +
                ", phone=" + phone +
                '}';
    }
}

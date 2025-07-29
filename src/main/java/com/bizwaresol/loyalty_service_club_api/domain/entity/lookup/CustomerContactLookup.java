package com.bizwaresol.loyalty_service_club_api.domain.entity.lookup;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "customer_contact_lookup")
public class CustomerContactLookup {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "email_id")
    private Long emailId;

    @Column(name = "phone_id")
    private Long phoneId;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "phone", length = 13)
    private String phone;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "preferred_username")
    private String preferredUsername;

    @Column(name = "has_email")
    private Boolean hasEmail;

    @Column(name = "has_phone")
    private Boolean hasPhone;

    @Column(name = "contact_type", length = 10)
    private String contactType;

    public CustomerContactLookup() {}

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getEmailId() {
        return emailId;
    }

    public void setEmailId(Long emailId) {
        this.emailId = emailId;
    }

    public Long getPhoneId() {
        return phoneId;
    }

    public void setPhoneId(Long phoneId) {
        this.phoneId = phoneId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public Boolean getHasEmail() {
        return hasEmail;
    }

    public void setHasEmail(Boolean hasEmail) {
        this.hasEmail = hasEmail;
    }

    public Boolean getHasPhone() {
        return hasPhone;
    }

    public void setHasPhone(Boolean hasPhone) {
        this.hasPhone = hasPhone;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    @Override
    public String toString() {
        return "CustomerContactLookup{" +
                "customerId=" + customerId +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", preferredUsername='" + preferredUsername + '\'' +
                ", contactType='" + contactType + '\'' +
                '}';
    }
}
package com.bizwaresol.loyalty_service_club_api.security.auth;

import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountVerificationStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails implementation for CustomerAccount
 */
public class CustomerUserDetails implements UserDetails {

    private final CustomerAccount customerAccount;

    public CustomerUserDetails(CustomerAccount customerAccount) {
        this.customerAccount = customerAccount;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // For now, all customers have ROLE_CUSTOMER
        // You can expand this later for different customer roles
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @Override
    public String getPassword() {
        return customerAccount.getPassword();
    }

    @Override
    public String getUsername() {
        return customerAccount.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        // Accounts don't expire in your system
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Only suspended accounts are "locked"
        return customerAccount.getActivityStatus() != CustomerAccountActivityStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Credentials don't expire in your system
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Account is enabled if not suspended
        return customerAccount.getActivityStatus() != CustomerAccountActivityStatus.SUSPENDED;
    }

    // ===== CUSTOM GETTERS =====

    /**
     * Get the underlying CustomerAccount entity
     * @return the CustomerAccount
     */
    public CustomerAccount getCustomerAccount() {
        return customerAccount;
    }

    /**
     * Get the customer ID
     * @return customer ID
     */
    public Long getCustomerId() {
        return customerAccount.getCustomer().getId();
    }

    /**
     * Get the account ID
     * @return account ID
     */
    public Long getAccountId() {
        return customerAccount.getId();
    }

    /**
     * Check if account is verified
     * @return true if fully verified
     */
    public boolean isVerified() {
        return customerAccount.getVerificationStatus() == CustomerAccountVerificationStatus.FULLY_VERIFIED;
    }

    /**
     * Get verification status
     * @return verification status
     */
    public CustomerAccountVerificationStatus getVerificationStatus() {
        return customerAccount.getVerificationStatus();
    }

    /**
     * Get activity status
     * @return activity status
     */
    public CustomerAccountActivityStatus getActivityStatus() {
        return customerAccount.getActivityStatus();
    }
}
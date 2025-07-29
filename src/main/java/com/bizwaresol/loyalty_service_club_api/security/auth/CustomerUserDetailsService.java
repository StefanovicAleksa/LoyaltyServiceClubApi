package com.bizwaresol.loyalty_service_club_api.security.auth;

import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.service.data.CustomerAccountService;
import com.bizwaresol.loyalty_service_club_api.exception.business.resource.CustomerAccountNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService implementation for loading customer accounts
 */
@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerAccountService customerAccountService;

    public CustomerUserDetailsService(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            CustomerAccount customerAccount = customerAccountService.findByUsername(username);
            return new CustomerUserDetails(customerAccount);
        } catch (CustomerAccountNotFoundException e) {
            throw new UsernameNotFoundException("User not found with username: " + username, e);
        } catch (Exception e) {
            throw new UsernameNotFoundException("Error loading user with username: " + username, e);
        }
    }
}
package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.login;

public class AccountSuspendedException extends AuthenticationException {

    public AccountSuspendedException(String identifier) {
        super("Account is suspended and cannot login: " + identifier, "ACCOUNT_SUSPENDED");
    }

    public AccountSuspendedException(Long accountId) {
        super("Account is suspended and cannot login (ID: " + accountId + ")", "ACCOUNT_SUSPENDED");
    }

    public AccountSuspendedException(String identifier, Throwable cause) {
        super("Account is suspended and cannot login: " + identifier, "ACCOUNT_SUSPENDED", cause);
    }
}
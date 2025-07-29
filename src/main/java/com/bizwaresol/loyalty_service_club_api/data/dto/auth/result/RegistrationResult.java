package com.bizwaresol.loyalty_service_club_api.data.dto.auth.result;

import com.bizwaresol.loyalty_service_club_api.domain.entity.Customer;
import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;

public record RegistrationResult(
        boolean success,
        CustomerAccount account,
        Customer customer,
        boolean emailProvided,
        boolean phoneProvided,
        boolean rememberMeEnabled,
        String preferredContactMethod
) {

    public static RegistrationResult success(
            CustomerAccount account,
            Customer customer,
            boolean emailProvided,
            boolean phoneProvided,
            boolean rememberMeEnabled
    ) {
        String preferredContact = determinePreferredContact(emailProvided, phoneProvided);

        return new RegistrationResult(
                true,
                account,
                customer,
                emailProvided,
                phoneProvided,
                rememberMeEnabled,
                preferredContact
        );
    }

    public static RegistrationResult failure() {
        return new RegistrationResult(false, null, null, false, false, false, null);
    }

    private static String determinePreferredContact(boolean hasEmail, boolean hasPhone) {
        if (hasEmail && hasPhone) {
            return "both";
        } else if (hasEmail) {
            return "email";
        } else if (hasPhone) {
            return "phone";
        }
        return "none";
    }
}
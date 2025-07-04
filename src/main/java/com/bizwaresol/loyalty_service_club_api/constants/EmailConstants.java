package com.bizwaresol.loyalty_service_club_api.constants;

import java.util.Set;

public final class EmailConstants {
    private EmailConstants() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    public static final Set<String> ALLOWED_PERSONAL_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
            "protonmail.com", "icloud.com", "mail.com"
    );

    public static final String ALLOWED_DOMAINS_MESSAGE = String.join(", ", ALLOWED_PERSONAL_EMAIL_DOMAINS);
}
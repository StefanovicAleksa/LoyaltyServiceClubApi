package com.bizwaresol.loyalty_service_club_api.data.dto.auth.request;

public record RegistrationRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        boolean rememberMe
) {}
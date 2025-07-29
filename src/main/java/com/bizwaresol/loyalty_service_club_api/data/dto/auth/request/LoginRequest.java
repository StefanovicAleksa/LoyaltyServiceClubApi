package com.bizwaresol.loyalty_service_club_api.data.dto.auth.request;

public record LoginRequest(
        String identifier,
        String password,
        boolean rememberMe
) {}
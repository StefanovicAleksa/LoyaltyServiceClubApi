package com.bizwaresol.loyalty_service_club_api.data.dto.auth.result;

import com.bizwaresol.loyalty_service_club_api.domain.entity.CustomerAccount;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountActivityStatus;
import com.bizwaresol.loyalty_service_club_api.domain.enums.CustomerAccountVerificationStatus;

import java.time.OffsetDateTime;

public record LoginResult(
        boolean success,
        CustomerAccount account,
        boolean rememberMeEnabled,
        OffsetDateTime previousLoginAt,
        CustomerAccountActivityStatus activityStatus,
        CustomerAccountVerificationStatus verificationStatus
) {

    public static LoginResult success(
            CustomerAccount account,
            boolean rememberMeEnabled,
            OffsetDateTime previousLoginAt
    ) {
        return new LoginResult(
                true,
                account,
                rememberMeEnabled,
                previousLoginAt,
                account.getActivityStatus(),
                account.getVerificationStatus()
        );
    }

    public static LoginResult failure() {
        return new LoginResult(false, null, false, null, null, null);
    }
}
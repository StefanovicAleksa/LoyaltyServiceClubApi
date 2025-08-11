package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.verification;

import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class OtpVerificationException extends ServiceException {

    public OtpVerificationException(String message, String errorCode) {
        super(message, errorCode, 400);
    }

    public OtpVerificationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 400, cause);
    }
}
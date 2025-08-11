package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending;


import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;

public abstract class OtpSendingException extends ServiceException {

    public OtpSendingException(String message, String errorCode) {
        super(message, errorCode, 429); // 429 Too Many Requests for rate limiting
    }

    public OtpSendingException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, 429, cause);
    }
}
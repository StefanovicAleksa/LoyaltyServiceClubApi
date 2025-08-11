package com.bizwaresol.loyalty_service_club_api.exception.security.auth.customer.otp.sending;

public class OtpDeliveryFailedException extends OtpSendingException {

    public OtpDeliveryFailedException(String contact, String deliveryMethod, String reason) {
        super("Failed to deliver OTP to: " + contact + " via " + deliveryMethod + ". Reason: " + reason, "OTP_DELIVERY_FAILED");
    }

    public OtpDeliveryFailedException(String contact, String deliveryMethod, Throwable cause) {
        super("Failed to deliver OTP to: " + contact + " via " + deliveryMethod, "OTP_DELIVERY_FAILED", cause);
    }

    public OtpDeliveryFailedException(String contact, String deliveryMethod, String reason, Throwable cause) {
        super("Failed to deliver OTP to: " + contact + " via " + deliveryMethod + ". Reason: " + reason, "OTP_DELIVERY_FAILED", cause);
    }
}

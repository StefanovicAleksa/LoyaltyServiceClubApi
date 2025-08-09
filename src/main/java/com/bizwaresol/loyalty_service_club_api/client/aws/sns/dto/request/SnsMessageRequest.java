package com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SnsMessageRequest {

    private String phoneNumber;
    private String message;
    private Map<String, String> messageAttributes;

    public SnsMessageRequest() {
        this.messageAttributes = new HashMap<>();
    }

    public SnsMessageRequest(String phoneNumber, String message) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.messageAttributes = new HashMap<>();
    }

    public SnsMessageRequest(String phoneNumber, String message, Map<String, String> messageAttributes) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.messageAttributes = messageAttributes != null ? new HashMap<>(messageAttributes) : new HashMap<>();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getMessageAttributes() {
        return messageAttributes;
    }

    public void setMessageAttributes(Map<String, String> messageAttributes) {
        this.messageAttributes = messageAttributes != null ? new HashMap<>(messageAttributes) : new HashMap<>();
    }

    // Helper methods for common SMS attributes
    public void setSmsType(String smsType) {
        messageAttributes.put("SMS.SMSType", smsType);
    }

    public void setSenderId(String senderId) {
        messageAttributes.put("SMS.SenderID", senderId);
    }

    public void setMaxPrice(String maxPrice) {
        messageAttributes.put("SMS.MaxPrice", maxPrice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SnsMessageRequest that)) return false;
        return Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(message, that.message) &&
                Objects.equals(messageAttributes, that.messageAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phoneNumber, message, messageAttributes);
    }

    @Override
    public String toString() {
        return "SnsMessageRequest{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", message='" + message + '\'' +
                ", messageAttributes=" + messageAttributes +
                '}';
    }
}
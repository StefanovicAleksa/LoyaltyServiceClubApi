package com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.response;

import java.time.OffsetDateTime;
import java.util.Objects;

public class SesEmailResponse {

    private String messageId;
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private OffsetDateTime timestamp;

    public SesEmailResponse() {}

    public SesEmailResponse(String messageId, boolean success, String errorCode, String errorMessage, OffsetDateTime timestamp) {
        this.messageId = messageId;
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    // Factory methods for easier creation
    public static SesEmailResponse success(String messageId) {
        return new SesEmailResponse(messageId, true, null, null, OffsetDateTime.now());
    }

    public static SesEmailResponse failure(String errorCode, String errorMessage) {
        return new SesEmailResponse(null, false, errorCode, errorMessage, OffsetDateTime.now());
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SesEmailResponse that)) return false;
        return success == that.success &&
                Objects.equals(messageId, that.messageId) &&
                Objects.equals(errorCode, that.errorCode) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, success, errorCode, errorMessage, timestamp);
    }

    @Override
    public String toString() {
        return "SesEmailResponse{" +
                "messageId='" + messageId + '\'' +
                ", success=" + success +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
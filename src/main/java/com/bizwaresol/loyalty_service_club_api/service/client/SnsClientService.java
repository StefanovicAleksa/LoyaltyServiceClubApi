package com.bizwaresol.loyalty_service_club_api.service.client;

import com.bizwaresol.loyalty_service_club_api.client.aws.sns.SnsClient;
import com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.request.SnsMessageRequest;
import com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.response.SnsMessageResponse;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;
import com.bizwaresol.loyalty_service_club_api.util.mappers.AwsErrorMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SnsClientService {

    private final SnsClient snsClient;

    // AWS SNS SMS limits
    private static final int MAX_SMS_LENGTH = 1600; // AWS SNS limit
    private static final int STANDARD_SMS_LENGTH = 160; // Standard SMS length
    private static final int MAX_SENDER_ID_LENGTH = 11; // AWS limit for sender ID
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$"); // E.164 format
    private static final Pattern PRICE_PATTERN = Pattern.compile("^\\d+(\\.\\d{2})?$"); // USD format

    public SnsClientService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    /**
     * Sends an SMS message via AWS SNS
     * @param request the SMS request containing phone number, message, and attributes
     * @return SnsMessageResponse with success details and message ID
     * @throws ServiceException if AWS SNS operation fails (mapped from AWS exceptions)
     */
    public SnsMessageResponse sendMessage(SnsMessageRequest request) throws ServiceException {
        validateSmsRequest(request);

        try {
            return snsClient.sendMessage(request);
        } catch (Exception e) {
            throw AwsErrorMapper.mapException(e);
        }
    }

    /**
     * Sends a simple SMS message
     * @param phoneNumber destination phone number (format: +381xxxxxxxx)
     * @param message SMS message content
     * @return SnsMessageResponse with success details and message ID
     * @throws ServiceException if AWS SNS operation fails
     */
    public SnsMessageResponse sendSms(String phoneNumber, String message) throws ServiceException {
        SnsMessageRequest request = new SnsMessageRequest(phoneNumber, message);
        return sendMessage(request);
    }

    /**
     * Sends a transactional SMS (for OTP, alerts, etc.)
     * @param phoneNumber destination phone number
     * @param message SMS message content
     * @param senderId optional sender ID (your app name)
     * @return SnsMessageResponse with success details and message ID
     * @throws ServiceException if AWS SNS operation fails
     */
    public SnsMessageResponse sendTransactionalSms(String phoneNumber, String message, String senderId) throws ServiceException {
        SnsMessageRequest request = new SnsMessageRequest(phoneNumber, message);

        // Set SMS as transactional (higher priority, better delivery)
        request.setSmsType("Transactional");

        // Set sender ID if provided
        if (senderId != null && !senderId.trim().isEmpty()) {
            validateSenderId(senderId);
            request.setSenderId(senderId.trim());
        }

        return sendMessage(request);
    }

    /**
     * Sends a promotional SMS (for marketing, newsletters, etc.)
     * @param phoneNumber destination phone number
     * @param message SMS message content
     * @param senderId optional sender ID
     * @return SnsMessageResponse with success details and message ID
     * @throws ServiceException if AWS SNS operation fails
     */
    public SnsMessageResponse sendPromotionalSms(String phoneNumber, String message, String senderId) throws ServiceException {
        SnsMessageRequest request = new SnsMessageRequest(phoneNumber, message);

        // Set SMS as promotional (lower cost, best effort delivery)
        request.setSmsType("Promotional");

        // Set sender ID if provided
        if (senderId != null && !senderId.trim().isEmpty()) {
            validateSenderId(senderId);
            request.setSenderId(senderId.trim());
        }

        return sendMessage(request);
    }

    /**
     * Sends SMS with custom attributes and maximum price limit
     * @param phoneNumber destination phone number
     * @param message SMS message content
     * @param attributes custom SMS attributes
     * @param maxPriceUsd maximum price willing to pay (in USD)
     * @return SnsMessageResponse with success details and message ID
     * @throws ServiceException if AWS SNS operation fails
     */
    public SnsMessageResponse sendSmsWithAttributes(String phoneNumber, String message,
                                                    Map<String, String> attributes, String maxPriceUsd) throws ServiceException {
        Map<String, String> allAttributes = new HashMap<>();

        // Add custom attributes if provided
        if (attributes != null) {
            allAttributes.putAll(attributes);
        }

        // Set max price if provided
        if (maxPriceUsd != null && !maxPriceUsd.trim().isEmpty()) {
            validatePrice(maxPriceUsd);
            allAttributes.put("SMS.MaxPrice", maxPriceUsd.trim());
        }

        SnsMessageRequest request = new SnsMessageRequest(phoneNumber, message, allAttributes);
        return sendMessage(request);
    }

    /**
     * Sends an OTP SMS with optimized settings for verification codes
     * @param phoneNumber destination phone number
     * @param otpMessage OTP message content
     * @param appName your application name for sender ID
     * @return SnsMessageResponse with success details and message ID
     * @throws ServiceException if AWS SNS operation fails
     */
    public SnsMessageResponse sendOtpSms(String phoneNumber, String otpMessage, String appName) throws ServiceException {
        SnsMessageRequest request = new SnsMessageRequest(phoneNumber, otpMessage);

        // Configure for OTP delivery (transactional, high priority)
        request.setSmsType("Transactional");

        if (appName != null && !appName.trim().isEmpty()) {
            validateSenderId(appName);
            request.setSenderId(appName.trim());
        }

        // Set reasonable max price for OTP delivery
        validatePrice("0.50");
        request.setMaxPrice("0.50"); // 50 cents max for OTP delivery

        return sendMessage(request);
    }

    // ===== PRIVATE VALIDATION METHODS =====

    /**
     * Validates SMS request for AWS SNS requirements
     */
    private void validateSmsRequest(SnsMessageRequest request) {
        if (request == null) {
            throw new NullFieldException("snsMessageRequest");
        }

        // Validate phone number
        validatePhoneNumber(request.getPhoneNumber());

        // Validate message
        validateSmsMessage(request.getMessage());

        // Validate SMS type if provided
        validateSmsType(request.getMessageAttributes());

        // Validate price if provided
        String maxPrice = request.getMessageAttributes().get("SMS.MaxPrice");
        if (maxPrice != null && !maxPrice.trim().isEmpty()) {
            validatePrice(maxPrice);
        }

        // Validate sender ID if provided
        String senderId = request.getMessageAttributes().get("SMS.SenderID");
        if (senderId != null && !senderId.trim().isEmpty()) {
            validateSenderId(senderId);
        }
    }

    /**
     * Validates phone number format (E.164)
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new NullFieldException("phoneNumber");
        }

        String trimmedPhone = phoneNumber.trim();
        if (trimmedPhone.isEmpty()) {
            throw new EmptyFieldException("phoneNumber");
        }

        if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            throw new InvalidPhoneFormatException(trimmedPhone + " (Expected E.164 format: +[country][number])");
        }
    }

    /**
     * Validates SMS message content
     */
    private void validateSmsMessage(String message) {
        if (message == null) {
            throw new NullFieldException("message");
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.isEmpty()) {
            throw new EmptyFieldException("message");
        }

        if (trimmedMessage.length() > MAX_SMS_LENGTH) {
            throw new FieldTooLongException("message", MAX_SMS_LENGTH, trimmedMessage.length());
        }

        // Warning for messages longer than standard SMS length (will be split into multiple SMS)
        if (trimmedMessage.length() > STANDARD_SMS_LENGTH) {
            // This is just informational - AWS SNS will handle splitting
            // Could log a warning here if needed
        }
    }

    /**
     * Validates SMS type
     */
    private void validateSmsType(Map<String, String> attributes) {
        if (attributes == null) return;

        String smsType = attributes.get("SMS.SMSType");
        if (smsType != null && !smsType.trim().isEmpty()) {
            String trimmedType = smsType.trim();
            if (!trimmedType.equals("Transactional") && !trimmedType.equals("Promotional")) {
                throw new IllegalArgumentException("SMS type must be 'Transactional' or 'Promotional', got: " + trimmedType);
            }
        }
    }

    /**
     * Validates sender ID
     */
    private void validateSenderId(String senderId) {
        if (senderId == null) return;

        String trimmedSenderId = senderId.trim();
        if (trimmedSenderId.length() > MAX_SENDER_ID_LENGTH) {
            throw new FieldTooLongException("senderId", MAX_SENDER_ID_LENGTH, trimmedSenderId.length());
        }

        // Sender ID should be alphanumeric (AWS requirement)
        if (!trimmedSenderId.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Sender ID must be alphanumeric: " + trimmedSenderId);
        }
    }

    /**
     * Validates price format
     */
    private void validatePrice(String price) {
        if (price == null) return;

        String trimmedPrice = price.trim();
        if (!PRICE_PATTERN.matcher(trimmedPrice).matches()) {
            throw new IllegalArgumentException("Price must be in USD format (e.g., '0.50', '1.00'): " + trimmedPrice);
        }

        // Additional validation: reasonable price range
        double priceValue = Double.parseDouble(trimmedPrice);
        if (priceValue < 0 || priceValue > 10.00) {
            throw new IllegalArgumentException("Price must be between $0.00 and $10.00: " + trimmedPrice);
        }
    }
}
package com.bizwaresol.loyalty_service_club_api.service.client;

import com.bizwaresol.loyalty_service_club_api.client.aws.ses.SesClient;
import com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.request.SesEmailRequest;
import com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.response.SesEmailResponse;
import com.bizwaresol.loyalty_service_club_api.exception.base.ServiceException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;
import com.bizwaresol.loyalty_service_club_api.util.mappers.AwsErrorMapper;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class SesClientService {

    private final SesClient sesClient;

    // AWS SES limits
    private static final int MAX_EMAIL_LENGTH = 320; // RFC 5321 limit
    private static final int MAX_SUBJECT_LENGTH = 998; // RFC 2822 limit
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; // 10MB AWS SES limit
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public SesClientService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    /**
     * Sends an email via AWS SES
     * @param request the email request containing all email details
     * @return SesEmailResponse with success details and message ID
     * @throws ServiceException if AWS SES operation fails (mapped from AWS exceptions)
     */
    public SesEmailResponse sendEmail(SesEmailRequest request) throws ServiceException {
        validateEmailRequest(request);

        try {
            return sesClient.sendEmail(request);
        } catch (Exception e) {
            throw AwsErrorMapper.mapException(e);
        }
    }

    /**
     * Sends a simple text email
     * @param fromEmail source email address
     * @param toEmail destination email address
     * @param subject email subject
     * @param textContent email text content
     * @return SesEmailResponse with success details and message ID
     * @throws ServiceException if AWS SES operation fails
     */
    public SesEmailResponse sendTextEmail(String fromEmail, String toEmail, String subject, String textContent) throws ServiceException {
        SesEmailRequest request = new SesEmailRequest(fromEmail, toEmail, subject, textContent, null);
        return sendEmail(request);
    }

    /**
     * Sends an HTML email
     * @param fromEmail source email address
     * @param toEmail destination email address
     * @param subject email subject
     * @param htmlContent email HTML content
     * @return SesEmailResponse with success details and message ID
     * @throws ServiceException if AWS SES operation fails
     */
    public SesEmailResponse sendHtmlEmail(String fromEmail, String toEmail, String subject, String htmlContent) throws ServiceException {
        SesEmailRequest request = new SesEmailRequest(fromEmail, toEmail, subject, null, htmlContent);
        return sendEmail(request);
    }

    /**
     * Sends an email with both text and HTML content
     * @param fromEmail source email address
     * @param toEmail destination email address
     * @param subject email subject
     * @param textContent email text content (fallback)
     * @param htmlContent email HTML content
     * @return SesEmailResponse with success details and message ID
     * @throws ServiceException if AWS SES operation fails
     */
    public SesEmailResponse sendMultipartEmail(String fromEmail, String toEmail, String subject,
                                               String textContent, String htmlContent) throws ServiceException {
        SesEmailRequest request = new SesEmailRequest(fromEmail, toEmail, subject, textContent, htmlContent);
        return sendEmail(request);
    }

    // ===== PRIVATE VALIDATION METHODS =====

    /**
     * Validates SES email request for AWS API requirements
     */
    private void validateEmailRequest(SesEmailRequest request) {
        if (request == null) {
            throw new NullFieldException("sesEmailRequest");
        }

        // Validate source email
        validateEmailAddress(request.getSourceEmail(), "sourceEmail");

        // Validate destination email
        validateEmailAddress(request.getDestinationEmail(), "destinationEmail");

        // Validate subject
        validateSubject(request.getSubject());

        // Validate content
        validateEmailContent(request.getTextContent(), request.getHtmlContent());
    }

    /**
     * Validates email address format and length
     */
    private void validateEmailAddress(String email, String fieldName) {
        if (email == null) {
            throw new NullFieldException(fieldName);
        }

        String trimmedEmail = email.trim();
        if (trimmedEmail.isEmpty()) {
            throw new EmptyFieldException(fieldName);
        }

        if (trimmedEmail.length() > MAX_EMAIL_LENGTH) {
            throw new FieldTooLongException(fieldName, MAX_EMAIL_LENGTH, trimmedEmail.length());
        }

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new InvalidEmailFormatException(trimmedEmail);
        }
    }

    /**
     * Validates email subject
     */
    private void validateSubject(String subject) {
        if (subject == null) {
            throw new NullFieldException("subject");
        }

        String trimmedSubject = subject.trim();
        if (trimmedSubject.isEmpty()) {
            throw new EmptyFieldException("subject");
        }

        if (trimmedSubject.length() > MAX_SUBJECT_LENGTH) {
            throw new FieldTooLongException("subject", MAX_SUBJECT_LENGTH, trimmedSubject.length());
        }
    }

    /**
     * Validates email content (text and/or HTML)
     */
    private void validateEmailContent(String textContent, String htmlContent) {
        // At least one content type must be provided
        boolean hasTextContent = textContent != null && !textContent.trim().isEmpty();
        boolean hasHtmlContent = htmlContent != null && !htmlContent.trim().isEmpty();

        if (!hasTextContent && !hasHtmlContent) {
            throw new EmptyFieldException("emailContent - at least text or HTML content must be provided");
        }

        // Validate message size (combined text + HTML)
        int totalSize = 0;
        if (hasTextContent) {
            totalSize += textContent.getBytes().length;
        }
        if (hasHtmlContent) {
            totalSize += htmlContent.getBytes().length;
        }

        if (totalSize > MAX_MESSAGE_SIZE) {
            throw new FieldTooLongException("emailContent", MAX_MESSAGE_SIZE, totalSize);
        }
    }
}
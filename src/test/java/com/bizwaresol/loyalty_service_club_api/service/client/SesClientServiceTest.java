package com.bizwaresol.loyalty_service_club_api.service.client;

import com.bizwaresol.loyalty_service_club_api.client.aws.ses.SesClient;
import com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.request.SesEmailRequest;
import com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.response.SesEmailResponse;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.ses.SesQuotaExceededException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidEmailFormatException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.ses.model.SesException;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SesClientService Unit Tests")
class SesClientServiceTest {

    @Mock
    private SesClient sesClient;

    @InjectMocks
    private SesClientService sesClientService;

    private SesEmailRequest validRequest;
    private SesEmailResponse successResponse;
    private final String VALID_FROM_EMAIL = "test@example.com";
    private final String VALID_TO_EMAIL = "recipient@example.com";
    private final String VALID_SUBJECT = "Test Subject";
    private final String VALID_TEXT_CONTENT = "Test message content";
    private final String VALID_HTML_CONTENT = "<p>Test HTML content</p>";
    private final String MESSAGE_ID = "ses-message-id-123";

    @BeforeEach
    void setUp() {
        validRequest = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, VALID_HTML_CONTENT);
        successResponse = SesEmailResponse.success(MESSAGE_ID);
    }

    // ===== SEND EMAIL TESTS =====

    @Nested
    @DisplayName("sendEmail() Tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should throw NullFieldException when request is null")
        void shouldThrowNullFieldExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> sesClientService.sendEmail(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'sesEmailRequest' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when source email is null")
        void shouldThrowNullFieldExceptionWhenSourceEmailIsNull() {
            SesEmailRequest request = new SesEmailRequest(null, VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'sourceEmail' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when destination email is null")
        void shouldThrowNullFieldExceptionWhenDestinationEmailIsNull() {
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, null, VALID_SUBJECT, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'destinationEmail' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when source email is empty")
        void shouldThrowEmptyFieldExceptionWhenSourceEmailIsEmpty() {
            SesEmailRequest request = new SesEmailRequest("   ", VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'sourceEmail' cannot be empty");
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException when source email is invalid")
        void shouldThrowInvalidEmailFormatExceptionWhenSourceEmailIsInvalid() {
            SesEmailRequest request = new SesEmailRequest("invalid-email", VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(InvalidEmailFormatException.class)
                    .hasMessage("Invalid email format: invalid-email");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when email exceeds length limit")
        void shouldThrowFieldTooLongExceptionWhenEmailExceedsLengthLimit() {
            String longEmail = "a".repeat(310) + "@example.com"; // This will be 322 characters total
            SesEmailRequest request = new SesEmailRequest(longEmail, VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessageContaining("sourceEmail too long");
        }

        @Test
        @DisplayName("Should throw NullFieldException when subject is null")
        void shouldThrowNullFieldExceptionWhenSubjectIsNull() {
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, null, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'subject' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when subject is empty")
        void shouldThrowEmptyFieldExceptionWhenSubjectIsEmpty() {
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, "   ", VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'subject' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when subject exceeds length limit")
        void shouldThrowFieldTooLongExceptionWhenSubjectExceedsLengthLimit() {
            String longSubject = "a".repeat(1000);
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, longSubject, VALID_TEXT_CONTENT, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessageContaining("subject too long");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when both text and HTML content are empty")
        void shouldThrowEmptyFieldExceptionWhenBothTextAndHtmlContentAreEmpty() {
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, null, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessageContaining("emailContent - at least text or HTML content must be provided");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when content exceeds size limit")
        void shouldThrowFieldTooLongExceptionWhenContentExceedsSizeLimit() {
            String hugeContent = "a".repeat(11 * 1024 * 1024); // 11MB (exceeds 10MB limit)
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, hugeContent, null);

            assertThatThrownBy(() -> sesClientService.sendEmail(request))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessageContaining("emailContent too long");
        }

        @Test
        @DisplayName("Should send email successfully with text content")
        void shouldSendEmailSuccessfullyWithTextContent() {
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, null);
            when(sesClient.sendEmail(any(SesEmailRequest.class))).thenReturn(successResponse);

            SesEmailResponse result = sesClientService.sendEmail(request);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }

        @Test
        @DisplayName("Should send email successfully with HTML content")
        void shouldSendEmailSuccessfullyWithHtmlContent() {
            SesEmailRequest request = new SesEmailRequest(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, null, VALID_HTML_CONTENT);
            when(sesClient.sendEmail(any(SesEmailRequest.class))).thenReturn(successResponse);

            SesEmailResponse result = sesClientService.sendEmail(request);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }

        @Test
        @DisplayName("Should send email successfully with both text and HTML content")
        void shouldSendEmailSuccessfullyWithBothTextAndHtmlContent() {
            when(sesClient.sendEmail(any(SesEmailRequest.class))).thenReturn(successResponse);

            SesEmailResponse result = sesClientService.sendEmail(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }

        @Test
        @DisplayName("Should map AWS SES exception to service exception")
        void shouldMapAwsSesExceptionToServiceException() {
            // Create SES exception with proper error details for quota exceeded
            SesException sesException = (SesException) SesException.builder()
                    .message("Daily sending quota exceeded")
                    .awsErrorDetails(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                            .errorCode("DailyQuotaExceeded")
                            .errorMessage("Daily sending quota exceeded")
                            .build())
                    .requestId("test-request-id")
                    .build();

            when(sesClient.sendEmail(any(SesEmailRequest.class)))
                    .thenThrow(sesException);

            assertThatThrownBy(() -> sesClientService.sendEmail(validRequest))
                    .isInstanceOf(SesQuotaExceededException.class);

            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }
    }

    // ===== CONVENIENCE METHODS TESTS =====

    @Nested
    @DisplayName("sendTextEmail() Tests")
    class SendTextEmailTests {

        @Test
        @DisplayName("Should send text email successfully")
        void shouldSendTextEmailSuccessfully() {
            when(sesClient.sendEmail(any(SesEmailRequest.class))).thenReturn(successResponse);

            SesEmailResponse result = sesClientService.sendTextEmail(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }

        @Test
        @DisplayName("Should throw InvalidEmailFormatException for invalid email")
        void shouldThrowInvalidEmailFormatExceptionForInvalidEmail() {
            assertThatThrownBy(() -> sesClientService.sendTextEmail("invalid-email", VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT))
                    .isInstanceOf(InvalidEmailFormatException.class);
        }
    }

    @Nested
    @DisplayName("sendHtmlEmail() Tests")
    class SendHtmlEmailTests {

        @Test
        @DisplayName("Should send HTML email successfully")
        void shouldSendHtmlEmailSuccessfully() {
            when(sesClient.sendEmail(any(SesEmailRequest.class))).thenReturn(successResponse);

            SesEmailResponse result = sesClientService.sendHtmlEmail(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, VALID_HTML_CONTENT);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }

        @Test
        @DisplayName("Should throw NullFieldException when HTML content is null")
        void shouldThrowNullFieldExceptionWhenHtmlContentIsNull() {
            assertThatThrownBy(() -> sesClientService.sendHtmlEmail(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, null))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessageContaining("emailContent - at least text or HTML content must be provided");
        }
    }

    @Nested
    @DisplayName("sendMultipartEmail() Tests")
    class SendMultipartEmailTests {

        @Test
        @DisplayName("Should send multipart email successfully")
        void shouldSendMultipartEmailSuccessfully() {
            when(sesClient.sendEmail(any(SesEmailRequest.class))).thenReturn(successResponse);

            SesEmailResponse result = sesClientService.sendMultipartEmail(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, VALID_TEXT_CONTENT, VALID_HTML_CONTENT);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(sesClient).sendEmail(any(SesEmailRequest.class));
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when both contents are null")
        void shouldThrowEmptyFieldExceptionWhenBothContentsAreNull() {
            assertThatThrownBy(() -> sesClientService.sendMultipartEmail(VALID_FROM_EMAIL, VALID_TO_EMAIL, VALID_SUBJECT, null, null))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessageContaining("emailContent - at least text or HTML content must be provided");
        }
    }
}
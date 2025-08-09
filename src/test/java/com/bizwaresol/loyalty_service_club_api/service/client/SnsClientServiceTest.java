package com.bizwaresol.loyalty_service_club_api.service.client;

import com.bizwaresol.loyalty_service_club_api.client.aws.sns.SnsClient;
import com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.request.SnsMessageRequest;
import com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.response.SnsMessageResponse;
import com.bizwaresol.loyalty_service_club_api.exception.system.client.aws.sns.SnsThrottlingException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.EmptyFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.FieldTooLongException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.field.NullFieldException;
import com.bizwaresol.loyalty_service_club_api.exception.validation.format.InvalidPhoneFormatException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnsClientService Unit Tests")
class SnsClientServiceTest {

    @Mock
    private SnsClient snsClient;

    @InjectMocks
    private SnsClientService snsClientService;

    private SnsMessageRequest validRequest;
    private SnsMessageResponse successResponse;
    private final String VALID_PHONE_NUMBER = "+381123456789";
    private final String VALID_MESSAGE = "Test SMS message";
    private final String VALID_SENDER_ID = "TestApp";
    private final String MESSAGE_ID = "sns-message-id-123";

    @BeforeEach
    void setUp() {
        validRequest = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
        successResponse = SnsMessageResponse.success(MESSAGE_ID);
    }

    // ===== SEND MESSAGE TESTS =====

    @Nested
    @DisplayName("sendMessage() Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should throw NullFieldException when request is null")
        void shouldThrowNullFieldExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> snsClientService.sendMessage(null))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'snsMessageRequest' cannot be null");
        }

        @Test
        @DisplayName("Should throw NullFieldException when phone number is null")
        void shouldThrowNullFieldExceptionWhenPhoneNumberIsNull() {
            SnsMessageRequest request = new SnsMessageRequest(null, VALID_MESSAGE);

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'phoneNumber' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when phone number is empty")
        void shouldThrowEmptyFieldExceptionWhenPhoneNumberIsEmpty() {
            SnsMessageRequest request = new SnsMessageRequest("   ", VALID_MESSAGE);

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'phoneNumber' cannot be empty");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException when phone number is invalid")
        void shouldThrowInvalidPhoneFormatExceptionWhenPhoneNumberIsInvalid() {
            SnsMessageRequest request = new SnsMessageRequest("123456789", VALID_MESSAGE);

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(InvalidPhoneFormatException.class)
                    .hasMessageContaining("Expected E.164 format");
        }

        @Test
        @DisplayName("Should throw NullFieldException when message is null")
        void shouldThrowNullFieldExceptionWhenMessageIsNull() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, null);

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'message' cannot be null");
        }

        @Test
        @DisplayName("Should throw EmptyFieldException when message is empty")
        void shouldThrowEmptyFieldExceptionWhenMessageIsEmpty() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, "   ");

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(EmptyFieldException.class)
                    .hasMessage("Field 'message' cannot be empty");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when message exceeds length limit")
        void shouldThrowFieldTooLongExceptionWhenMessageExceedsLengthLimit() {
            String longMessage = "a".repeat(1601);
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, longMessage);

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessageContaining("message too long");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when SMS type is invalid")
        void shouldThrowIllegalArgumentExceptionWhenSmsTypeIsInvalid() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
            request.setSmsType("InvalidType");

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SMS type must be 'Transactional' or 'Promotional'");
        }

        @Test
        @DisplayName("Should throw FieldTooLongException when sender ID exceeds length limit")
        void shouldThrowFieldTooLongExceptionWhenSenderIdExceedsLengthLimit() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
            request.setSenderId("VeryLongSenderID123");

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(FieldTooLongException.class)
                    .hasMessageContaining("senderId too long");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when sender ID contains invalid characters")
        void shouldThrowIllegalArgumentExceptionWhenSenderIdContainsInvalidCharacters() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
            request.setSenderId("Test@App");

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sender ID must be alphanumeric");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when price format is invalid")
        void shouldThrowIllegalArgumentExceptionWhenPriceFormatIsInvalid() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
            request.setMaxPrice("invalid-price");

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be in USD format");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when price is out of range")
        void shouldThrowIllegalArgumentExceptionWhenPriceIsOutOfRange() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
            request.setMaxPrice("15.00");

            assertThatThrownBy(() -> snsClientService.sendMessage(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be between $0.00 and $10.00");
        }

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendMessage(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should send message successfully with valid attributes")
        void shouldSendMessageSuccessfullyWithValidAttributes() {
            SnsMessageRequest request = new SnsMessageRequest(VALID_PHONE_NUMBER, VALID_MESSAGE);
            request.setSmsType("Transactional");
            request.setSenderId("TestApp");
            request.setMaxPrice("0.50");

            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendMessage(request);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should map AWS SNS exception to service exception")
        void shouldMapAwsSnsExceptionToServiceException() {
            // Create SNS exception with proper error details for throttling
            SnsException snsException = (SnsException) SnsException.builder()
                    .message("Request was throttled")
                    .awsErrorDetails(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                            .errorCode("Throttling")
                            .errorMessage("Request was throttled")
                            .build())
                    .requestId("test-request-id")
                    .build();

            when(snsClient.sendMessage(any(SnsMessageRequest.class)))
                    .thenThrow(snsException);

            assertThatThrownBy(() -> snsClientService.sendMessage(validRequest))
                    .isInstanceOf(SnsThrottlingException.class);

            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }
    }

    // ===== CONVENIENCE METHODS TESTS =====

    @Nested
    @DisplayName("sendSms() Tests")
    class SendSmsTests {

        @Test
        @DisplayName("Should send SMS successfully")
        void shouldSendSmsSuccessfully() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendSms(VALID_PHONE_NUMBER, VALID_MESSAGE);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException for invalid phone")
        void shouldThrowInvalidPhoneFormatExceptionForInvalidPhone() {
            assertThatThrownBy(() -> snsClientService.sendSms("123456789", VALID_MESSAGE))
                    .isInstanceOf(InvalidPhoneFormatException.class);
        }
    }

    @Nested
    @DisplayName("sendTransactionalSms() Tests")
    class SendTransactionalSmsTests {

        @Test
        @DisplayName("Should send transactional SMS successfully")
        void shouldSendTransactionalSmsSuccessfully() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendTransactionalSms(VALID_PHONE_NUMBER, VALID_MESSAGE, VALID_SENDER_ID);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should send transactional SMS successfully without sender ID")
        void shouldSendTransactionalSmsSuccessfullyWithoutSenderId() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendTransactionalSms(VALID_PHONE_NUMBER, VALID_MESSAGE, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid sender ID")
        void shouldThrowIllegalArgumentExceptionForInvalidSenderId() {
            assertThatThrownBy(() -> snsClientService.sendTransactionalSms(VALID_PHONE_NUMBER, VALID_MESSAGE, "Test@App"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sender ID must be alphanumeric");
        }
    }

    @Nested
    @DisplayName("sendPromotionalSms() Tests")
    class SendPromotionalSmsTests {

        @Test
        @DisplayName("Should send promotional SMS successfully")
        void shouldSendPromotionalSmsSuccessfully() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendPromotionalSms(VALID_PHONE_NUMBER, VALID_MESSAGE, VALID_SENDER_ID);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should send promotional SMS successfully without sender ID")
        void shouldSendPromotionalSmsSuccessfullyWithoutSenderId() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendPromotionalSms(VALID_PHONE_NUMBER, VALID_MESSAGE, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }
    }

    @Nested
    @DisplayName("sendSmsWithAttributes() Tests")
    class SendSmsWithAttributesTests {

        @Test
        @DisplayName("Should send SMS with custom attributes successfully")
        void shouldSendSmsWithCustomAttributesSuccessfully() {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("SMS.SMSType", "Transactional");

            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendSmsWithAttributes(VALID_PHONE_NUMBER, VALID_MESSAGE, attributes, "1.00");

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should send SMS with null attributes successfully")
        void shouldSendSmsWithNullAttributesSuccessfully() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendSmsWithAttributes(VALID_PHONE_NUMBER, VALID_MESSAGE, null, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid price")
        void shouldThrowIllegalArgumentExceptionForInvalidPrice() {
            assertThatThrownBy(() -> snsClientService.sendSmsWithAttributes(VALID_PHONE_NUMBER, VALID_MESSAGE, null, "invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be in USD format");
        }
    }

    @Nested
    @DisplayName("sendOtpSms() Tests")
    class SendOtpSmsTests {

        private final String OTP_MESSAGE = "Your OTP code is 123456";

        @Test
        @DisplayName("Should send OTP SMS successfully")
        void shouldSendOtpSmsSuccessfully() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendOtpSms(VALID_PHONE_NUMBER, OTP_MESSAGE, VALID_SENDER_ID);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).isEqualTo(MESSAGE_ID);
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should send OTP SMS successfully without app name")
        void shouldSendOtpSmsSuccessfullyWithoutAppName() {
            when(snsClient.sendMessage(any(SnsMessageRequest.class))).thenReturn(successResponse);

            SnsMessageResponse result = snsClientService.sendOtpSms(VALID_PHONE_NUMBER, OTP_MESSAGE, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(snsClient).sendMessage(any(SnsMessageRequest.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid app name")
        void shouldThrowIllegalArgumentExceptionForInvalidAppName() {
            assertThatThrownBy(() -> snsClientService.sendOtpSms(VALID_PHONE_NUMBER, OTP_MESSAGE, "App@Name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sender ID must be alphanumeric");
        }

        @Test
        @DisplayName("Should throw InvalidPhoneFormatException for invalid phone")
        void shouldThrowInvalidPhoneFormatExceptionForInvalidPhone() {
            assertThatThrownBy(() -> snsClientService.sendOtpSms("123456789", OTP_MESSAGE, VALID_SENDER_ID))
                    .isInstanceOf(InvalidPhoneFormatException.class);
        }

        @Test
        @DisplayName("Should throw NullFieldException for null message")
        void shouldThrowNullFieldExceptionForNullMessage() {
            assertThatThrownBy(() -> snsClientService.sendOtpSms(VALID_PHONE_NUMBER, null, VALID_SENDER_ID))
                    .isInstanceOf(NullFieldException.class)
                    .hasMessage("Field 'message' cannot be null");
        }
    }
}
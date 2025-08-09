package com.bizwaresol.loyalty_service_club_api.client.aws.sns;

import com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.request.SnsMessageRequest;
import com.bizwaresol.loyalty_service_club_api.client.aws.sns.dto.response.SnsMessageResponse;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Map;

@Component
public class SnsClient {

    private final software.amazon.awssdk.services.sns.SnsClient awsSnsClient;

    public SnsClient(software.amazon.awssdk.services.sns.SnsClient awsSnsClient) {
        this.awsSnsClient = awsSnsClient;
    }

    /**
     * Sends an SMS message using AWS SNS
     * @param request our SMS request DTO
     * @return our SMS response DTO
     * @throws SnsException if AWS SNS call fails
     */
    public SnsMessageResponse sendMessage(SnsMessageRequest request) {
        // Convert our DTO to AWS request
        PublishRequest awsRequest = buildAwsPublishRequest(request);

        // Call AWS SNS (exceptions bubble up naturally)
        PublishResponse awsResponse = awsSnsClient.publish(awsRequest);

        // Convert AWS response to our DTO
        return SnsMessageResponse.success(awsResponse.messageId());
    }

    /**
     * Converts our SnsMessageRequest to AWS PublishRequest
     */
    private PublishRequest buildAwsPublishRequest(SnsMessageRequest request) {
        PublishRequest.Builder requestBuilder = PublishRequest.builder()
                .phoneNumber(request.getPhoneNumber())
                .message(request.getMessage());

        // Add message attributes if provided
        if (request.getMessageAttributes() != null && !request.getMessageAttributes().isEmpty()) {
            Map<String, MessageAttributeValue> awsMessageAttributes = new HashMap<>();

            for (Map.Entry<String, String> entry : request.getMessageAttributes().entrySet()) {
                awsMessageAttributes.put(
                        entry.getKey(),
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(entry.getValue())
                                .build()
                );
            }

            requestBuilder.messageAttributes(awsMessageAttributes);
        }

        return requestBuilder.build();
    }
}
package com.bizwaresol.loyalty_service_club_api.client.aws.ses;

import com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.request.SesEmailRequest;
import com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.response.SesEmailResponse;
import software.amazon.awssdk.services.ses.model.*;

public class SesClient {

    private final software.amazon.awssdk.services.ses.SesClient awsSesClient;

    public SesClient(software.amazon.awssdk.services.ses.SesClient awsSesClient) {
        this.awsSesClient = awsSesClient;
    }

    /**
     * Sends an email using AWS SES
     * @param request our email request DTO
     * @return our email response DTO
     * @throws SesException if AWS SES call fails
     */
    public SesEmailResponse sendEmail(SesEmailRequest request) {
        // Convert our DTO to AWS request
        SendEmailRequest awsRequest = buildAwsEmailRequest(request);

        // Call AWS SES (exceptions bubble up naturally)
        SendEmailResponse awsResponse = awsSesClient.sendEmail(awsRequest);

        // Convert AWS response to our DTO
        return SesEmailResponse.success(awsResponse.messageId());
    }

    /**
     * Converts our SesEmailRequest to AWS SendEmailRequest
     */
    private SendEmailRequest buildAwsEmailRequest(SesEmailRequest request) {
        // Build the message body
        Body.Builder bodyBuilder = Body.builder();

        if (request.getTextContent() != null && !request.getTextContent().trim().isEmpty()) {
            bodyBuilder.text(Content.builder()
                    .data(request.getTextContent())
                    .charset("UTF-8")
                    .build());
        }

        if (request.getHtmlContent() != null && !request.getHtmlContent().trim().isEmpty()) {
            bodyBuilder.html(Content.builder()
                    .data(request.getHtmlContent())
                    .charset("UTF-8")
                    .build());
        }

        // Build the complete message
        Message message = Message.builder()
                .subject(Content.builder()
                        .data(request.getSubject())
                        .charset("UTF-8")
                        .build())
                .body(bodyBuilder.build())
                .build();

        // Build the destination
        Destination destination = Destination.builder()
                .toAddresses(request.getDestinationEmail())
                .build();

        // Build the final AWS request
        return SendEmailRequest.builder()
                .source(request.getSourceEmail())
                .destination(destination)
                .message(message)
                .build();
    }
}

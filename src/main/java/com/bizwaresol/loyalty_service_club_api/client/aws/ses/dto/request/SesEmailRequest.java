package com.bizwaresol.loyalty_service_club_api.client.aws.ses.dto.request;

import java.util.Objects;

public class SesEmailRequest {

    private String sourceEmail;
    private String destinationEmail;
    private String subject;
    private String textContent;
    private String htmlContent;

    public SesEmailRequest() {}

    public SesEmailRequest(String sourceEmail, String destinationEmail, String subject, String textContent, String htmlContent) {
        this.sourceEmail = sourceEmail;
        this.destinationEmail = destinationEmail;
        this.subject = subject;
        this.textContent = textContent;
        this.htmlContent = htmlContent;
    }

    public String getSourceEmail() {
        return sourceEmail;
    }

    public void setSourceEmail(String sourceEmail) {
        this.sourceEmail = sourceEmail;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }

    public void setDestinationEmail(String destinationEmail) {
        this.destinationEmail = destinationEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SesEmailRequest that)) return false;
        return Objects.equals(sourceEmail, that.sourceEmail) &&
                Objects.equals(destinationEmail, that.destinationEmail) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(textContent, that.textContent) &&
                Objects.equals(htmlContent, that.htmlContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceEmail, destinationEmail, subject, textContent, htmlContent);
    }

    @Override
    public String toString() {
        return "SesEmailRequest{" +
                "sourceEmail='" + sourceEmail + '\'' +
                ", destinationEmail='" + destinationEmail + '\'' +
                ", subject='" + subject + '\'' +
                ", textContent='" + textContent + '\'' +
                ", htmlContent='" + htmlContent + '\'' +
                '}';
    }
}
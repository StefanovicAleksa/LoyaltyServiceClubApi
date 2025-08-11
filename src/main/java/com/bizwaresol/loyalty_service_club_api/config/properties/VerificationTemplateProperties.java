package com.bizwaresol.loyalty_service_club_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verification.templates")
public class VerificationTemplateProperties {

    // Email properties
    private String emailSubject;
    private String emailHtmlTemplate;
    private String emailTextTemplate;

    // SMS properties
    private String smsTemplate;

    // Getters and setters
    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailHtmlTemplate() {
        return emailHtmlTemplate;
    }

    public void setEmailHtmlTemplate(String emailHtmlTemplate) {
        this.emailHtmlTemplate = emailHtmlTemplate;
    }

    public String getEmailTextTemplate() {
        return emailTextTemplate;
    }

    public void setEmailTextTemplate(String emailTextTemplate) {
        this.emailTextTemplate = emailTextTemplate;
    }

    public String getSmsTemplate() {
        return smsTemplate;
    }

    public void setSmsTemplate(String smsTemplate) {
        this.smsTemplate = smsTemplate;
    }

    // Template formatting methods
    public String formatEmailSubject() {
        return emailSubject != null ? emailSubject : "Verify Your Email Address";
    }

    public String formatEmailHtml(String otpCode, int expiryMinutes) {
        if (emailHtmlTemplate == null) {
            return getDefaultHtmlTemplate(otpCode, expiryMinutes);
        }
        return emailHtmlTemplate
                .replace("{otpCode}", otpCode)
                .replace("{expiryMinutes}", String.valueOf(expiryMinutes));
    }

    public String formatEmailText(String otpCode, int expiryMinutes) {
        if (emailTextTemplate == null) {
            return getDefaultTextTemplate(otpCode, expiryMinutes);
        }
        return emailTextTemplate
                .replace("{otpCode}", otpCode)
                .replace("{expiryMinutes}", String.valueOf(expiryMinutes));
    }

    public String formatSms(String otpCode) {
        if (smsTemplate == null) {
            return "Your verification code is " + otpCode;
        }
        return smsTemplate.replace("{otpCode}", otpCode);
    }

    // Default templates if not configured
    private String getDefaultHtmlTemplate(String otpCode, int expiryMinutes) {
        return """
                <html>
                <body>
                    <h2>Email Verification</h2>
                    <p>Your verification code is: <strong>%s</strong></p>
                    <p>This code will expire in %d minutes.</p>
                    <p>If you didn't request this code, please ignore this email.</p>
                </body>
                </html>
                """.formatted(otpCode, expiryMinutes);
    }

    private String getDefaultTextTemplate(String otpCode, int expiryMinutes) {
        return "Your verification code is %s. This code expires in %d minutes. If you didn't request this code, please ignore this email."
                .formatted(otpCode, expiryMinutes);
    }

    @Override
    public String toString() {
        return "VerificationTemplateProperties{" +
                "emailSubject='" + emailSubject + '\'' +
                ", emailHtmlTemplate='" + (emailHtmlTemplate != null ? "configured" : "using default") + '\'' +
                ", emailTextTemplate='" + (emailTextTemplate != null ? "configured" : "using default") + '\'' +
                ", smsTemplate='" + (smsTemplate != null ? "configured" : "using default") + '\'' +
                '}';
    }
}
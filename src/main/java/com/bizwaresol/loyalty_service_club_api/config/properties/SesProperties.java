package com.bizwaresol.loyalty_service_club_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ses")
public class SesProperties {
    private String sourceEmail;
    private String region;

    public String getSourceEmail() {
        return sourceEmail;
    }

    public void setSourceEmail(String sourceEmail) {
        this.sourceEmail = sourceEmail;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "SesProperties{" +
                "sourceEmail='" + sourceEmail + '\'' +
                ", region='" + region + '\'' +
                '}';
    }
}
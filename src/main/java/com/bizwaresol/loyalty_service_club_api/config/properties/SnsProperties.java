package com.bizwaresol.loyalty_service_club_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sns")
public class SnsProperties {
    private String topicArn;

    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    @Override
    public String toString() {
        return "SnsProperties{" +
                "topicArn='" + topicArn + '\'' +
                '}';
    }
}
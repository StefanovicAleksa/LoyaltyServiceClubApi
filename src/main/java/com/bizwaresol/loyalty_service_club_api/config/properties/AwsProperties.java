package com.bizwaresol.loyalty_service_club_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String roleArn;

    // Getters and Setters
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    // Security: Override toString to mask sensitive data
    @Override
    public String toString() {
        return "AwsProperties{" +
                "region='" + region + '\'' +
                ", accessKeyId='" + maskValue(accessKeyId) + '\'' +
                ", secretAccessKey='***masked***'" +
                ", roleArn='" + roleArn + '\'' +
                '}';
    }

    private String maskValue(String value) {
        if (value == null || value.length() < 4) return "***";
        return value.substring(0, 4) + "***" + value.substring(value.length() - 2);
    }
}
package com.bizwaresol.loyalty_service_club_api.config;

import com.bizwaresol.loyalty_service_club_api.config.properties.AwsProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.SesProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.SnsProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
@EnableConfigurationProperties({
        AwsProperties.class,
        SnsProperties.class,
        SesProperties.class,
        VerificationProperties.class
})
public class AwsConfig {

    @Bean
    public SnsClient snsClient(AwsProperties awsProperties) {
        return SnsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(createCredentialsProvider(awsProperties))
                .build();
    }

    @Bean
    public SesClient sesClient(AwsProperties awsProperties, SesProperties sesProperties) {
        // Use SES-specific region if different from main AWS region
        String sesRegion = sesProperties.getRegion() != null ?
                sesProperties.getRegion() : awsProperties.getRegion();

        return SesClient.builder()
                .region(Region.of(sesRegion))
                .credentialsProvider(createCredentialsProvider(awsProperties))
                .build();
    }

    /**
     * Creates AWS credentials provider from properties
     * In production, this could be enhanced to support IAM roles
     */
    private StaticCredentialsProvider createCredentialsProvider(AwsProperties awsProperties) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                awsProperties.getAccessKeyId(),
                awsProperties.getSecretAccessKey()
        );
        return StaticCredentialsProvider.create(awsCredentials);
    }
}
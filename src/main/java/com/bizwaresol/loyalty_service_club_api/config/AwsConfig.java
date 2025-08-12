package com.bizwaresol.loyalty_service_club_api.config;

import com.bizwaresol.loyalty_service_club_api.client.aws.ses.SesClient;
import com.bizwaresol.loyalty_service_club_api.client.aws.sns.SnsClient;
import com.bizwaresol.loyalty_service_club_api.config.properties.AwsProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.SesProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.SnsProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationProperties;
import com.bizwaresol.loyalty_service_club_api.config.properties.VerificationTemplateProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@Configuration
@EnableConfigurationProperties({
        AwsProperties.class,
        SnsProperties.class,
        SesProperties.class,
        VerificationProperties.class,
        VerificationTemplateProperties.class
})
public class AwsConfig {

    // ===== AWS SDK CLIENT BEANS (LOW-LEVEL) =====

    @Bean
    public software.amazon.awssdk.services.sns.SnsClient awsSnsClient(AwsProperties awsProperties) {
        return software.amazon.awssdk.services.sns.SnsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(createCredentialsProvider(awsProperties))
                .build();
    }

    @Bean
    public software.amazon.awssdk.services.ses.SesClient awsSesClient(AwsProperties awsProperties, SesProperties sesProperties) {
        String sesRegion = sesProperties.getRegion() != null ?
                sesProperties.getRegion() : awsProperties.getRegion();

        return software.amazon.awssdk.services.ses.SesClient.builder()
                .region(Region.of(sesRegion))
                .credentialsProvider(createCredentialsProvider(awsProperties))
                .build();
    }

    // ===== CUSTOM WRAPPER CLIENT BEANS (APPLICATION-LEVEL) =====

    @Bean
    public SnsClient snsClient(software.amazon.awssdk.services.sns.SnsClient awsSnsClient) {
        return new SnsClient(awsSnsClient);
    }

    @Bean
    public SesClient sesClient(software.amazon.awssdk.services.ses.SesClient awsSesClient) {
        return new SesClient(awsSesClient);
    }

    /**
     * Creates AWS credentials provider from properties.
     * In production, this could be enhanced to support IAM roles.
     */
    private StaticCredentialsProvider createCredentialsProvider(AwsProperties awsProperties) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                awsProperties.getAccessKeyId(),
                awsProperties.getSecretAccessKey()
        );
        return StaticCredentialsProvider.create(awsCredentials);
    }
}

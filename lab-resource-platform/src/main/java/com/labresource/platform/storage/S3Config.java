
package com.labresource.platform.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public ProfileCredentialsProvider s3CredentialsProvider() {
        return ProfileCredentialsProvider.builder()
                .profileName("lab-resource-platform")
                .build();
    }

    @Bean
    public S3Client s3Client(
            @Value("${app.aws.region}") String region,
            ProfileCredentialsProvider credentialsProvider) {

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${app.aws.region}") String region,
            ProfileCredentialsProvider credentialsProvider) {

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}

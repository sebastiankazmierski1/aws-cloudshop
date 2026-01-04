package com.cloudshop.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS SDK Configuration.
 * 
 * Provides S3 clients configured for different environments:
 * - local: Uses LocalStack for local development
 * - dev/prod: Uses real AWS with IAM roles or credentials
 */
@Configuration
@Slf4j
public class AwsConfig {

    @Value("${aws.region:eu-central-1}")
    private String awsRegion;

    /**
     * S3 Client for local development using LocalStack
     */
    @Bean
    @Profile("local")
    public S3Client localS3Client(
            @Value("${aws.s3.endpoint:http://localhost:4566}") String endpoint,
            @Value("${aws.accessKeyId:test}") String accessKeyId,
            @Value("${aws.secretAccessKey:test}") String secretAccessKey
    ) {
        log.info("Configuring S3 client for LOCAL environment with endpoint: {}", endpoint);
        
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .forcePathStyle(true) // Required for LocalStack
                .build();
    }

    /**
     * S3 Presigner for local development using LocalStack
     */
    @Bean
    @Profile("local")
    public S3Presigner localS3Presigner(
            @Value("${aws.s3.endpoint:http://localhost:4566}") String endpoint,
            @Value("${aws.accessKeyId:test}") String accessKeyId,
            @Value("${aws.secretAccessKey:test}") String secretAccessKey
    ) {
        log.info("Configuring S3 presigner for LOCAL environment");
        
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
    }

    /**
     * S3 Client for AWS environments (dev, prod)
     * Uses default credential chain (IAM roles, environment variables, etc.)
     */
    @Bean
    @Profile({"dev", "prod"})
    public S3Client awsS3Client() {
        log.info("Configuring S3 client for AWS environment in region: {}", awsRegion);
        
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * S3 Presigner for AWS environments (dev, prod)
     */
    @Bean
    @Profile({"dev", "prod"})
    public S3Presigner awsS3Presigner() {
        log.info("Configuring S3 presigner for AWS environment in region: {}", awsRegion);
        
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

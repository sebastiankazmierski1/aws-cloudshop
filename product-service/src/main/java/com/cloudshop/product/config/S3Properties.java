package com.cloudshop.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * S3 configuration properties.
 * 
 * Example configuration:
 * aws:
 *   s3:
 *     bucket:
 *       products: cloudshop-product-images-dev
 *     presigned-url-expiration: 3600
 *     max-file-size: 5242880
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
@Validated
public class S3Properties {

    /**
     * Bucket names for different purposes
     */
    private BucketConfig bucket = new BucketConfig();

    /**
     * Presigned URL expiration time in seconds (default: 1 hour)
     */
    @Positive
    private long presignedUrlExpiration = 3600;

    /**
     * Maximum file size in bytes (default: 5MB)
     */
    @Positive
    private long maxFileSize = 5 * 1024 * 1024;

    /**
     * Allowed content types for product images
     */
    private String[] allowedContentTypes = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    };

    @Data
    public static class BucketConfig {
        
        @NotBlank
        private String products = "cloudshop-product-images";
    }
}

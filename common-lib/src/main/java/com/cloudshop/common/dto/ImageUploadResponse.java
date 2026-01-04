package com.cloudshop.common.dto;

import lombok.*;

/**
 * Response DTO for image upload operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {

    /**
     * S3 key where the image is stored
     */
    private String s3Key;

    /**
     * Public URL for the image (if bucket is public)
     */
    private String publicUrl;

    /**
     * Presigned URL for temporary access (if bucket is private)
     */
    private String presignedUrl;

    /**
     * Expiration time for presigned URL in seconds
     */
    private Long presignedUrlExpiresIn;

    /**
     * Original filename
     */
    private String originalFilename;

    /**
     * Content type of the image
     */
    private String contentType;

    /**
     * Size in bytes
     */
    private Long size;
}

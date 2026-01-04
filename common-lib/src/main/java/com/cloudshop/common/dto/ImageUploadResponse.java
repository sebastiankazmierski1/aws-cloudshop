package com.cloudshop.common.dto;

/**
 * Response DTO for image upload operations.
 * 
 * Java 25: Converted to Record for immutable data carrier.
 * Records provide: constructor, getters, equals, hashCode, toString automatically.
 */
public record ImageUploadResponse(
        /**
         * S3 key where the image is stored
         */
        String s3Key,

        /**
         * Public URL for the image (if bucket is public)
         */
        String publicUrl,

        /**
         * Presigned URL for temporary access (if bucket is private)
         */
        String presignedUrl,

        /**
         * Expiration time for presigned URL in seconds
         */
        Long presignedUrlExpiresIn,

        /**
         * Original filename
         */
        String originalFilename,

        /**
         * Content type of the image
         */
        String contentType,

        /**
         * Size in bytes
         */
        Long size
) {
    /**
     * Builder pattern for backwards compatibility with existing code.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String s3Key;
        private String publicUrl;
        private String presignedUrl;
        private Long presignedUrlExpiresIn;
        private String originalFilename;
        private String contentType;
        private Long size;

        public Builder s3Key(String s3Key) {
            this.s3Key = s3Key;
            return this;
        }

        public Builder publicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
            return this;
        }

        public Builder presignedUrl(String presignedUrl) {
            this.presignedUrl = presignedUrl;
            return this;
        }

        public Builder presignedUrlExpiresIn(Long presignedUrlExpiresIn) {
            this.presignedUrlExpiresIn = presignedUrlExpiresIn;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public ImageUploadResponse build() {
            return new ImageUploadResponse(
                    s3Key, publicUrl, presignedUrl, presignedUrlExpiresIn,
                    originalFilename, contentType, size
            );
        }
    }
}

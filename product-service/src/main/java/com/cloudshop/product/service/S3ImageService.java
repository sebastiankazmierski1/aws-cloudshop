package com.cloudshop.product.service;

import com.cloudshop.common.dto.ImageUploadResponse;
import com.cloudshop.common.exception.InvalidFileException;
import com.cloudshop.common.exception.S3OperationException;
import com.cloudshop.product.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service for managing product images in AWS S3.
 * 
 * Handles:
 * - Image upload with validation
 * - Image deletion
 * - Presigned URL generation for secure access
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ImageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * Upload a product image to S3
     * 
     * @param productId The product ID (used for organizing files)
     * @param file The image file to upload
     * @return Upload response with S3 key and URLs
     */
    public ImageUploadResponse uploadProductImage(Long productId, MultipartFile file) {
        // Validate file
        validateFile(file);

        // Generate unique key
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String key = generateS3Key(productId, extension);

        log.info("Uploading image for product {} with key: {}", productId, key);

        try {
            // Build put request
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket().getProducts())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                            "product-id", String.valueOf(productId),
                            "original-filename", originalFilename != null ? originalFilename : "unknown"
                    ))
                    .build();

            // Upload file
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Successfully uploaded image: {}", key);

            // Generate presigned URL
            String presignedUrl = generatePresignedUrl(key);

            return ImageUploadResponse.builder()
                    .s3Key(key)
                    .presignedUrl(presignedUrl)
                    .presignedUrlExpiresIn(s3Properties.getPresignedUrlExpiration())
                    .originalFilename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("Failed to read file for upload: {}", originalFilename, e);
            throw S3OperationException.uploadFailed(originalFilename, e);
        } catch (S3Exception e) {
            log.error("S3 error during upload: {}", e.getMessage(), e);
            throw S3OperationException.uploadFailed(originalFilename, e);
        }
    }

    /**
     * Delete a product image from S3
     * 
     * @param s3Key The S3 key of the image to delete
     */
    public void deleteImage(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            log.warn("Attempted to delete image with null or blank key");
            return;
        }

        log.info("Deleting image with key: {}", s3Key);

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket().getProducts())
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted image: {}", s3Key);

        } catch (S3Exception e) {
            log.error("Failed to delete image: {}", s3Key, e);
            throw S3OperationException.deleteFailed(s3Key, e);
        }
    }

    /**
     * Generate a presigned URL for secure image access
     * 
     * @param s3Key The S3 key of the image
     * @return Presigned URL valid for configured duration
     */
    public String generatePresignedUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket().getProducts())
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(s3Properties.getPresignedUrlExpiration()))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            
            return presignedRequest.url().toString();

        } catch (S3Exception e) {
            log.error("Failed to generate presigned URL for key: {}", s3Key, e);
            return null;
        }
    }

    /**
     * Check if an image exists in S3
     * 
     * @param s3Key The S3 key to check
     * @return true if the image exists
     */
    public boolean imageExists(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return false;
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket().getProducts())
                    .key(s3Key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.warn("Error checking if image exists: {}", s3Key, e);
            return false;
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file == null || file.isEmpty()) {
            throw InvalidFileException.emptyFile();
        }

        // Check file size
        if (file.getSize() > s3Properties.getMaxFileSize()) {
            throw InvalidFileException.fileTooLarge(file.getSize(), s3Properties.getMaxFileSize());
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw InvalidFileException.invalidContentType(contentType);
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.contains("..")) {
            throw InvalidFileException.invalidFilename(filename);
        }
    }

    /**
     * Check if content type is allowed
     */
    private boolean isAllowedContentType(String contentType) {
        return Arrays.asList(s3Properties.getAllowedContentTypes()).contains(contentType);
    }

    /**
     * Generate S3 key for product image
     * Format: products/{productId}/{uuid}.{extension}
     */
    private String generateS3Key(Long productId, String extension) {
        return String.format("products/%d/%s.%s",
                productId,
                UUID.randomUUID().toString(),
                extension);
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}

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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing product images in AWS S3.
 * 
 * Java 25 Features Used:
 * - Records for internal data structures
 * - Pattern matching in validation
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
     * Java 25: Record for encapsulating file metadata - immutable and clear.
     */
    private record FileMetadata(
            String originalFilename,
            String extension,
            String contentType,
            long size
    ) {
        static FileMetadata from(MultipartFile file) {
            String filename = file.getOriginalFilename();
            return new FileMetadata(
                    filename,
                    extractExtension(filename),
                    file.getContentType(),
                    file.getSize()
            );
        }

        private static String extractExtension(String filename) {
            if (filename == null || !filename.contains(".")) {
                return "jpg";
            }
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
    }

    /**
     * Upload a product image to S3
     * 
     * @param productId The product ID (used for organizing files)
     * @param file The image file to upload
     * @return Upload response with S3 key and URLs
     */
    public ImageUploadResponse uploadProductImage(Long productId, MultipartFile file) {
        validateFile(file);

        FileMetadata metadata = FileMetadata.from(file);
        String key = generateS3Key(productId, metadata.extension());

        log.info("Uploading image for product {} with key: {}", productId, key);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket().getProducts())
                    .key(key)
                    .contentType(metadata.contentType())
                    .contentLength(metadata.size())
                    .metadata(Map.of(
                            "product-id", String.valueOf(productId),
                            "original-filename", metadata.originalFilename() != null 
                                    ? metadata.originalFilename() 
                                    : "unknown"
                    ))
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(
                    file.getInputStream(), 
                    metadata.size()
            ));

            log.info("Successfully uploaded image: {}", key);

            String presignedUrl = generatePresignedUrl(key);

            return ImageUploadResponse.builder()
                    .s3Key(key)
                    .presignedUrl(presignedUrl)
                    .presignedUrlExpiresIn(s3Properties.getPresignedUrlExpiration())
                    .originalFilename(metadata.originalFilename())
                    .contentType(metadata.contentType())
                    .size(metadata.size())
                    .build();

        } catch (IOException e) {
            log.error("Failed to read file for upload: {}", metadata.originalFilename(), e);
            throw S3OperationException.uploadFailed(metadata.originalFilename(), e);
        } catch (S3Exception e) {
            log.error("S3 error during upload: {}", e.getMessage(), e);
            throw S3OperationException.uploadFailed(metadata.originalFilename(), e);
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
            return "";
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
            return "";
        }
    }

    /**
     * Validate uploaded file.
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
        if (!isAllowedContentType(contentType)) {
            throw InvalidFileException.invalidContentType(contentType);
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (!isValidFilename(filename)) {
            throw InvalidFileException.invalidFilename(filename);
        }
    }

    /**
     * Check if content type is allowed.
     */
    private boolean isAllowedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        return Arrays.stream(s3Properties.getAllowedContentTypes())
                .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));
    }

    /**
     * Validate filename for security.
     */
    private boolean isValidFilename(String filename) {
        return filename != null 
                && !filename.isBlank() 
                && !filename.contains("..")
                && !filename.contains("/")
                && !filename.contains("\\");
    }

    /**
     * Generate S3 key for product image.
     */
    private String generateS3Key(Long productId, String extension) {
        return "products/%d/%s.%s".formatted(productId, UUID.randomUUID(), extension);
    }
}

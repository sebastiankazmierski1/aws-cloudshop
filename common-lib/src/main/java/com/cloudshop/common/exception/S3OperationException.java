package com.cloudshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when S3 operations fail.
 * 
 * Java 25: Part of sealed exception hierarchy.
 * Uses sealed interface with records for type-safe operation tracking.
 */
public non-sealed class S3OperationException extends CloudShopException {

    private final S3Operation operation;

    /**
     * Java 25: Sealed interface with records for type-safe S3 operations.
     */
    public sealed interface S3Operation {
        record Upload(String filename) implements S3Operation {}
        record Download(String key) implements S3Operation {}
        record Delete(String key) implements S3Operation {}
        record PresignUrl(String key) implements S3Operation {}
    }

    public S3OperationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "S3_OPERATION_FAILED");
        this.operation = null;
    }

    public S3OperationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "S3_OPERATION_FAILED");
        this.operation = null;
    }

    public S3OperationException(String message, Throwable cause, S3Operation operation) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "S3_OPERATION_FAILED");
        this.operation = operation;
    }

    public S3Operation getOperation() {
        return operation;
    }

    /**
     * Java 25: Pattern matching with switch expression and record patterns.
     */
    public String getOperationDetails() {
        if (operation == null) {
            return "Unknown S3 operation";
        }
        
        return switch (operation) {
            case S3Operation.Upload(var filename) -> "Upload failed for file: " + filename;
            case S3Operation.Download(var key) -> "Download failed for key: " + key;
            case S3Operation.Delete(var key) -> "Delete failed for key: " + key;
            case S3Operation.PresignUrl(var key) -> "Presigned URL generation failed for key: " + key;
        };
    }

    // Static factory methods
    public static S3OperationException uploadFailed(String filename, Throwable cause) {
        return new S3OperationException(
            "Failed to upload file: " + filename, 
            cause,
            new S3Operation.Upload(filename)
        );
    }

    public static S3OperationException downloadFailed(String key, Throwable cause) {
        return new S3OperationException(
            "Failed to download file with key: " + key, 
            cause,
            new S3Operation.Download(key)
        );
    }

    public static S3OperationException deleteFailed(String key, Throwable cause) {
        return new S3OperationException(
            "Failed to delete file with key: " + key, 
            cause,
            new S3Operation.Delete(key)
        );
    }

    public static S3OperationException presignFailed(String key, Throwable cause) {
        return new S3OperationException(
            "Failed to generate presigned URL for key: " + key, 
            cause,
            new S3Operation.PresignUrl(key)
        );
    }
}

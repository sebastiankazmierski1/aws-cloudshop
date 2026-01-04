package com.cloudshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when S3 operations fail.
 */
public class S3OperationException extends CloudShopException {

    public S3OperationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "S3_OPERATION_FAILED");
    }

    public S3OperationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "S3_OPERATION_FAILED");
    }

    public static S3OperationException uploadFailed(String filename, Throwable cause) {
        return new S3OperationException("Failed to upload file: " + filename, cause);
    }

    public static S3OperationException downloadFailed(String key, Throwable cause) {
        return new S3OperationException("Failed to download file with key: " + key, cause);
    }

    public static S3OperationException deleteFailed(String key, Throwable cause) {
        return new S3OperationException("Failed to delete file with key: " + key, cause);
    }
}

package com.cloudshop.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for CloudShop application.
 * 
 * Java 25: Made sealed to restrict exception hierarchy.
 * This ensures all CloudShop exceptions are known at compile time,
 * enabling exhaustive pattern matching in exception handlers.
 */
@Getter
public sealed class CloudShopException extends RuntimeException 
        permits ResourceNotFoundException, DuplicateResourceException, 
                InvalidFileException, S3OperationException {

    private final HttpStatus status;
    private final String errorCode;

    public CloudShopException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CloudShopException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CloudShopException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public CloudShopException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CloudShopException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    /**
     * Formatted error representation.
     */
    public String toFormattedString() {
        return "[%s] %s (HTTP %d)".formatted(errorCode, getMessage(), status.value());
    }
}

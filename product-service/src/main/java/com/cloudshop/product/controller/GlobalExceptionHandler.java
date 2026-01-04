package com.cloudshop.product.controller;

import com.cloudshop.common.dto.ApiResponse;
import com.cloudshop.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Global exception handler for REST API.
 * 
 * Java 25: Uses pattern matching with switch expressions for sealed exceptions.
 * Exhaustive handling of CloudShopException hierarchy.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Java 25: Pattern matching with sealed class hierarchy.
     * The switch is exhaustive - compiler ensures all subtypes are handled.
     */
    @ExceptionHandler(CloudShopException.class)
    public ResponseEntity<ApiResponse<Void>> handleCloudShopException(CloudShopException ex) {
        // Note: ProductNotFoundException extends ResourceNotFoundException, so it must come first
        // ResourceNotFoundException is non-sealed, requiring a default case for exhaustiveness
        return switch (ex) {
            case ProductNotFoundException e -> {
                log.warn("Product not found: {}", e.getUserMessage());
                yield ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getUserMessage()));
            }
            case ResourceNotFoundException e -> {
                log.warn("Resource not found: {}", e.getMessage());
                yield ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(e.getMessage()));
            }
            case DuplicateResourceException e -> {
                log.warn("Duplicate resource: {}", e.getMessage());
                yield ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage()));
            }
            case InvalidFileException e -> {
                log.warn("Invalid file: {}", e.getDetailedMessage());
                yield ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(e.getDetailedMessage()));
            }
            case S3OperationException e -> {
                log.error("S3 operation failed: {} - {}", e.getOperationDetails(), e.getMessage(), e);
                yield ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("File operation failed. Please try again."));
            }
            // Default case required because ResourceNotFoundException is non-sealed
            // and may have other subclasses at runtime
            default -> {
                log.error("Unhandled CloudShop exception: {}", ex.getMessage(), ex);
                yield ResponseEntity
                        .status(ex.getStatus())
                        .body(ApiResponse.error(ex.getMessage()));
            }
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("File size exceeds maximum allowed limit"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }

    private String formatFieldError(FieldError error) {
        return "%s: %s".formatted(error.getField(), error.getDefaultMessage());
    }
}

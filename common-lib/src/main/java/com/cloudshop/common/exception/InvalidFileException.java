package com.cloudshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when file validation fails.
 * 
 * Java 25: Part of sealed exception hierarchy.
 * Uses sealed interface with records for type-safe validation errors.
 */
public non-sealed class InvalidFileException extends CloudShopException {

    private final FileValidationError validationError;

    /**
     * Java 25: Sealed interface for type-safe validation errors with record patterns.
     */
    public sealed interface FileValidationError {
        record EmptyFile() implements FileValidationError {}
        record InvalidContentType(String contentType) implements FileValidationError {}
        record FileTooLarge(long actualSize, long maxSize) implements FileValidationError {}
        record InvalidFilename(String filename) implements FileValidationError {}
    }

    public InvalidFileException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
        this.validationError = null;
    }

    public InvalidFileException(String message, FileValidationError error) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
        this.validationError = error;
    }

    public FileValidationError getValidationError() {
        return validationError;
    }

    /**
     * Java 25: Pattern matching with switch expression and record patterns.
     */
    public String getDetailedMessage() {
        if (validationError == null) {
            return getMessage();
        }
        
        return switch (validationError) {
            case FileValidationError.EmptyFile() -> "The uploaded file is empty";
            case FileValidationError.InvalidContentType(var type) -> 
                "Invalid content type '%s'. Only images are allowed.".formatted(type);
            case FileValidationError.FileTooLarge(var actual, var max) -> 
                "File size %s exceeds maximum %s".formatted(formatBytes(actual), formatBytes(max));
            case FileValidationError.InvalidFilename(var name) -> 
                "Invalid filename: '%s'".formatted(name);
        };
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    // Static factory methods
    public static InvalidFileException emptyFile() {
        return new InvalidFileException("File is empty", new FileValidationError.EmptyFile());
    }

    public static InvalidFileException invalidContentType(String contentType) {
        return new InvalidFileException(
            "Invalid file type: %s. Only images are allowed.".formatted(contentType),
            new FileValidationError.InvalidContentType(contentType)
        );
    }

    public static InvalidFileException fileTooLarge(long size, long maxSize) {
        return new InvalidFileException(
            "File size (%d bytes) exceeds maximum allowed size (%d bytes)".formatted(size, maxSize),
            new FileValidationError.FileTooLarge(size, maxSize)
        );
    }

    public static InvalidFileException invalidFilename(String filename) {
        return new InvalidFileException(
            "Invalid filename: %s".formatted(filename),
            new FileValidationError.InvalidFilename(filename)
        );
    }
}

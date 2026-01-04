package com.cloudshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when file validation fails.
 */
public class InvalidFileException extends CloudShopException {

    public InvalidFileException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
    }

    public static InvalidFileException emptyFile() {
        return new InvalidFileException("File is empty");
    }

    public static InvalidFileException invalidContentType(String contentType) {
        return new InvalidFileException("Invalid file type: " + contentType + ". Only images are allowed.");
    }

    public static InvalidFileException fileTooLarge(long size, long maxSize) {
        return new InvalidFileException(
            String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", size, maxSize)
        );
    }

    public static InvalidFileException invalidFilename(String filename) {
        return new InvalidFileException("Invalid filename: " + filename);
    }
}

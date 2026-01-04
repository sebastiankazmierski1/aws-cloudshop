package com.cloudshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends CloudShopException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue),
            HttpStatus.CONFLICT,
            "DUPLICATE_RESOURCE"
        );
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}

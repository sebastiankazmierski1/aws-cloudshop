package com.cloudshop.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for CloudShop application.
 */
@Getter
public class CloudShopException extends RuntimeException {

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
}

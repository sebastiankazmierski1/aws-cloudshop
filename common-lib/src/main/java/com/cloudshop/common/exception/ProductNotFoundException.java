package com.cloudshop.common.exception;

/**
 * Exception thrown when a product is not found.
 */
public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(Long id) {
        super("Product", "id", id);
    }

    public ProductNotFoundException(String sku) {
        super("Product", "sku", sku);
    }
}

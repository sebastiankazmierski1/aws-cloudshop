package com.cloudshop.common.exception;

/**
 * Exception thrown when a product is not found.
 * 
 * Java 25: Pattern-friendly design with enhanced methods.
 */
public class ProductNotFoundException extends ResourceNotFoundException {

    private final Long productId;
    private final String productSku;

    public ProductNotFoundException(Long id) {
        super("Product", "id", id);
        this.productId = id;
        this.productSku = null;
    }

    public ProductNotFoundException(String sku) {
        super("Product", "sku", sku);
        this.productId = null;
        this.productSku = sku;
    }

    /**
     * Check which identifier was used for lookup.
     */
    public boolean wasSearchedById() {
        return productId != null;
    }

    public boolean wasSearchedBySku() {
        return productSku != null;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductSku() {
        return productSku;
    }

    /**
     * User-friendly message for API responses.
     */
    public String getUserMessage() {
        if (productId != null) {
            return "Product with ID %d was not found. Please check the ID and try again.".formatted(productId);
        } else {
            return "Product with SKU '%s' was not found. Please verify the SKU.".formatted(productSku);
        }
    }
}

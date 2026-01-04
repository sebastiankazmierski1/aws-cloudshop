package com.cloudshop.common.entity;

/**
 * Product lifecycle status.
 */
public enum ProductStatus {
    
    /**
     * Product is being prepared, not visible to customers
     */
    DRAFT,
    
    /**
     * Product is active and available for purchase
     */
    ACTIVE,
    
    /**
     * Product is temporarily unavailable
     */
    INACTIVE,
    
    /**
     * Product has been discontinued
     */
    DISCONTINUED,
    
    /**
     * Product is archived (soft delete)
     */
    ARCHIVED
}

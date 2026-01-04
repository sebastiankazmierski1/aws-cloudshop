package com.cloudshop.common.entity;

/**
 * Product lifecycle status.
 * 
 * Java 25: Enhanced enum with pattern matching friendly design.
 * Can be used with switch expressions for exhaustive handling.
 */
public enum ProductStatus {
    
    /**
     * Product is being prepared, not visible to customers
     */
    DRAFT("Draft", false, false),
    
    /**
     * Product is active and available for purchase
     */
    ACTIVE("Active", true, true),
    
    /**
     * Product is temporarily unavailable
     */
    INACTIVE("Inactive", true, false),
    
    /**
     * Product has been discontinued
     */
    DISCONTINUED("Discontinued", true, false),
    
    /**
     * Product is archived (soft delete)
     */
    ARCHIVED("Archived", false, false);

    private final String displayName;
    private final boolean visibleToCustomers;
    private final boolean purchasable;

    ProductStatus(String displayName, boolean visibleToCustomers, boolean purchasable) {
        this.displayName = displayName;
        this.visibleToCustomers = visibleToCustomers;
        this.purchasable = purchasable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVisibleToCustomers() {
        return visibleToCustomers;
    }

    public boolean isPurchasable() {
        return purchasable;
    }

    /**
     * Java 25: Pattern matching with switch expression for status descriptions.
     * Exhaustive matching ensures all cases are handled.
     */
    public String getDescription() {
        return switch (this) {
            case DRAFT -> "Product is being prepared and not yet visible to customers";
            case ACTIVE -> "Product is live and available for purchase";
            case INACTIVE -> "Product is temporarily unavailable but may return";
            case DISCONTINUED -> "Product has been permanently discontinued";
            case ARCHIVED -> "Product has been archived and is no longer accessible";
        };
    }

    /**
     * Check if transition to target status is valid.
     * Java 25: Using switch expression with pattern matching.
     */
    public boolean canTransitionTo(ProductStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == INACTIVE || target == DISCONTINUED || target == ARCHIVED;
            case INACTIVE -> target == ACTIVE || target == DISCONTINUED || target == ARCHIVED;
            case DISCONTINUED -> target == ARCHIVED;
            case ARCHIVED -> false; // Cannot transition from archived
        };
    }
}

package com.cloudshop.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing items in the catalog.
 * Stored in PostgreSQL via RDS.
 * 
 * Java 25 Features Used:
 * - Nested Records for immutable data snapshots
 * - Pattern matching ready design
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_sku", columnList = "sku", unique = true),
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "s3_image_key", length = 255)
    private String s3ImageKey;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false)
    private boolean featured = false;

    @Column(precision = 3, scale = 2)
    private BigDecimal weight;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * Check if product is in stock
     */
    public boolean isInStock() {
        return quantity > 0;
    }

    /**
     * Check if product is on sale
     */
    public boolean isOnSale() {
        return compareAtPrice != null && 
               price != null && 
               compareAtPrice.compareTo(price) > 0;
    }

    /**
     * Calculate discount percentage.
     */
    public BigDecimal getDiscountPercentage() {
        if (!isOnSale()) {
            return BigDecimal.ZERO;
        }
        
        return compareAtPrice.subtract(price)
                .divide(compareAtPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get savings amount when on sale.
     */
    public BigDecimal getSavingsAmount() {
        if (!isOnSale()) {
            return BigDecimal.ZERO;
        }
        return compareAtPrice.subtract(price);
    }

    /**
     * Reserve stock for an order.
     */
    public void reserveStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (quantity < amount) {
            throw new IllegalStateException(
                "Insufficient stock for product %s. Available: %d, Requested: %d"
                    .formatted(sku, quantity, amount)
            );
        }
        this.quantity -= amount;
    }

    /**
     * Release reserved stock (e.g., cancelled order)
     */
    public void releaseStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.quantity += amount;
    }

    /**
     * Check if product can be purchased.
     * Uses enhanced enum method from ProductStatus.
     */
    public boolean canBePurchased() {
        return status.isPurchasable() && isInStock();
    }

    /**
     * Check if product is visible to customers.
     * Uses enhanced enum method from ProductStatus.
     */
    public boolean isVisibleToCustomers() {
        return status.isVisibleToCustomers();
    }

    /**
     * Java 25: Record for stock info - immutable snapshot.
     */
    public record StockInfo(int quantity, boolean inStock, boolean lowStock) {
        private static final int LOW_STOCK_THRESHOLD = 10;
        
        public static StockInfo from(Product product) {
            return new StockInfo(
                    product.quantity,
                    product.isInStock(),
                    product.quantity > 0 && product.quantity <= LOW_STOCK_THRESHOLD
            );
        }
    }

    /**
     * Get stock information as immutable record.
     */
    public StockInfo getStockInfo() {
        return StockInfo.from(this);
    }

    /**
     * Java 25: Record for pricing info - immutable snapshot.
     */
    public record PricingInfo(
            BigDecimal currentPrice,
            BigDecimal originalPrice,
            boolean onSale,
            BigDecimal discountPercentage,
            BigDecimal savingsAmount
    ) {
        public static PricingInfo from(Product product) {
            return new PricingInfo(
                    product.price,
                    product.compareAtPrice,
                    product.isOnSale(),
                    product.getDiscountPercentage(),
                    product.getSavingsAmount()
            );
        }
    }

    /**
     * Get pricing information as immutable record.
     */
    public PricingInfo getPricingInfo() {
        return PricingInfo.from(this);
    }
}

package com.cloudshop.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing items in the catalog.
 * Stored in PostgreSQL via RDS.
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
     * Reserve stock for an order
     */
    public void reserveStock(int amount) {
        if (quantity < amount) {
            throw new IllegalStateException(
                "Insufficient stock for product " + sku + ". Available: " + quantity + ", Requested: " + amount
            );
        }
        this.quantity -= amount;
    }

    /**
     * Release reserved stock (e.g., cancelled order)
     */
    public void releaseStock(int amount) {
        this.quantity += amount;
    }
}

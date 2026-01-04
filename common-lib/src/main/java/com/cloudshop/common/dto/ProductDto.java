package com.cloudshop.common.dto;

import com.cloudshop.common.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product Data Transfer Object for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private Integer quantity;
    private String category;
    private String brand;
    private ProductStatus status;
    private String imageUrl;
    private List<String> tags;
    private Boolean featured;
    private BigDecimal weight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private Boolean inStock;
    private Boolean onSale;
    private BigDecimal discountPercentage;

    /**
     * Calculate discount percentage if on sale
     */
    public BigDecimal getDiscountPercentage() {
        if (onSale != null && onSale && compareAtPrice != null && price != null) {
            return compareAtPrice.subtract(price)
                    .divide(compareAtPrice, 2, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return null;
    }
}

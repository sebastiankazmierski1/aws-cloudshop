package com.cloudshop.product.repository;

import com.cloudshop.common.entity.Product;
import com.cloudshop.common.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity operations.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find product by SKU
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if product with SKU exists
     */
    boolean existsBySku(String sku);

    /**
     * Find all products by status
     */
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * Find all products by category
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * Find all products by brand
     */
    Page<Product> findByBrand(String brand, Pageable pageable);

    /**
     * Find featured products
     */
    List<Product> findByFeaturedTrueAndStatus(ProductStatus status);

    /**
     * Find products with low stock
     */
    @Query("SELECT p FROM Product p WHERE p.quantity <= :threshold AND p.status = :status")
    List<Product> findLowStockProducts(
            @Param("threshold") int threshold,
            @Param("status") ProductStatus status
    );

    /**
     * Search products by name or description
     */
    @Query("""
            SELECT p FROM Product p 
            WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) 
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))
            AND p.status = :status
            """)
    Page<Product> searchProducts(
            @Param("query") String query,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    /**
     * Find products by category and status with pagination
     */
    @Query("""
            SELECT p FROM Product p 
            WHERE (:category IS NULL OR p.category = :category)
            AND (:brand IS NULL OR p.brand = :brand)
            AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> findByFilters(
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    /**
     * Get distinct categories
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();

    /**
     * Get distinct brands
     */
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findAllBrands();

    /**
     * Update product status
     */
    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") ProductStatus status);

    /**
     * Update product quantity
     */
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :delta WHERE p.id = :id AND p.quantity + :delta >= 0")
    int updateQuantity(@Param("id") Long id, @Param("delta") int delta);
}

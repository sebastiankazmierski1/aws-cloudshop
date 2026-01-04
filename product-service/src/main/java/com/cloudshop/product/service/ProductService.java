package com.cloudshop.product.service;

import com.cloudshop.common.dto.*;
import com.cloudshop.common.entity.Product;
import com.cloudshop.common.entity.ProductStatus;
import com.cloudshop.common.exception.DuplicateResourceException;
import com.cloudshop.common.exception.ProductNotFoundException;
import com.cloudshop.product.mapper.ProductMapper;
import com.cloudshop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for product management operations.
 * 
 * Handles:
 * - Product CRUD operations
 * - Image management via S3
 * - Product search and filtering
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final S3ImageService s3ImageService;

    /**
     * Get product by ID
     */
    public ProductDto getProductById(Long id) {
        Product product = findProductById(id);
        ProductDto dto = productMapper.toDto(product);
        
        // Add presigned URL for image
        if (product.getS3ImageKey() != null) {
            dto.setImageUrl(s3ImageService.generatePresignedUrl(product.getS3ImageKey()));
        }
        
        return dto;
    }

    /**
     * Get product by SKU
     */
    public ProductDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        
        ProductDto dto = productMapper.toDto(product);
        
        if (product.getS3ImageKey() != null) {
            dto.setImageUrl(s3ImageService.generatePresignedUrl(product.getS3ImageKey()));
        }
        
        return dto;
    }

    /**
     * Get all products with pagination
     */
    public PagedResponse<ProductDto> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return PagedResponse.from(products, this::mapWithImageUrl);
    }

    /**
     * Get products by status with pagination
     */
    public PagedResponse<ProductDto> getProductsByStatus(ProductStatus status, Pageable pageable) {
        Page<Product> products = productRepository.findByStatus(status, pageable);
        return PagedResponse.from(products, this::mapWithImageUrl);
    }

    /**
     * Search products
     */
    public PagedResponse<ProductDto> searchProducts(String query, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(query, ProductStatus.ACTIVE, pageable);
        return PagedResponse.from(products, this::mapWithImageUrl);
    }

    /**
     * Get products by filters
     */
    public PagedResponse<ProductDto> getProductsByFilters(
            String category,
            String brand,
            ProductStatus status,
            Pageable pageable
    ) {
        Page<Product> products = productRepository.findByFilters(category, brand, status, pageable);
        return PagedResponse.from(products, this::mapWithImageUrl);
    }

    /**
     * Get featured products
     */
    public List<ProductDto> getFeaturedProducts() {
        List<Product> products = productRepository.findByFeaturedTrueAndStatus(ProductStatus.ACTIVE);
        return products.stream()
                .map(this::mapWithImageUrl)
                .toList();
    }

    /**
     * Get low stock products
     */
    public List<ProductDto> getLowStockProducts(int threshold) {
        List<Product> products = productRepository.findLowStockProducts(threshold, ProductStatus.ACTIVE);
        return productMapper.toDtoList(products);
    }

    /**
     * Get all categories
     */
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    /**
     * Get all brands
     */
    public List<String> getAllBrands() {
        return productRepository.findAllBrands();
    }

    /**
     * Create a new product
     */
    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        // Check for duplicate SKU
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Product product = productMapper.toEntity(request);
        product = productRepository.save(product);

        log.info("Created product with ID: {}", product.getId());
        return productMapper.toDto(product);
    }

    /**
     * Update an existing product
     */
    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = findProductById(id);
        productMapper.updateEntityFromRequest(request, product);
        product = productRepository.save(product);

        log.info("Updated product with ID: {}", id);
        return mapWithImageUrl(product);
    }

    /**
     * Update product status
     */
    @Transactional
    public ProductDto updateProductStatus(Long id, ProductStatus status) {
        log.info("Updating status of product {} to {}", id, status);

        Product product = findProductById(id);
        product.setStatus(status);
        product = productRepository.save(product);

        return productMapper.toDto(product);
    }

    /**
     * Upload product image
     */
    @Transactional
    public ImageUploadResponse uploadProductImage(Long id, MultipartFile file) {
        log.info("Uploading image for product: {}", id);

        Product product = findProductById(id);

        // Delete old image if exists
        if (product.getS3ImageKey() != null) {
            log.info("Deleting old image: {}", product.getS3ImageKey());
            s3ImageService.deleteImage(product.getS3ImageKey());
        }

        // Upload new image
        ImageUploadResponse response = s3ImageService.uploadProductImage(id, file);

        // Update product with new image key
        product.setS3ImageKey(response.getS3Key());
        product.setImageUrl(response.getPresignedUrl());
        productRepository.save(product);

        return response;
    }

    /**
     * Delete product image
     */
    @Transactional
    public void deleteProductImage(Long id) {
        log.info("Deleting image for product: {}", id);

        Product product = findProductById(id);

        if (product.getS3ImageKey() != null) {
            s3ImageService.deleteImage(product.getS3ImageKey());
            product.setS3ImageKey(null);
            product.setImageUrl(null);
            productRepository.save(product);
        }
    }

    /**
     * Delete a product (soft delete - archive)
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting (archiving) product with ID: {}", id);

        Product product = findProductById(id);
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);

        log.info("Archived product with ID: {}", id);
    }

    /**
     * Hard delete a product
     */
    @Transactional
    public void hardDeleteProduct(Long id) {
        log.info("Hard deleting product with ID: {}", id);

        Product product = findProductById(id);

        // Delete image from S3 first
        if (product.getS3ImageKey() != null) {
            s3ImageService.deleteImage(product.getS3ImageKey());
        }

        productRepository.delete(product);
        log.info("Hard deleted product with ID: {}", id);
    }

    /**
     * Update product quantity
     */
    @Transactional
    public ProductDto updateQuantity(Long id, int delta) {
        log.info("Updating quantity of product {} by {}", id, delta);

        int updated = productRepository.updateQuantity(id, delta);
        if (updated == 0) {
            throw new IllegalStateException("Cannot update quantity - would result in negative stock");
        }

        return getProductById(id);
    }

    // ==================== Private Helper Methods ====================

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductDto mapWithImageUrl(Product product) {
        ProductDto dto = productMapper.toDto(product);
        if (product.getS3ImageKey() != null) {
            dto.setImageUrl(s3ImageService.generatePresignedUrl(product.getS3ImageKey()));
        }
        return dto;
    }
}

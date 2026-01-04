package com.cloudshop.product.controller;

import com.cloudshop.common.dto.*;
import com.cloudshop.common.entity.ProductStatus;
import com.cloudshop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for Product operations.
 * 
 * Provides endpoints for:
 * - Product CRUD operations
 * - Image upload/delete
 * - Product search and filtering
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product catalog management API")
public class ProductController {

    private final ProductService productService;

    // ==================== GET Endpoints ====================

    @GetMapping
    @Operation(
            summary = "Get all products",
            description = "Retrieve paginated list of all products with optional filtering"
    )
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("Getting products - category: {}, brand: {}, status: {}", category, brand, status);
        
        PagedResponse<ProductDto> products;
        
        if (category != null || brand != null || status != null) {
            products = productService.getProductsByFilters(category, brand, status, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            )
    })
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        log.info("Getting product by ID: {}", id);
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ApiResponse<ProductDto>> getProductBySku(
            @Parameter(description = "Product SKU") @PathVariable String sku
    ) {
        log.info("Getting product by SKU: {}", sku);
        ProductDto product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name or description")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> searchProducts(
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("Searching products with query: {}", q);
        PagedResponse<ProductDto> products = productService.searchProducts(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getFeaturedProducts() {
        log.info("Getting featured products");
        List<ProductDto> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getLowStockProducts(
            @Parameter(description = "Stock threshold") @RequestParam(defaultValue = "10") int threshold
    ) {
        log.info("Getting low stock products with threshold: {}", threshold);
        List<ProductDto> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        List<String> categories = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/brands")
    @Operation(summary = "Get all brands")
    public ResponseEntity<ApiResponse<List<String>>> getAllBrands() {
        List<String> brands = productService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    // ==================== POST Endpoints ====================

    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Product with this SKU already exists"
            )
    })
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        log.info("Creating product with SKU: {}", request.getSku());
        ProductDto product = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product image", description = "Upload an image for a product to AWS S3")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Image uploaded successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid file"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"
            )
    })
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadProductImage(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "Image file (JPEG, PNG, GIF, WebP)", 
                      content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Uploading image for product: {}, filename: {}", id, file.getOriginalFilename());
        ImageUploadResponse response = productService.uploadProductImage(id, file);
        return ResponseEntity.ok(ApiResponse.success(response, "Image uploaded successfully"));
    }

    // ==================== PUT/PATCH Endpoints ====================

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        log.info("Updating product: {}", id);
        ProductDto product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product status")
    public ResponseEntity<ApiResponse<ProductDto>> updateProductStatus(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam ProductStatus status
    ) {
        log.info("Updating status of product {} to {}", id, status);
        ProductDto product = productService.updateProductStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(product, "Status updated successfully"));
    }

    @PatchMapping("/{id}/quantity")
    @Operation(summary = "Update product quantity", description = "Add or subtract from current quantity")
    public ResponseEntity<ApiResponse<ProductDto>> updateProductQuantity(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "Quantity change (positive to add, negative to subtract)") 
            @RequestParam int delta
    ) {
        log.info("Updating quantity of product {} by {}", id, delta);
        ProductDto product = productService.updateQuantity(id, delta);
        return ResponseEntity.ok(ApiResponse.success(product, "Quantity updated successfully"));
    }

    // ==================== DELETE Endpoints ====================

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product", description = "Soft delete (archive) a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        log.info("Deleting product: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Hard delete a product", description = "Permanently delete a product and its image")
    public ResponseEntity<ApiResponse<Void>> hardDeleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        log.info("Hard deleting product: {}", id);
        productService.hardDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product permanently deleted"));
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "Delete product image")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        log.info("Deleting image for product: {}", id);
        productService.deleteProductImage(id);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
    }
}

package com.cloudshop.product.service;

import com.cloudshop.common.dto.CreateProductRequest;
import com.cloudshop.common.dto.ProductDto;
import com.cloudshop.common.dto.UpdateProductRequest;
import com.cloudshop.common.entity.Product;
import com.cloudshop.common.entity.ProductStatus;
import com.cloudshop.common.exception.DuplicateResourceException;
import com.cloudshop.common.exception.ProductNotFoundException;
import com.cloudshop.product.mapper.ProductMapper;
import com.cloudshop.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProductService Tests
 * 
 * Java 25 Features Used:
 * - Records for test data
 * - Pattern matching in assertions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private ProductService productService;

    /**
     * Java 25: Record for test data - immutable and clear.
     */
    record TestProductData(
            Long id,
            String sku,
            String name,
            BigDecimal price,
            int quantity,
            String category
    ) {
        static TestProductData standard() {
            return new TestProductData(
                    1L,
                    "TEST-001",
                    "Test Product",
                    BigDecimal.valueOf(99.99),
                    50,
                    "Electronics"
            );
        }

        Product toEntity() {
            return Product.builder()
                    .id(id)
                    .sku(sku)
                    .name(name)
                    .price(price)
                    .quantity(quantity)
                    .category(category)
                    .status(ProductStatus.ACTIVE)
                    .build();
        }

        ProductDto toDto() {
            return ProductDto.builder()
                    .id(id)
                    .sku(sku)
                    .name(name)
                    .price(price)
                    .quantity(quantity)
                    .category(category)
                    .status(ProductStatus.ACTIVE)
                    .inStock(quantity > 0)
                    .onSale(false)
                    .build();
        }

        CreateProductRequest toCreateRequest() {
            return CreateProductRequest.builder()
                    .sku(sku)
                    .name(name)
                    .price(price)
                    .quantity(quantity)
                    .category(category)
                    .build();
        }
    }

    private TestProductData testData;
    private Product testProduct;
    private ProductDto testProductDto;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        testData = TestProductData.standard();
        testProduct = testData.toEntity();
        testProductDto = testData.toDto();
        createRequest = testData.toCreateRequest();
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            // Given
            when(productRepository.findById(testData.id())).thenReturn(Optional.of(testProduct));
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.getProductById(testData.id());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testData.id());
            assertThat(result.getSku()).isEqualTo(testData.sku());
            verify(productRepository).findById(testData.id());
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long nonExistentId = 999L;
            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.getProductById(nonExistentId))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining(String.valueOf(nonExistentId));
        }

        @Test
        @DisplayName("should include presigned URL when image exists")
        void shouldIncludePresignedUrlWhenImageExists() {
            // Given
            String imageKey = "products/%d/image.jpg".formatted(testData.id());
            String presignedUrl = "https://s3.amazonaws.com/presigned-url";
            
            testProduct.setS3ImageKey(imageKey);
            when(productRepository.findById(testData.id())).thenReturn(Optional.of(testProduct));
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);
            when(s3ImageService.generatePresignedUrl(imageKey)).thenReturn(presignedUrl);

            // When
            ProductDto result = productService.getProductById(testData.id());

            // Then
            assertThat(result.getImageUrl()).isEqualTo(presignedUrl);
            verify(s3ImageService).generatePresignedUrl(imageKey);
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully")
        void shouldCreateProductSuccessfully() {
            // Given
            when(productRepository.existsBySku(testData.sku())).thenReturn(false);
            when(productMapper.toEntity(createRequest)).thenReturn(testProduct);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.createProduct(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSku()).isEqualTo(testData.sku());
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw exception when SKU already exists")
        void shouldThrowExceptionWhenSkuExists() {
            // Given
            when(productRepository.existsBySku(testData.sku())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> productService.createProduct(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(testData.sku());

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update product successfully")
        void shouldUpdateProductSuccessfully() {
            // Given
            UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                    .name("Updated Product")
                    .price(BigDecimal.valueOf(149.99))
                    .build();

            when(productRepository.findById(testData.id())).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.updateProduct(testData.id(), updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(productMapper).updateEntityFromRequest(updateRequest, testProduct);
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long nonExistentId = 999L;
            UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                    .name("Updated Product")
                    .build();

            when(productRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.updateProduct(nonExistentId, updateRequest))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should soft delete (archive) product")
        void shouldSoftDeleteProduct() {
            // Given
            when(productRepository.findById(testData.id())).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // When
            productService.deleteProduct(testData.id());

            // Then - Using enhanced enum methods
            assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
            assertThat(testProduct.getStatus().isPurchasable()).isFalse();
            assertThat(testProduct.getStatus().isVisibleToCustomers()).isFalse();
            verify(productRepository).save(testProduct);
            verify(productRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should hard delete product and S3 image")
        void shouldHardDeleteProductAndImage() {
            // Given
            String imageKey = "products/%d/image.jpg".formatted(testData.id());
            testProduct.setS3ImageKey(imageKey);
            when(productRepository.findById(testData.id())).thenReturn(Optional.of(testProduct));

            // When
            productService.hardDeleteProduct(testData.id());

            // Then
            verify(s3ImageService).deleteImage(imageKey);
            verify(productRepository).delete(testProduct);
        }
    }

    @Nested
    @DisplayName("updateProductStatus")
    class UpdateProductStatus {

        @Test
        @DisplayName("should update status successfully")
        void shouldUpdateStatusSuccessfully() {
            // Given
            when(productRepository.findById(testData.id())).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.updateProductStatus(testData.id(), ProductStatus.INACTIVE);

            // Then
            assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.INACTIVE);
            verify(productRepository).save(testProduct);
        }

        /**
         * Parameterized test with enum source for status transitions.
         */
        @ParameterizedTest
        @EnumSource(value = ProductStatus.class, names = {"INACTIVE", "DISCONTINUED", "ARCHIVED"})
        @DisplayName("should allow valid transitions from ACTIVE status")
        void shouldAllowValidTransitionsFromActive(ProductStatus targetStatus) {
            // Verify the transition is valid using enhanced enum method
            assertThat(ProductStatus.ACTIVE.canTransitionTo(targetStatus)).isTrue();
        }
    }

    @Nested
    @DisplayName("Product Entity Features")
    class ProductEntityFeatures {

        @Test
        @DisplayName("should calculate discount percentage correctly")
        void shouldCalculateDiscountPercentage() {
            // Given
            testProduct.setPrice(BigDecimal.valueOf(80.00));
            testProduct.setCompareAtPrice(BigDecimal.valueOf(100.00));

            // When
            BigDecimal discount = testProduct.getDiscountPercentage();

            // Then
            assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(20.00));
            assertThat(testProduct.isOnSale()).isTrue();
        }

        @Test
        @DisplayName("should provide stock info via record")
        void shouldProvideStockInfoViaRecord() {
            // Given
            testProduct.setQuantity(5);

            // When
            Product.StockInfo stockInfo = testProduct.getStockInfo();

            // Then - Record accessors
            assertThat(stockInfo.quantity()).isEqualTo(5);
            assertThat(stockInfo.inStock()).isTrue();
            assertThat(stockInfo.lowStock()).isTrue();
        }

        @Test
        @DisplayName("should provide pricing info via record")
        void shouldProvidePricingInfoViaRecord() {
            // Given
            testProduct.setPrice(BigDecimal.valueOf(75.00));
            testProduct.setCompareAtPrice(BigDecimal.valueOf(100.00));

            // When
            Product.PricingInfo pricingInfo = testProduct.getPricingInfo();

            // Then - Record accessors
            assertThat(pricingInfo.onSale()).isTrue();
            assertThat(pricingInfo.savingsAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
            assertThat(pricingInfo.discountPercentage()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        }
    }
}

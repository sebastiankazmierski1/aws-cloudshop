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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private Product testProduct;
    private ProductDto testProductDto;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .sku("TEST-001")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .quantity(50)
                .category("Electronics")
                .status(ProductStatus.ACTIVE)
                .build();

        testProductDto = ProductDto.builder()
                .id(1L)
                .sku("TEST-001")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .quantity(50)
                .category("Electronics")
                .status(ProductStatus.ACTIVE)
                .inStock(true)
                .onSale(false)
                .build();

        createRequest = CreateProductRequest.builder()
                .sku("TEST-001")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .quantity(50)
                .category("Electronics")
                .build();
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            // Given
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.getProductById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSku()).isEqualTo("TEST-001");
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("should include presigned URL when image exists")
        void shouldIncludePresignedUrlWhenImageExists() {
            // Given
            testProduct.setS3ImageKey("products/1/image.jpg");
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);
            when(s3ImageService.generatePresignedUrl("products/1/image.jpg"))
                    .thenReturn("https://s3.amazonaws.com/presigned-url");

            // When
            ProductDto result = productService.getProductById(1L);

            // Then
            assertThat(result.getImageUrl()).isEqualTo("https://s3.amazonaws.com/presigned-url");
            verify(s3ImageService).generatePresignedUrl("products/1/image.jpg");
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully")
        void shouldCreateProductSuccessfully() {
            // Given
            when(productRepository.existsBySku("TEST-001")).thenReturn(false);
            when(productMapper.toEntity(createRequest)).thenReturn(testProduct);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.createProduct(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSku()).isEqualTo("TEST-001");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw exception when SKU already exists")
        void shouldThrowExceptionWhenSkuExists() {
            // Given
            when(productRepository.existsBySku("TEST-001")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> productService.createProduct(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("TEST-001");

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

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.updateProduct(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(productMapper).updateEntityFromRequest(updateRequest, testProduct);
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                    .name("Updated Product")
                    .build();

            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> productService.updateProduct(999L, updateRequest))
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
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // When
            productService.deleteProduct(1L);

            // Then
            assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
            verify(productRepository).save(testProduct);
            verify(productRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should hard delete product and S3 image")
        void shouldHardDeleteProductAndImage() {
            // Given
            testProduct.setS3ImageKey("products/1/image.jpg");
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            // When
            productService.hardDeleteProduct(1L);

            // Then
            verify(s3ImageService).deleteImage("products/1/image.jpg");
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
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toDto(testProduct)).thenReturn(testProductDto);

            // When
            ProductDto result = productService.updateProductStatus(1L, ProductStatus.INACTIVE);

            // Then
            assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.INACTIVE);
            verify(productRepository).save(testProduct);
        }
    }
}

package com.cloudshop.product.mapper;

import com.cloudshop.common.dto.CreateProductRequest;
import com.cloudshop.common.dto.ProductDto;
import com.cloudshop.common.dto.UpdateProductRequest;
import com.cloudshop.common.entity.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Product entity <-> DTO conversions.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    /**
     * Map Product entity to DTO
     */
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "onSale", expression = "java(product.isOnSale())")
    ProductDto toDto(Product product);

    /**
     * Map list of Products to DTOs
     */
    List<ProductDto> toDtoList(List<Product> products);

    /**
     * Map CreateProductRequest to Product entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "featured", defaultValue = "false")
    @Mapping(target = "quantity", defaultValue = "0")
    @Mapping(target = "tags", defaultExpression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "s3ImageKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(CreateProductRequest request);

    /**
     * Update existing Product entity from UpdateProductRequest
     * Only updates non-null fields
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true) // SKU cannot be updated
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "s3ImageKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget Product product);

    /**
     * After mapping hook to set computed fields
     */
    @AfterMapping
    default void setComputedFields(@MappingTarget ProductDto dto, Product product) {
        dto.setInStock(product.isInStock());
        dto.setOnSale(product.isOnSale());
    }
}

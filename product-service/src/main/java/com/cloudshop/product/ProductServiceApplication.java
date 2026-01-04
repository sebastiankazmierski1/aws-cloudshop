package com.cloudshop.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Product Service Application
 * 
 * Provides product catalog management with S3 integration for images.
 * 
 * AWS Services used:
 * - S3: Product image storage
 * - RDS (PostgreSQL): Product data storage
 * 
 * @author CloudShop Team
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.cloudshop.common.entity", "com.cloudshop.product.entity"})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}

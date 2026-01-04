package com.cloudshop.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger documentation configuration.
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI spec at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CloudShop Product Service API")
                        .description("""
                                Product catalog management API for CloudShop e-commerce platform.
                                
                                ## Features
                                - Product CRUD operations
                                - Image upload to AWS S3
                                - Product search and filtering
                                - Inventory management
                                
                                ## AWS Integration
                                - **S3**: Product image storage
                                - **RDS**: PostgreSQL for product data
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CloudShop Team")
                                .email("team@cloudshop.example.com")
                                .url("https://github.com/your-username/cloudshop"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development"),
                        new Server()
                                .url("https://api.cloudshop.example.com")
                                .description("Production")
                ));
    }
}

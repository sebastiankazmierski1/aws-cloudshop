# CloudShop - Distributed E-Commerce Platform

A portfolio project demonstrating AWS cloud architecture with Spring Boot microservices, showcasing **Java 25 LTS** features.

## 🛠 Tech Stack

### Core Technologies
- **Java 25 LTS** (Long-Term Support, September 2025)
- **Spring Boot 3.5.9** (Java 25 ready)
- **Spring Cloud 2025.0.1**
- **Maven** (multi-module project)

### AWS Services (via LocalStack for local development)
- **S3** - Product image storage
- **RDS/PostgreSQL** - Product catalog database
- **SQS** - Order processing queue
- **SNS** - Notifications
- **DynamoDB** - Order storage
- **Lambda** - Serverless functions
- **Secrets Manager** - Credentials management

### Database & Storage
- **PostgreSQL 17** - Primary database
- **Flyway 10.x** - Database migrations
- **Redis 7** - Caching (ElastiCache simulation)

### Development Tools
- **Docker & Docker Compose** - Local development environment
- **LocalStack 4.0** - AWS services emulation
- **TestContainers 1.20.4** - Integration testing
- **MapStruct 1.6.3** - Object mapping
- **Lombok 1.18.40** - Boilerplate reduction
- **Mockito 5.19.0** - Testing (Java 25 compatible)
- **SpringDoc OpenAPI 2.7** - API documentation

## 📁 Project Structure

```
cloudshop/
├── common-lib/           # Shared DTOs, entities, exceptions
├── product-service/      # Product catalog microservice (S3 + PostgreSQL)
├── order-service/        # Order processing (SQS + DynamoDB)
├── admin-dashboard/      # Admin UI (Thymeleaf)
├── infrastructure/       # LocalStack initialization scripts
└── docker-compose.yml    # Local development environment
```

## 🚀 Getting Started

### Prerequisites
- **Java 25** (JDK 25 LTS - e.g., Temurin, Oracle, Corretto)
- **Maven 3.9+**
- **Docker & Docker Compose**

### 1. Start Local Infrastructure

```bash
docker-compose up -d
```

This starts:
- LocalStack (AWS services) on port 4566
- PostgreSQL on port 5432
- Redis on port 6379

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Product Service

```bash
cd product-service
mvn spring-boot:run
```

The service will be available at:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console (local): http://localhost:8080/h2-console

## ☕ Java 25 LTS Features Used

This project demonstrates the following Java 25 features:

### 1. **Records** (Immutable Data Carriers)
Compact, immutable classes with automatic methods:
```java
// Immutable response types
public record ImageUploadResponse(
    String s3Key,
    String presignedUrl,
    Long size
) {}

// Nested records for domain modeling
public record StockInfo(int quantity, boolean inStock, boolean lowStock) {}
public record PricingInfo(BigDecimal price, boolean onSale, BigDecimal discount) {}
```

### 2. **Sealed Classes** (Restricted Hierarchies)
Type-safe exception hierarchy with exhaustive pattern matching:
```java
public sealed class CloudShopException extends RuntimeException 
    permits ResourceNotFoundException, DuplicateResourceException, 
            InvalidFileException, S3OperationException {
    // ...
}
```

### 3. **Pattern Matching for switch** (Finalized)
Exhaustive handling of sealed types:
```java
return switch (ex) {
    case ProductNotFoundException e -> ResponseEntity.status(404).body(e.getUserMessage());
    case DuplicateResourceException e -> ResponseEntity.status(409).body(e.getMessage());
    case InvalidFileException e -> ResponseEntity.status(400).body(e.getDetailedMessage());
    case S3OperationException e -> ResponseEntity.status(500).body("File operation failed");
};
```

### 4. **Record Patterns** (Finalized)
Destructuring records in pattern matching:
```java
public sealed interface FileValidationError {
    record EmptyFile() implements FileValidationError {}
    record FileTooLarge(long actual, long max) implements FileValidationError {}
}

String message = switch (error) {
    case EmptyFile() -> "File is empty";
    case FileTooLarge(var actual, var max) -> 
        "Size %d exceeds %d".formatted(actual, max);
};
```

### 5. **Enhanced Enums with Pattern Matching**
Enums designed for exhaustive switch expressions:
```java
public enum ProductStatus {
    DRAFT, ACTIVE, INACTIVE, DISCONTINUED, ARCHIVED;
    
    public String getDescription() {
        return switch (this) {
            case DRAFT -> "Product being prepared";
            case ACTIVE -> "Available for purchase";
            case INACTIVE -> "Temporarily unavailable";
            case DISCONTINUED -> "No longer sold";
            case ARCHIVED -> "Soft deleted";
        };
    }
    
    public boolean canTransitionTo(ProductStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == INACTIVE || target == DISCONTINUED || target == ARCHIVED;
            case INACTIVE -> target == ACTIVE || target == DISCONTINUED || target == ARCHIVED;
            case DISCONTINUED -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
```

### 6. **Formatted Strings** (`.formatted()` method)
Clean string formatting:
```java
// Using .formatted() instead of String.format()
String message = "Product %s not found with id: %d".formatted(name, id);
String key = "products/%d/%s.%s".formatted(productId, uuid, extension);
```

### 7. **Sequenced Collections** (Java 21+)
First/last element access:
```java
List<String> categories = productRepository.findAllCategories();
String firstCategory = categories.getFirst();
String lastCategory = categories.getLast();
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests (requires Docker)
```bash
mvn verify -P integration-tests
```

## 📖 API Documentation

When the application is running, access the OpenAPI documentation:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🐳 Docker

### Build Docker Image
```bash
cd product-service
docker build -t cloudshop/product-service:latest .
```

### Run with Docker
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  cloudshop/product-service:latest
```

## 📝 Configuration Profiles

| Profile | Description |
|---------|-------------|
| `local` | H2 database + LocalStack (default) |
| `dev`   | PostgreSQL + Real AWS services |
| `prod`  | Production configuration |

## 📦 Dependency Versions

| Dependency | Version | Notes |
|------------|---------|-------|
| Java | 25 LTS | Released September 2025 |
| Spring Boot | 3.5.9 | Java 25 support since 3.5.5 |
| Spring Cloud | 2025.0.1 | Latest release train |
| AWS SDK | 2.29.46 | BOM managed |
| PostgreSQL Driver | 42.7.4 | JDBC 4.2 compatible |
| Flyway | 10.22.0 | Requires flyway-database-postgresql |
| Lombok | 1.18.40 | Java 25 support since 1.18.39 |
| MapStruct | 1.6.3 | Latest stable |
| Mockito | 5.19.0 | Java 25 support with ByteBuddy 1.17.7 |
| ByteBuddy | 1.17.7 | Required for Java 25 mocking |
| TestContainers | 1.20.4 | Docker integration tests |

## 🔧 Maven Compiler Configuration

Java 25 requires specific Maven configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>25</release>
        <parameters>true</parameters>
        <!-- Required for Java 25 due to Plexus compiler compatibility -->
        <fork>true</fork>
    </configuration>
</plugin>
```

## 🤝 Contributing

This is a personal learning/portfolio project, but suggestions are welcome!

## 📄 License

MIT License

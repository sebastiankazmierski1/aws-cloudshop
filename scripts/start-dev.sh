#!/bin/bash
# ============================================
# CloudShop Development Startup Script
# ============================================

set -e

echo "============================================"
echo "  CloudShop Development Environment Setup"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
check_prerequisites() {
    echo "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}Error: Java is not installed${NC}"
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo -e "${RED}Error: Java 17+ is required (found: $JAVA_VERSION)${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}Error: Maven is not installed${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Maven$(mvn -v | head -1 | cut -d' ' -f3)${NC}"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}Warning: Docker is not installed (required for LocalStack)${NC}"
    else
        echo -e "${GREEN}✓ Docker${NC}"
    fi
    
    echo ""
}

# Start infrastructure
start_infrastructure() {
    echo "Starting infrastructure (LocalStack, PostgreSQL, Redis)..."
    
    if command -v docker &> /dev/null; then
        docker-compose up -d
        
        echo "Waiting for services to be ready..."
        sleep 10
        
        # Check LocalStack health
        if curl -s http://localhost:4566/_localstack/health | grep -q "running"; then
            echo -e "${GREEN}✓ LocalStack is running${NC}"
        else
            echo -e "${YELLOW}⚠ LocalStack may not be fully ready${NC}"
        fi
        
        # Check PostgreSQL
        if docker exec cloudshop-postgres pg_isready -U cloudshop &> /dev/null; then
            echo -e "${GREEN}✓ PostgreSQL is running${NC}"
        else
            echo -e "${YELLOW}⚠ PostgreSQL may not be ready${NC}"
        fi
        
        # Check Redis
        if docker exec cloudshop-redis redis-cli ping &> /dev/null; then
            echo -e "${GREEN}✓ Redis is running${NC}"
        else
            echo -e "${YELLOW}⚠ Redis may not be ready${NC}"
        fi
    else
        echo -e "${YELLOW}Skipping infrastructure (Docker not available)${NC}"
        echo "The application will use H2 in-memory database"
    fi
    
    echo ""
}

# Build project
build_project() {
    echo "Building project..."
    mvn clean install -DskipTests
    echo -e "${GREEN}✓ Build successful${NC}"
    echo ""
}

# Run product service
run_product_service() {
    echo "Starting Product Service..."
    echo "============================================"
    echo ""
    echo "Access points:"
    echo "  API:        http://localhost:8080/api/v1/products"
    echo "  Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  H2 Console: http://localhost:8080/h2-console"
    echo "  Health:     http://localhost:8080/actuator/health"
    echo ""
    echo "Press Ctrl+C to stop"
    echo "============================================"
    echo ""
    
    cd product-service
    mvn spring-boot:run -Dspring-boot.run.profiles=local
}

# Main
main() {
    check_prerequisites
    
    case "${1:-run}" in
        "infra")
            start_infrastructure
            ;;
        "build")
            build_project
            ;;
        "run")
            start_infrastructure
            build_project
            run_product_service
            ;;
        "stop")
            echo "Stopping infrastructure..."
            docker-compose down
            echo -e "${GREEN}✓ Infrastructure stopped${NC}"
            ;;
        "clean")
            echo "Cleaning up..."
            docker-compose down -v
            mvn clean
            echo -e "${GREEN}✓ Cleanup complete${NC}"
            ;;
        *)
            echo "Usage: $0 {infra|build|run|stop|clean}"
            echo ""
            echo "Commands:"
            echo "  infra  - Start infrastructure only (LocalStack, PostgreSQL, Redis)"
            echo "  build  - Build the project"
            echo "  run    - Start infrastructure, build, and run product-service (default)"
            echo "  stop   - Stop infrastructure"
            echo "  clean  - Clean up everything"
            exit 1
            ;;
    esac
}

main "$@"

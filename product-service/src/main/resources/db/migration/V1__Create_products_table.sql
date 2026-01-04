-- V1__Create_products_table.sql
-- Initial schema for products

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    compare_at_price DECIMAL(10, 2),
    quantity INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(100),
    brand VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    image_url VARCHAR(500),
    s3_image_key VARCHAR(255),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    weight DECIMAL(5, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT chk_price_positive CHECK (price >= 0),
    CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_status_valid CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'DISCONTINUED', 'ARCHIVED'))
);

-- Indexes
CREATE INDEX idx_product_sku ON products(sku);
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_product_brand ON products(brand);
CREATE INDEX idx_product_status ON products(status);
CREATE INDEX idx_product_featured ON products(featured) WHERE featured = TRUE;
CREATE INDEX idx_product_created_at ON products(created_at DESC);

-- Product tags (separate table for normalization)
CREATE TABLE product_tags (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (product_id, tag)
);

CREATE INDEX idx_product_tags_tag ON product_tags(tag);

-- Comments
COMMENT ON TABLE products IS 'Product catalog for CloudShop e-commerce platform';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - unique product identifier';
COMMENT ON COLUMN products.compare_at_price IS 'Original price for sale calculations';
COMMENT ON COLUMN products.s3_image_key IS 'AWS S3 object key for product image';
COMMENT ON COLUMN products.version IS 'Optimistic locking version';

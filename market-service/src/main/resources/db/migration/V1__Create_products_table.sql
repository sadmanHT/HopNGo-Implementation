-- Create products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100),
    sku VARCHAR(100) UNIQUE NOT NULL,
    
    -- Pricing
    purchase_price DECIMAL(12,2) NOT NULL CHECK (purchase_price >= 0),
    rental_price_per_day DECIMAL(12,2) CHECK (rental_price_per_day >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    
    -- Stock management
    purchase_stock INTEGER NOT NULL DEFAULT 0 CHECK (purchase_stock >= 0),
    rental_stock INTEGER NOT NULL DEFAULT 0 CHECK (rental_stock >= 0),
    reserved_stock INTEGER NOT NULL DEFAULT 0 CHECK (reserved_stock >= 0),
    
    -- Product attributes
    weight DECIMAL(8,2) CHECK (weight >= 0),
    dimensions VARCHAR(100),
    color VARCHAR(50),
    size VARCHAR(50),
    material VARCHAR(100),
    
    -- Availability flags
    available_for_purchase BOOLEAN NOT NULL DEFAULT true,
    available_for_rental BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Images and media
    image_urls TEXT[],
    
    -- SEO and metadata
    tags VARCHAR(255)[],
    metadata JSONB,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_products_purchase_available ON products(available_for_purchase);
CREATE INDEX idx_products_rental_available ON products(available_for_rental);
CREATE INDEX idx_products_created_at ON products(created_at);

-- Create GIN index for full-text search on name and description
CREATE INDEX idx_products_search ON products USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- Create GIN index for tags array
CREATE INDEX idx_products_tags ON products USING GIN(tags);

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
-- Create products table
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    
    -- Pricing
    price DECIMAL(12,2) NOT NULL CHECK (price >= 0),
    rental_price_per_day DECIMAL(12,2) CHECK (rental_price_per_day >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    
    -- Stock management
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    rental_stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (rental_stock_quantity >= 0),
    
    -- Product attributes
    weight_kg DECIMAL(8,2) CHECK (weight_kg >= 0),
    specifications TEXT,
    
    -- Availability flags
    is_available_for_purchase BOOLEAN NOT NULL DEFAULT true,
    is_available_for_rental BOOLEAN NOT NULL DEFAULT false,
    
    -- Images and media
    image_url VARCHAR(500),
    

    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_purchase_available ON products(is_available_for_purchase);
CREATE INDEX idx_products_rental_available ON products(is_available_for_rental);
CREATE INDEX idx_products_created_at ON products(created_at);

-- Create GIN index for full-text search on name and description
CREATE INDEX idx_products_search ON products USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

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
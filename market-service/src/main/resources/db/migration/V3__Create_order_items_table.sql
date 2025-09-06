-- Create order_items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    
    -- Item details
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
    total_price DECIMAL(12,2) NOT NULL CHECK (total_price >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    
    -- Rental-specific fields
    is_rental BOOLEAN NOT NULL DEFAULT false,
    rental_days INTEGER CHECK (rental_days > 0),
    rental_start_date TIMESTAMP,
    rental_end_date TIMESTAMP,
    
    -- Product snapshot (for historical reference)
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    product_description TEXT,
    product_image_url VARCHAR(500),
    
    -- Additional information
    notes TEXT,
    metadata JSONB,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_order_items_created_at ON order_items(created_at);
CREATE INDEX idx_order_items_rental ON order_items(is_rental);

-- Create composite indexes for common queries
CREATE INDEX idx_order_items_order_product ON order_items(order_id, product_id);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_order_items_updated_at
    BEFORE UPDATE ON order_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add constraints for rental items
ALTER TABLE order_items ADD CONSTRAINT check_rental_item_dates 
    CHECK (
        (is_rental = true AND rental_days IS NOT NULL AND rental_start_date IS NOT NULL AND rental_end_date IS NOT NULL) 
        OR 
        (is_rental = false AND rental_days IS NULL AND rental_start_date IS NULL AND rental_end_date IS NULL)
    );

ALTER TABLE order_items ADD CONSTRAINT check_rental_item_date_order 
    CHECK (rental_start_date IS NULL OR rental_end_date IS NULL OR rental_start_date <= rental_end_date);

-- Add constraint to ensure total_price calculation is correct
ALTER TABLE order_items ADD CONSTRAINT check_total_price_calculation 
    CHECK (
        (is_rental = false AND total_price = unit_price * quantity) 
        OR 
        (is_rental = true AND rental_days IS NOT NULL AND total_price = unit_price * quantity * rental_days)
    );
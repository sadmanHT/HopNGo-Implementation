-- Create orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    
    -- Order details
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    type VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE', 'RENTAL')),
    
    -- Financial information
    total_amount DECIMAL(12,2) NOT NULL CHECK (total_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    
    -- Shipping addresses
    shipping_address_line1 VARCHAR(255),
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(100),
    
    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(100),
    
    -- Tracking and fulfillment
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    
    -- Rental-specific fields
    rental_start_date DATE,
    rental_end_date DATE,
    rental_days INTEGER CHECK (rental_days > 0),
    
    -- Order lifecycle timestamps
    paid_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Additional information
    notes TEXT,
    metadata JSONB,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_type ON orders(type);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_paid_at ON orders(paid_at);
CREATE INDEX idx_orders_tracking_number ON orders(tracking_number);

-- Create composite indexes for common queries
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add constraints for rental orders
ALTER TABLE orders ADD CONSTRAINT check_rental_dates 
    CHECK (
        (type = 'RENTAL' AND rental_start_date IS NOT NULL AND rental_end_date IS NOT NULL AND rental_days IS NOT NULL) 
        OR 
        (type = 'PURCHASE' AND rental_start_date IS NULL AND rental_end_date IS NULL AND rental_days IS NULL)
    );

ALTER TABLE orders ADD CONSTRAINT check_rental_date_order 
    CHECK (rental_start_date IS NULL OR rental_end_date IS NULL OR rental_start_date <= rental_end_date);
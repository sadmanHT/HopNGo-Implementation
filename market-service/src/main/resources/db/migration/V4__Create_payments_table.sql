-- Create payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    
    -- Payment details
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('MOCK', 'STRIPE', 'BKASH', 'NAGAD', 'PAYPAL')),
    
    -- Financial information
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    
    -- Transaction tracking
    transaction_reference VARCHAR(255) UNIQUE NOT NULL,
    provider_transaction_id VARCHAR(255),
    payment_intent_id VARCHAR(255),
    payment_method VARCHAR(100),
    
    -- Error handling
    failure_reason TEXT,
    
    -- Timestamps
    processed_at TIMESTAMP,
    webhook_received_at TIMESTAMP,
    
    -- Additional information
    metadata TEXT,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    
    -- Ensure one payment per order
    CONSTRAINT uk_payments_order_id UNIQUE (order_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_provider ON payments(provider);
CREATE INDEX idx_payments_transaction_reference ON payments(transaction_reference);
CREATE INDEX idx_payments_provider_transaction_id ON payments(provider_transaction_id);
CREATE INDEX idx_payments_created_at ON payments(created_at);
CREATE INDEX idx_payments_processed_at ON payments(processed_at);

-- Create composite indexes for common queries
CREATE INDEX idx_payments_provider_status ON payments(provider, status);
CREATE INDEX idx_payments_status_created ON payments(status, created_at DESC);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add constraints for successful payments
ALTER TABLE payments ADD CONSTRAINT check_succeeded_payment_fields 
    CHECK (
        (status = 'SUCCEEDED' AND processed_at IS NOT NULL) 
        OR 
        (status != 'SUCCEEDED')
    );

-- Add constraint for failed payments
ALTER TABLE payments ADD CONSTRAINT check_failed_payment_reason 
    CHECK (
        (status = 'FAILED' AND failure_reason IS NOT NULL) 
        OR 
        (status != 'FAILED')
    );
-- Create refunds table
CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    
    -- Refund details
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    
    -- Financial information
    amount DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    
    -- Transaction tracking
    refund_reference VARCHAR(255) UNIQUE NOT NULL,
    provider_refund_id VARCHAR(255),
    
    -- Reason and error handling
    reason TEXT,
    failure_reason TEXT,
    
    -- Timestamps
    processed_at TIMESTAMP,
    
    -- Additional information
    metadata TEXT,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_booking_id ON refunds(booking_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_refund_reference ON refunds(refund_reference);
CREATE INDEX idx_refunds_provider_refund_id ON refunds(provider_refund_id);
CREATE INDEX idx_refunds_created_at ON refunds(created_at);
CREATE INDEX idx_refunds_processed_at ON refunds(processed_at);

-- Create composite indexes for common queries
CREATE INDEX idx_refunds_status_created ON refunds(status, created_at DESC);
CREATE INDEX idx_refunds_booking_status ON refunds(booking_id, status);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_refunds_updated_at
    BEFORE UPDATE ON refunds
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add constraints for successful refunds
ALTER TABLE refunds ADD CONSTRAINT check_succeeded_refund_fields 
    CHECK (
        (status = 'SUCCEEDED' AND processed_at IS NOT NULL) 
        OR 
        (status != 'SUCCEEDED')
    );

-- Add constraint for failed refunds
ALTER TABLE refunds ADD CONSTRAINT check_failed_refund_reason 
    CHECK (
        (status = 'FAILED' AND failure_reason IS NOT NULL) 
        OR 
        (status != 'FAILED')
    );

-- Add constraint to ensure refund amount doesn't exceed original payment
-- This will be enforced at application level since we need to check across tables
-- Create webhook_events table for idempotency tracking
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Webhook identification
    webhook_id VARCHAR(255) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    
    -- Request details
    request_body TEXT NOT NULL,
    request_headers JSONB,
    signature VARCHAR(500),
    
    -- Processing status
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED' CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    processed_at TIMESTAMP,
    failure_reason TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    
    -- Related entities
    payment_id UUID,
    order_id UUID,
    
    -- Additional metadata
    metadata JSONB,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_webhook_events_webhook_id ON webhook_events(webhook_id);
CREATE INDEX idx_webhook_events_provider ON webhook_events(provider);
CREATE INDEX idx_webhook_events_event_type ON webhook_events(event_type);
CREATE INDEX idx_webhook_events_status ON webhook_events(status);
CREATE INDEX idx_webhook_events_payment_id ON webhook_events(payment_id);
CREATE INDEX idx_webhook_events_order_id ON webhook_events(order_id);
CREATE INDEX idx_webhook_events_created_at ON webhook_events(created_at);

-- Create composite indexes for common queries
CREATE INDEX idx_webhook_events_provider_status ON webhook_events(provider, status);
CREATE INDEX idx_webhook_events_provider_created ON webhook_events(provider, created_at DESC);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_webhook_events_updated_at
    BEFORE UPDATE ON webhook_events
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add foreign key constraints (optional, depending on your referential integrity requirements)
-- Note: These are commented out as they might cause issues if payments/orders are deleted
-- ALTER TABLE webhook_events ADD CONSTRAINT fk_webhook_events_payment_id 
--     FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL;
-- ALTER TABLE webhook_events ADD CONSTRAINT fk_webhook_events_order_id 
--     FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;

-- Create partial index for unprocessed events (performance optimization)
CREATE INDEX idx_webhook_events_unprocessed ON webhook_events(created_at) 
    WHERE status IN ('RECEIVED', 'PROCESSING', 'FAILED');
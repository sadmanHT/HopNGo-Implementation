-- Add correlation_id column to bookings table for payment tracking
ALTER TABLE bookings ADD COLUMN correlation_id VARCHAR(255);

-- Create index for correlation_id lookups
CREATE INDEX idx_bookings_correlation_id ON bookings(correlation_id);

-- Create processed_events table for idempotency
CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for processed_events
CREATE INDEX idx_processed_events_message_id ON processed_events(message_id);
CREATE INDEX idx_processed_events_event_type ON processed_events(event_type);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);

-- Create trigger to automatically update updated_at for processed_events
CREATE TRIGGER update_processed_events_updated_at BEFORE UPDATE ON processed_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add method to find bookings by correlation_id
COMMENT ON COLUMN bookings.correlation_id IS 'Correlation ID for tracking payment orders in SAGA pattern';
COMMENT ON TABLE processed_events IS 'Table for tracking processed payment events to ensure idempotency';
COMMENT ON COLUMN processed_events.message_id IS 'Unique message ID from RabbitMQ for idempotency checks';
COMMENT ON COLUMN processed_events.event_type IS 'Type of payment event (payment.succeeded, payment.failed)';
COMMENT ON COLUMN processed_events.processed_at IS 'Timestamp when the event was successfully processed';
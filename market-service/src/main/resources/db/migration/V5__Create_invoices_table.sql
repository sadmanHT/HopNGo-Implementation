-- Create invoices table
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Reference to order or booking
    order_id UUID,
    booking_id UUID,
    user_id UUID NOT NULL,
    
    -- Invoice identification
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    
    -- Financial details (stored in minor units - paisa/cents)
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    subtotal_minor BIGINT NOT NULL CHECK (subtotal_minor >= 0),
    tax_minor BIGINT NOT NULL DEFAULT 0 CHECK (tax_minor >= 0),
    fees_minor BIGINT NOT NULL DEFAULT 0 CHECK (fees_minor >= 0),
    total_minor BIGINT NOT NULL CHECK (total_minor >= 0),
    
    -- Tax breakdown
    tax_rate DECIMAL(5,4) DEFAULT 0.0000 CHECK (tax_rate >= 0 AND tax_rate <= 1),
    tax_country VARCHAR(3), -- ISO country code
    
    -- Fee breakdown
    platform_fee_rate DECIMAL(5,4) DEFAULT 0.0000 CHECK (platform_fee_rate >= 0 AND platform_fee_rate <= 1),
    platform_fee_minor BIGINT NOT NULL DEFAULT 0 CHECK (platform_fee_minor >= 0),
    payment_processing_fee_minor BIGINT NOT NULL DEFAULT 0 CHECK (payment_processing_fee_minor >= 0),
    
    -- Invoice status and dates
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'CANCELLED', 'REFUNDED')),
    issued_at TIMESTAMP,
    due_at TIMESTAMP,
    paid_at TIMESTAMP,
    
    -- PDF and document management
    pdf_url VARCHAR(500),
    pdf_generated_at TIMESTAMP,
    
    -- Additional metadata
    description TEXT,
    metadata JSONB,
    
    -- Auditing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_invoice_reference CHECK (
        (order_id IS NOT NULL AND booking_id IS NULL) OR 
        (order_id IS NULL AND booking_id IS NOT NULL)
    ),
    
    CONSTRAINT chk_invoice_total CHECK (
        total_minor = subtotal_minor + tax_minor + fees_minor
    ),
    
    CONSTRAINT chk_invoice_fees CHECK (
        fees_minor = platform_fee_minor + payment_processing_fee_minor
    ),
    
    CONSTRAINT chk_issued_invoice CHECK (
        (status = 'ISSUED' AND issued_at IS NOT NULL) OR 
        (status != 'ISSUED')
    ),
    
    CONSTRAINT chk_paid_invoice CHECK (
        (status = 'PAID' AND paid_at IS NOT NULL) OR 
        (status != 'PAID')
    )
);

-- Create indexes for better query performance
CREATE INDEX idx_invoices_order_id ON invoices(order_id);
CREATE INDEX idx_invoices_booking_id ON invoices(booking_id);
CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_currency ON invoices(currency);
CREATE INDEX idx_invoices_issued_at ON invoices(issued_at);
CREATE INDEX idx_invoices_created_at ON invoices(created_at DESC);

-- Create composite indexes for common queries
CREATE INDEX idx_invoices_user_status ON invoices(user_id, status);
CREATE INDEX idx_invoices_status_issued ON invoices(status, issued_at DESC);
CREATE INDEX idx_invoices_currency_status ON invoices(currency, status);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create sequence for invoice numbering
CREATE SEQUENCE invoice_number_seq START 1000;

-- Function to generate invoice number
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    next_num BIGINT;
    year_part VARCHAR(4);
    invoice_num VARCHAR(50);
BEGIN
    next_num := nextval('invoice_number_seq');
    year_part := EXTRACT(YEAR FROM CURRENT_DATE)::VARCHAR;
    invoice_num := 'INV-' || year_part || '-' || LPAD(next_num::VARCHAR, 6, '0');
    RETURN invoice_num;
END;
$$ LANGUAGE plpgsql;

-- Add foreign key constraints (assuming these tables exist)
-- ALTER TABLE invoices ADD CONSTRAINT fk_invoices_order 
--     FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
-- ALTER TABLE invoices ADD CONSTRAINT fk_invoices_booking 
--     FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL;
-- ALTER TABLE invoices ADD CONSTRAINT fk_invoices_user 
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Create view for invoice amounts in decimal format
CREATE VIEW invoice_amounts_view AS
SELECT 
    id,
    invoice_number,
    order_id,
    booking_id,
    user_id,
    currency,
    
    -- Convert minor units to decimal amounts
    CASE 
        WHEN currency = 'BDT' THEN subtotal_minor / 100.0
        WHEN currency = 'USD' THEN subtotal_minor / 100.0
        ELSE subtotal_minor / 100.0
    END as subtotal,
    
    CASE 
        WHEN currency = 'BDT' THEN tax_minor / 100.0
        WHEN currency = 'USD' THEN tax_minor / 100.0
        ELSE tax_minor / 100.0
    END as tax,
    
    CASE 
        WHEN currency = 'BDT' THEN fees_minor / 100.0
        WHEN currency = 'USD' THEN fees_minor / 100.0
        ELSE fees_minor / 100.0
    END as fees,
    
    CASE 
        WHEN currency = 'BDT' THEN total_minor / 100.0
        WHEN currency = 'USD' THEN total_minor / 100.0
        ELSE total_minor / 100.0
    END as total,
    
    CASE 
        WHEN currency = 'BDT' THEN platform_fee_minor / 100.0
        WHEN currency = 'USD' THEN platform_fee_minor / 100.0
        ELSE platform_fee_minor / 100.0
    END as platform_fee,
    
    CASE 
        WHEN currency = 'BDT' THEN payment_processing_fee_minor / 100.0
        WHEN currency = 'USD' THEN payment_processing_fee_minor / 100.0
        ELSE payment_processing_fee_minor / 100.0
    END as payment_processing_fee,
    
    tax_rate,
    tax_country,
    platform_fee_rate,
    status,
    issued_at,
    due_at,
    paid_at,
    pdf_url,
    created_at,
    updated_at
FROM invoices;

-- Grant permissions
-- GRANT SELECT, INSERT, UPDATE, DELETE ON invoices TO market_service_user;
-- GRANT SELECT ON invoice_amounts_view TO market_service_user;
-- GRANT USAGE ON invoice_number_seq TO market_service_user;
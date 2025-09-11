-- Create payouts table for provider payouts
CREATE TABLE payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL CHECK (LENGTH(currency) = 3),
    method VARCHAR(20) NOT NULL CHECK (method IN ('BANK', 'BKASH', 'NAGAD', 'ROCKET')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED')),
    
    -- Bank details (for BANK method)
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    account_holder_name VARCHAR(100),
    routing_number VARCHAR(20),
    
    -- Mobile money details (for BKASH, NAGAD, ROCKET)
    mobile_number VARCHAR(20),
    mobile_account_name VARCHAR(100),
    
    -- Processing details
    requested_by UUID,
    approved_by UUID,
    processed_by UUID,
    
    -- Reference and tracking
    reference_number VARCHAR(100),
    external_transaction_id VARCHAR(100),
    failure_reason TEXT,
    notes TEXT,
    
    -- Timestamps
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    processed_at TIMESTAMP,
    executed_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_payouts_provider_id ON payouts(provider_id);
CREATE INDEX idx_payouts_status ON payouts(status);
CREATE INDEX idx_payouts_method ON payouts(method);
CREATE INDEX idx_payouts_currency ON payouts(currency);
CREATE INDEX idx_payouts_requested_at ON payouts(requested_at);
CREATE INDEX idx_payouts_executed_at ON payouts(executed_at);
CREATE INDEX idx_payouts_reference_number ON payouts(reference_number);
CREATE INDEX idx_payouts_external_transaction_id ON payouts(external_transaction_id);

-- Composite indexes for common queries
CREATE INDEX idx_payouts_provider_status ON payouts(provider_id, status);
CREATE INDEX idx_payouts_status_method ON payouts(status, method);
CREATE INDEX idx_payouts_currency_status ON payouts(currency, status);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_payouts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_payouts_updated_at
    BEFORE UPDATE ON payouts
    FOR EACH ROW
    EXECUTE FUNCTION update_payouts_updated_at();

-- Create function to validate payout method details
CREATE OR REPLACE FUNCTION validate_payout_method_details()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate bank method details
    IF NEW.method = 'BANK' THEN
        IF NEW.bank_name IS NULL OR NEW.account_number IS NULL OR NEW.account_holder_name IS NULL THEN
            RAISE EXCEPTION 'Bank name, account number, and account holder name are required for BANK method';
        END IF;
        -- Clear mobile money fields
        NEW.mobile_number = NULL;
        NEW.mobile_account_name = NULL;
    END IF;
    
    -- Validate mobile money method details
    IF NEW.method IN ('BKASH', 'NAGAD', 'ROCKET') THEN
        IF NEW.mobile_number IS NULL THEN
            RAISE EXCEPTION 'Mobile number is required for mobile money methods';
        END IF;
        -- Clear bank fields
        NEW.bank_name = NULL;
        NEW.account_number = NULL;
        NEW.account_holder_name = NULL;
        NEW.routing_number = NULL;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_payout_method_details
    BEFORE INSERT OR UPDATE ON payouts
    FOR EACH ROW
    EXECUTE FUNCTION validate_payout_method_details();

-- Create function to validate status transitions
CREATE OR REPLACE FUNCTION validate_payout_status_transition()
RETURNS TRIGGER AS $$
BEGIN
    -- Allow any status on insert
    IF TG_OP = 'INSERT' THEN
        RETURN NEW;
    END IF;
    
    -- Validate status transitions on update
    IF OLD.status = 'PENDING' AND NEW.status NOT IN ('APPROVED', 'CANCELLED') THEN
        RAISE EXCEPTION 'PENDING payout can only transition to APPROVED or CANCELLED';
    END IF;
    
    IF OLD.status = 'APPROVED' AND NEW.status NOT IN ('PROCESSING', 'CANCELLED') THEN
        RAISE EXCEPTION 'APPROVED payout can only transition to PROCESSING or CANCELLED';
    END IF;
    
    IF OLD.status = 'PROCESSING' AND NEW.status NOT IN ('PAID', 'FAILED') THEN
        RAISE EXCEPTION 'PROCESSING payout can only transition to PAID or FAILED';
    END IF;
    
    IF OLD.status IN ('PAID', 'FAILED', 'CANCELLED') THEN
        RAISE EXCEPTION 'Cannot change status of finalized payout';
    END IF;
    
    -- Set timestamps based on status
    IF NEW.status = 'APPROVED' AND OLD.status != 'APPROVED' THEN
        NEW.approved_at = CURRENT_TIMESTAMP;
    END IF;
    
    IF NEW.status = 'PROCESSING' AND OLD.status != 'PROCESSING' THEN
        NEW.processed_at = CURRENT_TIMESTAMP;
    END IF;
    
    IF NEW.status IN ('PAID', 'FAILED') AND OLD.status NOT IN ('PAID', 'FAILED') THEN
        NEW.executed_at = CURRENT_TIMESTAMP;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_payout_status_transition
    BEFORE UPDATE ON payouts
    FOR EACH ROW
    EXECUTE FUNCTION validate_payout_status_transition();

-- Create view for payout summary
CREATE VIEW payout_summary AS
SELECT 
    currency,
    method,
    status,
    COUNT(*) as payout_count,
    SUM(amount_minor) as total_amount_minor,
    AVG(amount_minor) as avg_amount_minor,
    MIN(amount_minor) as min_amount_minor,
    MAX(amount_minor) as max_amount_minor
FROM payouts
GROUP BY currency, method, status;

-- Create view for provider payout summary
CREATE VIEW provider_payout_summary AS
SELECT 
    provider_id,
    currency,
    COUNT(*) as total_payouts,
    COUNT(CASE WHEN status = 'PAID' THEN 1 END) as paid_payouts,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_payouts,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_payouts,
    SUM(amount_minor) as total_requested_minor,
    SUM(CASE WHEN status = 'PAID' THEN amount_minor ELSE 0 END) as total_paid_minor,
    SUM(CASE WHEN status = 'PENDING' THEN amount_minor ELSE 0 END) as total_pending_minor,
    MIN(requested_at) as first_payout_at,
    MAX(requested_at) as last_payout_at
FROM payouts
GROUP BY provider_id, currency;

-- Create function to get pending payout amount for provider
CREATE OR REPLACE FUNCTION get_provider_pending_payout_amount(p_provider_id UUID, p_currency VARCHAR(3))
RETURNS BIGINT AS $$
BEGIN
    RETURN COALESCE(
        (SELECT SUM(amount_minor) 
         FROM payouts 
         WHERE provider_id = p_provider_id 
           AND currency = p_currency 
           AND status IN ('PENDING', 'APPROVED', 'PROCESSING')), 
        0
    );
END;
$$ LANGUAGE plpgsql;

-- Create function to get total paid amount for provider
CREATE OR REPLACE FUNCTION get_provider_total_paid_amount(p_provider_id UUID, p_currency VARCHAR(3))
RETURNS BIGINT AS $$
BEGIN
    RETURN COALESCE(
        (SELECT SUM(amount_minor) 
         FROM payouts 
         WHERE provider_id = p_provider_id 
           AND currency = p_currency 
           AND status = 'PAID'), 
        0
    );
END;
$$ LANGUAGE plpgsql;

-- Create function to calculate available balance for payout
CREATE OR REPLACE FUNCTION get_provider_available_for_payout(p_provider_id UUID, p_currency VARCHAR(3))
RETURNS BIGINT AS $$
DECLARE
    account_balance BIGINT;
    pending_payouts BIGINT;
BEGIN
    -- Get current account balance
    SELECT COALESCE(balance_minor, 0) INTO account_balance
    FROM accounts 
    WHERE owner_id = p_provider_id 
      AND owner_type = 'PROVIDER' 
      AND currency = p_currency;
    
    -- Get pending payout amount
    pending_payouts := get_provider_pending_payout_amount(p_provider_id, p_currency);
    
    -- Return available amount (balance minus pending payouts)
    RETURN GREATEST(account_balance - pending_payouts, 0);
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE payouts IS 'Provider payout requests and processing records';
COMMENT ON COLUMN payouts.amount_minor IS 'Payout amount in minor currency units (paisa/cents)';
COMMENT ON COLUMN payouts.method IS 'Payout method: BANK, BKASH, NAGAD, ROCKET';
COMMENT ON COLUMN payouts.status IS 'Payout status: PENDING, APPROVED, PROCESSING, PAID, FAILED, CANCELLED';
COMMENT ON COLUMN payouts.reference_number IS 'Internal reference number for tracking';
COMMENT ON COLUMN payouts.external_transaction_id IS 'External payment processor transaction ID';
COMMENT ON VIEW payout_summary IS 'Summary statistics for payouts by currency, method, and status';
COMMENT ON VIEW provider_payout_summary IS 'Summary statistics for payouts by provider and currency';
COMMENT ON FUNCTION get_provider_available_for_payout(UUID, VARCHAR) IS 'Calculate available balance for provider payout after pending payouts';
-- Create accounts table for ledger system
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_type VARCHAR(20) NOT NULL,
    owner_id UUID,
    owner_type VARCHAR(20),
    currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    balance_minor BIGINT NOT NULL DEFAULT 0,
    available_balance_minor BIGINT NOT NULL DEFAULT 0,
    reserved_balance_minor BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT accounts_type_check CHECK (account_type IN ('PLATFORM', 'PROVIDER', 'USER', 'ESCROW', 'RESERVE')),
    CONSTRAINT accounts_owner_type_check CHECK (owner_type IN ('PLATFORM', 'PROVIDER', 'USER') OR owner_type IS NULL),
    CONSTRAINT accounts_currency_check CHECK (LENGTH(currency) = 3),
    CONSTRAINT accounts_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT accounts_balance_consistency CHECK (balance_minor = available_balance_minor + reserved_balance_minor),
    
    -- Unique constraint for account type and owner combination
    CONSTRAINT accounts_unique_owner UNIQUE (account_type, owner_id, currency)
);

-- Create ledger_entries table for double-entry bookkeeping
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(10) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_type VARCHAR(30),
    reference_id UUID,
    description TEXT,
    transaction_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT ledger_entries_type_check CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ledger_entries_currency_check CHECK (LENGTH(currency) = 3),
    CONSTRAINT ledger_entries_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT ledger_entries_reference_type_check CHECK (
        reference_type IN ('BOOKING', 'ORDER', 'INVOICE', 'PAYOUT', 'REFUND', 'FEE', 'ADJUSTMENT', 'TRANSFER')
    )
);

-- Create transactions table for grouping related ledger entries
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(30) NOT NULL,
    reference_type VARCHAR(30),
    reference_id UUID,
    description TEXT,
    total_amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT transactions_type_check CHECK (
        transaction_type IN ('BOOKING_PAYMENT', 'PROVIDER_PAYOUT', 'REFUND', 'FEE_COLLECTION', 'ADJUSTMENT', 'TRANSFER')
    ),
    CONSTRAINT transactions_status_check CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT transactions_currency_check CHECK (LENGTH(currency) = 3),
    CONSTRAINT transactions_amount_positive CHECK (total_amount_minor > 0)
);

-- Create indexes for performance
CREATE INDEX idx_accounts_owner ON accounts(owner_id, owner_type);
CREATE INDEX idx_accounts_type ON accounts(account_type);
CREATE INDEX idx_accounts_currency ON accounts(currency);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_updated_at ON accounts(updated_at DESC);

CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_reference ON ledger_entries(reference_type, reference_id);
CREATE INDEX idx_ledger_entries_created_at ON ledger_entries(created_at DESC);
CREATE INDEX idx_ledger_entries_type ON ledger_entries(entry_type);

CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_reference ON transactions(reference_type, reference_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_created_by ON transactions(created_by);

-- Create triggers to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_accounts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER accounts_updated_at_trigger
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_accounts_updated_at();

-- Create function to validate double-entry bookkeeping
CREATE OR REPLACE FUNCTION validate_transaction_balance(transaction_uuid UUID)
RETURNS BOOLEAN AS $$
DECLARE
    debit_total BIGINT;
    credit_total BIGINT;
BEGIN
    -- Calculate total debits
    SELECT COALESCE(SUM(amount_minor), 0) INTO debit_total
    FROM ledger_entries
    WHERE transaction_id = transaction_uuid AND entry_type = 'DEBIT';
    
    -- Calculate total credits
    SELECT COALESCE(SUM(amount_minor), 0) INTO credit_total
    FROM ledger_entries
    WHERE transaction_id = transaction_uuid AND entry_type = 'CREDIT';
    
    -- Return true if debits equal credits
    RETURN debit_total = credit_total;
END;
$$ LANGUAGE plpgsql;

-- Create function to update account balances
CREATE OR REPLACE FUNCTION update_account_balance(account_uuid UUID)
RETURNS VOID AS $$
DECLARE
    debit_total BIGINT;
    credit_total BIGINT;
    new_balance BIGINT;
BEGIN
    -- Calculate total debits for the account
    SELECT COALESCE(SUM(amount_minor), 0) INTO debit_total
    FROM ledger_entries
    WHERE account_id = account_uuid AND entry_type = 'DEBIT';
    
    -- Calculate total credits for the account
    SELECT COALESCE(SUM(amount_minor), 0) INTO credit_total
    FROM ledger_entries
    WHERE account_id = account_uuid AND entry_type = 'CREDIT';
    
    -- Calculate new balance (credits - debits for asset accounts)
    new_balance = credit_total - debit_total;
    
    -- Update account balance
    UPDATE accounts
    SET balance_minor = new_balance,
        available_balance_minor = GREATEST(new_balance - reserved_balance_minor, 0)
    WHERE id = account_uuid;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to update account balance after ledger entry changes
CREATE OR REPLACE FUNCTION trigger_update_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    -- Update balance for the affected account
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        PERFORM update_account_balance(NEW.account_id);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM update_account_balance(OLD.account_id);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_entries_balance_trigger
    AFTER INSERT OR UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION trigger_update_account_balance();

-- Create view for account balances with currency conversion
CREATE OR REPLACE VIEW account_balances_view AS
SELECT 
    a.id,
    a.account_type,
    a.owner_id,
    a.owner_type,
    a.currency,
    a.balance_minor,
    a.available_balance_minor,
    a.reserved_balance_minor,
    a.status,
    ROUND(a.balance_minor::DECIMAL / 100, 2) as balance_decimal,
    ROUND(a.available_balance_minor::DECIMAL / 100, 2) as available_balance_decimal,
    ROUND(a.reserved_balance_minor::DECIMAL / 100, 2) as reserved_balance_decimal,
    a.created_at,
    a.updated_at
FROM accounts a;

-- Create view for transaction summaries
CREATE OR REPLACE VIEW transaction_summaries AS
SELECT 
    t.id,
    t.transaction_type,
    t.reference_type,
    t.reference_id,
    t.description,
    t.total_amount_minor,
    t.currency,
    ROUND(t.total_amount_minor::DECIMAL / 100, 2) as total_amount_decimal,
    t.status,
    t.created_by,
    t.created_at,
    t.completed_at,
    COUNT(le.id) as entry_count,
    validate_transaction_balance(t.id) as is_balanced
FROM transactions t
LEFT JOIN ledger_entries le ON t.id = le.transaction_id
GROUP BY t.id, t.transaction_type, t.reference_type, t.reference_id, 
         t.description, t.total_amount_minor, t.currency, t.status, 
         t.created_by, t.created_at, t.completed_at;

-- Insert platform accounts
INSERT INTO accounts (account_type, owner_type, currency, status) 
VALUES 
    ('PLATFORM', 'PLATFORM', 'BDT', 'ACTIVE'),
    ('PLATFORM', 'PLATFORM', 'USD', 'ACTIVE'),
    ('RESERVE', 'PLATFORM', 'BDT', 'ACTIVE'),
    ('ESCROW', 'PLATFORM', 'BDT', 'ACTIVE')
ON CONFLICT (account_type, owner_id, currency) DO NOTHING;

-- Add comments
COMMENT ON TABLE accounts IS 'Accounts for the ledger system supporting multi-currency balances';
COMMENT ON TABLE ledger_entries IS 'Double-entry bookkeeping ledger entries';
COMMENT ON TABLE transactions IS 'Transaction groupings for related ledger entries';
COMMENT ON COLUMN accounts.balance_minor IS 'Total account balance in minor currency units';
COMMENT ON COLUMN accounts.available_balance_minor IS 'Available balance (total - reserved)';
COMMENT ON COLUMN accounts.reserved_balance_minor IS 'Reserved/held balance';
COMMENT ON COLUMN ledger_entries.amount_minor IS 'Entry amount in minor currency units';
COMMENT ON FUNCTION validate_transaction_balance IS 'Validates that debits equal credits for a transaction';
COMMENT ON FUNCTION update_account_balance IS 'Recalculates account balance from ledger entries';
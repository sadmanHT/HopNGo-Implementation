-- Create referrals table for tracking referral links and rewards
CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    referrer_user_id VARCHAR(36) NOT NULL,
    referred_user_id VARCHAR(36),
    referral_code VARCHAR(50) NOT NULL UNIQUE,
    referral_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, EXPIRED
    source VARCHAR(50), -- web, mobile, email, social
    campaign VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    referrer_page TEXT,
    landing_page TEXT,
    conversion_event VARCHAR(100), -- signup, first_booking, etc.
    conversion_value_minor BIGINT DEFAULT 0, -- Value in minor currency units
    points_awarded INTEGER DEFAULT 0,
    points_pending INTEGER DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE,
    converted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for referrals
CREATE INDEX idx_referrals_referrer_user ON referrals(referrer_user_id);
CREATE INDEX idx_referrals_referred_user ON referrals(referred_user_id) WHERE referred_user_id IS NOT NULL;
CREATE INDEX idx_referrals_code ON referrals(referral_code);
CREATE INDEX idx_referrals_status ON referrals(status);
CREATE INDEX idx_referrals_created_at ON referrals(created_at);
CREATE INDEX idx_referrals_converted_at ON referrals(converted_at) WHERE converted_at IS NOT NULL;
CREATE INDEX idx_referrals_expires_at ON referrals(expires_at) WHERE expires_at IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_referrals_referrer_status ON referrals(referrer_user_id, status);
CREATE INDEX idx_referrals_referrer_created ON referrals(referrer_user_id, created_at);

-- Create points_ledger table for tracking referral rewards
CREATE TABLE points_ledger (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- EARNED, REDEEMED, EXPIRED, ADJUSTED
    points_amount INTEGER NOT NULL, -- Positive for earned, negative for spent
    balance_after INTEGER NOT NULL,
    source VARCHAR(50) NOT NULL, -- referral, signup_bonus, booking_reward, etc.
    source_id VARCHAR(36), -- Reference to referral_id, booking_id, etc.
    description TEXT,
    metadata JSONB,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for points_ledger
CREATE INDEX idx_points_ledger_user_id ON points_ledger(user_id);
CREATE INDEX idx_points_ledger_user_created ON points_ledger(user_id, created_at);
CREATE INDEX idx_points_ledger_transaction_type ON points_ledger(transaction_type);
CREATE INDEX idx_points_ledger_source ON points_ledger(source);
CREATE INDEX idx_points_ledger_source_id ON points_ledger(source_id) WHERE source_id IS NOT NULL;
CREATE INDEX idx_points_ledger_expires_at ON points_ledger(expires_at) WHERE expires_at IS NOT NULL;

-- GIN index for metadata JSONB column
CREATE INDEX idx_points_ledger_metadata_gin ON points_ledger USING GIN(metadata);

-- Create subscribers table for email capture
CREATE TABLE subscribers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, UNSUBSCRIBED, BOUNCED
    source VARCHAR(50), -- footer, popup, referral, etc.
    tags TEXT[], -- Array of tags for segmentation
    user_id VARCHAR(36), -- Link to registered user if applicable
    ip_address INET,
    user_agent TEXT,
    referrer TEXT,
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    metadata JSONB,
    subscribed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for subscribers
CREATE UNIQUE INDEX idx_subscribers_email ON subscribers(email);
CREATE INDEX idx_subscribers_status ON subscribers(status);
CREATE INDEX idx_subscribers_source ON subscribers(source);
CREATE INDEX idx_subscribers_user_id ON subscribers(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_subscribers_created_at ON subscribers(created_at);
CREATE INDEX idx_subscribers_subscribed_at ON subscribers(subscribed_at);

-- GIN indexes for array and JSONB columns
CREATE INDEX idx_subscribers_tags_gin ON subscribers USING GIN(tags);
CREATE INDEX idx_subscribers_metadata_gin ON subscribers USING GIN(metadata);

-- Create views for easier querying

-- Referral performance summary view
CREATE VIEW referral_performance AS
SELECT 
    r.referrer_user_id,
    COUNT(*) as total_referrals,
    COUNT(CASE WHEN r.status = 'COMPLETED' THEN 1 END) as completed_referrals,
    COUNT(CASE WHEN r.status = 'PENDING' THEN 1 END) as pending_referrals,
    SUM(r.conversion_value_minor) as total_conversion_value_minor,
    SUM(r.points_awarded) as total_points_awarded,
    SUM(r.points_pending) as total_points_pending,
    ROUND(
        CASE 
            WHEN COUNT(*) > 0 THEN 
                (COUNT(CASE WHEN r.status = 'COMPLETED' THEN 1 END)::DECIMAL / COUNT(*)) * 100
            ELSE 0
        END, 2
    ) as conversion_rate_percent,
    MIN(r.created_at) as first_referral_at,
    MAX(r.created_at) as last_referral_at
FROM referrals r
GROUP BY r.referrer_user_id;

-- User points balance view
CREATE VIEW user_points_balance AS
SELECT 
    pl.user_id,
    COALESCE(SUM(pl.points_amount), 0) as total_points_balance,
    COALESCE(SUM(CASE WHEN pl.transaction_type = 'EARNED' THEN pl.points_amount ELSE 0 END), 0) as total_earned,
    COALESCE(SUM(CASE WHEN pl.transaction_type = 'REDEEMED' THEN ABS(pl.points_amount) ELSE 0 END), 0) as total_redeemed,
    COUNT(CASE WHEN pl.transaction_type = 'EARNED' THEN 1 END) as earning_transactions,
    COUNT(CASE WHEN pl.transaction_type = 'REDEEMED' THEN 1 END) as redemption_transactions,
    MAX(pl.created_at) as last_transaction_at
FROM points_ledger pl
WHERE pl.expires_at IS NULL OR pl.expires_at > CURRENT_TIMESTAMP
GROUP BY pl.user_id;

-- Add trigger to update updated_at columns
CREATE TRIGGER update_referrals_updated_at BEFORE UPDATE ON referrals FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_subscribers_updated_at BEFORE UPDATE ON subscribers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE referrals IS 'Tracks referral links and their conversion status';
COMMENT ON TABLE points_ledger IS 'Ledger for tracking user points earned and spent';
COMMENT ON TABLE subscribers IS 'Email subscribers for newsletters and marketing';

COMMENT ON COLUMN referrals.referral_code IS 'Unique code for tracking referrals';
COMMENT ON COLUMN referrals.conversion_value_minor IS 'Value of conversion in minor currency units';
COMMENT ON COLUMN points_ledger.points_amount IS 'Points amount - positive for earned, negative for spent';
COMMENT ON COLUMN points_ledger.balance_after IS 'User points balance after this transaction';
COMMENT ON COLUMN subscribers.tags IS 'Array of tags for subscriber segmentation';
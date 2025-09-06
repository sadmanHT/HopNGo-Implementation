-- Add KYC tables for provider verification

-- Create kyc_requests table
CREATE TABLE kyc_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    fields JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create user_flags table
CREATE TABLE user_flags (
    user_id BIGINT PRIMARY KEY,
    verified_provider BOOLEAN NOT NULL DEFAULT false,
    banned BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_kyc_requests_user_id ON kyc_requests(user_id);
CREATE INDEX idx_kyc_requests_status ON kyc_requests(status);
CREATE INDEX idx_kyc_requests_created_at ON kyc_requests(created_at);
CREATE INDEX idx_user_flags_verified_provider ON user_flags(verified_provider);
CREATE INDEX idx_user_flags_banned ON user_flags(banned);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_kyc_requests_updated_at BEFORE UPDATE ON kyc_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_flags_updated_at BEFORE UPDATE ON user_flags
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
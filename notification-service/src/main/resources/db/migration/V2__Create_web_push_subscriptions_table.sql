-- Create web_push_subscriptions table
CREATE TABLE web_push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    endpoint VARCHAR(1000) NOT NULL,
    p256dh VARCHAR(500) NOT NULL,
    auth VARCHAR(500) NOT NULL,
    user_agent VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique combination of user_id and endpoint
    CONSTRAINT uk_web_push_user_endpoint UNIQUE (user_id, endpoint)
);

-- Create indexes for better performance
CREATE INDEX idx_web_push_user_id ON web_push_subscriptions(user_id);
CREATE INDEX idx_web_push_endpoint ON web_push_subscriptions(endpoint);
CREATE INDEX idx_web_push_is_active ON web_push_subscriptions(is_active);
CREATE INDEX idx_web_push_user_active ON web_push_subscriptions(user_id, is_active);
CREATE INDEX idx_web_push_created_at ON web_push_subscriptions(created_at);
CREATE INDEX idx_web_push_updated_at ON web_push_subscriptions(updated_at);

-- Create trigger to update updated_at timestamp
CREATE TRIGGER update_web_push_subscriptions_updated_at
    BEFORE UPDATE ON web_push_subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE web_push_subscriptions IS 'Stores web push notification subscriptions for users';
COMMENT ON COLUMN web_push_subscriptions.user_id IS 'ID of the user who subscribed';
COMMENT ON COLUMN web_push_subscriptions.endpoint IS 'Push service endpoint URL';
COMMENT ON COLUMN web_push_subscriptions.p256dh IS 'P256DH key for encryption';
COMMENT ON COLUMN web_push_subscriptions.auth IS 'Auth key for encryption';
COMMENT ON COLUMN web_push_subscriptions.user_agent IS 'Browser user agent string';
COMMENT ON COLUMN web_push_subscriptions.is_active IS 'Whether the subscription is active';
COMMENT ON COLUMN web_push_subscriptions.created_at IS 'When the subscription was created';
COMMENT ON COLUMN web_push_subscriptions.updated_at IS 'When the subscription was last updated';
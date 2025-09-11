-- Create feature_flags table
CREATE TABLE feature_flags (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on key for faster lookups
CREATE INDEX idx_feature_flags_key ON feature_flags(key);

-- Create index on enabled for faster filtering
CREATE INDEX idx_feature_flags_enabled ON feature_flags(enabled);

-- Insert default feature flags
INSERT INTO feature_flags (key, description, enabled, payload) VALUES 
('recs_v1', 'Enable recommendations system v1 features', false, '{"rollout_percentage": 0}'),
('new_ui_design', 'Enable new UI design components', false, null);
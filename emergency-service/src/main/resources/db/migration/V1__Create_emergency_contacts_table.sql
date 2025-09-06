CREATE TABLE emergency_contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    relation VARCHAR(100) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on user_id for faster queries
CREATE INDEX idx_emergency_contacts_user_id ON emergency_contacts(user_id);

-- Create index on is_primary for faster primary contact queries
CREATE INDEX idx_emergency_contacts_primary ON emergency_contacts(user_id, is_primary);
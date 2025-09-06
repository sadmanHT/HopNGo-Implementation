-- Create moderation_items table
CREATE TABLE moderation_items (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('POST', 'COMMENT', 'LISTING', 'REVIEW', 'USER')),
    ref_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'APPROVED', 'REJECTED', 'REMOVED')),
    reason TEXT,
    reporter_user_id BIGINT NOT NULL,
    assignee_user_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decision_note TEXT
);

-- Create admin_audit table
CREATE TABLE admin_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_moderation_items_status ON moderation_items(status);
CREATE INDEX idx_moderation_items_type ON moderation_items(type);
CREATE INDEX idx_moderation_items_assignee ON moderation_items(assignee_user_id);
CREATE INDEX idx_moderation_items_created_at ON moderation_items(created_at);
CREATE INDEX idx_moderation_items_ref_id_type ON moderation_items(ref_id, type);

CREATE INDEX idx_admin_audit_actor ON admin_audit(actor_user_id);
CREATE INDEX idx_admin_audit_target ON admin_audit(target_type, target_id);
CREATE INDEX idx_admin_audit_created_at ON admin_audit(created_at);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for moderation_items
CREATE TRIGGER update_moderation_items_updated_at
    BEFORE UPDATE ON moderation_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE moderation_items IS 'Stores content moderation items flagged for review';
COMMENT ON TABLE admin_audit IS 'Audit log for all admin actions performed in the system';
COMMENT ON COLUMN moderation_items.type IS 'Type of content being moderated: POST, COMMENT, LISTING, REVIEW, USER';
COMMENT ON COLUMN moderation_items.status IS 'Current status: OPEN, APPROVED, REJECTED, REMOVED';
COMMENT ON COLUMN moderation_items.ref_id IS 'ID of the referenced content in the respective service';
COMMENT ON COLUMN admin_audit.details IS 'JSON object containing additional context and metadata for the action';
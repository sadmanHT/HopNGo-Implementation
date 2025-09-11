-- Create itinerary sharing, versioning, and collaboration tables

-- Create itinerary_shares table for sharing configuration
CREATE TABLE itinerary_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PRIVATE', 'LINK', 'PUBLIC')),
    can_comment BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create itinerary_versions table for version history
CREATE TABLE itinerary_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    plan JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_user_id VARCHAR(255) NOT NULL
);

-- Create itinerary_comments table for collaboration
CREATE TABLE itinerary_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    author_user_id VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance

-- Indexes for itinerary_shares
CREATE INDEX idx_itinerary_shares_itinerary_id ON itinerary_shares(itinerary_id);
CREATE INDEX idx_itinerary_shares_token ON itinerary_shares(token);
CREATE INDEX idx_itinerary_shares_visibility ON itinerary_shares(visibility);

-- Indexes for itinerary_versions
CREATE INDEX idx_itinerary_versions_itinerary_id ON itinerary_versions(itinerary_id);
CREATE INDEX idx_itinerary_versions_itinerary_version ON itinerary_versions(itinerary_id, version DESC);
CREATE INDEX idx_itinerary_versions_author ON itinerary_versions(author_user_id);
CREATE INDEX idx_itinerary_versions_created_at ON itinerary_versions(created_at DESC);

-- Indexes for itinerary_comments
CREATE INDEX idx_itinerary_comments_itinerary_id ON itinerary_comments(itinerary_id);
CREATE INDEX idx_itinerary_comments_author ON itinerary_comments(author_user_id);
CREATE INDEX idx_itinerary_comments_created_at ON itinerary_comments(created_at DESC);
CREATE INDEX idx_itinerary_comments_itinerary_created ON itinerary_comments(itinerary_id, created_at DESC);

-- Add unique constraint for itinerary_id and version combination
ALTER TABLE itinerary_versions ADD CONSTRAINT uk_itinerary_versions_itinerary_version UNIQUE (itinerary_id, version);

-- Add comments for documentation
COMMENT ON TABLE itinerary_shares IS 'Stores sharing configuration for itineraries';
COMMENT ON COLUMN itinerary_shares.id IS 'Unique identifier for the share configuration';
COMMENT ON COLUMN itinerary_shares.itinerary_id IS 'Reference to the shared itinerary';
COMMENT ON COLUMN itinerary_shares.token IS 'Unique token for accessing shared itinerary';
COMMENT ON COLUMN itinerary_shares.visibility IS 'Visibility level: PRIVATE, LINK, or PUBLIC';
COMMENT ON COLUMN itinerary_shares.can_comment IS 'Whether comments are allowed on this shared itinerary';
COMMENT ON COLUMN itinerary_shares.created_at IS 'Timestamp when sharing was configured';

COMMENT ON TABLE itinerary_versions IS 'Stores version history of itinerary changes';
COMMENT ON COLUMN itinerary_versions.id IS 'Unique identifier for the version';
COMMENT ON COLUMN itinerary_versions.itinerary_id IS 'Reference to the itinerary';
COMMENT ON COLUMN itinerary_versions.version IS 'Version number (incremental)';
COMMENT ON COLUMN itinerary_versions.plan IS 'Snapshot of the itinerary plan at this version';
COMMENT ON COLUMN itinerary_versions.created_at IS 'Timestamp when this version was created';
COMMENT ON COLUMN itinerary_versions.author_user_id IS 'User who made the changes in this version';

COMMENT ON TABLE itinerary_comments IS 'Stores comments on shared itineraries';
COMMENT ON COLUMN itinerary_comments.id IS 'Unique identifier for the comment';
COMMENT ON COLUMN itinerary_comments.itinerary_id IS 'Reference to the commented itinerary';
COMMENT ON COLUMN itinerary_comments.author_user_id IS 'User who wrote the comment';
COMMENT ON COLUMN itinerary_comments.message IS 'Comment text content';
COMMENT ON COLUMN itinerary_comments.created_at IS 'Timestamp when comment was posted';
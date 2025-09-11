-- Create saved_routes table for storing user's saved routes
CREATE TABLE saved_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(500) NOT NULL,
    waypoints JSONB NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL CHECK (distance_km >= 0),
    duration_min INTEGER NOT NULL CHECK (duration_min >= 0),
    mode VARCHAR(50) NOT NULL CHECK (mode IN ('driving', 'walking', 'cycling')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_saved_routes_user_id ON saved_routes(user_id);
CREATE INDEX idx_saved_routes_user_created ON saved_routes(user_id, created_at DESC);
CREATE INDEX idx_saved_routes_created_at ON saved_routes(created_at DESC);
CREATE INDEX idx_saved_routes_mode ON saved_routes(mode);

-- Create trigger to automatically update updated_at on row updates
CREATE TRIGGER update_saved_routes_updated_at
    BEFORE UPDATE ON saved_routes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE saved_routes IS 'Stores user-saved routes with waypoints and travel information';
COMMENT ON COLUMN saved_routes.id IS 'Unique identifier for the saved route';
COMMENT ON COLUMN saved_routes.user_id IS 'ID of the user who owns this saved route';
COMMENT ON COLUMN saved_routes.name IS 'User-defined name for the saved route';
COMMENT ON COLUMN saved_routes.waypoints IS 'JSON array of route waypoints with coordinates';
COMMENT ON COLUMN saved_routes.distance_km IS 'Total route distance in kilometers';
COMMENT ON COLUMN saved_routes.duration_min IS 'Estimated travel duration in minutes';
COMMENT ON COLUMN saved_routes.mode IS 'Transportation mode (driving, walking, cycling)';
COMMENT ON COLUMN saved_routes.created_at IS 'Timestamp when the route was saved';
COMMENT ON COLUMN saved_routes.updated_at IS 'Timestamp when the route was last updated';
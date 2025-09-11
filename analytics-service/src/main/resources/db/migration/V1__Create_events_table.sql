-- Create events table for analytics tracking
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    user_id VARCHAR(36),
    session_id VARCHAR(36),
    ip_address INET,
    user_agent TEXT,
    referrer TEXT,
    page_url TEXT,
    event_data JSONB,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 1
);

-- Create indexes for performance
CREATE INDEX idx_events_event_type ON events(event_type);
CREATE INDEX idx_events_event_category ON events(event_category);
CREATE INDEX idx_events_user_id ON events(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_events_session_id ON events(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_events_created_at ON events(created_at);
CREATE INDEX idx_events_processed_at ON events(processed_at) WHERE processed_at IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_events_user_created ON events(user_id, created_at) WHERE user_id IS NOT NULL;
CREATE INDEX idx_events_type_created ON events(event_type, created_at);
CREATE INDEX idx_events_category_created ON events(event_category, created_at);

-- GIN index for JSONB columns
CREATE INDEX idx_events_event_data_gin ON events USING GIN(event_data);
CREATE INDEX idx_events_metadata_gin ON events USING GIN(metadata);

-- Create aggregated_metrics table for pre-computed KPIs
CREATE TABLE aggregated_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL, -- DAU, WAU, MAU, conversion_rate, etc.
    dimension_key VARCHAR(100), -- user_segment, event_category, etc.
    dimension_value VARCHAR(200),
    metric_value DECIMAL(15,4) NOT NULL,
    count_value BIGINT,
    date_key DATE NOT NULL,
    hour_key INTEGER, -- 0-23 for hourly metrics
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for aggregated_metrics
CREATE UNIQUE INDEX idx_aggregated_metrics_unique ON aggregated_metrics(
    metric_name, metric_type, dimension_key, dimension_value, date_key, hour_key
);
CREATE INDEX idx_aggregated_metrics_date ON aggregated_metrics(date_key);
CREATE INDEX idx_aggregated_metrics_name_date ON aggregated_metrics(metric_name, date_key);
CREATE INDEX idx_aggregated_metrics_type_date ON aggregated_metrics(metric_type, date_key);

-- Create user_sessions table for session tracking
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(36),
    ip_address INET,
    user_agent TEXT,
    referrer TEXT,
    landing_page TEXT,
    exit_page TEXT,
    session_start TIMESTAMP WITH TIME ZONE NOT NULL,
    session_end TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    page_views INTEGER DEFAULT 0,
    events_count INTEGER DEFAULT 0,
    is_bounce BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user_sessions
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_user_sessions_start ON user_sessions(session_start);
CREATE INDEX idx_user_sessions_end ON user_sessions(session_end) WHERE session_end IS NOT NULL;
CREATE INDEX idx_user_sessions_duration ON user_sessions(duration_seconds) WHERE duration_seconds IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE events IS 'Stores all analytics events with flexible JSONB data';
COMMENT ON TABLE aggregated_metrics IS 'Pre-computed metrics for fast KPI queries';
COMMENT ON TABLE user_sessions IS 'User session tracking and analytics';

COMMENT ON COLUMN events.event_id IS 'Unique identifier for deduplication';
COMMENT ON COLUMN events.event_data IS 'Flexible event properties as JSON';
COMMENT ON COLUMN events.metadata IS 'System metadata like client info, timestamps';
COMMENT ON COLUMN aggregated_metrics.dimension_key IS 'Grouping dimension (e.g., user_segment)';
COMMENT ON COLUMN aggregated_metrics.dimension_value IS 'Dimension value (e.g., premium_user)';
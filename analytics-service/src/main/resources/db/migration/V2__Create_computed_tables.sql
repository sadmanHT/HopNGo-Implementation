-- Create user_content_stats table for user engagement metrics
CREATE TABLE user_content_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    posts_count INTEGER DEFAULT 0,
    likes_given INTEGER DEFAULT 0,
    likes_received INTEGER DEFAULT 0,
    bookmarks_count INTEGER DEFAULT 0,
    follows_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create item_popularity table for content popularity metrics
CREATE TABLE item_popularity (
    id BIGSERIAL PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('POST', 'LISTING')),
    likes INTEGER DEFAULT 0,
    bookmarks INTEGER DEFAULT 0,
    views INTEGER DEFAULT 0,
    recency_score DECIMAL(10,4) DEFAULT 0.0,
    popularity_score DECIMAL(10,4) DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(item_id, item_type)
);

-- Indexes for user_content_stats
CREATE INDEX idx_user_content_stats_user_id ON user_content_stats(user_id);
CREATE INDEX idx_user_content_stats_posts_count ON user_content_stats(posts_count DESC);
CREATE INDEX idx_user_content_stats_likes_received ON user_content_stats(likes_received DESC);
CREATE INDEX idx_user_content_stats_updated_at ON user_content_stats(updated_at);

-- Indexes for item_popularity
CREATE INDEX idx_item_popularity_item_id ON item_popularity(item_id);
CREATE INDEX idx_item_popularity_item_type ON item_popularity(item_type);
CREATE INDEX idx_item_popularity_likes ON item_popularity(likes DESC);
CREATE INDEX idx_item_popularity_bookmarks ON item_popularity(bookmarks DESC);
CREATE INDEX idx_item_popularity_recency_score ON item_popularity(recency_score DESC);
CREATE INDEX idx_item_popularity_popularity_score ON item_popularity(popularity_score DESC);
CREATE INDEX idx_item_popularity_type_popularity ON item_popularity(item_type, popularity_score DESC);
CREATE INDEX idx_item_popularity_updated_at ON item_popularity(updated_at);

-- Comments for documentation
COMMENT ON TABLE user_content_stats IS 'Computed user engagement statistics for recommendations';
COMMENT ON TABLE item_popularity IS 'Computed item popularity metrics for content recommendations';

COMMENT ON COLUMN user_content_stats.posts_count IS 'Total number of posts created by user';
COMMENT ON COLUMN user_content_stats.likes_given IS 'Total likes given by user';
COMMENT ON COLUMN user_content_stats.likes_received IS 'Total likes received on user content';
COMMENT ON COLUMN user_content_stats.bookmarks_count IS 'Total bookmarks made by user';
COMMENT ON COLUMN user_content_stats.follows_count IS 'Total users followed by this user';

COMMENT ON COLUMN item_popularity.item_type IS 'Type of item: POST or LISTING';
COMMENT ON COLUMN item_popularity.recency_score IS 'Time-based decay score for recent content';
COMMENT ON COLUMN item_popularity.popularity_score IS 'Combined popularity score (likes + bookmarks + recency)';
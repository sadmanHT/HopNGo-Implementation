-- Add personalization tables for collaborative filtering recommendation engine

-- User preferences table to store user travel preferences
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    preference_type VARCHAR(100) NOT NULL, -- 'destination_type', 'budget_range', 'trip_duration', etc.
    preference_value VARCHAR(500) NOT NULL,
    weight DECIMAL(3,2) DEFAULT 1.0, -- Preference strength (0.0 to 1.0)
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User similarities table for collaborative filtering
CREATE TABLE user_similarities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id_1 VARCHAR(255) NOT NULL,
    user_id_2 VARCHAR(255) NOT NULL,
    similarity_score DECIMAL(5,4) NOT NULL, -- Cosine similarity score (0.0 to 1.0)
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_pair UNIQUE (user_id_1, user_id_2)
);

-- Itinerary ratings table to track user interactions
CREATE TABLE itinerary_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    interaction_type VARCHAR(50) NOT NULL, -- 'created', 'viewed', 'saved', 'shared', 'rated'
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Itinerary recommendations table to cache recommendations
CREATE TABLE itinerary_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    recommended_itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    recommendation_score DECIMAL(5,4) NOT NULL,
    recommendation_reason VARCHAR(500), -- Why this was recommended
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days')
);

-- Create indexes for better query performance
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_type ON user_preferences(preference_type);
CREATE INDEX idx_user_similarities_user1 ON user_similarities(user_id_1);
CREATE INDEX idx_user_similarities_user2 ON user_similarities(user_id_2);
CREATE INDEX idx_user_similarities_score ON user_similarities(similarity_score DESC);
CREATE INDEX idx_itinerary_ratings_user_id ON itinerary_ratings(user_id);
CREATE INDEX idx_itinerary_ratings_itinerary_id ON itinerary_ratings(itinerary_id);
CREATE INDEX idx_itinerary_ratings_interaction ON itinerary_ratings(interaction_type);
CREATE INDEX idx_recommendations_user_id ON itinerary_recommendations(user_id);
CREATE INDEX idx_recommendations_score ON itinerary_recommendations(recommendation_score DESC);
CREATE INDEX idx_recommendations_expires ON itinerary_recommendations(expires_at);

-- Create triggers for updated_at timestamps
CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE user_preferences IS 'Stores user travel preferences for personalization';
COMMENT ON TABLE user_similarities IS 'Stores calculated similarity scores between users for collaborative filtering';
COMMENT ON TABLE itinerary_ratings IS 'Tracks user interactions and ratings with itineraries';
COMMENT ON TABLE itinerary_recommendations IS 'Caches personalized itinerary recommendations for users';

COMMENT ON COLUMN user_preferences.preference_type IS 'Type of preference: destination_type, budget_range, trip_duration, activity_type, etc.';
COMMENT ON COLUMN user_preferences.weight IS 'Preference strength from 0.0 (weak) to 1.0 (strong)';
COMMENT ON COLUMN user_similarities.similarity_score IS 'Cosine similarity score between two users (0.0 to 1.0)';
COMMENT ON COLUMN itinerary_ratings.interaction_type IS 'Type of interaction: created, viewed, saved, shared, rated';
COMMENT ON COLUMN itinerary_recommendations.recommendation_score IS 'Calculated recommendation score (0.0 to 1.0)';
COMMENT ON COLUMN itinerary_recommendations.expires_at IS 'When this recommendation expires and should be recalculated';
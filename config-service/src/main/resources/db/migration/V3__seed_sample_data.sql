-- Sample Feature Flags
INSERT INTO feature_flags (id, name, description, enabled, created_at, updated_at) VALUES
('visual-search', 'visual-search', 'Enable visual search functionality using image uploads', true, NOW(), NOW()),
('advanced-filters', 'advanced-filters', 'Show advanced filtering options in search', true, NOW(), NOW()),
('dark-mode', 'dark-mode', 'Enable dark mode theme toggle', false, NOW(), NOW()),
('real-time-chat', 'real-time-chat', 'Enable real-time messaging features', true, NOW(), NOW()),
('premium-features', 'premium-features', 'Unlock premium subscription features', false, NOW(), NOW());

-- Sample Experiments
INSERT INTO experiments (id, name, description, enabled, traffic_percentage, created_at, updated_at) VALUES
('booking-search-layout', 'booking-search-layout', 'Test different booking search interface layouts', true, 50.0, NOW(), NOW()),
('pricing-display', 'pricing-display', 'Test different ways to display pricing information', true, 30.0, NOW(), NOW()),
('onboarding-flow', 'onboarding-flow', 'Test different user onboarding experiences', false, 25.0, NOW(), NOW());

-- Sample Experiment Variants for booking-search-layout
INSERT INTO experiment_variants (id, experiment_id, name, description, weight, payload, created_at, updated_at) VALUES
('compact-layout', 'booking-search-layout', 'compact', 'Compact horizontal search layout with minimal controls', 50.0, '{"layout": "compact", "features": ["quick-search", "basic-sort"]}', NOW(), NOW()),
('enhanced-layout', 'booking-search-layout', 'enhanced', 'Enhanced vertical layout with advanced filtering options', 50.0, '{"layout": "enhanced", "features": ["advanced-search", "detailed-filters", "multiple-sort"]}', NOW(), NOW());

-- Sample Experiment Variants for pricing-display
INSERT INTO experiment_variants (id, experiment_id, name, description, weight, payload, created_at, updated_at) VALUES
('standard-pricing', 'pricing-display', 'standard', 'Standard pricing display with currency symbols', 60.0, '{"format": "standard", "show_currency": true, "decimal_places": 2}', NOW(), NOW()),
('simplified-pricing', 'pricing-display', 'simplified', 'Simplified pricing without currency symbols', 40.0, '{"format": "simplified", "show_currency": false, "decimal_places": 0}', NOW(), NOW());

-- Sample Experiment Variants for onboarding-flow
INSERT INTO experiment_variants (id, experiment_id, name, description, weight, payload, created_at, updated_at) VALUES
('guided-tour', 'onboarding-flow', 'guided', 'Step-by-step guided tour of features', 50.0, '{"type": "guided", "steps": 5, "interactive": true}', NOW(), NOW()),
('quick-start', 'onboarding-flow', 'quick', 'Quick start with essential features only', 50.0, '{"type": "quick", "steps": 2, "interactive": false}', NOW(), NOW());

-- Sample User Assignments (for demonstration)
-- Note: In a real application, these would be created dynamically when users access experiments
INSERT INTO assignments (id, user_id, experiment_id, variant_id, assigned_at) VALUES
('assignment-1', 'user-123', 'booking-search-layout', 'compact-layout', NOW()),
('assignment-2', 'user-456', 'booking-search-layout', 'enhanced-layout', NOW()),
('assignment-3', 'user-789', 'pricing-display', 'standard-pricing', NOW()),
('assignment-4', 'user-123', 'pricing-display', 'simplified-pricing', NOW());

-- Add some metadata comments
COMMENT ON TABLE feature_flags IS 'Feature flags for controlling application features';
COMMENT ON TABLE experiments IS 'A/B testing experiments configuration';
COMMENT ON TABLE experiment_variants IS 'Variants for each experiment with their configurations';
COMMENT ON TABLE assignments IS 'User assignments to experiment variants';

COMMENT ON COLUMN feature_flags.enabled IS 'Whether the feature flag is currently active';
COMMENT ON COLUMN experiments.traffic_percentage IS 'Percentage of users who should see this experiment';
COMMENT ON COLUMN experiment_variants.weight IS 'Relative weight for variant selection within an experiment';
COMMENT ON COLUMN experiment_variants.payload IS 'JSON configuration data for the variant';
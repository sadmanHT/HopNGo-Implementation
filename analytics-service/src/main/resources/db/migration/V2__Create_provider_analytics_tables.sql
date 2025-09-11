-- Create provider metrics tables for analytics and SLA tracking

-- Provider bookings daily metrics
CREATE TABLE provider_bookings_daily (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    bookings INTEGER NOT NULL DEFAULT 0,
    cancellations INTEGER NOT NULL DEFAULT 0,
    revenue_minor BIGINT NOT NULL DEFAULT 0, -- Revenue in minor currency units (cents)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for provider_bookings_daily
CREATE UNIQUE INDEX idx_provider_bookings_daily_unique ON provider_bookings_daily(provider_id, date);
CREATE INDEX idx_provider_bookings_daily_provider ON provider_bookings_daily(provider_id);
CREATE INDEX idx_provider_bookings_daily_date ON provider_bookings_daily(date);
CREATE INDEX idx_provider_bookings_daily_revenue ON provider_bookings_daily(revenue_minor);

-- Provider response times metrics
CREATE TABLE provider_response_times (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(36) NOT NULL UNIQUE,
    avg_first_reply_sec INTEGER NOT NULL DEFAULT 0,
    total_conversations INTEGER NOT NULL DEFAULT 0,
    total_response_time_sec BIGINT NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for provider_response_times
CREATE INDEX idx_provider_response_times_provider ON provider_response_times(provider_id);
CREATE INDEX idx_provider_response_times_avg_reply ON provider_response_times(avg_first_reply_sec);
CREATE INDEX idx_provider_response_times_updated ON provider_response_times(updated_at);

-- Provider listing funnel metrics
CREATE TABLE provider_listing_funnel (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    impressions INTEGER NOT NULL DEFAULT 0,
    detail_views INTEGER NOT NULL DEFAULT 0,
    add_to_cart INTEGER NOT NULL DEFAULT 0,
    bookings INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for provider_listing_funnel
CREATE UNIQUE INDEX idx_provider_listing_funnel_unique ON provider_listing_funnel(provider_id, date);
CREATE INDEX idx_provider_listing_funnel_provider ON provider_listing_funnel(provider_id);
CREATE INDEX idx_provider_listing_funnel_date ON provider_listing_funnel(date);
CREATE INDEX idx_provider_listing_funnel_impressions ON provider_listing_funnel(impressions);

-- Provider SLA configuration table
CREATE TABLE provider_sla_config (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(36) NOT NULL UNIQUE,
    target_response_time_sec INTEGER NOT NULL DEFAULT 1800, -- 30 minutes default
    target_booking_conversion_rate DECIMAL(5,4) DEFAULT 0.05, -- 5% default
    target_monthly_revenue_minor BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for provider_sla_config
CREATE INDEX idx_provider_sla_config_provider ON provider_sla_config(provider_id);
CREATE INDEX idx_provider_sla_config_response_time ON provider_sla_config(target_response_time_sec);

-- Create views for easier querying

-- Provider daily summary view
CREATE VIEW provider_daily_summary AS
SELECT 
    pbd.provider_id,
    pbd.date,
    pbd.bookings,
    pbd.cancellations,
    pbd.revenue_minor,
    CASE 
        WHEN pbd.bookings > 0 THEN ROUND((pbd.cancellations::DECIMAL / pbd.bookings) * 100, 2)
        ELSE 0
    END as cancellation_rate_percent,
    plf.impressions,
    plf.detail_views,
    plf.add_to_cart,
    CASE 
        WHEN plf.impressions > 0 THEN ROUND((plf.detail_views::DECIMAL / plf.impressions) * 100, 2)
        ELSE 0
    END as impression_to_detail_rate_percent,
    CASE 
        WHEN plf.detail_views > 0 THEN ROUND((plf.add_to_cart::DECIMAL / plf.detail_views) * 100, 2)
        ELSE 0
    END as detail_to_cart_rate_percent,
    CASE 
        WHEN plf.add_to_cart > 0 THEN ROUND((plf.bookings::DECIMAL / plf.add_to_cart) * 100, 2)
        ELSE 0
    END as cart_to_booking_rate_percent
FROM provider_bookings_daily pbd
LEFT JOIN provider_listing_funnel plf ON pbd.provider_id = plf.provider_id AND pbd.date = plf.date;

-- Provider SLA performance view
CREATE VIEW provider_sla_performance AS
SELECT 
    prt.provider_id,
    prt.avg_first_reply_sec,
    psc.target_response_time_sec,
    CASE 
        WHEN prt.avg_first_reply_sec <= psc.target_response_time_sec THEN 'MEETING'
        WHEN prt.avg_first_reply_sec <= (psc.target_response_time_sec * 1.2) THEN 'WARNING'
        ELSE 'FAILING'
    END as sla_status,
    ROUND(((psc.target_response_time_sec::DECIMAL - prt.avg_first_reply_sec) / psc.target_response_time_sec) * 100, 2) as sla_performance_percent,
    prt.total_conversations,
    prt.last_calculated_at,
    psc.target_booking_conversion_rate,
    psc.target_monthly_revenue_minor
FROM provider_response_times prt
LEFT JOIN provider_sla_config psc ON prt.provider_id = psc.provider_id;

-- Add trigger to update updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_provider_bookings_daily_updated_at BEFORE UPDATE ON provider_bookings_daily FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_provider_response_times_updated_at BEFORE UPDATE ON provider_response_times FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_provider_listing_funnel_updated_at BEFORE UPDATE ON provider_listing_funnel FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_provider_sla_config_updated_at BEFORE UPDATE ON provider_sla_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
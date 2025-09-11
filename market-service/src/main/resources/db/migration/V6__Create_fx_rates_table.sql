-- Create FX rates table for currency exchange rates
CREATE TABLE fx_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rate_to_bdt DECIMAL(15,6) NOT NULL,
    source VARCHAR(50) DEFAULT 'MANUAL',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fx_rates_currency_check CHECK (LENGTH(currency) = 3),
    CONSTRAINT fx_rates_rate_positive CHECK (rate_to_bdt > 0),
    CONSTRAINT fx_rates_date_not_future CHECK (date <= CURRENT_DATE),
    
    -- Unique constraint for date-currency combination
    CONSTRAINT fx_rates_date_currency_unique UNIQUE (date, currency)
);

-- Create indexes for performance
CREATE INDEX idx_fx_rates_date ON fx_rates(date DESC);
CREATE INDEX idx_fx_rates_currency ON fx_rates(currency);
CREATE INDEX idx_fx_rates_date_currency ON fx_rates(date, currency);
CREATE INDEX idx_fx_rates_created_at ON fx_rates(created_at DESC);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_fx_rates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER fx_rates_updated_at_trigger
    BEFORE UPDATE ON fx_rates
    FOR EACH ROW
    EXECUTE FUNCTION update_fx_rates_updated_at();

-- Insert initial BDT rate (1.0 as base currency)
INSERT INTO fx_rates (date, currency, rate_to_bdt, source) 
VALUES (CURRENT_DATE, 'BDT', 1.000000, 'SYSTEM')
ON CONFLICT (date, currency) DO NOTHING;

-- Insert some common currency rates (these should be updated by the FX service)
-- USD to BDT (approximate rate as of 2024)
INSERT INTO fx_rates (date, currency, rate_to_bdt, source) 
VALUES (CURRENT_DATE, 'USD', 110.000000, 'INITIAL')
ON CONFLICT (date, currency) DO NOTHING;

-- EUR to BDT (approximate rate)
INSERT INTO fx_rates (date, currency, rate_to_bdt, source) 
VALUES (CURRENT_DATE, 'EUR', 120.000000, 'INITIAL')
ON CONFLICT (date, currency) DO NOTHING;

-- GBP to BDT (approximate rate)
INSERT INTO fx_rates (date, currency, rate_to_bdt, source) 
VALUES (CURRENT_DATE, 'GBP', 140.000000, 'INITIAL')
ON CONFLICT (date, currency) DO NOTHING;

-- INR to BDT (approximate rate)
INSERT INTO fx_rates (date, currency, rate_to_bdt, source) 
VALUES (CURRENT_DATE, 'INR', 1.320000, 'INITIAL')
ON CONFLICT (date, currency) DO NOTHING;

-- Create view for latest FX rates
CREATE OR REPLACE VIEW latest_fx_rates AS
SELECT DISTINCT ON (currency) 
    currency,
    rate_to_bdt,
    date,
    source,
    created_at
FROM fx_rates
ORDER BY currency, date DESC, created_at DESC;

-- Create function to get FX rate for a specific date and currency
CREATE OR REPLACE FUNCTION get_fx_rate(target_currency VARCHAR(3), target_date DATE DEFAULT CURRENT_DATE)
RETURNS DECIMAL(15,6) AS $$
DECLARE
    fx_rate DECIMAL(15,6);
BEGIN
    -- Get the most recent rate for the currency on or before the target date
    SELECT rate_to_bdt INTO fx_rate
    FROM fx_rates
    WHERE currency = target_currency
      AND date <= target_date
    ORDER BY date DESC, created_at DESC
    LIMIT 1;
    
    -- If no rate found, return NULL
    RETURN fx_rate;
END;
$$ LANGUAGE plpgsql;

-- Create function to convert amount between currencies
CREATE OR REPLACE FUNCTION convert_currency(
    amount_minor BIGINT,
    from_currency VARCHAR(3),
    to_currency VARCHAR(3),
    conversion_date DATE DEFAULT CURRENT_DATE
) RETURNS BIGINT AS $$
DECLARE
    from_rate DECIMAL(15,6);
    to_rate DECIMAL(15,6);
    bdt_amount DECIMAL(15,2);
    converted_amount DECIMAL(15,2);
BEGIN
    -- If same currency, return original amount
    IF from_currency = to_currency THEN
        RETURN amount_minor;
    END IF;
    
    -- Get exchange rates
    SELECT get_fx_rate(from_currency, conversion_date) INTO from_rate;
    SELECT get_fx_rate(to_currency, conversion_date) INTO to_rate;
    
    -- Check if rates are available
    IF from_rate IS NULL OR to_rate IS NULL THEN
        RAISE EXCEPTION 'Exchange rate not available for % to % on %', from_currency, to_currency, conversion_date;
    END IF;
    
    -- Convert to BDT first, then to target currency
    bdt_amount = (amount_minor::DECIMAL / 100.0) * from_rate;
    converted_amount = bdt_amount / to_rate;
    
    -- Return as minor units (rounded)
    RETURN ROUND(converted_amount * 100);
END;
$$ LANGUAGE plpgsql;

-- Add comments
COMMENT ON TABLE fx_rates IS 'Foreign exchange rates with BDT as base currency';
COMMENT ON COLUMN fx_rates.rate_to_bdt IS 'Exchange rate from currency to BDT (1 unit of currency = rate_to_bdt BDT)';
COMMENT ON COLUMN fx_rates.source IS 'Source of the exchange rate (API, MANUAL, SYSTEM, etc.)';
COMMENT ON VIEW latest_fx_rates IS 'Latest exchange rates for each currency';
COMMENT ON FUNCTION get_fx_rate IS 'Get exchange rate for a currency on a specific date';
COMMENT ON FUNCTION convert_currency IS 'Convert amount between currencies using exchange rates';
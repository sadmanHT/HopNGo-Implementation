-- Support Service Database Schema

-- Create enum types
CREATE TYPE ticket_status AS ENUM ('OPEN', 'PENDING', 'RESOLVED', 'CLOSED');
CREATE TYPE ticket_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE message_sender AS ENUM ('USER', 'AGENT', 'SYSTEM');

-- Tickets table
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    status ticket_status NOT NULL DEFAULT 'OPEN',
    priority ticket_priority NOT NULL DEFAULT 'MEDIUM',
    assigned_agent_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ticket messages table
CREATE TABLE ticket_messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    sender message_sender NOT NULL,
    sender_id VARCHAR(255),
    sender_name VARCHAR(255),
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Canned replies table
CREATE TABLE canned_replies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(100),
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Help articles table
CREATE TABLE help_articles (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    body_md TEXT NOT NULL,
    tags TEXT[] DEFAULT '{}',
    published BOOLEAN NOT NULL DEFAULT false,
    view_count INTEGER NOT NULL DEFAULT 0,
    author_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_tickets_user_id ON tickets(user_id);
CREATE INDEX idx_tickets_email ON tickets(email);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_assigned_agent ON tickets(assigned_agent_id);

CREATE INDEX idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);
CREATE INDEX idx_ticket_messages_created_at ON ticket_messages(created_at);

CREATE INDEX idx_canned_replies_category ON canned_replies(category);

CREATE INDEX idx_help_articles_slug ON help_articles(slug);
CREATE INDEX idx_help_articles_published ON help_articles(published);
CREATE INDEX idx_help_articles_tags ON help_articles USING GIN(tags);
CREATE INDEX idx_help_articles_title_search ON help_articles USING GIN(to_tsvector('english', title));
CREATE INDEX idx_help_articles_body_search ON help_articles USING GIN(to_tsvector('english', body_md));

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_tickets_updated_at BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_canned_replies_updated_at BEFORE UPDATE ON canned_replies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_help_articles_updated_at BEFORE UPDATE ON help_articles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert some default canned replies
INSERT INTO canned_replies (title, body, category, created_by) VALUES
('Welcome Message', 'Thank you for contacting HopNGo support. We have received your request and will respond within 24 hours.', 'General', 'system'),
('Account Issue Resolution', 'We have investigated your account issue and have applied the necessary fixes. Please try logging in again and let us know if you continue to experience problems.', 'Account', 'system'),
('Booking Confirmation', 'Your booking has been confirmed. You will receive a confirmation email shortly with all the details. If you need to make any changes, please contact us at least 24 hours before your scheduled time.', 'Booking', 'system'),
('Refund Process', 'We have processed your refund request. The refund will appear in your original payment method within 3-5 business days. You will receive an email confirmation once the refund is complete.', 'Billing', 'system'),
('Technical Support', 'We are aware of the technical issue you reported and our development team is working on a fix. We will notify you once the issue is resolved. Thank you for your patience.', 'Technical', 'system');

-- Insert some sample help articles
INSERT INTO help_articles (slug, title, body_md, tags, published, author_id) VALUES
('getting-started', 'Getting Started with HopNGo', '# Getting Started with HopNGo\n\nWelcome to HopNGo! This guide will help you get started with our platform.\n\n## Creating Your Account\n\n1. Visit our homepage\n2. Click "Sign Up"\n3. Fill in your details\n4. Verify your email\n\n## Making Your First Booking\n\n1. Search for your destination\n2. Browse available options\n3. Select your preferred choice\n4. Complete the booking process\n\n## Need Help?\n\nIf you need assistance, you can:\n- Contact our support team\n- Browse our help articles\n- Check our FAQ section', ARRAY['getting-started', 'basics', 'account'], true, 'system'),
('account-management', 'Managing Your Account', '# Managing Your Account\n\n## Profile Settings\n\nYou can update your profile information by:\n\n1. Logging into your account\n2. Navigating to "Profile Settings"\n3. Making your desired changes\n4. Saving your updates\n\n## Privacy Settings\n\nControl your privacy preferences:\n\n- Visibility settings\n- Communication preferences\n- Data sharing options\n\n## Security\n\nKeep your account secure:\n\n- Use a strong password\n- Enable two-factor authentication\n- Regularly review account activity', ARRAY['account', 'profile', 'security'], true, 'system'),
('booking-help', 'Booking Help and FAQ', '# Booking Help\n\n## How to Make a Booking\n\nFollow these simple steps:\n\n1. **Search**: Enter your destination and dates\n2. **Browse**: Review available options\n3. **Select**: Choose your preferred option\n4. **Book**: Complete the booking process\n\n## Cancellation Policy\n\n- Free cancellation up to 24 hours before\n- Partial refund for cancellations within 24 hours\n- No refund for no-shows\n\n## Payment Methods\n\nWe accept:\n- Credit/Debit cards\n- PayPal\n- Bank transfers\n- Digital wallets\n\n## Common Issues\n\n### Booking Not Confirmed\n\nIf your booking is not confirmed:\n\n1. Check your email for confirmation\n2. Verify payment was processed\n3. Contact support if issues persist', ARRAY['booking', 'payment', 'cancellation'], true, 'system');
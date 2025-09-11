-- Add 2FA fields to users table
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN is_2fa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN backup_codes TEXT;

-- Create index for performance
CREATE INDEX idx_users_2fa_enabled ON users(is_2fa_enabled);
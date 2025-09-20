-- Initial data for Auth Service

-- Insert admin user (password: admin123)
-- BCrypt hash for 'admin123' with strength 12
INSERT INTO users (email, password_hash, first_name, last_name, role, is_active) 
VALUES (
    'admin@hopngo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfBLDdisaoD0BPW',
    'Admin',
    'User',
    'ADMIN',
    true
);

-- Insert test user (password: user123)  
-- BCrypt hash for 'user123' with strength 12
INSERT INTO users (email, password_hash, first_name, last_name, role, is_active) 
VALUES (
    'user@hopngo.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'Test',
    'User',
    'USER',
    true
);
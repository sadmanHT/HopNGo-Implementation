-- Demo users for testing and development
-- Password for all users: 'password123' (hashed)

INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_verified, created_at, updated_at) VALUES
-- Customers
('550e8400-e29b-41d4-a716-446655440001', 'john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'John', 'Doe', 'CUSTOMER', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440002', 'jane.smith@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Jane', 'Smith', 'CUSTOMER', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440003', 'mike.johnson@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Mike', 'Johnson', 'CUSTOMER', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440004', 'sarah.wilson@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Sarah', 'Wilson', 'CUSTOMER', true, NOW(), NOW()),

-- Vendors
('550e8400-e29b-41d4-a716-446655440005', 'hotel.manager@grandhotel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Robert', 'Anderson', 'VENDOR', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440006', 'owner@cozycabin.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Emily', 'Brown', 'VENDOR', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440007', 'contact@luxuryresort.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'David', 'Martinez', 'VENDOR', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440008', 'info@beachvilla.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Lisa', 'Garcia', 'VENDOR', true, NOW(), NOW()),

-- Admins
('550e8400-e29b-41d4-a716-446655440009', 'admin@hopngo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Admin', 'User', 'ADMIN', true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440010', 'support@hopngo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.Uo0yCQOXYsxyd4Lo/Oz0laUd.LadAa', 'Support', 'Team', 'ADMIN', true, NOW(), NOW());

-- User profiles
INSERT INTO user_profiles (user_id, phone_number, date_of_birth, address, city, country, postal_code, profile_picture_url, bio, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', '+1-555-0101', '1990-05-15', '123 Main St', 'New York', 'USA', '10001', 'https://example.com/avatars/john.jpg', 'Love traveling and exploring new places!', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440002', '+1-555-0102', '1988-08-22', '456 Oak Ave', 'Los Angeles', 'USA', '90210', 'https://example.com/avatars/jane.jpg', 'Adventure seeker and food enthusiast.', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440003', '+1-555-0103', '1992-12-03', '789 Pine Rd', 'Chicago', 'USA', '60601', 'https://example.com/avatars/mike.jpg', 'Business traveler who enjoys luxury stays.', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440004', '+1-555-0104', '1985-03-18', '321 Elm St', 'Miami', 'USA', '33101', 'https://example.com/avatars/sarah.jpg', 'Beach lover and wellness enthusiast.', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440005', '+1-555-0105', '1975-11-30', '555 Hotel Blvd', 'Las Vegas', 'USA', '89101', 'https://example.com/avatars/robert.jpg', 'Hospitality professional with 20+ years experience.', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440006', '+1-555-0106', '1980-07-14', '777 Mountain View', 'Denver', 'USA', '80201', 'https://example.com/avatars/emily.jpg', 'Mountain cabin owner, nature lover.', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440007', '+1-555-0107', '1970-09-25', '999 Resort Way', 'Honolulu', 'USA', '96801', 'https://example.com/avatars/david.jpg', 'Luxury resort manager in paradise.', NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440008', '+1-555-0108', '1983-04-12', '111 Beach Dr', 'San Diego', 'USA', '92101', 'https://example.com/avatars/lisa.jpg', 'Beachfront property owner and surfer.', NOW(), NOW());
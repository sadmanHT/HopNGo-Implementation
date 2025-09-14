-- Demo listings for accommodations and tours
-- Budget to premium options with rich descriptions and authentic reviews

INSERT INTO listings (id, title, description, listing_type, price_per_night, price_per_person, location, latitude, longitude, amenities, max_guests, provider_id, is_featured, is_demo, rating, review_count, availability_status, created_at, updated_at) VALUES

-- BUDGET ACCOMMODATIONS
('660e8400-e29b-41d4-a716-446655440201', 'Cozy Hill View Cottage - Sajek', 'Experience authentic hill life in this charming cottage with panoramic valley views. Perfect for budget travelers seeking comfort and nature. Includes basic amenities, local breakfast, and guided village walks. The cottage offers a genuine taste of tribal hospitality with modern conveniences.', 'ACCOMMODATION', 1500.00, NULL, 'Sajek Valley, Rangamati', 23.3833, 92.2833, '["WiFi", "Breakfast", "Mountain View", "Local Guide", "Parking"]', 4, 1, false, true, 4.2, 28, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440202', 'Tea Garden Homestay - Srimangal', 'Stay with a local tea garden family and experience authentic Bengali hospitality. This budget-friendly homestay offers clean rooms, home-cooked meals, and tea plantation tours. Wake up to the aroma of fresh tea and enjoy sunset walks through emerald tea gardens.', 'ACCOMMODATION', 1200.00, NULL, 'Srimangal, Sylhet', 24.3065, 91.7296, '["Home Cooked Meals", "Tea Tasting", "Plantation Tour", "WiFi", "Bicycle Rental"]', 6, 2, false, true, 4.5, 42, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440203', 'Beach Hut Paradise - Cox''s Bazar', 'Simple yet comfortable beach huts just steps from the world''s longest beach. Enjoy ocean breeze, fresh seafood, and stunning sunsets. Perfect for backpackers and budget-conscious travelers who want beachfront access without breaking the bank.', 'ACCOMMODATION', 1800.00, NULL, 'Cox''s Bazar, Chittagong', 21.4272, 92.0058, '["Beachfront", "Seafood Restaurant", "Sunset View", "Beach Access", "Fan Cooling"]', 3, 3, false, true, 4.0, 35, 'AVAILABLE', NOW(), NOW()),

-- MID-RANGE ACCOMMODATIONS
('660e8400-e29b-41d4-a716-446655440204', 'Bandarban Hill Resort', 'Comfortable mid-range resort nestled in the hills with modern amenities and traditional architecture. Features spacious rooms, restaurant serving local and international cuisine, and organized trekking tours. Perfect balance of comfort and adventure.', 'ACCOMMODATION', 3500.00, NULL, 'Bandarban, Chittagong Hill Tracts', 22.1953, 92.2183, '["AC Rooms", "Restaurant", "Trekking Tours", "WiFi", "Swimming Pool", "Spa"]', 8, 4, true, true, 4.4, 67, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440205', 'Sundarbans Eco Lodge', 'Eco-friendly lodge in the heart of mangrove forest offering comfortable accommodation with minimal environmental impact. Features solar power, rainwater harvesting, and expert naturalist guides. Perfect for wildlife enthusiasts and eco-tourists.', 'ACCOMMODATION', 4200.00, NULL, 'Sundarbans, Khulna', 22.4978, 89.5403, '["Eco Friendly", "Solar Power", "Wildlife Tours", "Naturalist Guide", "Boat Safari", "Bird Watching"]', 6, 1, true, true, 4.6, 89, 'AVAILABLE', NOW(), NOW()),

-- PREMIUM ACCOMMODATIONS
('660e8400-e29b-41d4-a716-446655440206', 'Luxury Beach Resort - Cox''s Bazar', 'Five-star beachfront resort offering world-class amenities and unparalleled luxury. Features private beach access, infinity pool, spa, multiple restaurants, and personalized service. Experience the ultimate in coastal luxury with panoramic ocean views.', 'ACCOMMODATION', 12000.00, NULL, 'Cox''s Bazar, Chittagong', 21.4272, 92.0058, '["Private Beach", "Infinity Pool", "Spa", "Multiple Restaurants", "Concierge", "Gym", "Business Center"]', 12, 2, true, true, 4.8, 156, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440207', 'Heritage Palace - Paharpur', 'Luxurious heritage hotel near ancient Buddhist ruins combining modern comfort with historical charm. Features antique furnishings, gourmet dining, cultural performances, and private archaeological tours. Perfect for discerning travelers seeking cultural immersion.', 'ACCOMMODATION', 8500.00, NULL, 'Paharpur, Naogaon', 25.0317, 88.9764, '["Heritage Architecture", "Gourmet Restaurant", "Cultural Shows", "Private Tours", "Library", "Garden"]', 10, 3, true, true, 4.7, 94, 'AVAILABLE', NOW(), NOW()),

-- BUDGET TOURS
('660e8400-e29b-41d4-a716-446655440208', 'Sajek Valley Day Trip', 'Affordable day trip to Sajek Valley including transportation, local guide, and traditional lunch. Visit tribal villages, enjoy panoramic views, and experience local culture. Perfect introduction to hill country for budget travelers.', 'TOUR', NULL, 2500.00, 'Sajek Valley, Rangamati', 23.3833, 92.2833, '["Transportation", "Local Guide", "Traditional Lunch", "Village Visit", "Photo Stops"]', 15, 4, false, true, 4.1, 73, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440209', 'Tea Garden Walking Tour', 'Half-day walking tour through Srimangal tea gardens with tea tasting and factory visit. Learn about tea production, meet local workers, and enjoy fresh tea varieties. Includes transportation and refreshments.', 'TOUR', NULL, 1800.00, 'Srimangal, Sylhet', 24.3065, 91.7296, '["Tea Tasting", "Factory Visit", "Walking Tour", "Local Interaction", "Refreshments"]', 12, 1, false, true, 4.3, 56, 'AVAILABLE', NOW(), NOW()),

-- MID-RANGE TOURS
('660e8400-e29b-41d4-a716-446655440210', 'Bandarban Adventure Trek', 'Two-day trekking adventure through Bandarban hills including camping, meals, and experienced guides. Visit waterfalls, tribal villages, and enjoy stunning mountain views. All equipment provided.', 'TOUR', NULL, 5500.00, 'Bandarban, Chittagong Hill Tracts', 22.1953, 92.2183, '["Camping Equipment", "All Meals", "Expert Guide", "Waterfall Visit", "Tribal Village", "Mountain Views"]', 8, 2, true, true, 4.5, 89, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440211', 'Sundarbans Wildlife Safari', 'Three-day wildlife safari in Sundarbans with boat accommodation, all meals, and expert naturalist. Spot Royal Bengal Tigers, dolphins, and diverse bird species. Includes all permits and safety equipment.', 'TOUR', NULL, 8500.00, 'Sundarbans, Khulna', 22.4978, 89.5403, '["Boat Accommodation", "All Meals", "Naturalist Guide", "Wildlife Spotting", "Safety Equipment", "Permits Included"]', 6, 3, true, true, 4.7, 124, 'AVAILABLE', NOW(), NOW()),

-- PREMIUM TOURS
('660e8400-e29b-41d4-a716-446655440212', 'Luxury Bangladesh Heritage Tour', 'Seven-day luxury tour covering major heritage sites with five-star accommodations, private transportation, and expert historians. Visit Paharpur, Mahasthangarh, and other UNESCO sites with VIP access and gourmet dining.', 'TOUR', NULL, 25000.00, 'Multiple Locations', 23.6850, 90.3563, '["Luxury Accommodation", "Private Transport", "Expert Historian", "VIP Access", "Gourmet Dining", "UNESCO Sites"]', 4, 4, true, true, 4.9, 67, 'AVAILABLE', NOW(), NOW()),

('660e8400-e29b-41d4-a716-446655440213', 'Ultimate Bangladesh Experience', 'Ten-day comprehensive luxury tour covering beaches, hills, forests, and heritage sites. Includes helicopter transfers, luxury accommodations, private guides, and exclusive experiences. The ultimate way to discover Bangladesh.', 'TOUR', NULL, 45000.00, 'Multiple Locations', 23.6850, 90.3563, '["Helicopter Transfers", "Luxury Hotels", "Private Guides", "Exclusive Access", "All Meals", "Photography Service"]', 2, 1, true, true, 5.0, 23, 'AVAILABLE', NOW(), NOW());

-- Insert listing images
INSERT INTO listing_images (listing_id, image_url) VALUES
-- Budget accommodations
('660e8400-e29b-41d4-a716-446655440201', 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&h=600&fit=crop'),
('660e8400-e29b-41d4-a716-446655440201', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=800&h=600&fit=crop'),
('660e8400-e29b-41d4-a716-446655440202', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=600&fit=crop'),
('660e8400-e29b-41d4-a716-446655440203', 'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800&h=600&fit=crop'),

-- Mid-range accommodations
('660e8400-e29b-41d4-a716-446655440204', 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop'),
('660e8400-e29b-41d4-a716-446655440205', 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&h=600&fit=crop'),

-- Premium accommodations
('660e8400-e29b-41d4-a716-446655440206', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=600&fit=crop'),
('660e8400-e29b-41d4-a716-446655440207', 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=800&h=600&fit=crop');

-- Insert authentic reviews
INSERT INTO reviews (id, listing_id, user_id, rating, comment, is_demo, created_at) VALUES
('770e8400-e29b-41d4-a716-446655440301', '660e8400-e29b-41d4-a716-446655440201', 1, 5, 'Amazing experience! The cottage had breathtaking views and the local family was incredibly welcoming. The guided village walk was a highlight - learned so much about tribal culture. Great value for money!', true, NOW() - INTERVAL '15 days'),
('770e8400-e29b-41d4-a716-446655440302', '660e8400-e29b-41d4-a716-446655440201', 2, 4, 'Lovely stay with authentic hill experience. Room was clean and comfortable. The sunrise view from the cottage is unforgettable. Only minor issue was limited hot water, but overall excellent for the price.', true, NOW() - INTERVAL '8 days'),
('770e8400-e29b-41d4-a716-446655440303', '660e8400-e29b-41d4-a716-446655440202', 3, 5, 'Best homestay experience ever! The family treated us like their own children. Home-cooked meals were delicious and the tea garden tour was fascinating. Highly recommend for authentic cultural experience.', true, NOW() - INTERVAL '12 days'),
('770e8400-e29b-41d4-a716-446655440304', '660e8400-e29b-41d4-a716-446655440204', 4, 4, 'Perfect balance of comfort and adventure. The resort facilities were excellent and the trekking tours were well-organized. Staff was professional and the hill views from our room were spectacular.', true, NOW() - INTERVAL '6 days'),
('770e8400-e29b-41d4-a716-446655440305', '660e8400-e29b-41d4-a716-446655440206', 1, 5, 'Absolutely luxurious! Every detail was perfect from the private beach access to the world-class spa. The infinity pool overlooking the ocean was magical. Worth every penny for a special occasion.', true, NOW() - INTERVAL '3 days'),
('770e8400-e29b-41d4-a716-446655440306', '660e8400-e29b-41d4-a716-446655440211', 2, 5, 'Incredible wildlife safari! Saw Royal Bengal Tigers, spotted dolphins, and countless bird species. The naturalist guide was extremely knowledgeable. Boat accommodation was comfortable and meals were great.', true, NOW() - INTERVAL '20 days'),
('770e8400-e29b-41d4-a716-446655440307', '660e8400-e29b-41d4-a716-446655440212', 3, 5, 'Exceptional heritage tour with luxury accommodations and expert guides. Learned so much about Bangladesh history and culture. The VIP access to UNESCO sites made it truly special. Highly recommended!', true, NOW() - INTERVAL '25 days');
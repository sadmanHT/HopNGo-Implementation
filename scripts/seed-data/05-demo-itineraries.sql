-- Demo itinerary templates for 3-day, 7-day, and 10-day trips
-- Comprehensive travel plans with activities, costs, and scheduling

INSERT INTO itineraries (id, title, description, duration_days, estimated_cost, difficulty_level, category, is_featured, is_demo, user_id, created_at, updated_at) VALUES

-- 3-DAY ITINERARIES
('880e8400-e29b-41d4-a716-446655440401', 'Cox''s Bazar Beach Getaway', 'Perfect 3-day beach escape featuring the world''s longest natural sea beach. Enjoy stunning sunsets, fresh seafood, and relaxing beach activities. Ideal for couples and families seeking a quick coastal retreat.', 3, 15000.00, 'EASY', 'Beach', true, true, 1, NOW(), NOW()),

('880e8400-e29b-41d4-a716-446655440402', 'Srimangal Tea Country Explorer', 'Immerse yourself in Bangladesh''s tea capital with plantation tours, tea tasting, and nature walks. Experience authentic Bengali hospitality and discover the famous seven-layer tea. Perfect for nature lovers and culture enthusiasts.', 3, 12000.00, 'EASY', 'Nature', true, true, 2, NOW(), NOW()),

('880e8400-e29b-41d4-a716-446655440403', 'Sajek Valley Hill Adventure', 'Escape to the "Queen of Hills" for breathtaking mountain views, tribal culture, and cloud-kissed peaks. Experience sunrise above the clouds and enjoy traditional hill station activities.', 3, 18000.00, 'MODERATE', 'Adventure', false, true, 3, NOW(), NOW()),

-- 7-DAY ITINERARIES
('880e8400-e29b-41d4-a716-446655440404', 'Bangladesh Highlights Circuit', 'Comprehensive 7-day journey covering beaches, hills, and heritage sites. Experience the diversity of Bangladesh from Cox''s Bazar beaches to Bandarban hills and ancient Buddhist ruins at Paharpur.', 7, 45000.00, 'MODERATE', 'Cultural', true, true, 4, NOW(), NOW()),

('880e8400-e29b-41d4-a716-446655440405', 'Chittagong Hill Tracts Adventure', 'Week-long adventure through the scenic hill districts featuring trekking, tribal culture, and pristine nature. Visit Bandarban, Rangamati, and Khagrachari for an authentic hill country experience.', 7, 38000.00, 'CHALLENGING', 'Adventure', true, true, 1, NOW(), NOW()),

('880e8400-e29b-41d4-a716-446655440406', 'Wildlife & Nature Discovery', 'Seven days exploring Bangladesh''s natural wonders including Sundarbans mangrove forest, Ratargul swamp forest, and various national parks. Perfect for wildlife enthusiasts and photographers.', 7, 42000.00, 'MODERATE', 'Wildlife', false, true, 2, NOW(), NOW()),

-- 10-DAY ITINERARIES
('880e8400-e29b-41d4-a716-446655440407', 'Ultimate Bangladesh Experience', 'The most comprehensive Bangladesh tour covering all major destinations. From bustling Dhaka to serene Sundarbans, from hill stations to ancient heritage sites. The complete Bangladesh experience.', 10, 75000.00, 'MODERATE', 'Comprehensive', true, true, 3, NOW(), NOW()),

('880e8400-e29b-41d4-a716-446655440408', 'Heritage & Culture Immersion', 'Deep dive into Bangladesh''s rich cultural heritage visiting UNESCO World Heritage sites, ancient ruins, traditional villages, and experiencing authentic local life across the country.', 10, 65000.00, 'EASY', 'Cultural', true, true, 4, NOW(), NOW()),

('880e8400-e29b-41d4-a716-446655440409', 'Adventure Seeker''s Paradise', 'Ten days of thrilling adventures including hill trekking, river rafting, wildlife safaris, and extreme sports. Perfect for adrenaline junkies and adventure enthusiasts.', 10, 85000.00, 'CHALLENGING', 'Adventure', false, true, 1, NOW(), NOW());

-- Insert itinerary days
INSERT INTO itinerary_days (id, itinerary_id, day_number, title, description, location) VALUES

-- Cox's Bazar Beach Getaway (3 days)
('990e8400-e29b-41d4-a716-446655440501', '880e8400-e29b-41d4-a716-446655440401', 1, 'Arrival & Beach Exploration', 'Arrive in Cox''s Bazar, check into beachfront accommodation, and spend the afternoon exploring the world''s longest beach. Evening sunset viewing and seafood dinner.', 'Cox''s Bazar'),
('990e8400-e29b-41d4-a716-446655440502', '880e8400-e29b-41d4-a716-446655440401', 2, 'Beach Activities & Local Culture', 'Full day of beach activities including water sports, local market visit, and interaction with fishing communities. Afternoon visit to nearby Buddhist temples.', 'Cox''s Bazar'),
('990e8400-e29b-41d4-a716-446655440503', '880e8400-e29b-41d4-a716-446655440401', 3, 'Relaxation & Departure', 'Morning beach walk, spa treatment, and souvenir shopping. Afternoon departure with beautiful memories of coastal Bangladesh.', 'Cox''s Bazar'),

-- Srimangal Tea Country Explorer (3 days)
('990e8400-e29b-41d4-a716-446655440504', '880e8400-e29b-41d4-a716-446655440402', 1, 'Tea Garden Introduction', 'Arrive in Srimangal, check into tea garden homestay, and take introductory walk through tea plantations. Evening tea tasting session with local varieties.', 'Srimangal'),
('990e8400-e29b-41d4-a716-446655440505', '880e8400-e29b-41d4-a716-446655440402', 2, 'Deep Tea Experience', 'Full day tea plantation tour including factory visit, meeting tea workers, and learning about tea production. Afternoon visit to Lawachara National Park for wildlife spotting.', 'Srimangal'),
('990e8400-e29b-41d4-a716-446655440506', '880e8400-e29b-41d4-a716-446655440402', 3, 'Seven-Layer Tea & Departure', 'Morning visit to famous seven-layer tea shops, final tea garden walk, and purchase of premium tea varieties. Afternoon departure.', 'Srimangal'),

-- Bangladesh Highlights Circuit (7 days)
('990e8400-e29b-41d4-a716-446655440507', '880e8400-e29b-41d4-a716-446655440404', 1, 'Dhaka Exploration', 'Arrive in Dhaka, city tour including Old Dhaka, Lalbagh Fort, and Ahsan Manzil. Evening river cruise on Buriganga River.', 'Dhaka'),
('990e8400-e29b-41d4-a716-446655440508', '880e8400-e29b-41d4-a716-446655440404', 2, 'Journey to Cox''s Bazar', 'Morning flight to Cox''s Bazar, check into beach resort, afternoon beach exploration and sunset viewing.', 'Cox''s Bazar'),
('990e8400-e29b-41d4-a716-446655440509', '880e8400-e29b-41d4-a716-446655440404', 3, 'Beach Day & Hill Journey', 'Morning beach activities, afternoon travel to Bandarban hill district, evening arrival and local dinner.', 'Bandarban'),
('990e8400-e29b-41d4-a716-446655440510', '880e8400-e29b-41d4-a716-446655440404', 4, 'Hill Trekking Adventure', 'Full day trekking in Bandarban hills, visit to tribal villages, waterfall exploration, and mountain viewpoints.', 'Bandarban'),
('990e8400-e29b-41d4-a716-446655440511', '880e8400-e29b-41d4-a716-446655440404', 5, 'Srimangal Tea Gardens', 'Travel to Srimangal, tea plantation tour, factory visit, and tea tasting experience.', 'Srimangal'),
('990e8400-e29b-41d4-a716-446655440512', '880e8400-e29b-41d4-a716-446655440404', 6, 'Heritage Sites Visit', 'Travel to Paharpur, explore ancient Buddhist monastery ruins, visit local archaeological museum.', 'Paharpur'),
('990e8400-e29b-41d4-a716-446655440513', '880e8400-e29b-41d4-a716-446655440404', 7, 'Return to Dhaka', 'Morning heritage site exploration, afternoon return to Dhaka, evening departure preparations.', 'Dhaka'),

-- Ultimate Bangladesh Experience (10 days)
('990e8400-e29b-41d4-a716-446655440514', '880e8400-e29b-41d4-a716-446655440407', 1, 'Dhaka Arrival & City Tour', 'Comprehensive Dhaka exploration including historical sites, museums, and cultural centers.', 'Dhaka'),
('990e8400-e29b-41d4-a716-446655440515', '880e8400-e29b-41d4-a716-446655440407', 2, 'Sundarbans Safari Begin', 'Travel to Sundarbans, board safari boat, begin wildlife exploration in mangrove forest.', 'Sundarbans'),
('990e8400-e29b-41d4-a716-446655440516', '880e8400-e29b-41d4-a716-446655440407', 3, 'Deep Sundarbans Exploration', 'Full day wildlife safari, tiger spotting, bird watching, and mangrove ecosystem study.', 'Sundarbans'),
('990e8400-e29b-41d4-a716-446655440517', '880e8400-e29b-41d4-a716-446655440407', 4, 'Cox''s Bazar Beach Paradise', 'Travel to Cox''s Bazar, beach resort check-in, sunset viewing and seafood dining.', 'Cox''s Bazar'),
('990e8400-e29b-41d4-a716-446655440518', '880e8400-e29b-41d4-a716-446655440407', 5, 'Beach & Water Activities', 'Full day beach activities, water sports, local market exploration, and cultural interactions.', 'Cox''s Bazar'),
('990e8400-e29b-41d4-a716-446655440519', '880e8400-e29b-41d4-a716-446655440407', 6, 'Hill Country Adventure', 'Travel to Bandarban, hill resort check-in, evening tribal cultural program.', 'Bandarban'),
('990e8400-e29b-41d4-a716-446655440520', '880e8400-e29b-41d4-a716-446655440407', 7, 'Mountain Trekking & Villages', 'Challenging trek to highest peaks, tribal village visits, traditional lunch with locals.', 'Bandarban'),
('990e8400-e29b-41d4-a716-446655440521', '880e8400-e29b-41d4-a716-446655440407', 8, 'Tea Country Experience', 'Travel to Srimangal, tea plantation immersion, factory tours, and premium tea tasting.', 'Srimangal'),
('990e8400-e29b-41d4-a716-446655440522', '880e8400-e29b-41d4-a716-446655440407', 9, 'Heritage & History', 'Visit Paharpur Buddhist ruins, Mahasthangarh ancient city, and archaeological museums.', 'Paharpur'),
('990e8400-e29b-41d4-a716-446655440523', '880e8400-e29b-41d4-a716-446655440407', 10, 'Final Dhaka & Departure', 'Return to Dhaka, final shopping, cultural show, and departure preparations.', 'Dhaka');

-- Insert itinerary activities
INSERT INTO itinerary_activities (id, itinerary_day_id, title, description, location, start_time, end_time, activity_type, estimated_cost, listing_id) VALUES

-- Day 1 Cox's Bazar activities
('aa0e8400-e29b-41d4-a716-446655440601', '990e8400-e29b-41d4-a716-446655440501', 'Airport Transfer', 'Comfortable transfer from Cox''s Bazar airport to beachfront hotel', 'Cox''s Bazar Airport', '14:00:00', '15:00:00', 'TRANSPORT', 800.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440602', '990e8400-e29b-41d4-a716-446655440501', 'Beach Walk & Exploration', 'First glimpse of the world''s longest beach with guided orientation walk', 'Cox''s Bazar Beach', '16:00:00', '18:00:00', 'SIGHTSEEING', 0.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440603', '990e8400-e29b-41d4-a716-446655440501', 'Sunset Viewing', 'Spectacular sunset viewing from the beach with photography opportunities', 'Cox''s Bazar Beach', '18:00:00', '19:00:00', 'SIGHTSEEING', 0.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440604', '990e8400-e29b-41d4-a716-446655440501', 'Seafood Dinner', 'Welcome dinner featuring fresh local seafood specialties', 'Beachfront Restaurant', '19:30:00', '21:00:00', 'DINING', 1500.00, NULL),

-- Day 2 Cox's Bazar activities
('aa0e8400-e29b-41d4-a716-446655440605', '990e8400-e29b-41d4-a716-446655440502', 'Water Sports Package', 'Jet skiing, parasailing, and banana boat rides with safety equipment', 'Cox''s Bazar Beach', '09:00:00', '12:00:00', 'ADVENTURE', 3500.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440606', '990e8400-e29b-41d4-a716-446655440502', 'Local Market Tour', 'Guided tour of fish market and local bazaar with cultural insights', 'Cox''s Bazar Market', '14:00:00', '16:00:00', 'CULTURAL', 500.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440607', '990e8400-e29b-41d4-a716-446655440502', 'Buddhist Temple Visit', 'Visit to nearby Aggmeda Khyang Buddhist monastery', 'Aggmeda Khyang', '16:30:00', '18:00:00', 'CULTURAL', 200.00, NULL),

-- Srimangal Day 1 activities
('aa0e8400-e29b-41d4-a716-446655440608', '990e8400-e29b-41d4-a716-446655440504', 'Homestay Check-in', 'Welcome to authentic tea garden family homestay', 'Tea Garden Homestay', '15:00:00', '16:00:00', 'ACCOMMODATION', 0.00, '660e8400-e29b-41d4-a716-446655440202'),
('aa0e8400-e29b-41d4-a716-446655440609', '990e8400-e29b-41d4-a716-446655440504', 'Tea Plantation Walk', 'Introductory walk through emerald tea gardens with local guide', 'Srimangal Tea Gardens', '16:30:00', '18:00:00', 'NATURE', 300.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440610', '990e8400-e29b-41d4-a716-446655440504', 'Tea Tasting Session', 'Professional tea tasting with different varieties and brewing techniques', 'Homestay', '19:00:00', '20:00:00', 'CULTURAL', 500.00, NULL),

-- Bangladesh Highlights Day 1 activities
('aa0e8400-e29b-41d4-a716-446655440611', '990e8400-e29b-41d4-a716-446655440507', 'Old Dhaka Heritage Walk', 'Guided walk through historic Old Dhaka including Lalbagh Fort', 'Old Dhaka', '09:00:00', '13:00:00', 'CULTURAL', 1200.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440612', '990e8400-e29b-41d4-a716-446655440507', 'Ahsan Manzil Visit', 'Explore the Pink Palace and learn about Dhaka''s Nawab history', 'Ahsan Manzil', '14:00:00', '16:00:00', 'CULTURAL', 300.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440613', '990e8400-e29b-41d4-a716-446655440507', 'Buriganga River Cruise', 'Evening boat cruise on historic Buriganga River with city views', 'Buriganga River', '17:00:00', '19:00:00', 'SIGHTSEEING', 800.00, NULL),

-- Ultimate Bangladesh Day 2 Sundarbans activities
('aa0e8400-e29b-41d4-a716-446655440614', '990e8400-e29b-41d4-a716-446655440515', 'Sundarbans Safari Boat', 'Board comfortable safari boat for mangrove forest exploration', 'Mongla Port', '08:00:00', '09:00:00', 'TRANSPORT', 0.00, '660e8400-e29b-41d4-a716-446655440211'),
('aa0e8400-e29b-41d4-a716-446655440615', '990e8400-e29b-41d4-a716-446655440515', 'Mangrove Forest Entry', 'First entry into Sundarbans with wildlife briefing', 'Sundarbans', '10:00:00', '12:00:00', 'WILDLIFE', 1500.00, NULL),
('aa0e8400-e29b-41d4-a716-446655440616', '990e8400-e29b-41d4-a716-446655440515', 'Tiger Spotting Safari', 'Afternoon tiger spotting expedition with expert naturalist', 'Sundarbans Core Area', '14:00:00', '17:00:00', 'WILDLIFE', 2500.00, NULL);
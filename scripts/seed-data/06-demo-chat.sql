-- Demo chat conversations and provider responses
-- Fake users flagged as demo with realistic conversation flows

INSERT INTO chat_conversations (id, title, conversation_type, status, user_id, provider_id, listing_id, is_demo, created_at, updated_at) VALUES

-- Booking inquiries
('bb0e8400-e29b-41d4-a716-446655440701', 'Sajek Valley Cottage Booking', 'BOOKING_INQUIRY', 'ACTIVE', 1, 1, '660e8400-e29b-41d4-a716-446655440201', true, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '5 minutes'),
('bb0e8400-e29b-41d4-a716-446655440702', 'Tea Garden Homestay Questions', 'BOOKING_INQUIRY', 'ACTIVE', 2, 2, '660e8400-e29b-41d4-a716-446655440202', true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '30 minutes'),
('bb0e8400-e29b-41d4-a716-446655440703', 'Luxury Beach Resort Inquiry', 'BOOKING_INQUIRY', 'RESOLVED', 3, 2, '660e8400-e29b-41d4-a716-446655440206', true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day'),

-- Tour planning
('bb0e8400-e29b-41d4-a716-446655440704', 'Bandarban Trekking Tour', 'TOUR_PLANNING', 'ACTIVE', 4, 2, '660e8400-e29b-41d4-a716-446655440210', true, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '15 minutes'),
('bb0e8400-e29b-41d4-a716-446655440705', 'Sundarbans Wildlife Safari', 'TOUR_PLANNING', 'ACTIVE', 1, 3, '660e8400-e29b-41d4-a716-446655440211', true, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '10 minutes'),

-- General inquiries
('bb0e8400-e29b-41d4-a716-446655440706', 'Cox''s Bazar Travel Tips', 'GENERAL_INQUIRY', 'RESOLVED', 2, 3, NULL, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
('bb0e8400-e29b-41d4-a716-446655440707', 'Heritage Tour Customization', 'GENERAL_INQUIRY', 'ACTIVE', 3, 4, '660e8400-e29b-41d4-a716-446655440212', true, NOW() - INTERVAL '8 hours', NOW() - INTERVAL '20 minutes'),

-- Emergency support
('bb0e8400-e29b-41d4-a716-446655440708', 'Weather Update Request', 'EMERGENCY_SUPPORT', 'RESOLVED', 4, 1, NULL, true, NOW() - INTERVAL '12 hours', NOW() - INTERVAL '8 hours');

-- Insert chat messages
INSERT INTO chat_messages (id, conversation_id, content, sender_type, message_type, user_id, provider_id, created_at) VALUES

-- Sajek Valley Cottage Booking conversation
('cc0e8400-e29b-41d4-a716-446655440801', 'bb0e8400-e29b-41d4-a716-446655440701', 'Hi! I''m interested in booking your Cozy Hill View Cottage for next weekend. Is it available?', 'USER', 'TEXT', 1, NULL, NOW() - INTERVAL '2 hours'),
('cc0e8400-e29b-41d4-a716-446655440802', 'bb0e8400-e29b-41d4-a716-446655440701', 'Hello! Yes, the cottage is available for next weekend. It''s perfect timing as the weather forecast shows clear skies for amazing valley views! 🏔️', 'PROVIDER', 'TEXT', NULL, 1, NOW() - INTERVAL '1 hour 50 minutes'),
('cc0e8400-e29b-41d4-a716-446655440803', 'bb0e8400-e29b-41d4-a716-446655440701', 'That sounds perfect! What''s included in the stay? I saw breakfast mentioned in the listing.', 'USER', 'TEXT', 1, NULL, NOW() - INTERVAL '1 hour 45 minutes'),
('cc0e8400-e29b-41d4-a716-446655440804', 'bb0e8400-e29b-41d4-a716-446655440701', 'Great question! The stay includes:\n• Traditional Bengali breakfast\n• Guided village walk\n• WiFi access\n• Parking\n• Amazing sunrise viewing spot\n\nWe also arrange tribal cultural programs on request! 🎭', 'PROVIDER', 'TEXT', NULL, 1, NOW() - INTERVAL '1 hour 40 minutes'),
('cc0e8400-e29b-41d4-a716-446655440805', 'bb0e8400-e29b-41d4-a716-446655440701', 'Excellent! How do I make the booking? And is there any advance payment required?', 'USER', 'TEXT', 1, NULL, NOW() - INTERVAL '1 hour 35 minutes'),
('cc0e8400-e29b-41d4-a716-446655440806', 'bb0e8400-e29b-41d4-a716-446655440701', 'You can book directly through the HopNGo app! We require 30% advance payment to confirm the booking. The remaining amount can be paid upon arrival. Would you like me to send you the booking link?', 'PROVIDER', 'TEXT', NULL, 1, NOW() - INTERVAL '5 minutes'),

-- Tea Garden Homestay Questions conversation
('cc0e8400-e29b-41d4-a716-446655440807', 'bb0e8400-e29b-41d4-a716-446655440702', 'Assalamu Alaikum! I''m planning a family trip to Srimangal. Can you accommodate 4 people including 2 children?', 'USER', 'TEXT', 2, NULL, NOW() - INTERVAL '1 day'),
('cc0e8400-e29b-41d4-a716-446655440808', 'bb0e8400-e29b-41d4-a716-446655440702', 'Wa alaikum assalam! Absolutely! Our homestay is perfect for families. We have rooms that can accommodate up to 6 people, and children love our tea garden! 🍃', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '23 hours'),
('cc0e8400-e29b-41d4-a716-446655440809', 'bb0e8400-e29b-41d4-a716-446655440702', 'That''s wonderful! What activities are suitable for children? My kids are 8 and 12 years old.', 'USER', 'TEXT', 2, NULL, NOW() - INTERVAL '22 hours'),
('cc0e8400-e29b-41d4-a716-446655440810', 'bb0e8400-e29b-41d4-a716-446655440702', 'Perfect ages for tea garden adventures! They can:\n• Learn tea plucking (very popular with kids!)\n• Bicycle rides through gardens\n• Visit our friendly farm animals\n• Nature photography walks\n• Traditional cooking lessons with my wife\n\nThey''ll have a blast! 📸🚲', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '21 hours'),
('cc0e8400-e29b-41d4-a716-446655440811', 'bb0e8400-e29b-41d4-a716-446655440702', 'This sounds amazing! What about food? Do you have options for picky eaters?', 'USER', 'TEXT', 2, NULL, NOW() - INTERVAL '20 hours'),
('cc0e8400-e29b-41d4-a716-446655440812', 'bb0e8400-e29b-41d4-a716-446655440702', 'Don''t worry! We''re experienced with families. We can prepare simple rice, dal, and chicken curry that kids usually love. Plus fresh fruits from our garden! We always ask about dietary preferences before your arrival. 🍛', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '30 minutes'),

-- Luxury Beach Resort conversation (resolved)
('cc0e8400-e29b-41d4-a716-446655440813', 'bb0e8400-e29b-41d4-a716-446655440703', 'I''m looking for a luxury beach resort for my anniversary. Your resort looks perfect! What special packages do you offer?', 'USER', 'TEXT', 3, NULL, NOW() - INTERVAL '3 days'),
('cc0e8400-e29b-41d4-a716-446655440814', 'bb0e8400-e29b-41d4-a716-446655440703', 'Congratulations on your anniversary! 🎉 We have a special "Romantic Getaway" package that includes:\n• Ocean view suite\n• Couples spa treatment\n• Private beach dinner\n• Champagne welcome\n• Late checkout\n\nWould you like more details?', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '3 days'),
('cc0e8400-e29b-41d4-a716-446655440815', 'bb0e8400-e29b-41d4-a716-446655440703', 'That sounds perfect! We booked it and had an amazing time. Thank you for making our anniversary so special! ⭐⭐⭐⭐⭐', 'USER', 'TEXT', 3, NULL, NOW() - INTERVAL '1 day'),
('cc0e8400-e29b-41d4-a716-446655440816', 'bb0e8400-e29b-41d4-a716-446655440703', 'We''re so happy you enjoyed your stay! Thank you for choosing us for your special celebration. We''d love to welcome you back anytime! 💕', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '1 day'),

-- Bandarban Trekking Tour conversation
('cc0e8400-e29b-41d4-a716-446655440817', 'bb0e8400-e29b-41d4-a716-446655440704', 'Hi! I''m interested in the 2-day Bandarban trekking tour. What''s the difficulty level? I''m a beginner.', 'USER', 'TEXT', 4, NULL, NOW() - INTERVAL '6 hours'),
('cc0e8400-e29b-41d4-a716-446655440818', 'bb0e8400-e29b-41d4-a716-446655440704', 'Hello! Great choice for your first trekking experience! Our Bandarban trek is moderate difficulty - perfect for beginners. We provide:\n• Experienced guide\n• All camping equipment\n• Safety briefing\n• Gradual trail with rest stops\n\nYou''ll love it! 🥾', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '5 hours 45 minutes'),
('cc0e8400-e29b-41d4-a716-446655440819', 'bb0e8400-e29b-41d4-a716-446655440704', 'What should I bring? And what about fitness requirements?', 'USER', 'TEXT', 4, NULL, NOW() - INTERVAL '5 hours 30 minutes'),
('cc0e8400-e29b-41d4-a716-446655440820', 'bb0e8400-e29b-41d4-a716-446655440704', 'Good questions! Bring:\n• Comfortable hiking shoes\n• Light backpack\n• Water bottle\n• Personal medications\n• Camera for amazing views!\n\nFitness: Basic fitness is enough. If you can walk for 2-3 hours with breaks, you''re good to go! We adjust pace for the group. 📷', 'PROVIDER', 'TEXT', NULL, 2, NOW() - INTERVAL '15 minutes'),

-- Sundarbans Wildlife Safari conversation
('cc0e8400-e29b-41d4-a716-446655440821', 'bb0e8400-e29b-41d4-a716-446655440705', 'I''m very excited about the Sundarbans safari! What are the chances of seeing Royal Bengal Tigers?', 'USER', 'TEXT', 1, NULL, NOW() - INTERVAL '4 hours'),
('cc0e8400-e29b-41d4-a716-446655440822', 'bb0e8400-e29b-41d4-a716-446655440705', 'Great question! While tiger sightings can''t be guaranteed (they''re wild animals!), we have about 60% success rate during our 3-day safaris. Even if we don''t see tigers, you''ll see:\n• Spotted deer\n• Wild boars\n• Crocodiles\n• 200+ bird species\n• Dolphins!\n\nIt''s always an amazing experience! 🐅', 'PROVIDER', 'TEXT', NULL, 3, NOW() - INTERVAL '3 hours 45 minutes'),
('cc0e8400-e29b-41d4-a716-446655440823', 'bb0e8400-e29b-41d4-a716-446655440705', 'That sounds incredible! What''s the accommodation like on the boat?', 'USER', 'TEXT', 1, NULL, NOW() - INTERVAL '3 hours 30 minutes'),
('cc0e8400-e29b-41d4-a716-446655440824', 'bb0e8400-e29b-41d4-a716-446655440705', 'Our safari boat is comfortable with:\n• AC cabins with attached bathrooms\n• Dining area with fresh meals\n• Upper deck for wildlife viewing\n• Safety equipment\n• Experienced crew\n\nYou''ll sleep to the sounds of the forest! 🛥️', 'PROVIDER', 'TEXT', NULL, 3, NOW() - INTERVAL '10 minutes'),

-- Cox's Bazar Travel Tips (resolved)
('cc0e8400-e29b-41d4-a716-446655440825', 'bb0e8400-e29b-41d4-a716-446655440706', 'First time visiting Cox''s Bazar. Any tips for the best experience?', 'USER', 'TEXT', 2, NULL, NOW() - INTERVAL '2 days'),
('cc0e8400-e29b-41d4-a716-446655440826', 'bb0e8400-e29b-41d4-a716-446655440706', 'Welcome to Cox''s Bazar! Here are my top tips:\n• Best time: Early morning & evening for cooler weather\n• Must-try: Fresh seafood at local restaurants\n• Don''t miss: Sunset at Laboni Beach\n• Bring: Sunscreen & hat\n• Visit: Himchari National Park nearby\n\nHave an amazing trip! 🌅', 'PROVIDER', 'TEXT', NULL, 3, NOW() - INTERVAL '2 days'),
('cc0e8400-e29b-41d4-a716-446655440827', 'bb0e8400-e29b-41d4-a716-446655440706', 'Thank you so much! Had a wonderful time following your suggestions. The sunset was breathtaking!', 'USER', 'TEXT', 2, NULL, NOW() - INTERVAL '1 day'),

-- Heritage Tour Customization conversation
('cc0e8400-e29b-41d4-a716-446655440828', 'bb0e8400-e29b-41d4-a716-446655440707', 'I''m interested in your heritage tour but would like to add some specific archaeological sites. Is customization possible?', 'USER', 'TEXT', 3, NULL, NOW() - INTERVAL '8 hours'),
('cc0e8400-e29b-41d4-a716-446655440829', 'bb0e8400-e29b-41d4-a716-446655440707', 'Absolutely! We love customizing tours for archaeology enthusiasts! Which specific sites are you interested in? We can modify the itinerary to include:\n• Mahasthangarh\n• Wari-Bateshwar\n• Mainamati\n• Paharpur (already included)\n\nWhat''s your preference? 🏛️', 'PROVIDER', 'TEXT', NULL, 4, NOW() - INTERVAL '7 hours 45 minutes'),
('cc0e8400-e29b-41d4-a716-446655440830', 'bb0e8400-e29b-41d4-a716-446655440707', 'Perfect! I''d love to include Mahasthangarh and Wari-Bateshwar. How would this affect the tour duration and cost?', 'USER', 'TEXT', 3, NULL, NOW() - INTERVAL '7 hours 30 minutes'),
('cc0e8400-e29b-41d4-a716-446655440831', 'bb0e8400-e29b-41d4-a716-446655440707', 'Excellent choices! Adding these sites would extend the tour to 9 days and increase cost by about 15%. You''ll get:\n• Expert archaeologist guide\n• Extended museum visits\n• Exclusive site access\n• Detailed historical briefings\n\nShall I prepare a detailed customized itinerary? 📜', 'PROVIDER', 'TEXT', NULL, 4, NOW() - INTERVAL '20 minutes'),

-- Emergency Weather Update (resolved)
('cc0e8400-e29b-41d4-a716-446655440832', 'bb0e8400-e29b-41d4-a716-446655440708', 'Hi! I''m currently in Sajek and heard there might be heavy rain tomorrow. Should I be concerned about road conditions?', 'USER', 'TEXT', 4, NULL, NOW() - INTERVAL '12 hours'),
('cc0e8400-e29b-41d4-a716-446655440833', 'bb0e8400-e29b-41d4-a716-446655440708', 'Thanks for reaching out! I''ve checked with local authorities. There is rain forecasted, but roads should remain passable. However, I recommend:\n• Start your return journey early morning\n• Keep emergency contacts handy\n• Stay updated with weather alerts\n\nI''ll monitor the situation and update you! 🌧️', 'PROVIDER', 'TEXT', NULL, 1, NOW() - INTERVAL '11 hours 30 minutes'),
('cc0e8400-e29b-41d4-a716-446655440834', 'bb0e8400-e29b-41d4-a716-446655440708', 'Thank you for the quick response and monitoring! Made it back safely. Great service! 👍', 'USER', 'TEXT', 4, NULL, NOW() - INTERVAL '8 hours');

-- Update user profiles to mark demo users
UPDATE users SET 
    is_demo = true,
    bio = CASE 
        WHEN id = 1 THEN 'Demo traveler exploring Bangladesh''s natural beauty. Love hills and beaches!'
        WHEN id = 2 THEN 'Family travel enthusiast. Always looking for kid-friendly destinations and authentic experiences.'
        WHEN id = 3 THEN 'Luxury travel blogger sharing premium experiences across Bangladesh.'
        WHEN id = 4 THEN 'Adventure seeker and trekking enthusiast. Always up for new challenges!'
    END,
    profile_image_url = CASE
        WHEN id = 1 THEN 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face'
        WHEN id = 2 THEN 'https://images.unsplash.com/photo-1494790108755-2616b612b786?w=150&h=150&fit=crop&crop=face'
        WHEN id = 3 THEN 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face'
        WHEN id = 4 THEN 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop&crop=face'
    END
WHERE id IN (1, 2, 3, 4);

-- Update provider profiles for demo
UPDATE providers SET 
    is_demo = true,
    description = CASE 
        WHEN id = 1 THEN 'Local hill station expert providing authentic experiences in Sajek Valley and surrounding areas. Family-run business with 15+ years of hospitality experience.'
        WHEN id = 2 THEN 'Luxury hospitality provider specializing in premium accommodations and curated experiences across Bangladesh. Award-winning service since 2010.'
        WHEN id = 3 THEN 'Wildlife and eco-tourism specialist offering sustainable travel experiences in Sundarbans and other natural reserves. Conservation-focused approach.'
        WHEN id = 4 THEN 'Adventure tourism expert providing trekking, cultural tours, and heritage experiences. Certified guides and safety-first approach.'
    END,
    profile_image_url = CASE
        WHEN id = 1 THEN 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150&h=150&fit=crop&crop=face'
        WHEN id = 2 THEN 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&h=150&fit=crop&crop=face'
        WHEN id = 3 THEN 'https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=150&h=150&fit=crop&crop=face'
        WHEN id = 4 THEN 'https://images.unsplash.com/photo-1507591064344-4c6ce005b128?w=150&h=150&fit=crop&crop=face'
    END
WHERE id IN (1, 2, 3, 4);
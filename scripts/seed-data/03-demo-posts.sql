-- Demo posts for iconic Bangladesh destinations
-- These posts showcase stunning locations with curated content for demo purposes

INSERT INTO posts (id, title, description, location, latitude, longitude, image_url, category, tags, is_featured, is_demo, view_count, like_count, author_id, created_at, updated_at) VALUES
-- Sajek Valley
('550e8400-e29b-41d4-a716-446655440101', 'Sajek Valley: The Queen of Hills', 'Experience the breathtaking beauty of Sajek Valley, known as the "Queen of Hills" in Bangladesh. This stunning destination offers panoramic views of rolling hills, cloud-kissed peaks, and vibrant tribal culture. Wake up to mesmerizing sunrises above the clouds and enjoy the serene atmosphere of this hill station paradise.', 'Sajek Valley, Rangamati', 23.3833, 92.2833, 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop', 'Nature', 'hills,sunrise,clouds,tribal culture,adventure', true, true, 1250, 89, 1, NOW(), NOW()),

-- Srimangal Tea Gardens
('550e8400-e29b-41d4-a716-446655440102', 'Srimangal: The Tea Capital of Bangladesh', 'Discover the lush green tea gardens of Srimangal, where endless rows of tea bushes create a mesmerizing carpet of emerald. Known as the tea capital of Bangladesh, this region offers peaceful walks through plantations, tea tasting experiences, and encounters with the famous seven-layer tea. The rolling hills and fresh mountain air make it a perfect retreat for nature lovers.', 'Srimangal, Sylhet', 24.3065, 91.7296, 'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?w=800&h=600&fit=crop', 'Nature', 'tea gardens,plantation,green hills,peaceful,authentic experience', true, true, 980, 67, 2, NOW(), NOW()),

-- Cox''s Bazar Beach
('550e8400-e29b-41d4-a716-446655440103', 'Cox''s Bazar: World''s Longest Natural Sea Beach', 'Experience the magic of Cox''s Bazar, home to the world''s longest unbroken sandy beach stretching over 120 kilometers. Watch spectacular sunsets paint the sky in brilliant colors, enjoy fresh seafood, and feel the gentle ocean breeze. This coastal paradise offers everything from beach activities to cultural experiences with local fishing communities.', 'Cox''s Bazar, Chittagong', 21.4272, 92.0058, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=600&fit=crop', 'Beach', 'longest beach,sunset,seafood,ocean,coastal paradise', true, true, 2150, 156, 3, NOW(), NOW()),

-- Bandarban Hills
('550e8400-e29b-41d4-a716-446655440104', 'Bandarban: The Switzerland of Bangladesh', 'Journey to Bandarban, often called the "Switzerland of Bangladesh," where majestic hills, crystal-clear streams, and indigenous cultures create an unforgettable experience. Trek through pristine forests, visit traditional tribal villages, and witness breathtaking views from the highest peaks in the country. This destination offers adventure, culture, and natural beauty in perfect harmony.', 'Bandarban, Chittagong Hill Tracts', 22.1953, 92.2183, 'https://images.unsplash.com/photo-1464822759844-d150baec93d5?w=800&h=600&fit=crop', 'Adventure', 'hills,trekking,tribal culture,forests,adventure,switzerland of bangladesh', true, true, 1680, 124, 4, NOW(), NOW()),

-- Sundarbans Mangrove Forest
('550e8400-e29b-41d4-a716-446655440105', 'Sundarbans: The Largest Mangrove Forest', 'Explore the mysterious Sundarbans, the world''s largest mangrove forest and home to the majestic Royal Bengal Tiger. Navigate through winding waterways, spot diverse wildlife, and experience the unique ecosystem where land meets sea. This UNESCO World Heritage site offers an unparalleled adventure into one of nature''s most remarkable creations.', 'Sundarbans, Khulna', 22.4978, 89.5403, 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&h=600&fit=crop', 'Wildlife', 'mangrove,tigers,wildlife,unesco,boat safari,nature conservation', true, true, 1420, 98, 1, NOW(), NOW()),

-- Paharpur Buddhist Monastery
('550e8400-e29b-41d4-a716-446655440106', 'Paharpur: Ancient Buddhist Heritage', 'Step back in time at Paharpur, home to the ruins of the ancient Somapura Mahavihara, once the largest Buddhist monastery south of the Himalayas. This UNESCO World Heritage site showcases remarkable architecture and offers insights into Bangladesh''s rich Buddhist heritage. The peaceful surroundings and historical significance make it a must-visit cultural destination.', 'Paharpur, Naogaon', 25.0317, 88.9764, 'https://images.unsplash.com/photo-1539650116574-75c0c6d73f6e?w=800&h=600&fit=crop', 'Heritage', 'buddhist monastery,unesco,ancient architecture,heritage,cultural site', false, true, 750, 45, 2, NOW(), NOW()),

-- Ratargul Swamp Forest
('550e8400-e29b-41d4-a716-446655440107', 'Ratargul: The Amazon of Bangladesh', 'Discover Ratargul Swamp Forest, known as the "Amazon of Bangladesh," where you can boat through submerged trees during monsoon season. This unique freshwater swamp forest offers a magical experience as you navigate between towering trees standing in crystal-clear water. The serene environment and unique ecosystem make it a photographer''s paradise.', 'Ratargul, Sylhet', 25.2144, 91.9231, 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&h=600&fit=crop', 'Nature', 'swamp forest,boat ride,amazon of bangladesh,unique ecosystem,photography', false, true, 890, 62, 3, NOW(), NOW()),

-- Kuakata Beach
('550e8400-e29b-41d4-a716-446655440108', 'Kuakata: Daughter of the Sea', 'Experience Kuakata, known as "Daughter of the Sea," where you can witness both sunrise and sunset from the same beach. This pristine coastal destination offers wide sandy beaches, traditional fishing villages, and the unique Rakhine culture. The panoramic sea views and cultural richness make it a hidden gem of Bangladesh.', 'Kuakata, Patuakhali', 21.8167, 90.1167, 'https://images.unsplash.com/photo-1439066615861-d1af74d74000?w=800&h=600&fit=crop', 'Beach', 'sunrise sunset beach,rakhine culture,fishing village,panoramic views,hidden gem', false, true, 1120, 78, 4, NOW(), NOW());

-- Insert post images for multiple photos per post
INSERT INTO post_images (post_id, image_url) VALUES
-- Sajek Valley additional images
('550e8400-e29b-41d4-a716-446655440101', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440101', 'https://images.unsplash.com/photo-1464822759844-d150baec93d5?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440101', 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&h=600&fit=crop'),

-- Srimangal additional images
('550e8400-e29b-41d4-a716-446655440102', 'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440102', 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440102', 'https://images.unsplash.com/photo-1597149960419-0d90ac2e3db4?w=800&h=600&fit=crop'),

-- Cox's Bazar additional images
('550e8400-e29b-41d4-a716-446655440103', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440103', 'https://images.unsplash.com/photo-1439066615861-d1af74d74000?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440103', 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&h=600&fit=crop'),

-- Bandarban additional images
('550e8400-e29b-41d4-a716-446655440104', 'https://images.unsplash.com/photo-1464822759844-d150baec93d5?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440104', 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&h=600&fit=crop'),
('550e8400-e29b-41d4-a716-446655440104', 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&h=600&fit=crop');
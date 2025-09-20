// Demo data for HopNGo platform
// This file contains seeded content for demo mode

export const DEMO_DESTINATIONS = [
  {
    id: 'demo-dest-001',
    name: 'Srimangal Tea Gardens',
    location: 'Sylhet Division, Bangladesh',
    description: 'Experience the lush green tea gardens and serene landscapes of Srimangal, known as the tea capital of Bangladesh.',
    image: '/images/demo/srimangal-tea-garden.jpg',
    rating: 4.8,
    reviewCount: 342,
    price: 2500,
    currency: 'BDT',
    category: 'Nature & Adventure',
    highlights: ['Tea Garden Tours', 'Lawachara National Park', 'Seven Color Tea', 'Tribal Culture'],
    coordinates: { lat: 24.3065, lng: 91.7296 },
    isPopular: true,
    isFeatured: true
  },
  {
    id: 'demo-dest-002',
    name: 'Cox\'s Bazar Beach',
    location: 'Chittagong Division, Bangladesh',
    description: 'The world\'s longest natural sea beach stretching 120 kilometers along the Bay of Bengal.',
    image: '/images/demo/coxs-bazar-beach.jpg',
    rating: 4.6,
    reviewCount: 1256,
    price: 3500,
    currency: 'BDT',
    category: 'Beach & Coastal',
    highlights: ['Longest Sea Beach', 'Sunset Views', 'Water Sports', 'Fresh Seafood'],
    coordinates: { lat: 21.4272, lng: 92.0058 },
    isPopular: true,
    isFeatured: true
  },
  {
    id: 'demo-dest-003',
    name: 'Sundarbans Mangrove Forest',
    location: 'Khulna Division, Bangladesh',
    description: 'UNESCO World Heritage site and home to the Royal Bengal Tiger in the world\'s largest mangrove forest.',
    image: '/images/demo/sundarbans-forest.jpg',
    rating: 4.9,
    reviewCount: 189,
    price: 4500,
    currency: 'BDT',
    category: 'Wildlife & Nature',
    highlights: ['Royal Bengal Tigers', 'Boat Safari', 'Bird Watching', 'Mangrove Ecosystem'],
    coordinates: { lat: 22.4953, lng: 89.5467 },
    isPopular: true,
    isFeatured: true
  },
  {
    id: 'demo-dest-004',
    name: 'Rangamati Hill District',
    location: 'Chittagong Hill Tracts, Bangladesh',
    description: 'Scenic hill station with Kaptai Lake, tribal culture, and breathtaking mountain views.',
    image: '/images/demo/rangamati-hills.jpg',
    rating: 4.7,
    reviewCount: 298,
    price: 3200,
    currency: 'BDT',
    category: 'Hills & Mountains',
    highlights: ['Kaptai Lake', 'Tribal Villages', 'Hanging Bridge', 'Buddhist Temples'],
    coordinates: { lat: 22.6533, lng: 92.1753 },
    isPopular: true,
    isFeatured: false
  }
];

export const DEMO_ACCOMMODATIONS = [
  {
    id: 'demo-acc-001',
    name: 'Tea Garden Resort Srimangal',
    location: 'Srimangal, Sylhet',
    description: 'Luxury eco-resort nestled in the heart of tea gardens with panoramic views.',
    image: '/images/demo/tea-garden-resort.jpg',
    rating: 4.8,
    reviewCount: 156,
    pricePerNight: 4500,
    currency: 'BDT',
    amenities: ['Free WiFi', 'Restaurant', 'Spa', 'Garden View', 'Tea Tours', 'Bicycle Rental'],
    roomTypes: [
      { type: 'Deluxe Garden View', price: 4500, capacity: 2 },
      { type: 'Premium Tea Suite', price: 6500, capacity: 4 },
      { type: 'Eco Cottage', price: 3500, capacity: 2 }
    ],
    coordinates: { lat: 24.3065, lng: 91.7296 },
    isVerified: true,
    hostId: 'demo-provider-001'
  },
  {
    id: 'demo-acc-002',
    name: 'Beachfront Villa Cox\'s Bazar',
    location: 'Cox\'s Bazar, Chittagong',
    description: 'Stunning beachfront villa with direct beach access and ocean views.',
    image: '/images/demo/beachfront-villa.jpg',
    rating: 4.6,
    reviewCount: 89,
    pricePerNight: 8500,
    currency: 'BDT',
    amenities: ['Beach Access', 'Ocean View', 'BBQ Area', 'Free Parking', 'Kitchen', 'Air Conditioning'],
    roomTypes: [
      { type: 'Ocean View Suite', price: 8500, capacity: 4 },
      { type: 'Beach Cottage', price: 6500, capacity: 2 }
    ],
    coordinates: { lat: 21.4272, lng: 92.0058 },
    isVerified: true,
    hostId: 'demo-provider-001'
  }
];

export const DEMO_ACTIVITIES = [
  {
    id: 'demo-act-001',
    name: 'Tea Garden Walking Tour',
    location: 'Srimangal',
    description: 'Guided walking tour through lush tea gardens with tea tasting session.',
    image: '/images/demo/tea-garden-tour.jpg',
    duration: '3 hours',
    price: 1200,
    currency: 'BDT',
    rating: 4.9,
    reviewCount: 234,
    category: 'Cultural Experience',
    includes: ['Professional Guide', 'Tea Tasting', 'Light Refreshments', 'Transportation'],
    maxParticipants: 12,
    difficulty: 'Easy'
  },
  {
    id: 'demo-act-002',
    name: 'Sundarbans Tiger Safari',
    location: 'Sundarbans',
    description: 'Full-day boat safari in search of the majestic Royal Bengal Tiger.',
    image: '/images/demo/tiger-safari.jpg',
    duration: '8 hours',
    price: 3500,
    currency: 'BDT',
    rating: 4.8,
    reviewCount: 167,
    category: 'Wildlife Safari',
    includes: ['Boat Safari', 'Expert Guide', 'Lunch', 'Binoculars', 'Life Jackets'],
    maxParticipants: 8,
    difficulty: 'Moderate'
  }
];

export const DEMO_MARKETPLACE_ITEMS = [
  {
    id: 'demo-gear-001',
    name: 'Professional Hiking Backpack',
    description: 'Durable 45L hiking backpack perfect for multi-day treks in Bangladesh hills.',
    image: '/images/demo/hiking-backpack.jpg',
    price: 3500,
    currency: 'BDT',
    rating: 4.7,
    reviewCount: 89,
    category: 'Backpacks & Bags',
    brand: 'TrekMaster',
    inStock: true,
    features: ['Waterproof', '45L Capacity', 'Ergonomic Design', 'Multiple Compartments'],
    sellerId: 'demo-provider-001'
  },
  {
    id: 'demo-gear-002',
    name: 'Portable Water Filter',
    description: 'Compact water filtration system for safe drinking water during travels.',
    image: '/images/demo/water-filter.jpg',
    price: 2200,
    currency: 'BDT',
    rating: 4.8,
    reviewCount: 156,
    category: 'Water & Hydration',
    brand: 'AquaPure',
    inStock: true,
    features: ['99.9% Filtration', 'Lightweight', 'Easy to Use', '1000L Capacity'],
    sellerId: 'demo-provider-001'
  }
];

export const DEMO_ITINERARIES = [
  {
    id: 'demo-itin-001',
    title: '3-Day Srimangal Tea Garden Experience',
    description: 'Immerse yourself in the tea culture of Bangladesh with garden tours, tastings, and nature walks.',
    duration: 3,
    totalCost: 12500,
    currency: 'BDT',
    destinations: ['Srimangal', 'Lawachara National Park'],
    activities: ['Tea Garden Tours', 'Nature Walks', 'Bird Watching', 'Cultural Experiences'],
    accommodation: 'Tea Garden Resort Srimangal',
    meals: 'All meals included',
    transportation: 'Private car with driver',
    difficulty: 'Easy',
    bestTime: 'October to March',
    groupSize: '2-8 people',
    highlights: [
      'Visit multiple tea gardens',
      'Learn tea processing techniques',
      'Explore Lawachara rainforest',
      'Meet local tribal communities',
      'Seven-color tea experience'
    ],
    dailyPlan: [
      {
        day: 1,
        title: 'Arrival & Tea Garden Introduction',
        activities: ['Check-in at resort', 'Welcome tea ceremony', 'Evening garden walk'],
        meals: ['Dinner']
      },
      {
        day: 2,
        title: 'Tea Gardens & Forest Exploration',
        activities: ['Morning tea garden tour', 'Tea processing workshop', 'Lawachara National Park visit'],
        meals: ['Breakfast', 'Lunch', 'Dinner']
      },
      {
        day: 3,
        title: 'Cultural Experience & Departure',
        activities: ['Tribal village visit', 'Seven-color tea tasting', 'Shopping for tea souvenirs'],
        meals: ['Breakfast', 'Lunch']
      }
    ]
  }
];

export const DEMO_REVIEWS = [
  {
    id: 'demo-review-001',
    userId: 'demo-traveler-001',
    userName: 'Alex Explorer',
    userAvatar: '/images/demo/traveler-avatar.jpg',
    itemId: 'demo-dest-001',
    itemType: 'destination',
    rating: 5,
    title: 'Absolutely magical experience!',
    content: 'The tea gardens of Srimangal exceeded all my expectations. The guided tour was informative, the scenery was breathtaking, and the seven-color tea was a unique experience. Highly recommended for nature lovers!',
    date: '2024-01-15',
    helpful: 23,
    verified: true,
    photos: ['/images/demo/review-photo-1.jpg', '/images/demo/review-photo-2.jpg']
  },
  {
    id: 'demo-review-002',
    userId: 'demo-traveler-002',
    userName: 'Sarah Adventure',
    userAvatar: '/images/demo/traveler-2-avatar.jpg',
    itemId: 'demo-acc-001',
    itemType: 'accommodation',
    rating: 5,
    title: 'Perfect stay in the tea gardens',
    content: 'The Tea Garden Resort provided an authentic experience with modern comforts. Waking up to the view of endless tea gardens was incredible. The staff was friendly and the food was delicious.',
    date: '2024-01-10',
    helpful: 18,
    verified: true,
    photos: ['/images/demo/review-photo-3.jpg']
  }
];

export const DEMO_MESSAGES = [
  {
    id: 'demo-msg-001',
    conversationId: 'demo-conv-001',
    senderId: 'demo-provider-001',
    senderName: 'Maya Host',
    senderAvatar: '/images/demo/provider-avatar.jpg',
    receiverId: 'demo-traveler-001',
    content: 'Welcome to Srimangal! I\'ve prepared a special welcome tea ceremony for your arrival. The weather is perfect for tea garden walks. Any dietary preferences I should know about?',
    timestamp: '2024-01-20T10:30:00Z',
    read: false,
    type: 'text'
  },
  {
    id: 'demo-msg-002',
    conversationId: 'demo-conv-001',
    senderId: 'demo-traveler-001',
    senderName: 'Alex Explorer',
    senderAvatar: '/images/demo/traveler-avatar.jpg',
    receiverId: 'demo-provider-001',
    content: 'Thank you so much! I\'m excited about the tea ceremony. I\'m vegetarian, so please keep that in mind for meals. Also, what\'s the best time for photography in the gardens?',
    timestamp: '2024-01-20T10:45:00Z',
    read: true,
    type: 'text'
  },
  {
    id: 'demo-msg-003',
    conversationId: 'demo-conv-001',
    senderId: 'demo-provider-001',
    senderName: 'Maya Host',
    senderAvatar: '/images/demo/provider-avatar.jpg',
    receiverId: 'demo-traveler-001',
    content: 'Perfect! We have excellent vegetarian options. For photography, early morning (6-8 AM) and late afternoon (4-6 PM) offer the best lighting. The mist in the morning creates magical shots! 📸',
    timestamp: '2024-01-20T11:00:00Z',
    read: false,
    type: 'text'
  }
];

export const DEMO_NOTIFICATIONS = [
  {
    id: 'demo-notif-001',
    type: 'booking_confirmed',
    title: 'Booking Confirmed',
    message: 'Your booking at Tea Garden Resort Srimangal has been confirmed for Jan 25-27, 2024.',
    timestamp: '2024-01-20T09:00:00Z',
    read: false,
    actionUrl: '/bookings/demo-booking-001',
    icon: 'check-circle'
  },
  {
    id: 'demo-notif-002',
    type: 'weather_alert',
    title: 'Weather Update',
    message: 'Perfect weather expected for your Srimangal trip! Sunny with temperatures around 22°C.',
    timestamp: '2024-01-20T08:30:00Z',
    read: false,
    actionUrl: '/weather/srimangal',
    icon: 'sun'
  },
  {
    id: 'demo-notif-003',
    type: 'message_received',
    title: 'New Message',
    message: 'Maya Host sent you a message about your upcoming stay.',
    timestamp: '2024-01-20T11:00:00Z',
    read: false,
    actionUrl: '/messages/demo-conv-001',
    icon: 'message-circle'
  }
];

export const DEMO_ANALYTICS = {
  provider: {
    totalBookings: 342,
    totalRevenue: 125000,
    averageRating: 4.8,
    responseTime: '< 1 hour',
    occupancyRate: 78,
    repeatCustomers: 23,
    monthlyGrowth: 12.5,
    topDestinations: ['Srimangal', 'Cox\'s Bazar', 'Rangamati'],
    recentBookings: [
      {
        id: 'demo-booking-001',
        guestName: 'Alex Explorer',
        checkIn: '2024-01-25',
        checkOut: '2024-01-27',
        amount: 9000,
        status: 'confirmed'
      },
      {
        id: 'demo-booking-002',
        guestName: 'Sarah Adventure',
        checkIn: '2024-01-28',
        checkOut: '2024-01-30',
        amount: 7500,
        status: 'pending'
      }
    ]
  },
  admin: {
    totalUsers: 15420,
    totalProviders: 1250,
    totalBookings: 8934,
    totalRevenue: 2450000,
    platformGrowth: 18.5,
    activeUsers: 3420,
    newSignups: 156,
    flaggedContent: 3,
    supportTickets: 12,
    systemHealth: 99.8
  }
};

// Helper functions for demo data
export const getDemoDataByType = (type: string, count?: number) => {
  const dataMap: Record<string, any[]> = {
    destinations: DEMO_DESTINATIONS,
    accommodations: DEMO_ACCOMMODATIONS,
    activities: DEMO_ACTIVITIES,
    marketplace: DEMO_MARKETPLACE_ITEMS,
    itineraries: DEMO_ITINERARIES,
    reviews: DEMO_REVIEWS,
    messages: DEMO_MESSAGES,
    notifications: DEMO_NOTIFICATIONS
  };
  
  const data = dataMap[type] || [];
  return count ? data.slice(0, count) : data;
};

export const getDemoItemById = (type: string, id: string) => {
  const data = getDemoDataByType(type);
  return data.find(item => item.id === id);
};

export const getRandomDemoItems = (type: string, count: number) => {
  const data = getDemoDataByType(type);
  const shuffled = [...data].sort(() => 0.5 - Math.random());
  return shuffled.slice(0, count);
};
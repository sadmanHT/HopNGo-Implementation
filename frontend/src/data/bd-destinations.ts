export interface BDDestination {
  slug: string;
  name_en: string;
  name_bn: string;
  district: string;
  region: string;
  best_season: string;
  highlights: string[];
  safety_notes: string;
  coordinates: {
    lat: number;
    lng: number;
  };
  imageUrls: string[];
  typical_budget_range: {
    min: number;
    max: number;
    currency: string;
  };
}

export const bdDestinations: BDDestination[] = [
  {
    slug: 'sajek-valley',
    name_en: 'Sajek Valley',
    name_bn: 'সাজেক ভ্যালি',
    district: 'Rangamati',
    region: 'Chittagong Hill Tracts',
    best_season: 'October to March',
    highlights: [
      'Cloud-kissed hilltops',
      'Indigenous culture',
      'Sunrise and sunset views',
      'Trekking trails',
      'Traditional cottages'
    ],
    safety_notes: 'Road conditions can be challenging during monsoon. Carry warm clothes for night stays. Respect local indigenous communities.',
    coordinates: {
      lat: 23.3833,
      lng: 92.2833
    },
    imageUrls: [
      '/images/destinations/sajek-valley-1.jpg',
      '/images/destinations/sajek-valley-2.jpg',
      '/images/destinations/sajek-valley-3.jpg'
    ],
    typical_budget_range: {
      min: 3000,
      max: 8000,
      currency: 'BDT'
    }
  },
  {
    slug: 'srimangal-lawachara',
    name_en: 'Srimangal & Lawachara',
    name_bn: 'শ্রীমঙ্গল ও লাউয়াছড়া',
    district: 'Moulvibazar',
    region: 'Sylhet',
    best_season: 'November to February',
    highlights: [
      'Tea gardens',
      'Lawachara National Park',
      'Seven-layer tea',
      'Hoolock gibbons',
      'Tribal villages'
    ],
    safety_notes: 'Monsoon season brings heavy rainfall and leeches in forest areas. Carry insect repellent and waterproof gear.',
    coordinates: {
      lat: 24.3065,
      lng: 91.7296
    },
    imageUrls: [
      '/images/destinations/srimangal-1.jpg',
      '/images/destinations/lawachara-1.jpg',
      '/images/destinations/tea-garden-1.jpg'
    ],
    typical_budget_range: {
      min: 2500,
      max: 6000,
      currency: 'BDT'
    }
  },
  {
    slug: 'coxs-bazar-himchori',
    name_en: "Cox's Bazar & Himchori",
    name_bn: 'কক্সবাজার ও হিমছড়ি',
    district: "Cox's Bazar",
    region: 'Chittagong',
    best_season: 'November to March',
    highlights: [
      "World's longest sea beach",
      'Himchori waterfalls',
      'Marine Drive',
      'Sunset at Laboni Beach',
      'Fresh seafood'
    ],
    safety_notes: 'Strong currents in sea - swim only in designated areas. Avoid monsoon season due to cyclone risks.',
    coordinates: {
      lat: 21.4272,
      lng: 92.0058
    },
    imageUrls: [
      '/images/destinations/coxs-bazar-1.jpg',
      '/images/destinations/himchori-1.jpg',
      '/images/destinations/marine-drive-1.jpg'
    ],
    typical_budget_range: {
      min: 4000,
      max: 12000,
      currency: 'BDT'
    }
  },
  {
    slug: 'bandarban-nilgiri-nafakhum',
    name_en: 'Bandarban (Nilgiri, Nafakhum)',
    name_bn: 'বান্দরবান (নীলগিরি, নাফাখুম)',
    district: 'Bandarban',
    region: 'Chittagong Hill Tracts',
    best_season: 'October to April',
    highlights: [
      'Nilgiri hilltop views',
      'Nafakhum waterfall',
      'Tribal culture',
      'Adventure trekking',
      'Cloud forests'
    ],
    safety_notes: 'Permits required for some areas. Hire local guides for trekking. Weather can change rapidly in hills.',
    coordinates: {
      lat: 22.1953,
      lng: 92.2183
    },
    imageUrls: [
      '/images/destinations/nilgiri-1.jpg',
      '/images/destinations/nafakhum-1.jpg',
      '/images/destinations/bandarban-1.jpg'
    ],
    typical_budget_range: {
      min: 5000,
      max: 15000,
      currency: 'BDT'
    }
  },
  {
    slug: 'sundarbans',
    name_en: 'Sundarbans',
    name_bn: 'সুন্দরবন',
    district: 'Khulna',
    region: 'Khulna',
    best_season: 'November to February',
    highlights: [
      'Royal Bengal tigers',
      'Mangrove forests',
      'Spotted deer',
      'Crocodiles',
      'Bird watching'
    ],
    safety_notes: 'Forest permits mandatory. Stay with authorized guides. Tiger encounters possible - follow safety protocols strictly.',
    coordinates: {
      lat: 22.4999,
      lng: 89.5403
    },
    imageUrls: [
      '/images/destinations/sundarbans-1.jpg',
      '/images/destinations/sundarbans-tiger-1.jpg',
      '/images/destinations/sundarbans-boat-1.jpg'
    ],
    typical_budget_range: {
      min: 8000,
      max: 20000,
      currency: 'BDT'
    }
  },
  {
    slug: 'ratargul-swamp-forest',
    name_en: 'Ratargul Swamp Forest',
    name_bn: 'রাতারগুল জলাবন',
    district: 'Sylhet',
    region: 'Sylhet',
    best_season: 'June to October (monsoon)',
    highlights: [
      'Freshwater swamp forest',
      'Boat rides through trees',
      'Unique ecosystem',
      'Bird watching',
      'Photography paradise'
    ],
    safety_notes: 'Best visited during monsoon when water levels are high. Wear life jackets during boat rides.',
    coordinates: {
      lat: 25.0048,
      lng: 91.8735
    },
    imageUrls: [
      '/images/destinations/ratargul-1.jpg',
      '/images/destinations/ratargul-boat-1.jpg',
      '/images/destinations/ratargul-trees-1.jpg'
    ],
    typical_budget_range: {
      min: 2000,
      max: 5000,
      currency: 'BDT'
    }
  },
  {
    slug: 'kuakata',
    name_en: 'Kuakata',
    name_bn: 'কুয়াকাটা',
    district: 'Patuakhali',
    region: 'Barisal',
    best_season: 'November to March',
    highlights: [
      'Sunrise and sunset from same beach',
      'Rakhine culture',
      'Buddhist temples',
      'Fatrar Char',
      'Mangrove forests'
    ],
    safety_notes: 'Check tide timings before visiting. Cyclone season (May-October) should be avoided.',
    coordinates: {
      lat: 21.8174,
      lng: 90.1198
    },
    imageUrls: [
      '/images/destinations/kuakata-sunrise-1.jpg',
      '/images/destinations/kuakata-sunset-1.jpg',
      '/images/destinations/kuakata-beach-1.jpg'
    ],
    typical_budget_range: {
      min: 3500,
      max: 8000,
      currency: 'BDT'
    }
  },
  {
    slug: 'panam-city',
    name_en: 'Panam City',
    name_bn: 'পানাম নগর',
    district: 'Narayanganj',
    region: 'Dhaka',
    best_season: 'October to March',
    highlights: [
      'Ancient city ruins',
      'Colonial architecture',
      'Historical significance',
      'Photography spots',
      'Archaeological site'
    ],
    safety_notes: 'Some structures are fragile - avoid climbing. Best visited during daylight hours.',
    coordinates: {
      lat: 23.6238,
      lng: 90.6636
    },
    imageUrls: [
      '/images/destinations/panam-city-1.jpg',
      '/images/destinations/panam-ruins-1.jpg',
      '/images/destinations/panam-architecture-1.jpg'
    ],
    typical_budget_range: {
      min: 1500,
      max: 3000,
      currency: 'BDT'
    }
  },
  {
    slug: 'paharpur',
    name_en: 'Paharpur',
    name_bn: 'পাহাড়পুর',
    district: 'Naogaon',
    region: 'Rajshahi',
    best_season: 'October to March',
    highlights: [
      'Somapura Mahavihara',
      'UNESCO World Heritage Site',
      'Buddhist monastery ruins',
      'Archaeological museum',
      'Ancient terracotta'
    ],
    safety_notes: 'Carry sun protection and water. Respect archaeological site rules and restrictions.',
    coordinates: {
      lat: 25.0317,
      lng: 88.9764
    },
    imageUrls: [
      '/images/destinations/paharpur-1.jpg',
      '/images/destinations/somapura-mahavihara-1.jpg',
      '/images/destinations/paharpur-museum-1.jpg'
    ],
    typical_budget_range: {
      min: 2000,
      max: 4000,
      currency: 'BDT'
    }
  }
];

// Helper functions
export const getDestinationBySlug = (slug: string): BDDestination | undefined => {
  return bdDestinations.find(dest => dest.slug === slug);
};

export const getDestinationsByRegion = (region: string): BDDestination[] => {
  return bdDestinations.filter(dest => dest.region === region);
};

export const getAllRegions = (): string[] => {
  return Array.from(new Set(bdDestinations.map(dest => dest.region)));
};

export const searchDestinations = (query: string): BDDestination[] => {
  const lowercaseQuery = query.toLowerCase();
  return bdDestinations.filter(dest => 
    dest.name_en.toLowerCase().includes(lowercaseQuery) ||
    dest.name_bn.includes(query) ||
    dest.district.toLowerCase().includes(lowercaseQuery) ||
    dest.region.toLowerCase().includes(lowercaseQuery) ||
    dest.highlights.some(highlight => highlight.toLowerCase().includes(lowercaseQuery))
  );
};
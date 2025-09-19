export interface BDFestival {
  slug: string;
  name_en: string;
  name_bn: string;
  description: string;
  dates: {
    type: 'fixed' | 'lunar' | 'seasonal';
    months: string[];
    specific_dates?: string[];
    lunar_calendar?: string;
  };
  regions: string[];
  travel_tips: string[];
  best_destinations: string[];
  cultural_significance: string;
  activities: string[];
  budget_impact: {
    accommodation: 'low' | 'medium' | 'high';
    transport: 'low' | 'medium' | 'high';
    food: 'low' | 'medium' | 'high';
  };
  safety_considerations: string[];
}

export const bdFestivals: BDFestival[] = [
  {
    slug: 'pohela-boishakh',
    name_en: 'Pohela Boishakh',
    name_bn: 'পহেলা বৈশাখ',
    description: 'Bengali New Year celebration marking the first day of the Bengali calendar',
    dates: {
      type: 'fixed',
      months: ['April'],
      specific_dates: ['April 14']
    },
    regions: ['Dhaka', 'Chittagong', 'Sylhet', 'Rajshahi', 'Khulna', 'Barisal', 'Rangpur', 'Mymensingh'],
    travel_tips: [
      'Book accommodations well in advance',
      'Expect heavy crowds in Dhaka, especially at Ramna Park',
      'Traditional dress (white and red) is encouraged',
      'Carry cash as many vendors prefer it',
      'Start early to avoid afternoon crowds'
    ],
    best_destinations: ['Dhaka (Ramna Park)', 'Chittagong', 'Sylhet', 'Rajshahi'],
    cultural_significance: 'Celebrates Bengali heritage, unity, and the arrival of spring with traditional foods, music, and cultural programs',
    activities: [
      'Mangal Shobhajatra procession',
      'Traditional Bengali breakfast',
      'Cultural performances',
      'Shopping at Boishakhi melas',
      'Visiting temples and cultural centers'
    ],
    budget_impact: {
      accommodation: 'high',
      transport: 'high',
      food: 'medium'
    },
    safety_considerations: [
      'Massive crowds - stay alert for pickpockets',
      'Traffic congestion throughout the day',
      'Stay hydrated in April heat',
      'Keep emergency contacts handy'
    ]
  },
  {
    slug: 'nabanna',
    name_en: 'Nabanna',
    name_bn: 'নবান্ন',
    description: 'Harvest festival celebrating the new rice crop, traditionally observed in rural Bangladesh',
    dates: {
      type: 'lunar',
      months: ['November', 'December'],
      lunar_calendar: 'Agrahayan month of Bengali calendar'
    },
    regions: ['Sylhet', 'Mymensingh', 'Rangpur', 'Rajshahi', 'Barisal'],
    travel_tips: [
      'Visit rural areas for authentic celebrations',
      'Try traditional rice-based dishes',
      'Participate in village festivities',
      'Bring warm clothes for evening celebrations',
      'Respect local customs and traditions'
    ],
    best_destinations: ['Srimangal', 'Mymensingh countryside', 'Rangpur villages', 'Sylhet rural areas'],
    cultural_significance: 'Celebrates agricultural abundance and gratitude to nature, strengthening community bonds in rural areas',
    activities: [
      'Traditional rice preparation',
      'Folk songs and dances',
      'Community feasts',
      'Village fairs',
      'Cultural competitions'
    ],
    budget_impact: {
      accommodation: 'low',
      transport: 'medium',
      food: 'low'
    },
    safety_considerations: [
      'Rural road conditions may be challenging',
      'Limited medical facilities in remote areas',
      'Inform locals about your visit plans',
      'Carry basic medications'
    ]
  },
  {
    slug: 'winter-picnics',
    name_en: 'Winter Picnics',
    name_bn: 'শীতকালীন পিকনিক',
    description: 'Traditional winter outings and picnics enjoyed by families and groups across Bangladesh',
    dates: {
      type: 'seasonal',
      months: ['December', 'January', 'February']
    },
    regions: ['Dhaka', 'Chittagong', 'Sylhet', 'Rajshahi', 'Khulna', 'Barisal', 'Rangpur', 'Mymensingh'],
    travel_tips: [
      'Book picnic spots early, especially weekends',
      'Carry warm clothes and blankets',
      'Prepare traditional winter foods',
      'Check weather forecasts for fog',
      'Plan group transportation'
    ],
    best_destinations: ['Savar', 'Gazipur', 'Manikganj', 'Tangail', 'Cumilla', 'Brahmanbaria'],
    cultural_significance: 'Brings families and communities together during the pleasant winter season, strengthening social bonds',
    activities: [
      'Outdoor cooking and barbecues',
      'Traditional games and sports',
      'Music and cultural programs',
      'Photography sessions',
      'Nature walks and exploration'
    ],
    budget_impact: {
      accommodation: 'low',
      transport: 'medium',
      food: 'medium'
    },
    safety_considerations: [
      'Dense fog can affect visibility',
      'Carry first aid kits for groups',
      'Be cautious with open fires',
      'Keep emergency contacts accessible'
    ]
  },
  {
    slug: 'durga-puja-routes',
    name_en: 'Durga Puja Routes',
    name_bn: 'দুর্গাপূজার রুট',
    description: 'Hindu festival celebrating Goddess Durga with elaborate pandals and cultural programs',
    dates: {
      type: 'lunar',
      months: ['September', 'October'],
      lunar_calendar: 'Ashwin month, typically 5-day celebration'
    },
    regions: ['Dhaka', 'Chittagong', 'Sylhet', 'Cumilla', 'Brahmanbaria'],
    travel_tips: [
      'Plan pandal-hopping routes in advance',
      'Use public transport to avoid parking issues',
      'Respect religious customs and dress codes',
      'Try traditional sweets and prasad',
      'Carry water and snacks for long walks'
    ],
    best_destinations: ['Dhakeshwari Temple (Dhaka)', 'Ramna Kali Mandir', 'Chittagong Hindu temples', 'Cumilla pandals'],
    cultural_significance: 'Celebrates the victory of good over evil, showcasing Hindu-Bengali culture and artistic traditions',
    activities: [
      'Pandal hopping and sightseeing',
      'Cultural performances and music',
      'Traditional food tasting',
      'Art and sculpture appreciation',
      'Photography of decorations'
    ],
    budget_impact: {
      accommodation: 'medium',
      transport: 'high',
      food: 'medium'
    },
    safety_considerations: [
      'Large crowds at popular pandals',
      'Traffic congestion in city areas',
      'Keep valuables secure',
      'Stay hydrated during long walks'
    ]
  },
  {
    slug: 'eid-celebrations',
    name_en: 'Eid Celebrations',
    name_bn: 'ঈদ উৎসব',
    description: 'Major Islamic festivals (Eid ul-Fitr and Eid ul-Adha) celebrated nationwide with family gatherings',
    dates: {
      type: 'lunar',
      months: ['Varies based on lunar calendar'],
      lunar_calendar: 'Shawwal (Eid ul-Fitr) and Dhul Hijjah (Eid ul-Adha)'
    },
    regions: ['Dhaka', 'Chittagong', 'Sylhet', 'Rajshahi', 'Khulna', 'Barisal', 'Rangpur', 'Mymensingh'],
    travel_tips: [
      'Book transport tickets well in advance',
      'Expect massive crowds at transport hubs',
      'Many businesses close for 2-3 days',
      'Carry sufficient cash',
      'Plan for extended travel times'
    ],
    best_destinations: ['Home districts', 'Dhaka (National Mosque)', 'Chittagong', 'Sylhet'],
    cultural_significance: 'Most important religious festivals bringing families together and emphasizing charity and community',
    activities: [
      'Eid prayers at mosques',
      'Family reunions and feasts',
      'Gift exchanges',
      'Visiting relatives and friends',
      'Charity and community service'
    ],
    budget_impact: {
      accommodation: 'high',
      transport: 'high',
      food: 'high'
    },
    safety_considerations: [
      'Extreme overcrowding at transport terminals',
      'Road accidents increase during Eid rush',
      'Keep important documents safe',
      'Plan alternative routes if possible'
    ]
  },
  {
    slug: 'kali-puja',
    name_en: 'Kali Puja',
    name_bn: 'কালীপূজা',
    description: 'Hindu festival dedicated to Goddess Kali, celebrated with night-long festivities',
    dates: {
      type: 'lunar',
      months: ['October', 'November'],
      lunar_calendar: 'New moon night of Kartik month'
    },
    regions: ['Dhaka', 'Chittagong', 'Sylhet', 'Cumilla'],
    travel_tips: [
      'Celebrations continue through the night',
      'Dress modestly when visiting temples',
      'Try traditional sweets and offerings',
      'Respect photography restrictions',
      'Plan for late-night transportation'
    ],
    best_destinations: ['Ramna Kali Mandir (Dhaka)', 'Chittagong Kali temples', 'Local community pandals'],
    cultural_significance: 'Honors the fierce aspect of the Divine Mother, emphasizing protection and strength',
    activities: [
      'Temple visits and prayers',
      'Cultural performances',
      'Traditional music and dance',
      'Community feasts',
      'Fireworks and celebrations'
    ],
    budget_impact: {
      accommodation: 'medium',
      transport: 'medium',
      food: 'medium'
    },
    safety_considerations: [
      'Night-time celebrations require extra caution',
      'Crowded temple areas',
      'Fireworks safety',
      'Keep emergency contacts ready'
    ]
  }
];

// Helper functions
export const getFestivalBySlug = (slug: string): BDFestival | undefined => {
  return bdFestivals.find(festival => festival.slug === slug);
};

export const getFestivalsByMonth = (month: string): BDFestival[] => {
  return bdFestivals.filter(festival => 
    festival.dates.months.includes(month) ||
    festival.dates.months.includes('Varies based on lunar calendar')
  );
};

export const getFestivalsByRegion = (region: string): BDFestival[] => {
  return bdFestivals.filter(festival => festival.regions.includes(region));
};

export const getCurrentSeasonFestivals = (): BDFestival[] => {
  const currentMonth = new Date().toLocaleString('default', { month: 'long' });
  return getFestivalsByMonth(currentMonth);
};

export const searchFestivals = (query: string): BDFestival[] => {
  const lowercaseQuery = query.toLowerCase();
  return bdFestivals.filter(festival => 
    festival.name_en.toLowerCase().includes(lowercaseQuery) ||
    festival.name_bn.includes(query) ||
    festival.description.toLowerCase().includes(lowercaseQuery) ||
    festival.cultural_significance.toLowerCase().includes(lowercaseQuery)
  );
};
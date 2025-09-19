import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Cloudinary configuration
const CLOUDINARY_CLOUD_NAME = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME || 'hopngo';
const CLOUDINARY_BASE_URL = `https://res.cloudinary.com/${CLOUDINARY_CLOUD_NAME}/image/upload`;

// Bangladesh-inspired color palette for OG images
const BD_COLORS = {
  green: '136D38',
  teal: '1DA5A3',
  sand: 'F5E9DA',
  sunrise: 'F6AA1C',
  coral: 'F06449',
  slate: '1F2937',
  white: 'FFFFFF',
} as const;

// Font configurations for Bengali and English text
const FONTS = {
  bengali: 'NotoSansBengali-Regular.ttf',
  english: 'Inter-Bold.ttf',
  englishRegular: 'Inter-Regular.ttf',
} as const;

interface BaseOGImageOptions {
  title: string;
  titleBn?: string;
  subtitle?: string;
  subtitleBn?: string;
  backgroundImage?: string;
  logo?: boolean;
  watermark?: boolean;
  width?: number;
  height?: number;
}

interface DestinationOGOptions extends BaseOGImageOptions {
  type: 'destination';
  location: string;
  locationBn?: string;
  category: string;
  categoryBn?: string;
  rating?: number;
  reviewCount?: number;
  price?: string;
  priceBn?: string;
}

interface ListingOGOptions extends BaseOGImageOptions {
  type: 'listing';
  listingType: 'hotel' | 'tour' | 'experience';
  location: string;
  locationBn?: string;
  rating?: number;
  reviewCount?: number;
  price?: string;
  priceBn?: string;
  amenities?: string[];
  amenitiesBn?: string[];
}

interface ItineraryOGOptions extends BaseOGImageOptions {
  type: 'itinerary';
  duration: string;
  durationBn?: string;
  destinations: string[];
  destinationsBn?: string[];
  difficulty?: 'easy' | 'moderate' | 'challenging';
  difficultyBn?: string;
  totalCost?: string;
  totalCostBn?: string;
}

interface StoryOGOptions extends BaseOGImageOptions {
  type: 'story';
  author: string;
  authorBn?: string;
  publishedAt: string;
  readTime: string;
  readTimeBn?: string;
  category: string;
  categoryBn?: string;
}

interface ProfileOGOptions extends BaseOGImageOptions {
  type: 'profile';
  username: string;
  usernameBn?: string;
  bio?: string;
  bioBn?: string;
  stats?: {
    trips: number;
    reviews: number;
    photos: number;
  };
  statsBn?: {
    trips: string;
    reviews: string;
    photos: string;
  };
}

type OGImageOptions = 
  | DestinationOGOptions 
  | ListingOGOptions 
  | ItineraryOGOptions 
  | StoryOGOptions 
  | ProfileOGOptions;

/**
 * Generates a dynamic Open Graph image URL using Cloudinary transformations
 */
export function generateOGImage(options: OGImageOptions): string {
  const {
    title,
    titleBn,
    subtitle,
    subtitleBn,
    backgroundImage,
    logo = true,
    watermark = true,
    width = 1200,
    height = 630,
  } = options;

  // Base transformations
  const transformations: string[] = [
    `w_${width},h_${height}`,
    'c_fill',
    'f_auto',
    'q_auto:good',
  ];

  // Background setup
  if (backgroundImage) {
    // Use provided background image with overlay
    transformations.push(
      `l_${backgroundImage.replace(/\//g, ':')},w_${width},h_${height},c_fill,o_60`
    );
  } else {
    // Create gradient background based on type
    const gradientColors = getGradientForType(options.type);
    transformations.push(
      `co_rgb:${gradientColors.start},l_text:Arial_200_bold:■,w_${width},h_${height},c_fit,o_100`,
      `co_rgb:${gradientColors.end},l_text:Arial_200_bold:■,w_${width},h_${height},c_fit,o_60,g_south_east`
    );
  }

  // Add dark overlay for better text readability
  transformations.push(
    `co_rgb:000000,l_text:Arial_200_bold:■,w_${width},h_${height},c_fit,o_40`
  );

  // Add logo if requested
  if (logo) {
    transformations.push(
      'l_hopngo-logo-white,w_120,g_north_west,x_60,y_60'
    );
  }

  // Add type-specific content
  switch (options.type) {
    case 'destination':
      addDestinationContent(transformations, options);
      break;
    case 'listing':
      addListingContent(transformations, options);
      break;
    case 'itinerary':
      addItineraryContent(transformations, options);
      break;
    case 'story':
      addStoryContent(transformations, options);
      break;
    case 'profile':
      addProfileContent(transformations, options);
      break;
  }

  // Add main title
  if (title) {
    const titleSize = title.length > 40 ? 48 : title.length > 25 ? 56 : 64;
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_${titleSize}_bold:${encodeURIComponent(title)},w_${width - 120},c_fit,g_center,y_-60`
    );
  }

  // Add Bengali title if provided
  if (titleBn) {
    const titleBnSize = titleBn.length > 40 ? 36 : titleBn.length > 25 ? 42 : 48;
    transformations.push(
      `co_rgb:${BD_COLORS.sand},l_text:${FONTS.bengali}_${titleBnSize}:${encodeURIComponent(titleBn)},w_${width - 120},c_fit,g_center,y_-10`
    );
  }

  // Add subtitle if provided
  if (subtitle) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_32:${encodeURIComponent(subtitle)},w_${width - 120},c_fit,g_center,y_40,o_90`
    );
  }

  // Add Bengali subtitle if provided
  if (subtitleBn) {
    transformations.push(
      `co_rgb:${BD_COLORS.sand},l_text:${FONTS.bengali}_28:${encodeURIComponent(subtitleBn)},w_${width - 120},c_fit,g_center,y_80,o_80`
    );
  }

  // Add watermark if requested
  if (watermark) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_16:HopNGo.com,g_south_east,x_60,y_40,o_60`
    );
  }

  // Create the final URL
  const baseImage = 'og-template-base.jpg'; // Base template image
  return `${CLOUDINARY_BASE_URL}/${transformations.join('/')}/${baseImage}`;
}

/**
 * Get gradient colors based on content type
 */
function getGradientForType(type: OGImageOptions['type']): { start: string; end: string } {
  switch (type) {
    case 'destination':
      return { start: BD_COLORS.green, end: BD_COLORS.teal };
    case 'listing':
      return { start: BD_COLORS.teal, end: BD_COLORS.sunrise };
    case 'itinerary':
      return { start: BD_COLORS.sunrise, end: BD_COLORS.coral };
    case 'story':
      return { start: BD_COLORS.coral, end: BD_COLORS.green };
    case 'profile':
      return { start: BD_COLORS.slate, end: BD_COLORS.teal };
    default:
      return { start: BD_COLORS.green, end: BD_COLORS.teal };
  }
}

/**
 * Add destination-specific content to transformations
 */
function addDestinationContent(transformations: string[], options: DestinationOGOptions) {
  const { location, locationBn, category, categoryBn, rating, reviewCount, price, priceBn } = options;

  // Add category badge
  if (category) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24_bold:${encodeURIComponent(category.toUpperCase())},g_north_west,x_60,y_140,b_rgb:${BD_COLORS.green},bo_8px_solid_rgb:${BD_COLORS.green}`
    );
  }

  // Add location
  if (location) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_28:📍 ${encodeURIComponent(location)},g_south_west,x_60,y_180,o_90`
    );
  }

  if (locationBn) {
    transformations.push(
      `co_rgb:${BD_COLORS.sand},l_text:${FONTS.bengali}_24:${encodeURIComponent(locationBn)},g_south_west,x_60,y_140,o_80`
    );
  }

  // Add rating and reviews
  if (rating && reviewCount) {
    const stars = '⭐'.repeat(Math.floor(rating));
    transformations.push(
      `co_rgb:${BD_COLORS.sunrise},l_text:${FONTS.english}_24:${stars} ${rating} (${reviewCount} reviews),g_south_east,x_60,y_180`
    );
  }

  // Add price
  if (price) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_32_bold:${encodeURIComponent(price)},g_south_east,x_60,y_140,b_rgb:${BD_COLORS.coral},bo_6px_solid_rgb:${BD_COLORS.coral}`
    );
  }
}

/**
 * Add listing-specific content to transformations
 */
function addListingContent(transformations: string[], options: ListingOGOptions) {
  const { listingType, location, locationBn, rating, reviewCount, price, priceBn } = options;

  // Add listing type badge
  const typeEmoji = {
    hotel: '🏨',
    tour: '🗺️',
    experience: '🎯'
  }[listingType];

  transformations.push(
    `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24_bold:${typeEmoji} ${encodeURIComponent(listingType.toUpperCase())},g_north_west,x_60,y_140,b_rgb:${BD_COLORS.teal},bo_8px_solid_rgb:${BD_COLORS.teal}`
  );

  // Add location
  if (location) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_28:📍 ${encodeURIComponent(location)},g_south_west,x_60,y_180,o_90`
    );
  }

  // Add rating and price similar to destination
  if (rating && reviewCount) {
    const stars = '⭐'.repeat(Math.floor(rating));
    transformations.push(
      `co_rgb:${BD_COLORS.sunrise},l_text:${FONTS.english}_24:${stars} ${rating} (${reviewCount}),g_south_east,x_60,y_180`
    );
  }

  if (price) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_32_bold:${encodeURIComponent(price)},g_south_east,x_60,y_140,b_rgb:${BD_COLORS.coral},bo_6px_solid_rgb:${BD_COLORS.coral}`
    );
  }
}

/**
 * Add itinerary-specific content to transformations
 */
function addItineraryContent(transformations: string[], options: ItineraryOGOptions) {
  const { duration, durationBn, destinations, destinationsBn, difficulty, totalCost } = options;

  // Add duration badge
  if (duration) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24_bold:⏱️ ${encodeURIComponent(duration)},g_north_west,x_60,y_140,b_rgb:${BD_COLORS.sunrise},bo_8px_solid_rgb:${BD_COLORS.sunrise}`
    );
  }

  // Add destinations count
  if (destinations && destinations.length > 0) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_28:📍 ${destinations.length} destinations,g_south_west,x_60,y_180,o_90`
    );
  }

  // Add difficulty
  if (difficulty) {
    const difficultyEmoji = {
      easy: '🟢',
      moderate: '🟡',
      challenging: '🔴'
    }[difficulty];
    
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24:${difficultyEmoji} ${encodeURIComponent(difficulty)},g_south_west,x_60,y_140`
    );
  }

  // Add total cost
  if (totalCost) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_32_bold:${encodeURIComponent(totalCost)},g_south_east,x_60,y_140,b_rgb:${BD_COLORS.coral},bo_6px_solid_rgb:${BD_COLORS.coral}`
    );
  }
}

/**
 * Add story-specific content to transformations
 */
function addStoryContent(transformations: string[], options: StoryOGOptions) {
  const { author, authorBn, publishedAt, readTime, readTimeBn, category, categoryBn } = options;

  // Add category badge
  if (category) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24_bold:📖 ${encodeURIComponent(category.toUpperCase())},g_north_west,x_60,y_140,b_rgb:${BD_COLORS.coral},bo_8px_solid_rgb:${BD_COLORS.coral}`
    );
  }

  // Add author
  if (author) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_28:✍️ ${encodeURIComponent(author)},g_south_west,x_60,y_180,o_90`
    );
  }

  // Add read time
  if (readTime) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24:⏱️ ${encodeURIComponent(readTime)},g_south_east,x_60,y_180`
    );
  }

  // Add published date
  if (publishedAt) {
    const date = new Date(publishedAt).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
    transformations.push(
      `co_rgb:${BD_COLORS.sand},l_text:${FONTS.englishRegular}_20:📅 ${date},g_south_east,x_60,y_140,o_80`
    );
  }
}

/**
 * Add profile-specific content to transformations
 */
function addProfileContent(transformations: string[], options: ProfileOGOptions) {
  const { username, usernameBn, bio, bioBn, stats, statsBn } = options;

  // Add profile badge
  transformations.push(
    `co_rgb:${BD_COLORS.white},l_text:${FONTS.english}_24_bold:👤 TRAVELER,g_north_west,x_60,y_140,b_rgb:${BD_COLORS.slate},bo_8px_solid_rgb:${BD_COLORS.slate}`
  );

  // Add username
  if (username) {
    transformations.push(
      `co_rgb:${BD_COLORS.white},l_text:${FONTS.englishRegular}_28:@${encodeURIComponent(username)},g_south_west,x_60,y_180,o_90`
    );
  }

  // Add stats
  if (stats) {
    const statsText = `🗺️ ${stats.trips} trips • ⭐ ${stats.reviews} reviews • 📸 ${stats.photos} photos`;
    transformations.push(
      `co_rgb:${BD_COLORS.sand},l_text:${FONTS.englishRegular}_20:${encodeURIComponent(statsText)},g_south_east,x_60,y_180,o_80`
    );
  }
}

/**
 * Generate OG image for destinations
 */
export function generateDestinationOGImage(options: Omit<DestinationOGOptions, 'type'>): string {
  return generateOGImage({ ...options, type: 'destination' });
}

/**
 * Generate OG image for listings (hotels, tours, experiences)
 */
export function generateListingOGImage(options: Omit<ListingOGOptions, 'type'>): string {
  return generateOGImage({ ...options, type: 'listing' });
}

/**
 * Generate OG image for itineraries
 */
export function generateItineraryOGImage(options: Omit<ItineraryOGOptions, 'type'>): string {
  return generateOGImage({ ...options, type: 'itinerary' });
}

/**
 * Generate OG image for stories/blog posts
 */
export function generateStoryOGImage(options: Omit<StoryOGOptions, 'type'>): string {
  return generateOGImage({ ...options, type: 'story' });
}

/**
 * Generate OG image for user profiles
 */
export function generateProfileOGImage(options: Omit<ProfileOGOptions, 'type'>): string {
  return generateOGImage({ ...options, type: 'profile' });
}

/**
 * Generate default HopNGo OG image
 */
export function generateDefaultOGImage(title?: string, titleBn?: string): string {
  return generateOGImage({
    type: 'destination',
    title: title || 'Discover Bangladesh with HopNGo',
    titleBn: titleBn || 'হপএনগো দিয়ে বাংলাদেশ আবিষ্কার করুন',
    subtitle: 'Your AI-powered travel companion',
    subtitleBn: 'আপনার এআই-চালিত ভ্রমণ সঙ্গী',
    location: 'Bangladesh',
    locationBn: 'বাংলাদেশ',
    category: 'Travel',
    categoryBn: 'ভ্রমণ',
  });
}

/**
 * Utility to get meta tags for OG images
 */
export function getOGImageMetaTags(ogImageUrl: string, options: {
  title: string;
  description: string;
  url?: string;
  siteName?: string;
}) {
  const { title, description, url, siteName = 'HopNGo' } = options;
  
  return {
    'og:title': title,
    'og:description': description,
    'og:image': ogImageUrl,
    'og:image:width': '1200',
    'og:image:height': '630',
    'og:image:type': 'image/jpeg',
    'og:type': 'website',
    'og:site_name': siteName,
    ...(url && { 'og:url': url }),
    'twitter:card': 'summary_large_image',
    'twitter:title': title,
    'twitter:description': description,
    'twitter:image': ogImageUrl,
  };
}

/**
 * Example usage and presets
 */
export const OG_PRESETS = {
  // Popular destinations
  sajekValley: () => generateDestinationOGImage({
    title: 'Sajek Valley',
    titleBn: 'সাজেক ভ্যালি',
    subtitle: 'Queen of Hills',
    subtitleBn: 'পাহাড়ের রানী',
    location: 'Rangamati, Chittagong Hill Tracts',
    locationBn: 'রাঙামাটি, পার্বত্য চট্টগ্রাম',
    category: 'Mountain',
    categoryBn: 'পাহাড়',
    rating: 4.8,
    reviewCount: 1247,
    price: '৳3,500',
    backgroundImage: 'destinations/sajek-valley-hero',
  }),
  
  coxsBazar: () => generateDestinationOGImage({
    title: "Cox's Bazar Beach",
    titleBn: 'কক্সবাজার সমুদ্র সৈকত',
    subtitle: "World's Longest Natural Sea Beach",
    subtitleBn: 'বিশ্বের দীর্ঘতম প্রাকৃতিক সমুদ্র সৈকত',
    location: "Cox's Bazar, Chittagong",
    locationBn: 'কক্সবাজার, চট্টগ্রাম',
    category: 'Beach',
    categoryBn: 'সৈকত',
    rating: 4.6,
    reviewCount: 2156,
    price: '৳2,500',
    backgroundImage: 'destinations/coxs-bazar-hero',
  }),
  
  sundarbans: () => generateDestinationOGImage({
    title: 'Sundarbans Mangrove Forest',
    titleBn: 'সুন্দরবন ম্যানগ্রোভ বন',
    subtitle: 'Home of Royal Bengal Tigers',
    subtitleBn: 'রয়েল বেঙ্গল টাইগারের আবাসস্থল',
    location: 'Khulna & Barisal',
    locationBn: 'খুলনা ও বরিশাল',
    category: 'Wildlife',
    categoryBn: 'বন্যপ্রাণী',
    rating: 4.9,
    reviewCount: 892,
    price: '৳4,200',
    backgroundImage: 'destinations/sundarbans-hero',
  }),
};
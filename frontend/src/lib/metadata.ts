import { Metadata } from 'next';
import {
  generateDestinationOGImage,
  generateListingOGImage,
  generateItineraryOGImage,
  generateStoryOGImage,
  generateProfileOGImage,
  generateDefaultOGImage,
  getOGImageMetaTags,
} from './og-images';

// Base metadata configuration
const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://hopngo.com';
const SITE_NAME = 'HopNGo';
const SITE_DESCRIPTION = 'Discover Bangladesh with AI-powered travel planning, local insights, and authentic experiences. Your ultimate guide to exploring the beauty of Bangladesh.';
const SITE_DESCRIPTION_BN = 'এআই-চালিত ভ্রমণ পরিকল্পনা, স্থানীয় অন্তর্দৃষ্টি এবং প্রামাণিক অভিজ্ঞতার সাথে বাংলাদেশ আবিষ্কার করুন। বাংলাদেশের সৌন্দর্য অন্বেষণের জন্য আপনার চূড়ান্ত গাইড।';

// Default metadata
export const defaultMetadata: Metadata = {
  title: {
    default: 'HopNGo - Discover Bangladesh',
    template: '%s | HopNGo',
  },
  description: SITE_DESCRIPTION,
  keywords: [
    'Bangladesh travel',
    'বাংলাদেশ ভ্রমণ',
    'travel guide',
    'ভ্রমণ গাইড',
    'tourism',
    'পর্যটন',
    'destinations',
    'গন্তব্য',
    'AI travel planning',
    'এআই ভ্রমণ পরিকল্পনা',
    'Cox\'s Bazar',
    'কক্সবাজার',
    'Sundarbans',
    'সুন্দরবন',
    'Sajek Valley',
    'সাজেক ভ্যালি',
    'Chittagong Hill Tracts',
    'পার্বত্য চট্টগ্রাম',
    'Sylhet',
    'সিলেট',
    'Dhaka',
    'ঢাকা',
  ],
  authors: [{ name: 'HopNGo Team' }],
  creator: 'HopNGo',
  publisher: 'HopNGo',
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  metadataBase: new URL(BASE_URL),
  alternates: {
    canonical: '/',
    languages: {
      'en-US': '/en',
      'bn-BD': '/bn',
    },
  },
  openGraph: {
    type: 'website',
    locale: 'en_US',
    alternateLocale: ['bn_BD'],
    url: BASE_URL,
    siteName: SITE_NAME,
    title: 'HopNGo - Discover Bangladesh',
    description: SITE_DESCRIPTION,
    images: [
      {
        url: generateDefaultOGImage(),
        width: 1200,
        height: 630,
        alt: 'HopNGo - Discover Bangladesh',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'HopNGo - Discover Bangladesh',
    description: SITE_DESCRIPTION,
    images: [generateDefaultOGImage()],
    creator: '@hopngo_bd',
    site: '@hopngo_bd',
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
  verification: {
    google: process.env.GOOGLE_SITE_VERIFICATION,
    yandex: process.env.YANDEX_VERIFICATION,
    yahoo: process.env.YAHOO_SITE_VERIFICATION,
  },
};

// Destination page metadata
export interface DestinationMetadataOptions {
  id: string;
  name: string;
  nameBn?: string;
  description: string;
  descriptionBn?: string;
  location: string;
  locationBn?: string;
  category: string;
  categoryBn?: string;
  rating?: number;
  reviewCount?: number;
  price?: string;
  priceBn?: string;
  images?: string[];
  backgroundImage?: string;
  keywords?: string[];
}

export function generateDestinationMetadata(options: DestinationMetadataOptions): Metadata {
  const {
    id,
    name,
    nameBn,
    description,
    descriptionBn,
    location,
    locationBn,
    category,
    categoryBn,
    rating,
    reviewCount,
    price,
    priceBn,
    images = [],
    backgroundImage,
    keywords = [],
  } = options;

  const title = `${name} - ${location} | ${SITE_NAME}`;
  const titleBn = nameBn && locationBn ? `${nameBn} - ${locationBn} | ${SITE_NAME}` : undefined;
  const url = `${BASE_URL}/destinations/${id}`;
  
  const ogImageUrl = generateDestinationOGImage({
    title: name,
    titleBn: nameBn,
    subtitle: `${category} in ${location}`,
    subtitleBn: categoryBn && locationBn ? `${locationBn}-এ ${categoryBn}` : undefined,
    location,
    locationBn,
    category,
    categoryBn,
    rating,
    reviewCount,
    price,
    priceBn,
    backgroundImage,
  });

  const fullDescription = rating && reviewCount 
    ? `${description} Rated ${rating}/5 by ${reviewCount} travelers. ${price ? `Starting from ${price}.` : ''}`
    : description;

  return {
    title,
    description: fullDescription,
    keywords: [
      name,
      ...(nameBn ? [nameBn] : []),
      location,
      ...(locationBn ? [locationBn] : []),
      category,
      ...(categoryBn ? [categoryBn] : []),
      'Bangladesh travel',
      'বাংলাদেশ ভ্রমণ',
      ...keywords,
    ],
    alternates: {
      canonical: url,
      languages: {
        'en-US': `${url}?lang=en`,
        'bn-BD': `${url}?lang=bn`,
      },
    },
    openGraph: {
      type: 'article',
      url,
      title,
      description: fullDescription,
      siteName: SITE_NAME,
      images: [
        {
          url: ogImageUrl,
          width: 1200,
          height: 630,
          alt: `${name} - ${location}`,
        },
        ...images.map((img, index) => ({
          url: img,
          alt: `${name} - Image ${index + 1}`,
        })),
      ],
      locale: 'en_US',
      alternateLocale: ['bn_BD'],
    },
    twitter: {
      card: 'summary_large_image',
      title,
      description: fullDescription,
      images: [ogImageUrl],
    },
  };
}

// Listing page metadata (hotels, tours, experiences)
export interface ListingMetadataOptions {
  id: string;
  name: string;
  nameBn?: string;
  description: string;
  descriptionBn?: string;
  listingType: 'hotel' | 'tour' | 'experience';
  location: string;
  locationBn?: string;
  rating?: number;
  reviewCount?: number;
  price?: string;
  priceBn?: string;
  amenities?: string[];
  amenitiesBn?: string[];
  images?: string[];
  backgroundImage?: string;
  keywords?: string[];
}

export function generateListingMetadata(options: ListingMetadataOptions): Metadata {
  const {
    id,
    name,
    nameBn,
    description,
    descriptionBn,
    listingType,
    location,
    locationBn,
    rating,
    reviewCount,
    price,
    priceBn,
    amenities = [],
    amenitiesBn = [],
    images = [],
    backgroundImage,
    keywords = [],
  } = options;

  const typeLabel = {
    hotel: 'Hotel',
    tour: 'Tour',
    experience: 'Experience',
  }[listingType];

  const title = `${name} - ${typeLabel} in ${location} | ${SITE_NAME}`;
  const url = `${BASE_URL}/${listingType}s/${id}`;
  
  const ogImageUrl = generateListingOGImage({
    title: name,
    titleBn: nameBn,
    subtitle: `${typeLabel} in ${location}`,
    subtitleBn: locationBn ? `${locationBn}-এ ${typeLabel}` : undefined,
    listingType,
    location,
    locationBn,
    rating,
    reviewCount,
    price,
    priceBn,
    amenities,
    amenitiesBn,
    backgroundImage,
  });

  const fullDescription = [
    description,
    rating && reviewCount ? `Rated ${rating}/5 by ${reviewCount} guests.` : '',
    amenities.length > 0 ? `Amenities: ${amenities.slice(0, 3).join(', ')}.` : '',
    price ? `Starting from ${price}.` : '',
  ].filter(Boolean).join(' ');

  return {
    title,
    description: fullDescription,
    keywords: [
      name,
      ...(nameBn ? [nameBn] : []),
      typeLabel,
      location,
      ...(locationBn ? [locationBn] : []),
      ...amenities,
      ...amenitiesBn,
      'Bangladesh travel',
      'বাংলাদেশ ভ্রমণ',
      ...keywords,
    ],
    alternates: {
      canonical: url,
    },
    openGraph: {
      type: 'article',
      url,
      title,
      description: fullDescription,
      siteName: SITE_NAME,
      images: [
        {
          url: ogImageUrl,
          width: 1200,
          height: 630,
          alt: `${name} - ${typeLabel} in ${location}`,
        },
        ...images.map((img, index) => ({
          url: img,
          alt: `${name} - Image ${index + 1}`,
        })),
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title,
      description: fullDescription,
      images: [ogImageUrl],
    },
  };
}

// Itinerary page metadata
export interface ItineraryMetadataOptions {
  id: string;
  title: string;
  titleBn?: string;
  description: string;
  descriptionBn?: string;
  duration: string;
  durationBn?: string;
  destinations: string[];
  destinationsBn?: string[];
  difficulty?: 'easy' | 'moderate' | 'challenging';
  difficultyBn?: string;
  totalCost?: string;
  totalCostBn?: string;
  author?: string;
  authorBn?: string;
  images?: string[];
  backgroundImage?: string;
  keywords?: string[];
}

export function generateItineraryMetadata(options: ItineraryMetadataOptions): Metadata {
  const {
    id,
    title,
    titleBn,
    description,
    descriptionBn,
    duration,
    durationBn,
    destinations,
    destinationsBn,
    difficulty,
    difficultyBn,
    totalCost,
    totalCostBn,
    author,
    authorBn,
    images = [],
    backgroundImage,
    keywords = [],
  } = options;

  const pageTitle = `${title} - ${duration} Itinerary | ${SITE_NAME}`;
  const url = `${BASE_URL}/itineraries/${id}`;
  
  const ogImageUrl = generateItineraryOGImage({
    title,
    titleBn,
    subtitle: `${duration} • ${destinations.length} destinations`,
    subtitleBn: durationBn && destinationsBn ? `${durationBn} • ${destinationsBn.length} গন্তব্য` : undefined,
    duration,
    durationBn,
    destinations,
    destinationsBn,
    difficulty,
    difficultyBn,
    totalCost,
    totalCostBn,
    backgroundImage,
  });

  const fullDescription = [
    description,
    `${duration} itinerary covering ${destinations.join(', ')}.`,
    difficulty ? `Difficulty: ${difficulty}.` : '',
    totalCost ? `Total cost: ${totalCost}.` : '',
    author ? `Created by ${author}.` : '',
  ].filter(Boolean).join(' ');

  return {
    title: pageTitle,
    description: fullDescription,
    keywords: [
      title,
      ...(titleBn ? [titleBn] : []),
      'itinerary',
      'ভ্রমণ পরিকল্পনা',
      'travel plan',
      ...destinations,
      ...(destinationsBn || []),
      'Bangladesh travel',
      'বাংলাদেশ ভ্রমণ',
      ...keywords,
    ],
    alternates: {
      canonical: url,
    },
    openGraph: {
      type: 'article',
      url,
      title: pageTitle,
      description: fullDescription,
      siteName: SITE_NAME,
      images: [
        {
          url: ogImageUrl,
          width: 1200,
          height: 630,
          alt: `${title} - ${duration} Itinerary`,
        },
        ...images.map((img, index) => ({
          url: img,
          alt: `${title} - Image ${index + 1}`,
        })),
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title: pageTitle,
      description: fullDescription,
      images: [ogImageUrl],
    },
  };
}

// Story/Blog post metadata
export interface StoryMetadataOptions {
  id: string;
  title: string;
  titleBn?: string;
  excerpt: string;
  excerptBn?: string;
  content?: string;
  contentBn?: string;
  author: string;
  authorBn?: string;
  publishedAt: string;
  updatedAt?: string;
  readTime: string;
  readTimeBn?: string;
  category: string;
  categoryBn?: string;
  tags?: string[];
  tagsBn?: string[];
  images?: string[];
  featuredImage?: string;
  keywords?: string[];
}

export function generateStoryMetadata(options: StoryMetadataOptions): Metadata {
  const {
    id,
    title,
    titleBn,
    excerpt,
    excerptBn,
    author,
    authorBn,
    publishedAt,
    updatedAt,
    readTime,
    readTimeBn,
    category,
    categoryBn,
    tags = [],
    tagsBn = [],
    images = [],
    featuredImage,
    keywords = [],
  } = options;

  const pageTitle = `${title} | ${SITE_NAME} Stories`;
  const url = `${BASE_URL}/stories/${id}`;
  
  const ogImageUrl = generateStoryOGImage({
    title,
    titleBn,
    subtitle: `By ${author}`,
    subtitleBn: authorBn ? `${authorBn} দ্বারা` : undefined,
    author,
    authorBn,
    publishedAt,
    readTime,
    readTimeBn,
    category,
    categoryBn,
    backgroundImage: featuredImage,
  });

  const publishDate = new Date(publishedAt);
  const updateDate = updatedAt ? new Date(updatedAt) : undefined;

  return {
    title: pageTitle,
    description: excerpt,
    keywords: [
      title,
      ...(titleBn ? [titleBn] : []),
      author,
      ...(authorBn ? [authorBn] : []),
      category,
      ...(categoryBn ? [categoryBn] : []),
      ...tags,
      ...tagsBn,
      'Bangladesh travel stories',
      'বাংলাদেশ ভ্রমণের গল্প',
      'travel blog',
      'ভ্রমণ ব্লগ',
      ...keywords,
    ],
    authors: [{ name: author }],
    alternates: {
      canonical: url,
    },
    openGraph: {
      type: 'article',
      url,
      title: pageTitle,
      description: excerpt,
      siteName: SITE_NAME,
      publishedTime: publishDate.toISOString(),
      modifiedTime: updateDate?.toISOString(),
      authors: [author],
      section: category,
      tags: [...tags, ...tagsBn],
      images: [
        {
          url: ogImageUrl,
          width: 1200,
          height: 630,
          alt: title,
        },
        ...(featuredImage ? [{
          url: featuredImage,
          alt: title,
        }] : []),
        ...images.map((img, index) => ({
          url: img,
          alt: `${title} - Image ${index + 1}`,
        })),
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title: pageTitle,
      description: excerpt,
      images: [ogImageUrl],
      creator: `@${author.toLowerCase().replace(/\s+/g, '_')}`,
    },
  };
}

// Profile page metadata
export interface ProfileMetadataOptions {
  username: string;
  usernameBn?: string;
  displayName: string;
  displayNameBn?: string;
  bio?: string;
  bioBn?: string;
  location?: string;
  locationBn?: string;
  joinedAt: string;
  stats: {
    trips: number;
    reviews: number;
    photos: number;
    followers?: number;
    following?: number;
  };
  statsBn?: {
    trips: string;
    reviews: string;
    photos: string;
    followers?: string;
    following?: string;
  };
  avatar?: string;
  coverImage?: string;
  verified?: boolean;
  keywords?: string[];
}

export function generateProfileMetadata(options: ProfileMetadataOptions): Metadata {
  const {
    username,
    usernameBn,
    displayName,
    displayNameBn,
    bio,
    bioBn,
    location,
    locationBn,
    joinedAt,
    stats,
    statsBn,
    avatar,
    coverImage,
    verified = false,
    keywords = [],
  } = options;

  const pageTitle = `${displayName} (@${username}) | ${SITE_NAME}`;
  const url = `${BASE_URL}/travelers/${username}`;
  
  const description = [
    bio || `${displayName} is a traveler on HopNGo.`,
    `${stats.trips} trips, ${stats.reviews} reviews, ${stats.photos} photos.`,
    location ? `Based in ${location}.` : '',
    `Joined ${new Date(joinedAt).getFullYear()}.`,
  ].filter(Boolean).join(' ');

  const ogImageUrl = generateProfileOGImage({
    title: displayName,
    titleBn: displayNameBn,
    subtitle: `@${username}`,
    subtitleBn: usernameBn ? `@${usernameBn}` : undefined,
    username,
    usernameBn,
    bio,
    bioBn,
    stats,
    statsBn,
    backgroundImage: coverImage,
  });

  return {
    title: pageTitle,
    description,
    keywords: [
      displayName,
      ...(displayNameBn ? [displayNameBn] : []),
      username,
      ...(usernameBn ? [usernameBn] : []),
      'traveler profile',
      'ভ্রমণকারী প্রোফাইল',
      'Bangladesh traveler',
      'বাংলাদেশি ভ্রমণকারী',
      ...(location ? [location] : []),
      ...(locationBn ? [locationBn] : []),
      ...keywords,
    ],
    alternates: {
      canonical: url,
    },
    openGraph: {
      type: 'profile',
      url,
      title: pageTitle,
      description,
      siteName: SITE_NAME,
      images: [
        {
          url: ogImageUrl,
          width: 1200,
          height: 630,
          alt: `${displayName} (@${username}) - HopNGo Profile`,
        },
        ...(avatar ? [{
          url: avatar,
          alt: `${displayName}'s avatar`,
        }] : []),
        ...(coverImage ? [{
          url: coverImage,
          alt: `${displayName}'s cover image`,
        }] : []),
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title: pageTitle,
      description,
      images: [ogImageUrl],
    },
  };
}

// Utility function to generate JSON-LD structured data
export function generateStructuredData(type: string, data: any) {
  const baseStructuredData = {
    '@context': 'https://schema.org',
    '@type': type,
    ...data,
  };

  return {
    __html: JSON.stringify(baseStructuredData),
  };
}

// Generate structured data for destinations
export function generateDestinationStructuredData(options: DestinationMetadataOptions) {
  return generateStructuredData('TouristDestination', {
    name: options.name,
    alternateName: options.nameBn,
    description: options.description,
    image: options.images,
    address: {
      '@type': 'PostalAddress',
      addressLocality: options.location,
      addressCountry: 'BD',
    },
    geo: {
      '@type': 'GeoCoordinates',
      // Add coordinates if available
    },
    aggregateRating: options.rating && options.reviewCount ? {
      '@type': 'AggregateRating',
      ratingValue: options.rating,
      reviewCount: options.reviewCount,
    } : undefined,
    offers: options.price ? {
      '@type': 'Offer',
      price: options.price,
      priceCurrency: 'BDT',
    } : undefined,
  });
}
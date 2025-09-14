import { Metadata } from 'next';

interface SEOData {
  title: string;
  description: string;
  image?: string;
  url?: string;
  type?: 'website' | 'article' | 'product';
  price?: number;
  currency?: string;
  location?: string;
  author?: string;
  publishedTime?: string;
  modifiedTime?: string;
}

export function generateSEOMetadata(data: SEOData): Metadata {
  const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'https://hopngo.com';
  const fullUrl = data.url ? `${baseUrl}${data.url}` : baseUrl;
  
  // Generate Cloudinary optimized image URL if image provided
  const optimizedImage = data.image 
    ? `https://res.cloudinary.com/hopngo/image/fetch/w_1200,h_630,c_fill,f_auto,q_auto/${encodeURIComponent(data.image)}`
    : `${baseUrl}/og-default.jpg`;

  const metadata: Metadata = {
    title: data.title,
    description: data.description,
    alternates: {
      canonical: fullUrl,
    },
    openGraph: {
      type: (data.type === 'product' ? 'website' : data.type) || 'website',
      title: data.title,
      description: data.description,
      url: fullUrl,
      siteName: 'HopNGo',
      images: [
        {
          url: optimizedImage,
          width: 1200,
          height: 630,
          alt: data.title,
        },
      ],
      locale: 'en_US',
    },
    twitter: {
      card: 'summary_large_image',
      title: data.title,
      description: data.description,
      images: [optimizedImage],
      creator: '@HopNGoBD',
      site: '@HopNGoBD',
    },
  };

  // Add article-specific metadata
  if (data.type === 'article' && data.publishedTime) {
    metadata.openGraph = {
      ...metadata.openGraph,
      type: 'article',
      publishedTime: data.publishedTime,
      modifiedTime: data.modifiedTime || data.publishedTime,
      authors: data.author ? [data.author] : undefined,
    };
  }

  // Add product-specific metadata
  if (data.type === 'product' && data.price && data.currency) {
    metadata.openGraph = {
      ...metadata.openGraph,
      type: 'website',
    };
  }

  return metadata;
}

export function generateListingMetadata(listing: {
  id: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  location: { city: string; country: string };
  images: string[];
  author: { name: string };
  createdAt: string;
}): Metadata {
  const truncatedDescription = listing.description.length > 160 
    ? `${listing.description.substring(0, 157)}...` 
    : listing.description;

  const priceFormatted = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: listing.currency,
  }).format(listing.price);

  return generateSEOMetadata({
    title: `${listing.title} - ${priceFormatted} | HopNGo`,
    description: `${truncatedDescription} Located in ${listing.location.city}, ${listing.location.country}. Book now on HopNGo.`,
    image: listing.images[0],
    url: `/places/${listing.id}`,
    type: 'product',
    price: listing.price,
    currency: listing.currency,
    location: `${listing.location.city}, ${listing.location.country}`,
    author: listing.author.name,
    publishedTime: listing.createdAt,
  });
}

export function generateItineraryMetadata(itinerary: {
  id: string;
  title: string;
  description: string;
  destination: string;
  duration: number;
  images: string[];
  author: { name: string };
  createdAt: string;
}): Metadata {
  const truncatedDescription = itinerary.description.length > 160 
    ? `${itinerary.description.substring(0, 157)}...` 
    : itinerary.description;

  return generateSEOMetadata({
    title: `${itinerary.title} - ${itinerary.duration} Days | HopNGo`,
    description: `${truncatedDescription} Explore ${itinerary.destination} with this ${itinerary.duration}-day itinerary.`,
    image: itinerary.images[0],
    url: `/itineraries/${itinerary.id}`,
    type: 'article',
    author: itinerary.author.name,
    publishedTime: itinerary.createdAt,
  });
}

export function generatePostMetadata(post: {
  id: string;
  title: string;
  excerpt: string;
  featuredImage?: string;
  author: { name: string };
  publishedAt: string;
  updatedAt?: string;
}): Metadata {
  const truncatedExcerpt = post.excerpt.length > 160 
    ? `${post.excerpt.substring(0, 157)}...` 
    : post.excerpt;

  return generateSEOMetadata({
    title: `${post.title} | HopNGo Blog`,
    description: truncatedExcerpt,
    image: post.featuredImage,
    url: `/blog/${post.id}`,
    type: 'article',
    author: post.author.name,
    publishedTime: post.publishedAt,
    modifiedTime: post.updatedAt,
  });
}

// JSON-LD Schema generators
export function generateProductSchema(listing: {
  id: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  images: string[];
  author: { name: string };
  location: { city: string; country: string; address?: string };
  category: string;
}) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: listing.title,
    description: listing.description,
    image: listing.images,
    offers: {
      '@type': 'Offer',
      price: listing.price,
      priceCurrency: listing.currency,
      availability: 'https://schema.org/InStock',
      seller: {
        '@type': 'Person',
        name: listing.author.name,
      },
    },
    category: listing.category,
    location: {
      '@type': 'Place',
      name: `${listing.location.city}, ${listing.location.country}`,
      address: listing.location.address || `${listing.location.city}, ${listing.location.country}`,
    },
  };
}

export function generatePlaceSchema(place: {
  name: string;
  description: string;
  address: string;
  city: string;
  country: string;
  images: string[];
  latitude?: number;
  longitude?: number;
}) {
  const schema: any = {
    '@context': 'https://schema.org',
    '@type': 'Place',
    name: place.name,
    description: place.description,
    image: place.images,
    address: {
      '@type': 'PostalAddress',
      streetAddress: place.address,
      addressLocality: place.city,
      addressCountry: place.country,
    },
  };

  if (place.latitude && place.longitude) {
    schema.geo = {
      '@type': 'GeoCoordinates',
      latitude: place.latitude,
      longitude: place.longitude,
    };
  }

  return schema;
}

export function generateTravelActionSchema(action: {
  name: string;
  description: string;
  location: string;
  price?: number;
  currency?: string;
  duration?: string;
}) {
  const schema: any = {
    '@context': 'https://schema.org',
    '@type': 'TravelAction',
    name: action.name,
    description: action.description,
    location: {
      '@type': 'Place',
      name: action.location,
    },
  };

  if (action.price && action.currency) {
    schema.price = {
      '@type': 'MonetaryAmount',
      value: action.price,
      currency: action.currency,
    };
  }

  if (action.duration) {
    schema.duration = action.duration;
  }

  return schema;
}

export function generateListingJsonLd(listing: any): string {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': listing.category === 'gear' ? 'Product' : 'Place',
    name: listing.title,
    description: listing.description,
    image: listing.images || [],
    url: `${process.env.NEXT_PUBLIC_BASE_URL}/places/${listing.id}`,
    ...(listing.category === 'gear' ? {
      // Product schema for gear
      brand: listing.brand || 'HopNGo',
      category: listing.category,
      offers: {
        '@type': 'Offer',
        price: listing.price,
        priceCurrency: listing.currency || 'USD',
        availability: listing.status === 'ACTIVE' ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock',
        seller: {
          '@type': 'Person',
          name: listing.author.name,
          image: listing.author.avatar
        }
      },
      aggregateRating: listing.averageRating ? {
        '@type': 'AggregateRating',
        ratingValue: listing.averageRating,
        reviewCount: listing.reviewCount || 0
      } : undefined
    } : {
      // Place schema for stays/experiences
      address: {
        '@type': 'PostalAddress',
        addressLocality: listing.location.city,
        addressCountry: listing.location.country,
        streetAddress: listing.location.address
      },
      geo: listing.location.coordinates ? {
        '@type': 'GeoCoordinates',
        latitude: listing.location.coordinates.lat,
        longitude: listing.location.coordinates.lng
      } : undefined,
      amenityFeature: listing.amenities?.map((amenity: string) => ({
        '@type': 'LocationFeatureSpecification',
        name: amenity
      })),
      priceRange: `${listing.currency || 'USD'} ${listing.price}`,
      aggregateRating: listing.averageRating ? {
        '@type': 'AggregateRating',
        ratingValue: listing.averageRating,
        reviewCount: listing.reviewCount || 0
      } : undefined
    })
  };

  return JSON.stringify(jsonLd);
}

export function generateItineraryJsonLd(itinerary: any): string {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'TravelAction',
    name: itinerary.title,
    description: itinerary.description,
    image: itinerary.images || [],
    url: `${process.env.NEXT_PUBLIC_BASE_URL}/itineraries/${itinerary.id}`,
    agent: {
      '@type': 'Person',
      name: itinerary.author.name,
      image: itinerary.author.avatar
    },
    location: {
      '@type': 'Place',
      name: `${itinerary.destination.city}, ${itinerary.destination.country}`,
      address: {
        '@type': 'PostalAddress',
        addressLocality: itinerary.destination.city,
        addressCountry: itinerary.destination.country
      }
    },
    duration: `P${itinerary.duration}D`,
    price: {
      '@type': 'MonetaryAmount',
      currency: itinerary.budget.currency,
      minValue: itinerary.budget.min,
      maxValue: itinerary.budget.max
    },
    itinerary: itinerary.items?.map((item: any, index: number) => ({
      '@type': 'TouristTrip',
      name: item.title,
      description: item.description,
      location: {
        '@type': 'Place',
        name: item.location.name,
        address: item.location.address
      },
      duration: item.duration ? `PT${item.duration}M` : undefined,
      position: index + 1
    })),
    aggregateRating: itinerary.averageRating ? {
      '@type': 'AggregateRating',
      ratingValue: itinerary.averageRating,
      reviewCount: itinerary.reviewCount || 0
    } : undefined
  };

  return JSON.stringify(jsonLd);
}

export function generatePostJsonLd(post: any): string {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: post.title,
    description: post.excerpt,
    image: post.featuredImage ? [post.featuredImage] : [],
    url: `${process.env.NEXT_PUBLIC_BASE_URL}/blog/${post.slug}`,
    datePublished: post.publishedAt || post.createdAt,
    dateModified: post.updatedAt || post.createdAt,
    author: {
      '@type': 'Person',
      name: post.author.name,
      image: post.author.avatar
    },
    publisher: {
      '@type': 'Organization',
      name: 'HopNGo',
      logo: {
        '@type': 'ImageObject',
        url: `${process.env.NEXT_PUBLIC_BASE_URL}/logo.png`
      }
    },
    mainEntityOfPage: {
      '@type': 'WebPage',
      '@id': `${process.env.NEXT_PUBLIC_BASE_URL}/blog/${post.slug}`
    },
    articleSection: post.category,
    keywords: post.tags?.join(', '),
    wordCount: post.content?.split(' ').length || 0,
    timeRequired: `PT${post.readTime || 5}M`,
    interactionStatistic: {
      '@type': 'InteractionCounter',
      interactionType: 'https://schema.org/ReadAction',
      userInteractionCount: post.views || 0
    }
  };

  return JSON.stringify(jsonLd);
}
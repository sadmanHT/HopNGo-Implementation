import { MetadataRoute } from 'next';
import { listingService } from '@/lib/services/listing';
import { blogService } from '@/lib/services/blog';
import { itineraryService } from '@/lib/services/itinerary';

const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://hopngo.com';

export async function GET(): Promise<Response> {
  const sitemap = await generateSitemap();
  
  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${sitemap.map(entry => `  <url>
    <loc>${entry.url}</loc>
    <lastmod>${entry.lastModified}</lastmod>
    <changefreq>${entry.changeFrequency}</changefreq>
    <priority>${entry.priority}</priority>
  </url>`).join('\n')}
</urlset>`;

  return new Response(xml, {
    headers: {
      'Content-Type': 'application/xml',
      'Cache-Control': 'public, max-age=3600, s-maxage=3600'
    }
  });
}

async function generateSitemap(): Promise<MetadataRoute.Sitemap> {
  const sitemap: MetadataRoute.Sitemap = [];
  const now = new Date().toISOString();

  // Static pages
  const staticPages = [
    { url: '', priority: 1.0, changeFrequency: 'daily' as const },
    { url: '/search', priority: 0.9, changeFrequency: 'daily' as const },
    { url: '/about', priority: 0.7, changeFrequency: 'monthly' as const },
    { url: '/help', priority: 0.6, changeFrequency: 'weekly' as const },
    { url: '/support/contact', priority: 0.5, changeFrequency: 'monthly' as const },
    { url: '/privacy', priority: 0.4, changeFrequency: 'yearly' as const },
    { url: '/terms', priority: 0.4, changeFrequency: 'yearly' as const },
  ];

  staticPages.forEach(page => {
    sitemap.push({
      url: `${BASE_URL}${page.url}`,
      lastModified: now,
      changeFrequency: page.changeFrequency,
      priority: page.priority
    });
  });

  try {
    // Dynamic listings
    const listings = await listingService.getPublicListings({ limit: 1000 });
    listings.forEach(listing => {
      sitemap.push({
        url: `${BASE_URL}/places/${listing.id}`,
        lastModified: listing.updatedAt || listing.createdAt,
        changeFrequency: 'weekly',
        priority: 0.8
      });
    });

    // Blog posts
    const posts = await blogService.getPublicPosts({ limit: 1000 });
    posts.forEach(post => {
      sitemap.push({
        url: `${BASE_URL}/blog/${post.slug}`,
        lastModified: post.updatedAt || post.createdAt,
        changeFrequency: 'monthly',
        priority: 0.7
      });
    });

    // Itineraries
    const itineraries = await itineraryService.getPublicItineraries({ limit: 1000 });
    itineraries.forEach(itinerary => {
      sitemap.push({
        url: `${BASE_URL}/itineraries/${itinerary.id}`,
        lastModified: itinerary.updatedAt || itinerary.createdAt,
        changeFrequency: 'weekly',
        priority: 0.8
      });
    });

    // Category pages
    const categories = ['stays', 'experiences', 'tours', 'gear'];
    categories.forEach(category => {
      sitemap.push({
        url: `${BASE_URL}/search?category=${category}`,
        lastModified: now,
        changeFrequency: 'daily',
        priority: 0.7
      });
    });

    // Location-based pages (popular destinations)
    const popularLocations = [
      'new-york', 'london', 'paris', 'tokyo', 'sydney',
      'barcelona', 'amsterdam', 'rome', 'berlin', 'bangkok'
    ];
    popularLocations.forEach(location => {
      sitemap.push({
        url: `${BASE_URL}/search?location=${location}`,
        lastModified: now,
        changeFrequency: 'daily',
        priority: 0.6
      });
    });

  } catch (error) {
    console.error('Error generating dynamic sitemap entries:', error);
    // Continue with static pages only if dynamic content fails
  }

  return sitemap;
}
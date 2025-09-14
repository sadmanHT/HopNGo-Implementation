import { NextResponse } from 'next/server';

const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://hopngo.com';

export async function GET(): Promise<NextResponse> {
  const robotsTxt = `User-agent: *
Allow: /

# Disallow admin and private areas
Disallow: /admin/
Disallow: /dashboard/
Disallow: /api/
Disallow: /auth/
Disallow: /_next/
Disallow: /checkout/
Disallow: /profile/
Disallow: /settings/
Disallow: /messages/

# Disallow search result pages with parameters to avoid duplicate content
Disallow: /search?*
Disallow: /*?ref=*
Disallow: /*?utm_*

# Allow specific search pages
Allow: /search$
Allow: /search/

# Disallow temporary and test pages
Disallow: /test/
Disallow: /temp/
Disallow: /dev/

# Crawl delay for respectful crawling
Crawl-delay: 1

# Sitemap location
Sitemap: ${BASE_URL}/sitemap.xml

# Specific rules for different bots
User-agent: Googlebot
Allow: /
Disallow: /admin/
Disallow: /dashboard/
Disallow: /api/
Crawl-delay: 1

User-agent: Bingbot
Allow: /
Disallow: /admin/
Disallow: /dashboard/
Disallow: /api/
Crawl-delay: 1

# Social media bots (allow for rich previews)
User-agent: facebookexternalhit
Allow: /

User-agent: Twitterbot
Allow: /

User-agent: LinkedInBot
Allow: /

# Block aggressive crawlers
User-agent: AhrefsBot
Disallow: /

User-agent: MJ12bot
Disallow: /

User-agent: DotBot
Disallow: /`;

  return new NextResponse(robotsTxt, {
    headers: {
      'Content-Type': 'text/plain',
      'Cache-Control': 'public, max-age=86400, s-maxage=86400' // Cache for 24 hours
    }
  });
}
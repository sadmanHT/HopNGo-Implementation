import { NextRequest, NextResponse } from 'next/server';

const locales = ['en', 'bn'];
const defaultLocale = 'bn';

/**
 * Add comprehensive security headers to response
 */
function addSecurityHeaders(response: NextResponse, pathname: string): NextResponse {
  // Content Security Policy - Strict policy for React app
  const csp = [
    "default-src 'self'",
    "script-src 'self' 'unsafe-eval' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com data:",
    "img-src 'self' data: https: blob:",
    "connect-src 'self' https://api.hopngo.com wss://api.hopngo.com",
    "media-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    "upgrade-insecure-requests",
    "block-all-mixed-content"
  ].join('; ');
  
  response.headers.set('Content-Security-Policy', csp);
  
  // HTTP Strict Transport Security with preload
  response.headers.set(
    'Strict-Transport-Security',
    'max-age=31536000; includeSubDomains; preload'
  );
  
  // Prevent MIME type sniffing
  response.headers.set('X-Content-Type-Options', 'nosniff');
  
  // Prevent clickjacking
  response.headers.set('X-Frame-Options', 'DENY');
  
  // Strict referrer policy
  response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
  
  // Permissions Policy - Restrict dangerous features
  const permissionsPolicy = [
    'geolocation=(self)',
    'microphone=()',
    'camera=()',
    'payment=(self)',
    'usb=()',
    'magnetometer=()',
    'gyroscope=()',
    'speaker=()',
    'vibrate=()',
    'fullscreen=(self)',
    'sync-xhr=()'
  ].join(', ');
  
  response.headers.set('Permissions-Policy', permissionsPolicy);
  
  // X-XSS-Protection (legacy but still useful)
  response.headers.set('X-XSS-Protection', '1; mode=block');
  
  // Cache control for sensitive pages
  if (pathname.includes('/admin') ||
      pathname.includes('/profile') ||
      pathname.includes('/dashboard')) {
    response.headers.set('Cache-Control', 'no-cache, no-store, must-revalidate');
    response.headers.set('Pragma', 'no-cache');
    response.headers.set('Expires', '0');
  }
  
  // Remove server information
  response.headers.delete('Server');
  response.headers.set('Server', 'HopNGo-Frontend');
  
  return response;
}

export function middleware(request: NextRequest) {
  // Check if there is any supported locale in the pathname
  const { pathname } = request.nextUrl;
  
  // Skip middleware for API routes, static files, and Next.js internals
  if (
    pathname.startsWith('/api/') ||
    pathname.startsWith('/_next/') ||
    pathname.startsWith('/favicon.ico') ||
    pathname.includes('.')
  ) {
    const response = NextResponse.next();
    return addSecurityHeaders(response, pathname);
  }

  const pathnameHasLocale = locales.some(
    (locale) => pathname.startsWith(`/${locale}/`) || pathname === `/${locale}`
  );

  if (pathnameHasLocale) {
    const response = NextResponse.next();
    return addSecurityHeaders(response, pathname);
  }

  // Get locale from Accept-Language header or use default
  const acceptLanguage = request.headers.get('accept-language');
  let locale = defaultLocale;
  
  if (acceptLanguage) {
    const preferredLocale = acceptLanguage
      .split(',')
      .map(lang => lang.split(';')[0].trim())
      .find(lang => locales.includes(lang.split('-')[0]));
    
    if (preferredLocale) {
      locale = preferredLocale.split('-')[0];
    }
  }

  // Check for stored language preference in cookie
  const storedLocale = request.cookies.get('preferred-language')?.value;
  if (storedLocale && locales.includes(storedLocale)) {
    locale = storedLocale;
  }

  // Redirect to the locale-prefixed URL
  const redirectUrl = new URL(`/${locale}${pathname}`, request.url);
  const response = NextResponse.redirect(redirectUrl);
  return addSecurityHeaders(response, pathname);
}

export const config = {
  matcher: [
    // Skip all internal paths (_next)
    '/((?!_next|api|favicon.ico).*)',
  ],
};
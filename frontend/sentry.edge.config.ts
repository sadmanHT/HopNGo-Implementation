import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  
  // Environment configuration
  environment: process.env.NODE_ENV || 'development',
  
  // Release version for tracking deployments
  release: process.env.APP_VERSION || '1.0.0',
  
  // Performance monitoring (lower sample rate for edge)
  tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.05 : 0.5,
  
  // Debug mode (disable in production)
  debug: process.env.NODE_ENV === 'development',
  
  // Privacy settings
  sendDefaultPii: false,
  
  // Tags
  initialScope: {
    tags: {
      service: 'frontend',
      component: 'edge',
      runtime: 'edge',
    },
  },
  
  // Ignore specific errors
  ignoreErrors: [
    // Edge runtime specific errors
    'Dynamic Code Evaluation',
    'WebAssembly',
    // Network errors
    'fetch failed',
    'NetworkError',
  ],
  
  // Before send callback
  beforeSend(event, hint) {
    // Filter out events in development
    if (process.env.NODE_ENV === 'development') {
      console.log('Sentry edge event (dev):', event);
    }
    
    // Don't send events if no DSN is configured
    if (!process.env.SENTRY_DSN) {
      return null;
    }
    
    // Add edge runtime context
    if (event.extra) {
      event.extra.runtime = 'edge';
    }
    
    return event;
  },
  
  // Before breadcrumb callback
  beforeBreadcrumb(breadcrumb, hint) {
    // Filter sensitive breadcrumbs for edge runtime
    if (breadcrumb.category === 'fetch') {
      // Remove sensitive data from fetch requests
      if (breadcrumb.data?.url) {
        breadcrumb.data.url = breadcrumb.data.url.replace(
          /([?&])(password|token|key|secret)=[^&]*/gi,
          '$1$2=***'
        );
      }
      return breadcrumb;
    }
    
    return breadcrumb;
  },
});
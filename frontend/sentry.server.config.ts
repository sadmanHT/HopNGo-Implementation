import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  
  // Environment configuration
  environment: process.env.NODE_ENV || 'development',
  
  // Release version for tracking deployments
  release: process.env.APP_VERSION || '1.0.0',
  
  // Performance monitoring
  tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
  
  // Profiling
  profilesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
  
  // Debug mode (disable in production)
  debug: process.env.NODE_ENV === 'development',
  
  // Privacy settings
  sendDefaultPii: false,
  
  // Server name
  serverName: process.env.HOSTNAME || 'hopngo-frontend',
  
  // Tags
  initialScope: {
    tags: {
      service: 'frontend',
      component: 'ssr',
      runtime: 'nodejs',
    },
  },
  
  // Ignore specific errors
  ignoreErrors: [
    // Next.js specific errors that are not actionable
    'ECONNRESET',
    'EPIPE',
    'ECANCELED',
    // Network timeouts
    'timeout',
    'ETIMEDOUT',
    // Aborted requests
    'AbortError',
    'The operation was aborted',
  ],
  
  // Before send callback
  beforeSend(event, hint) {
    // Filter out events in development
    if (process.env.NODE_ENV === 'development') {
      console.log('Sentry server event (dev):', event);
    }
    
    // Don't send events if no DSN is configured
    if (!process.env.SENTRY_DSN) {
      return null;
    }
    
    // Add server context
    if (event.extra) {
      event.extra.nodeVersion = process.version;
      event.extra.platform = process.platform;
      event.extra.arch = process.arch;
      
      // Add memory usage
      const memUsage = process.memoryUsage();
      event.extra.memoryUsage = {
        rss: Math.round(memUsage.rss / 1024 / 1024) + 'MB',
        heapTotal: Math.round(memUsage.heapTotal / 1024 / 1024) + 'MB',
        heapUsed: Math.round(memUsage.heapUsed / 1024 / 1024) + 'MB',
        external: Math.round(memUsage.external / 1024 / 1024) + 'MB',
      };
    }
    
    return event;
  },
  
  // Before breadcrumb callback
  beforeBreadcrumb(breadcrumb, hint) {
    // Filter sensitive breadcrumbs
    if (breadcrumb.category === 'http') {
      // Remove sensitive data from HTTP requests
      if (breadcrumb.data?.url) {
        breadcrumb.data.url = breadcrumb.data.url.replace(
          /([?&])(password|token|key|secret)=[^&]*/gi,
          '$1$2=***'
        );
      }
      
      // Remove sensitive headers
      if (breadcrumb.data?.headers) {
        const headers = breadcrumb.data.headers as Record<string, string>;
        Object.keys(headers).forEach(key => {
          if (key.toLowerCase().includes('authorization') ||
              key.toLowerCase().includes('cookie') ||
              key.toLowerCase().includes('token')) {
            headers[key] = '***';
          }
        });
      }
      
      return breadcrumb;
    }
    
    if (breadcrumb.category === 'console' && breadcrumb.level === 'error') {
      return breadcrumb;
    }
    
    return breadcrumb;
  },
  
  // Integrations
  integrations: [
    // Add Node.js specific integrations
  ],
});
import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  
  // Environment configuration
  environment: process.env.NODE_ENV || 'development',
  
  // Release version for tracking deployments
  release: process.env.NEXT_PUBLIC_APP_VERSION || '1.0.0',
  
  // Performance monitoring
  tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
  
  // Profiling
  profilesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0,
  
  // Session replay
  replaysSessionSampleRate: process.env.NODE_ENV === 'production' ? 0.01 : 0.1,
  replaysOnErrorSampleRate: 1.0,
  
  // Debug mode (disable in production)
  debug: process.env.NODE_ENV === 'development',
  
  // Privacy settings
  sendDefaultPii: false,
  
  // Integrations
  integrations: [
    Sentry.replayIntegration({
      // Mask all text and input content
      maskAllText: true,
      maskAllInputs: true,
      // Block media elements
      blockAllMedia: true,
    }),
    Sentry.browserTracingIntegration({
      // Capture interactions
      enableInp: true,
    }),
  ],
  
  // Tags
  initialScope: {
    tags: {
      service: 'frontend',
      component: 'web-app',
    },
  },
  
  // Ignore specific errors
  ignoreErrors: [
    // Browser extensions
    'top.GLOBALS',
    'originalCreateNotification',
    'canvas.contentDocument',
    'MyApp_RemoveAllHighlights',
    'http://tt.epicplay.com',
    "Can't find variable: ZiteReader",
    'jigsaw is not defined',
    'ComboSearch is not defined',
    'http://loading.retry.widdit.com/',
    'atomicFindClose',
    // Facebook borked
    'fb_xd_fragment',
    // ISP "optimizing" proxy - `Cache-Control: no-transform` seems to
    // reduce this. (thanks @acdha)
    // See http://stackoverflow.com/questions/4113268
    'bmi_SafeAddOnload',
    'EBCallBackMessageReceived',
    // See http://toolbar.conduit.com/Developer/HtmlAndGadget/Methods/JSInjection.aspx
    'conduitPage',
    // Network errors
    'Network request failed',
    'NetworkError',
    'Failed to fetch',
    // Chunk loading errors (common in SPAs)
    'Loading chunk',
    'ChunkLoadError',
    // Non-Error promise rejections
    'Non-Error promise rejection captured',
  ],
  
  // Ignore specific URLs
  denyUrls: [
    // Facebook flakiness
    /graph\.facebook\.com/i,
    // Facebook blocked
    /connect\.facebook\.net\/en_US\/all\.js/i,
    // Woopra flakiness
    /eatdifferent\.com\.woopra-ns\.com/i,
    /static\.woopra\.com\/js\/woopra\.js/i,
    // Chrome extensions
    /extensions\//i,
    /^chrome:\/\//i,
    /^chrome-extension:\/\//i,
    // Other plugins
    /127\.0\.0\.1:4001\/isrunning/i, // Cacaoweb
    /webappstoolbarba\.texthelp\.com\//i,
    /metrics\.itunes\.apple\.com\.edgesuite\.net\//i,
  ],
  
  // Before send callback
  beforeSend(event, hint) {
    // Filter out events in development
    if (process.env.NODE_ENV === 'development') {
      console.log('Sentry event (dev):', event);
    }
    
    // Don't send events if no DSN is configured
    if (!process.env.NEXT_PUBLIC_SENTRY_DSN) {
      return null;
    }
    
    return event;
  },
  
  // Before breadcrumb callback
  beforeBreadcrumb(breadcrumb, hint) {
    // Filter sensitive breadcrumbs
    if (breadcrumb.category === 'console' && breadcrumb.level === 'error') {
      return breadcrumb;
    }
    
    if (breadcrumb.category === 'navigation') {
      return breadcrumb;
    }
    
    if (breadcrumb.category === 'ui.click') {
      return breadcrumb;
    }
    
    if (breadcrumb.category === 'xhr' || breadcrumb.category === 'fetch') {
      // Remove sensitive data from API calls
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
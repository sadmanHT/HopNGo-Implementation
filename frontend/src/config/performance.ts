// Performance configuration and budgets

export const PERFORMANCE_BUDGETS = {
  // Core Web Vitals thresholds (in milliseconds)
  LCP: 2500, // Largest Contentful Paint
  FID: 100,  // First Input Delay
  CLS: 0.1,  // Cumulative Layout Shift
  
  // Additional metrics
  FCP: 1800, // First Contentful Paint
  TTFB: 800, // Time to First Byte
  
  // Bundle size budgets (in KB)
  INITIAL_JS: 200,
  TOTAL_JS: 500,
  CSS: 50,
  IMAGES: 1000,
};

export const CACHE_STRATEGIES = {
  // Static assets caching
  STATIC_ASSETS: {
    maxAge: 31536000, // 1 year
    staleWhileRevalidate: 86400, // 1 day
  },
  
  // API responses caching
  API_RESPONSES: {
    maxAge: 300, // 5 minutes
    staleWhileRevalidate: 60, // 1 minute
  },
  
  // Page caching
  PAGES: {
    maxAge: 3600, // 1 hour
    staleWhileRevalidate: 300, // 5 minutes
  },
  
  // Image caching
  IMAGES: {
    maxAge: 2592000, // 30 days
    staleWhileRevalidate: 86400, // 1 day
  },
};

export const OPTIMIZATION_CONFIG = {
  // Image optimization settings
  images: {
    formats: ['image/avif', 'image/webp', 'image/jpeg'],
    deviceSizes: [640, 750, 828, 1080, 1200, 1920, 2048, 3840],
    imageSizes: [16, 32, 48, 64, 96, 128, 256, 384],
    quality: {
      default: 75,
      hero: 85,
      thumbnail: 60,
      avatar: 70,
    },
  },
  
  // Code splitting configuration
  codeSplitting: {
    // Routes that should be lazy loaded
    lazyRoutes: [
      '/itinerary',
      '/bookings',
      '/analytics',
      '/admin',
    ],
    
    // Components that should be lazy loaded
    lazyComponents: [
      'ItineraryPlanner',
      'BookingSystem',
      'AnalyticsDashboard',
      'MapComponent',
      'PaymentForm',
    ],
  },
  
  // Preloading configuration
  preloading: {
    // Critical resources to preload
    critical: [
      '/api/user/profile',
      '/api/destinations/popular',
    ],
    
    // Resources to prefetch on idle
    prefetch: [
      '/api/itinerary/templates',
      '/api/bookings/history',
    ],
  },
  
  // Bundle optimization
  bundleOptimization: {
    // Libraries to optimize
    optimizePackageImports: [
      '@radix-ui/react-icons',
      'lucide-react',
      'date-fns',
    ],
    
    // External dependencies to load from CDN
    externals: {
      development: [],
      production: [
        // 'react',
        // 'react-dom',
      ],
    },
  },
};

// Performance monitoring configuration
export const MONITORING_CONFIG = {
  // Web Vitals reporting
  webVitals: {
    enabled: true,
    reportToConsole: process.env.NODE_ENV === 'development',
    reportToAnalytics: process.env.NODE_ENV === 'production',
  },
  
  // Performance observer configuration
  observer: {
    // Metrics to observe
    entryTypes: [
      'navigation',
      'paint',
      'largest-contentful-paint',
      'first-input',
      'layout-shift',
    ],
    
    // Sampling rate (0-1)
    samplingRate: process.env.NODE_ENV === 'development' ? 1 : 0.1,
  },
  
  // Resource timing monitoring
  resourceTiming: {
    enabled: true,
    // Resource types to monitor
    resourceTypes: ['script', 'stylesheet', 'image', 'fetch'],
    // Threshold for slow resources (in ms)
    slowResourceThreshold: 1000,
  },
};

// Feature flags for performance optimizations
export const PERFORMANCE_FEATURES = {
  // Enable/disable specific optimizations
  lazyLoading: true,
  imageOptimization: true,
  codeSplitting: true,
  preloading: true,
  caching: true,
  compression: true,
  minification: true,
  
  // Experimental features
  experimental: {
    turbopack: process.env.NODE_ENV === 'development',
    serverComponents: true,
    appDir: true,
  },
};

// Environment-specific configurations
export const getPerformanceConfig = () => {
  const isDevelopment = process.env.NODE_ENV === 'development';
  const isProduction = process.env.NODE_ENV === 'production';
  
  return {
    ...OPTIMIZATION_CONFIG,
    monitoring: {
      ...MONITORING_CONFIG,
      webVitals: {
        ...MONITORING_CONFIG.webVitals,
        reportToConsole: isDevelopment,
        reportToAnalytics: isProduction,
      },
    },
    features: {
      ...PERFORMANCE_FEATURES,
      experimental: {
        ...PERFORMANCE_FEATURES.experimental,
        turbopack: isDevelopment,
      },
    },
  };
};
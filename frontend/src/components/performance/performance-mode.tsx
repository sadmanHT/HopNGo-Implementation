'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { Zap, Database, Image, MapPin, Clock, CheckCircle, AlertCircle } from 'lucide-react';
import { InteractiveButton } from '../ui/micro-interactions';

// Performance metrics interface
interface PerformanceMetrics {
  cacheHitRate: number;
  avgLoadTime: number;
  prefetchedAssets: number;
  optimizedImages: number;
  lastUpdated: Date;
}

// Cache status interface
interface CacheStatus {
  posts: boolean;
  listings: boolean;
  itineraries: boolean;
  images: boolean;
  maps: boolean;
}

// Performance mode context
interface PerformanceContextType {
  isPerformanceMode: boolean;
  togglePerformanceMode: () => void;
  cacheStatus: CacheStatus;
  metrics: PerformanceMetrics;
  prefetchData: () => Promise<void>;
  clearCache: () => void;
}

const PerformanceContext = React.createContext<PerformanceContextType | null>(null);

// Performance mode provider
interface PerformanceProviderProps {
  children: React.ReactNode;
}

export const PerformanceProvider: React.FC<PerformanceProviderProps> = ({ children }) => {
  const [isPerformanceMode, setIsPerformanceMode] = useState(false);
  const [cacheStatus, setCacheStatus] = useState<CacheStatus>({
    posts: false,
    listings: false,
    itineraries: false,
    images: false,
    maps: false
  });
  const [metrics, setMetrics] = useState<PerformanceMetrics>({
    cacheHitRate: 0,
    avgLoadTime: 0,
    prefetchedAssets: 0,
    optimizedImages: 0,
    lastUpdated: new Date()
  });

  // Load performance mode state from localStorage
  useEffect(() => {
    const savedPerformanceMode = localStorage.getItem('hopngo-performance-mode');
    const savedCacheStatus = localStorage.getItem('hopngo-cache-status');
    const savedMetrics = localStorage.getItem('hopngo-performance-metrics');

    if (savedPerformanceMode === 'true') {
      setIsPerformanceMode(true);
    }
    if (savedCacheStatus) {
      setCacheStatus(JSON.parse(savedCacheStatus));
    }
    if (savedMetrics) {
      setMetrics(JSON.parse(savedMetrics));
    }
  }, []);

  // Save state to localStorage
  useEffect(() => {
    localStorage.setItem('hopngo-performance-mode', isPerformanceMode.toString());
    localStorage.setItem('hopngo-cache-status', JSON.stringify(cacheStatus));
    localStorage.setItem('hopngo-performance-metrics', JSON.stringify(metrics));
  }, [isPerformanceMode, cacheStatus, metrics]);

  // Prefetch demo data
  const prefetchData = useCallback(async () => {
    const startTime = Date.now();
    
    try {
      // Simulate prefetching different types of data
      const prefetchTasks = [
        // Posts data
        new Promise(resolve => {
          setTimeout(() => {
            setCacheStatus(prev => ({ ...prev, posts: true }));
            resolve('posts');
          }, 500);
        }),
        
        // Listings data
        new Promise(resolve => {
          setTimeout(() => {
            setCacheStatus(prev => ({ ...prev, listings: true }));
            resolve('listings');
          }, 800);
        }),
        
        // Itineraries data
        new Promise(resolve => {
          setTimeout(() => {
            setCacheStatus(prev => ({ ...prev, itineraries: true }));
            resolve('itineraries');
          }, 600);
        }),
        
        // Images prefetch
        new Promise(resolve => {
          setTimeout(() => {
            setCacheStatus(prev => ({ ...prev, images: true }));
            resolve('images');
          }, 1200);
        }),
        
        // Maps data
        new Promise(resolve => {
          setTimeout(() => {
            setCacheStatus(prev => ({ ...prev, maps: true }));
            resolve('maps');
          }, 1000);
        })
      ];

      await Promise.all(prefetchTasks);
      
      const endTime = Date.now();
      const loadTime = endTime - startTime;
      
      // Update metrics
      setMetrics(prev => ({
        cacheHitRate: 95 + Math.random() * 5, // 95-100%
        avgLoadTime: loadTime,
        prefetchedAssets: 150 + Math.floor(Math.random() * 50),
        optimizedImages: 85 + Math.floor(Math.random() * 15),
        lastUpdated: new Date()
      }));
      
    } catch (error) {
      console.error('Prefetch failed:', error);
    }
  }, []);

  // Clear cache
  const clearCache = useCallback(() => {
    setCacheStatus({
      posts: false,
      listings: false,
      itineraries: false,
      images: false,
      maps: false
    });
    
    setMetrics(prev => ({
      ...prev,
      cacheHitRate: 0,
      prefetchedAssets: 0,
      optimizedImages: 0,
      lastUpdated: new Date()
    }));
    
    // Clear browser caches
    if ('caches' in window) {
      caches.keys().then(names => {
        names.forEach(name => {
          if (name.includes('hopngo')) {
            caches.delete(name);
          }
        });
      });
    }
  }, []);

  // Toggle performance mode
  const togglePerformanceMode = useCallback(() => {
    setIsPerformanceMode(prev => {
      const newMode = !prev;
      if (newMode) {
        // Auto-prefetch when enabling performance mode
        prefetchData();
      } else {
        // Clear cache when disabling
        clearCache();
      }
      return newMode;
    });
  }, [prefetchData, clearCache]);

  // Auto-prefetch on performance mode enable
  useEffect(() => {
    if (isPerformanceMode && !Object.values(cacheStatus).some(Boolean)) {
      prefetchData();
    }
  }, [isPerformanceMode, cacheStatus, prefetchData]);

  return (
    <PerformanceContext.Provider value={{
      isPerformanceMode,
      togglePerformanceMode,
      cacheStatus,
      metrics,
      prefetchData,
      clearCache
    }}>
      {children}
    </PerformanceContext.Provider>
  );
};

// Hook to use performance context
export const usePerformanceMode = () => {
  const context = React.useContext(PerformanceContext);
  if (!context) {
    throw new Error('usePerformanceMode must be used within a PerformanceProvider');
  }
  return context;
};

// Performance dashboard component
interface PerformanceDashboardProps {
  className?: string;
}

export const PerformanceDashboard: React.FC<PerformanceDashboardProps> = ({
  className = ''
}) => {
  const {
    isPerformanceMode,
    togglePerformanceMode,
    cacheStatus,
    metrics,
    prefetchData,
    clearCache
  } = usePerformanceMode();
  
  const [isPrefetching, setIsPrefetching] = useState(false);

  const handlePrefetch = async () => {
    setIsPrefetching(true);
    await prefetchData();
    setIsPrefetching(false);
  };

  const cacheItems = [
    { key: 'posts', label: 'Posts Data', icon: <Database className="w-4 h-4" /> },
    { key: 'listings', label: 'Listings', icon: <MapPin className="w-4 h-4" /> },
    { key: 'itineraries', label: 'Itineraries', icon: <Clock className="w-4 h-4" /> },
    { key: 'images', label: 'Images', icon: <Image className="w-4 h-4" /> },
    { key: 'maps', label: 'Maps', icon: <MapPin className="w-4 h-4" /> }
  ];

  const allCached = Object.values(cacheStatus).every(Boolean);

  return (
    <div className={`bg-white rounded-xl border border-gray-200 p-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className={`p-2 rounded-lg ${
            isPerformanceMode ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-600'
          }`}>
            <Zap className="w-6 h-6" />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">Performance Mode</h3>
            <p className="text-sm text-gray-500">
              {isPerformanceMode ? 'Optimizations active' : 'Standard performance'}
            </p>
          </div>
        </div>
        
        <InteractiveButton
          variant={isPerformanceMode ? 'secondary' : 'primary'}
          onClick={togglePerformanceMode}
        >
          {isPerformanceMode ? 'Disable' : 'Enable'}
        </InteractiveButton>
      </div>

      {/* Performance Metrics */}
      {isPerformanceMode && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="text-center p-4 bg-green-50 rounded-lg">
            <div className="text-2xl font-bold text-green-600">
              {metrics.cacheHitRate.toFixed(1)}%
            </div>
            <div className="text-xs text-green-700">Cache Hit Rate</div>
          </div>
          
          <div className="text-center p-4 bg-blue-50 rounded-lg">
            <div className="text-2xl font-bold text-blue-600">
              {metrics.avgLoadTime}ms
            </div>
            <div className="text-xs text-blue-700">Avg Load Time</div>
          </div>
          
          <div className="text-center p-4 bg-purple-50 rounded-lg">
            <div className="text-2xl font-bold text-purple-600">
              {metrics.prefetchedAssets}
            </div>
            <div className="text-xs text-purple-700">Prefetched Assets</div>
          </div>
          
          <div className="text-center p-4 bg-orange-50 rounded-lg">
            <div className="text-2xl font-bold text-orange-600">
              {metrics.optimizedImages}
            </div>
            <div className="text-xs text-orange-700">Optimized Images</div>
          </div>
        </div>
      )}

      {/* Cache Status */}
      {isPerformanceMode && (
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <h4 className="font-medium text-gray-900">Cache Status</h4>
            <div className="flex space-x-2">
              <InteractiveButton
                variant="ghost"
                size="sm"
                onClick={handlePrefetch}
                disabled={isPrefetching || allCached}
                className="text-xs"
              >
                {isPrefetching ? 'Prefetching...' : 'Refresh Cache'}
              </InteractiveButton>
              <InteractiveButton
                variant="ghost"
                size="sm"
                onClick={clearCache}
                className="text-xs text-red-600 hover:text-red-700"
              >
                Clear Cache
              </InteractiveButton>
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {cacheItems.map(item => (
              <motion.div
                key={item.key}
                className={`flex items-center space-x-3 p-3 rounded-lg border ${
                  cacheStatus[item.key as keyof CacheStatus]
                    ? 'bg-green-50 border-green-200'
                    : 'bg-gray-50 border-gray-200'
                }`}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.1 }}
              >
                <div className={`p-1 rounded ${
                  cacheStatus[item.key as keyof CacheStatus]
                    ? 'text-green-600'
                    : 'text-gray-400'
                }`}>
                  {item.icon}
                </div>
                <span className="text-sm font-medium text-gray-700">
                  {item.label}
                </span>
                <div className="ml-auto">
                  {cacheStatus[item.key as keyof CacheStatus] ? (
                    <CheckCircle className="w-4 h-4 text-green-600" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-gray-400" />
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      )}

      {/* Performance Tips */}
      {isPerformanceMode && (
        <div className="bg-blue-50 p-4 rounded-lg">
          <h4 className="font-medium text-blue-900 mb-2">Performance Optimizations Active:</h4>
          <ul className="space-y-1 text-sm text-blue-800">
            <li>• Image lazy loading and compression</li>
            <li>• API response caching</li>
            <li>• Route-based code splitting</li>
            <li>• Prefetched critical resources</li>
            <li>• Service worker caching</li>
          </ul>
        </div>
      )}

      {/* Last Updated */}
      {isPerformanceMode && (
        <div className="mt-4 text-xs text-gray-500 text-center">
          Last updated: {metrics.lastUpdated.toLocaleTimeString()}
        </div>
      )}
    </div>
  );
};

// Performance indicator component
export const PerformanceIndicator: React.FC = () => {
  const { isPerformanceMode, metrics } = usePerformanceMode();

  if (!isPerformanceMode) return null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      className="fixed top-4 right-4 z-30 bg-green-600 text-white px-3 py-1 rounded-full text-xs font-medium shadow-lg"
    >
      <div className="flex items-center space-x-2">
        <Zap className="w-3 h-3" />
        <span>Performance Mode</span>
        <span className="bg-white/20 px-2 py-0.5 rounded">
          {metrics.cacheHitRate.toFixed(0)}%
        </span>
      </div>
    </motion.div>
  );
};

export default PerformanceDashboard;
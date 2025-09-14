'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/utils';
import { AccessibleAlert } from './accessibility';
import { InteractiveButton } from './micro-interactions';

// Hook to detect online/offline status
export function useOnlineStatus() {
  const [isOnline, setIsOnline] = useState(true);
  const [wasOffline, setWasOffline] = useState(false);

  useEffect(() => {
    // Check initial status
    setIsOnline(navigator.onLine);

    const handleOnline = () => {
      setIsOnline(true);
      if (wasOffline) {
        // Show reconnection notification
        setTimeout(() => setWasOffline(false), 3000);
      }
    };

    const handleOffline = () => {
      setIsOnline(false);
      setWasOffline(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [wasOffline]);

  return { isOnline, wasOffline };
}

// Offline banner component
interface OfflineBannerProps {
  className?: string;
  onRetry?: () => void;
  showRetryButton?: boolean;
}

export function OfflineBanner({ className, onRetry, showRetryButton = true }: OfflineBannerProps) {
  const { isOnline, wasOffline } = useOnlineStatus();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (!isOnline) {
      setIsVisible(true);
    } else if (wasOffline) {
      // Show reconnection message briefly
      setIsVisible(true);
      const timer = setTimeout(() => setIsVisible(false), 3000);
      return () => clearTimeout(timer);
    } else {
      setIsVisible(false);
    }
  }, [isOnline, wasOffline]);

  const handleRetry = useCallback(() => {
    if (onRetry) {
      onRetry();
    } else {
      window.location.reload();
    }
  }, [onRetry]);

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          className={cn('fixed top-0 left-0 right-0 z-50', className)}
          initial={{ y: -100, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: -100, opacity: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        >
          {!isOnline ? (
            <div className="bg-orange-500 text-white px-4 py-3 shadow-lg">
              <div className="flex items-center justify-between max-w-7xl mx-auto">
                <div className="flex items-center space-x-3">
                  <motion.svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    animate={{ rotate: [0, 10, -10, 0] }}
                    transition={{ duration: 2, repeat: Infinity }}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                    />
                  </motion.svg>
                  <div>
                    <p className="font-medium">You're currently offline</p>
                    <p className="text-sm opacity-90">
                      Check your internet connection. Some features may be limited.
                    </p>
                  </div>
                </div>
                {showRetryButton && (
                  <InteractiveButton
                    variant="ghost"
                    size="sm"
                    onClick={handleRetry}
                    className="text-white border-white hover:bg-white/10"
                  >
                    Retry
                  </InteractiveButton>
                )}
              </div>
            </div>
          ) : wasOffline ? (
            <div className="bg-green-500 text-white px-4 py-3 shadow-lg">
              <div className="flex items-center justify-center max-w-7xl mx-auto">
                <div className="flex items-center space-x-3">
                  <motion.svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    initial={{ scale: 0.8 }}
                    animate={{ scale: 1 }}
                    transition={{ type: 'spring', stiffness: 400 }}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </motion.svg>
                  <p className="font-medium">Back online! Your connection has been restored.</p>
                </div>
              </div>
            </div>
          ) : null}
        </motion.div>
      )}
    </AnimatePresence>
  );
}

// Offline-aware component wrapper
interface OfflineWrapperProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
  showBanner?: boolean;
}

export function OfflineWrapper({ children, fallback, showBanner = true }: OfflineWrapperProps) {
  const { isOnline } = useOnlineStatus();

  return (
    <>
      {showBanner && <OfflineBanner />}
      {isOnline ? children : (fallback || <OfflineFallback />)}
    </>
  );
}

// Default offline fallback component
function OfflineFallback() {
  const handleRetry = () => {
    window.location.reload();
  };

  const handleViewCached = () => {
    // Navigate to cached content or show cached data
    console.log('Viewing cached content...');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4">
      <motion.div
        className="max-w-md w-full text-center space-y-6"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <motion.div
          className="mx-auto w-24 h-24 bg-orange-100 dark:bg-orange-900/20 rounded-full flex items-center justify-center"
          animate={{ scale: [1, 1.05, 1] }}
          transition={{ duration: 2, repeat: Infinity }}
        >
          <svg
            className="w-12 h-12 text-orange-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192L5.636 18.364M12 2.25a9.75 9.75 0 100 19.5 9.75 9.75 0 000-19.5z"
            />
          </svg>
        </motion.div>
        
        <div className="space-y-2">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            You're offline
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            It looks like you've lost your internet connection. Check your network settings and try again.
          </p>
        </div>
        
        <div className="space-y-4">
          <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
            <h3 className="font-medium text-blue-900 dark:text-blue-100 mb-2">
              Troubleshooting steps:
            </h3>
            <ul className="text-sm text-blue-800 dark:text-blue-200 space-y-1">
              <li>• Check your Wi-Fi or mobile data connection</li>
              <li>• Try moving to a location with better signal</li>
              <li>• Restart your router or modem</li>
              <li>• Contact your internet service provider</li>
            </ul>
          </div>
          
          <div className="flex flex-col sm:flex-row gap-3">
            <InteractiveButton
              variant="primary"
              onClick={handleRetry}
              className="flex-1"
            >
              Try Again
            </InteractiveButton>
            <InteractiveButton
              variant="secondary"
              onClick={handleViewCached}
              className="flex-1"
            >
              View Cached Content
            </InteractiveButton>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

// Network status indicator
export function NetworkStatusIndicator({ className }: { className?: string }) {
  const { isOnline } = useOnlineStatus();
  const [showDetails, setShowDetails] = useState(false);

  return (
    <div className={cn('relative', className)}>
      <button
        onClick={() => setShowDetails(!showDetails)}
        className={cn(
          'flex items-center space-x-2 px-3 py-1 rounded-full text-xs font-medium transition-colors',
          isOnline
            ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
            : 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400'
        )}
        aria-label={`Network status: ${isOnline ? 'Online' : 'Offline'}`}
      >
        <div
          className={cn(
            'w-2 h-2 rounded-full',
            isOnline ? 'bg-green-500' : 'bg-red-500'
          )}
        />
        <span>{isOnline ? 'Online' : 'Offline'}</span>
      </button>
      
      <AnimatePresence>
        {showDetails && (
          <motion.div
            className="absolute top-full right-0 mt-2 w-64 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-4 z-50"
            initial={{ opacity: 0, scale: 0.95, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -10 }}
            transition={{ duration: 0.15 }}
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-medium text-gray-900 dark:text-gray-100">
                  Connection Status
                </span>
                <div
                  className={cn(
                    'w-3 h-3 rounded-full',
                    isOnline ? 'bg-green-500' : 'bg-red-500'
                  )}
                />
              </div>
              
              <div className="text-sm text-gray-600 dark:text-gray-400">
                {isOnline ? (
                  <div className="space-y-1">
                    <p>✓ Connected to the internet</p>
                    <p>✓ All features available</p>
                  </div>
                ) : (
                  <div className="space-y-1">
                    <p>✗ No internet connection</p>
                    <p>✗ Limited functionality</p>
                    <p className="mt-2 text-xs">
                      Some cached content may still be available
                    </p>
                  </div>
                )}
              </div>
              
              {!isOnline && (
                <InteractiveButton
                  variant="primary"
                  size="sm"
                  onClick={() => window.location.reload()}
                  className="w-full"
                >
                  Retry Connection
                </InteractiveButton>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// Hook for handling offline-aware API calls
export function useOfflineAwareRequest() {
  const { isOnline } = useOnlineStatus();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const makeRequest = useCallback(async (
    requestFn: () => Promise<any>,
    options?: {
      fallbackData?: any;
      showOfflineMessage?: boolean;
    }
  ) => {
    if (!isOnline) {
      if (options?.showOfflineMessage !== false) {
        setError('You are currently offline. Please check your internet connection.');
      }
      return options?.fallbackData || null;
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await requestFn();
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      setError(errorMessage);
      return options?.fallbackData || null;
    } finally {
      setIsLoading(false);
    }
  }, [isOnline]);

  return {
    makeRequest,
    isLoading,
    error,
    isOnline,
    clearError: () => setError(null),
  };
}

// Offline-aware image component
interface OfflineImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallbackSrc?: string;
  showOfflineIndicator?: boolean;
}

export function OfflineImage({ 
  src, 
  fallbackSrc, 
  showOfflineIndicator = true, 
  className, 
  alt,
  ...props 
}: OfflineImageProps) {
  const { isOnline } = useOnlineStatus();
  const [imageError, setImageError] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const handleImageLoad = () => {
    setIsLoading(false);
    setImageError(false);
  };

  const handleImageError = () => {
    setIsLoading(false);
    setImageError(true);
  };

  const shouldShowFallback = !isOnline || imageError;
  const imageSrc = shouldShowFallback ? fallbackSrc : src;

  return (
    <div className={cn('relative', className)}>
      {isLoading && (
        <div className="absolute inset-0 bg-gray-200 dark:bg-gray-700 animate-pulse rounded" />
      )}
      
      <img
        src={imageSrc}
        alt={alt}
        onLoad={handleImageLoad}
        onError={handleImageError}
        className={cn(
          'transition-opacity duration-300',
          isLoading ? 'opacity-0' : 'opacity-100'
        )}
        {...props}
      />
      
      {shouldShowFallback && showOfflineIndicator && (
        <div className="absolute top-2 right-2">
          <div className="bg-orange-500 text-white text-xs px-2 py-1 rounded-full flex items-center space-x-1">
            <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
            </svg>
            <span>Offline</span>
          </div>
        </div>
      )}
    </div>
  );
}

export default {
  useOnlineStatus,
  OfflineBanner,
  OfflineWrapper,
  NetworkStatusIndicator,
  useOfflineAwareRequest,
  OfflineImage,
};
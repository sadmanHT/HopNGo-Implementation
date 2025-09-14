'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Loader2, Wifi, WifiOff, RefreshCw, CheckCircle, XCircle, Clock, MapPin, Calendar, Users, Star } from 'lucide-react';

// Loading Spinner Variants
interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  variant?: 'primary' | 'secondary' | 'white' | 'muted';
  className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  variant = 'primary',
  className = '',
}) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
    xl: 'w-12 h-12',
  };

  const variantClasses = {
    primary: 'text-primary-500',
    secondary: 'text-secondary-500',
    white: 'text-white',
    muted: 'text-neutral-400',
  };

  return (
    <motion.div
      animate={{ rotate: 360 }}
      transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
      className={`${sizeClasses[size]} ${variantClasses[variant]} ${className}`}
    >
      <Loader2 className="w-full h-full" />
    </motion.div>
  );
};

// Pulse Loading Animation
interface PulseLoaderProps {
  count?: number;
  size?: 'sm' | 'md' | 'lg';
  color?: string;
}

export const PulseLoader: React.FC<PulseLoaderProps> = ({
  count = 3,
  size = 'md',
  color = 'bg-primary-500',
}) => {
  const sizeClasses = {
    sm: 'w-2 h-2',
    md: 'w-3 h-3',
    lg: 'w-4 h-4',
  };

  return (
    <div className="flex items-center space-x-1">
      {Array.from({ length: count }).map((_, i) => (
        <motion.div
          key={i}
          className={`${sizeClasses[size]} ${color} rounded-full`}
          animate={{
            scale: [1, 1.2, 1],
            opacity: [0.7, 1, 0.7],
          }}
          transition={{
            duration: 1.5,
            repeat: Infinity,
            delay: i * 0.2,
          }}
        />
      ))}
    </div>
  );
};

// Progress Bar
interface ProgressBarProps {
  progress: number;
  showPercentage?: boolean;
  variant?: 'primary' | 'success' | 'warning' | 'error';
  size?: 'sm' | 'md' | 'lg';
  animated?: boolean;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  progress,
  showPercentage = false,
  variant = 'primary',
  size = 'md',
  animated = true,
}) => {
  const variantClasses = {
    primary: 'bg-primary-500',
    success: 'bg-success-500',
    warning: 'bg-warning-500',
    error: 'bg-error-500',
  };

  const sizeClasses = {
    sm: 'h-1',
    md: 'h-2',
    lg: 'h-3',
  };

  return (
    <div className="w-full">
      <div className={`w-full bg-neutral-200 dark:bg-neutral-700 rounded-full overflow-hidden ${sizeClasses[size]}`}>
        <motion.div
          className={`h-full ${variantClasses[variant]} ${animated ? 'transition-all duration-300' : ''}`}
          initial={{ width: 0 }}
          animate={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
        />
      </div>
      {showPercentage && (
        <div className="mt-1 text-sm text-neutral-600 dark:text-neutral-400 text-right">
          {Math.round(progress)}%
        </div>
      )}
    </div>
  );
};

// Skeleton Components for Travel App
export const TripCardSkeleton: React.FC = () => {
  return (
    <div className="bg-white dark:bg-neutral-800 rounded-lg border border-neutral-200 dark:border-neutral-700 p-4 space-y-4">
      {/* Image skeleton */}
      <div className="w-full h-48 bg-neutral-200 dark:bg-neutral-700 rounded-lg animate-pulse" />
      
      {/* Content skeleton */}
      <div className="space-y-3">
        <div className="h-6 bg-neutral-200 dark:bg-neutral-700 rounded animate-pulse" />
        <div className="flex items-center space-x-2">
          <div className="w-4 h-4 bg-neutral-200 dark:bg-neutral-700 rounded animate-pulse" />
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded flex-1 animate-pulse" />
        </div>
        <div className="flex items-center space-x-2">
          <div className="w-4 h-4 bg-neutral-200 dark:bg-neutral-700 rounded animate-pulse" />
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-1/2 animate-pulse" />
        </div>
        <div className="flex justify-between items-center">
          <div className="h-5 bg-neutral-200 dark:bg-neutral-700 rounded w-1/3 animate-pulse" />
          <div className="h-8 bg-neutral-200 dark:bg-neutral-700 rounded w-20 animate-pulse" />
        </div>
      </div>
    </div>
  );
};

export const BookingCardSkeleton: React.FC = () => {
  return (
    <div className="bg-white dark:bg-neutral-800 rounded-lg border border-neutral-200 dark:border-neutral-700 p-6">
      <div className="flex items-start justify-between mb-4">
        <div className="space-y-2 flex-1">
          <div className="h-5 bg-neutral-200 dark:bg-neutral-700 rounded w-3/4 animate-pulse" />
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-1/2 animate-pulse" />
        </div>
        <div className="h-6 bg-neutral-200 dark:bg-neutral-700 rounded w-20 animate-pulse" />
      </div>
      
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div className="space-y-2">
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-16 animate-pulse" />
          <div className="h-5 bg-neutral-200 dark:bg-neutral-700 rounded animate-pulse" />
        </div>
        <div className="space-y-2">
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-16 animate-pulse" />
          <div className="h-5 bg-neutral-200 dark:bg-neutral-700 rounded animate-pulse" />
        </div>
      </div>
      
      <div className="flex justify-between items-center">
        <div className="h-6 bg-neutral-200 dark:bg-neutral-700 rounded w-24 animate-pulse" />
        <div className="h-9 bg-neutral-200 dark:bg-neutral-700 rounded w-28 animate-pulse" />
      </div>
    </div>
  );
};

export const SearchResultSkeleton: React.FC = () => {
  return (
    <div className="bg-white dark:bg-neutral-800 rounded-lg border border-neutral-200 dark:border-neutral-700 p-4">
      <div className="flex space-x-4">
        <div className="w-24 h-24 bg-neutral-200 dark:bg-neutral-700 rounded-lg animate-pulse flex-shrink-0" />
        <div className="flex-1 space-y-3">
          <div className="h-5 bg-neutral-200 dark:bg-neutral-700 rounded w-3/4 animate-pulse" />
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-1/2 animate-pulse" />
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-1">
              <div className="w-4 h-4 bg-neutral-200 dark:bg-neutral-700 rounded animate-pulse" />
              <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-12 animate-pulse" />
            </div>
            <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-16 animate-pulse" />
          </div>
        </div>
        <div className="text-right space-y-2">
          <div className="h-6 bg-neutral-200 dark:bg-neutral-700 rounded w-20 animate-pulse" />
          <div className="h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-16 animate-pulse" />
        </div>
      </div>
    </div>
  );
};

// Loading States for Different Scenarios
interface LoadingStateProps {
  type: 'search' | 'booking' | 'payment' | 'upload' | 'sync' | 'location';
  message?: string;
  progress?: number;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  type,
  message,
  progress,
}) => {
  const getLoadingContent = () => {
    switch (type) {
      case 'search':
        return {
          icon: <LoadingSpinner size="lg" />,
          title: 'Searching for trips...',
          description: 'Finding the best travel options for you',
        };
      case 'booking':
        return {
          icon: <LoadingSpinner size="lg" />,
          title: 'Processing your booking...',
          description: 'Please wait while we confirm your reservation',
        };
      case 'payment':
        return {
          icon: <LoadingSpinner size="lg" />,
          title: 'Processing payment...',
          description: 'Securely processing your transaction',
        };
      case 'upload':
        return {
          icon: <LoadingSpinner size="lg" />,
          title: 'Uploading files...',
          description: 'Your files are being uploaded',
        };
      case 'sync':
        return {
          icon: <RefreshCw className="w-8 h-8 text-primary-500 animate-spin" />,
          title: 'Syncing data...',
          description: 'Updating your information',
        };
      case 'location':
        return {
          icon: <MapPin className="w-8 h-8 text-primary-500 animate-pulse" />,
          title: 'Getting your location...',
          description: 'Please allow location access',
        };
      default:
        return {
          icon: <LoadingSpinner size="lg" />,
          title: 'Loading...',
          description: 'Please wait',
        };
    }
  };

  const content = getLoadingContent();

  return (
    <div className="flex flex-col items-center justify-center p-8 text-center">
      <motion.div
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.3 }}
        className="mb-4"
      >
        {content.icon}
      </motion.div>
      
      <h3 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100 mb-2">
        {message || content.title}
      </h3>
      
      <p className="text-neutral-600 dark:text-neutral-400 mb-4">
        {content.description}
      </p>
      
      {progress !== undefined && (
        <div className="w-full max-w-xs">
          <ProgressBar progress={progress} showPercentage />
        </div>
      )}
    </div>
  );
};

// Optimistic UI Components
interface OptimisticBookingProps {
  booking: {
    id: string;
    title: string;
    date: string;
    status: 'pending' | 'confirmed' | 'failed';
  };
  onRetry?: () => void;
}

export const OptimisticBooking: React.FC<OptimisticBookingProps> = ({
  booking,
  onRetry,
}) => {
  const getStatusIcon = () => {
    switch (booking.status) {
      case 'pending':
        return <Clock className="w-5 h-5 text-warning-500 animate-pulse" />;
      case 'confirmed':
        return <CheckCircle className="w-5 h-5 text-success-500" />;
      case 'failed':
        return <XCircle className="w-5 h-5 text-error-500" />;
    }
  };

  const getStatusText = () => {
    switch (booking.status) {
      case 'pending':
        return 'Confirming...';
      case 'confirmed':
        return 'Confirmed';
      case 'failed':
        return 'Failed';
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0.7, scale: 0.98 }}
      animate={{ 
        opacity: booking.status === 'confirmed' ? 1 : 0.7,
        scale: booking.status === 'confirmed' ? 1 : 0.98,
      }}
      className={`
        bg-white dark:bg-neutral-800 rounded-lg border p-4
        ${booking.status === 'pending' ? 'border-warning-200 bg-warning-50 dark:bg-warning-950' : ''}
        ${booking.status === 'confirmed' ? 'border-success-200 bg-success-50 dark:bg-success-950' : ''}
        ${booking.status === 'failed' ? 'border-error-200 bg-error-50 dark:bg-error-950' : ''}
      `}
    >
      <div className="flex items-center justify-between">
        <div>
          <h4 className="font-medium text-neutral-900 dark:text-neutral-100">
            {booking.title}
          </h4>
          <p className="text-sm text-neutral-600 dark:text-neutral-400">
            {booking.date}
          </p>
        </div>
        
        <div className="flex items-center space-x-2">
          {getStatusIcon()}
          <span className={`text-sm font-medium ${
            booking.status === 'pending' ? 'text-warning-600 dark:text-warning-400' : ''
          } ${
            booking.status === 'confirmed' ? 'text-success-600 dark:text-success-400' : ''
          } ${
            booking.status === 'failed' ? 'text-error-600 dark:text-error-400' : ''
          }`}>
            {getStatusText()}
          </span>
        </div>
      </div>
      
      {booking.status === 'failed' && onRetry && (
        <motion.button
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          onClick={onRetry}
          className="mt-3 px-3 py-1 text-sm bg-error-500 text-white rounded hover:bg-error-600 transition-colors"
        >
          Retry
        </motion.button>
      )}
    </motion.div>
  );
};

// Network Status Indicator
export const NetworkStatus: React.FC = () => {
  const [isOnline, setIsOnline] = useState(true);
  const [showStatus, setShowStatus] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowStatus(true);
      setTimeout(() => setShowStatus(false), 3000);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowStatus(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return (
    <AnimatePresence>
      {showStatus && (
        <motion.div
          initial={{ opacity: 0, y: -50 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -50 }}
          className={`
            fixed top-4 right-4 z-50 px-4 py-2 rounded-lg shadow-lg flex items-center space-x-2
            ${isOnline 
              ? 'bg-success-500 text-white' 
              : 'bg-error-500 text-white'
            }
          `}
        >
          {isOnline ? (
            <Wifi className="w-4 h-4" />
          ) : (
            <WifiOff className="w-4 h-4" />
          )}
          <span className="text-sm font-medium">
            {isOnline ? 'Back online' : 'No internet connection'}
          </span>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Loading Overlay
interface LoadingOverlayProps {
  isVisible: boolean;
  message?: string;
  type?: 'spinner' | 'pulse' | 'progress';
  progress?: number;
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  isVisible,
  message = 'Loading...',
  type = 'spinner',
  progress,
}) => {
  const renderLoader = () => {
    switch (type) {
      case 'pulse':
        return <PulseLoader size="lg" />;
      case 'progress':
        return (
          <div className="w-64">
            <ProgressBar progress={progress || 0} showPercentage />
          </div>
        );
      default:
        return <LoadingSpinner size="xl" variant="white" />;
    }
  };

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
        >
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.8, opacity: 0 }}
            className="bg-white dark:bg-neutral-800 rounded-lg p-8 text-center shadow-xl"
          >
            <div className="mb-4 flex justify-center">
              {renderLoader()}
            </div>
            <p className="text-neutral-900 dark:text-neutral-100 font-medium">
              {message}
            </p>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Infinite Scroll Loading
export const InfiniteScrollLoader: React.FC<{ isLoading: boolean }> = ({ isLoading }) => {
  return (
    <AnimatePresence>
      {isLoading && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 20 }}
          className="flex justify-center py-8"
        >
          <div className="flex items-center space-x-2 text-neutral-600 dark:text-neutral-400">
            <LoadingSpinner size="sm" variant="muted" />
            <span className="text-sm">Loading more...</span>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Button Loading State
interface LoadingButtonProps {
  isLoading: boolean;
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const LoadingButton: React.FC<LoadingButtonProps> = ({
  isLoading,
  children,
  onClick,
  disabled = false,
  variant = 'primary',
  size = 'md',
  className = '',
}) => {
  const baseClasses = 'inline-flex items-center justify-center font-medium rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2';
  
  const variantClasses = {
    primary: 'bg-primary-500 text-white hover:bg-primary-600 focus:ring-primary-500 disabled:bg-neutral-300',
    secondary: 'bg-neutral-200 text-neutral-900 hover:bg-neutral-300 focus:ring-neutral-500 disabled:bg-neutral-100',
    outline: 'border-2 border-primary-500 text-primary-500 hover:bg-primary-50 focus:ring-primary-500 disabled:border-neutral-300 disabled:text-neutral-300',
  };
  
  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-base',
    lg: 'px-6 py-3 text-lg',
  };

  return (
    <motion.button
      onClick={onClick}
      disabled={disabled || isLoading}
      whileHover={!disabled && !isLoading ? { scale: 1.02 } : {}}
      whileTap={!disabled && !isLoading ? { scale: 0.98 } : {}}
      className={`
        ${baseClasses}
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${disabled || isLoading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}
        ${className}
      `}
    >
      <AnimatePresence mode="wait">
        {isLoading ? (
          <motion.div
            key="loading"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex items-center space-x-2"
          >
            <LoadingSpinner size="sm" variant={variant === 'primary' ? 'white' : 'primary'} />
            <span>Loading...</span>
          </motion.div>
        ) : (
          <motion.div
            key="content"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            {children}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.button>
  );
};
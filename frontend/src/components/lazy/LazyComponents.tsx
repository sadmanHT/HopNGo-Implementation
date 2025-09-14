'use client';

import React, { Suspense } from 'react';
import { createLazyComponent } from '@/lib/performance';

// Loading fallback components
const LoadingSpinner = () => (
  <div className="flex items-center justify-center p-8">
    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
  </div>
);

const LoadingSkeleton = ({ className }: { className?: string }) => (
  <div className={`animate-pulse bg-gray-200 rounded ${className || 'h-32 w-full'}`} />
);

const LoadingCard = () => (
  <div className="border rounded-lg p-4 space-y-3">
    <LoadingSkeleton className="h-4 w-3/4" />
    <LoadingSkeleton className="h-3 w-1/2" />
    <LoadingSkeleton className="h-20 w-full" />
  </div>
);

// Lazy-loaded components with proper fallbacks
// Note: Components will be added here as they are implemented

// Wrapper components with custom loading states
// Note: Wrapper components will be added here as lazy components are implemented

// Dynamic import helper for route-based code splitting
export const createLazyPage = (importPath: string) => {
  return React.lazy(() => import(importPath));
};

// Preload helper for critical components
export const preloadComponent = (importFunc: () => Promise<any>) => {
  if (typeof window !== 'undefined') {
    // Preload on idle or after a delay
    if ('requestIdleCallback' in window) {
      requestIdleCallback(() => importFunc());
    } else {
      setTimeout(() => importFunc(), 100);
    }
  }
};

// Usage example for preloading
export const preloadCriticalComponents = () => {
  // Note: Preload functions will be added here as components are implemented
};
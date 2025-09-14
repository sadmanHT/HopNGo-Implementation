import React from 'react';

// Lazy loading helper for components
export const createLazyComponent = function<T extends React.ComponentType<any>>(
  importFunc: () => Promise<{ default: T }>
) {
  const LazyComponent = React.lazy(importFunc);
  
  return React.forwardRef<React.ElementRef<T>, React.ComponentProps<T>>((props, ref) => (
    <React.Suspense fallback={<div className="animate-pulse bg-gray-200 rounded h-8 w-full" />}>
      <LazyComponent {...(props as any)} ref={ref} />
    </React.Suspense>
  ));
};

// Simple performance tracking
export const trackWebVitals = () => {
  // Simplified version without external dependencies
  if (typeof window !== 'undefined') {
    console.log('Performance tracking initialized');
  }
};
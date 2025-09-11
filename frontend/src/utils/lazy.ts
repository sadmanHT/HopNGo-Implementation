import React from 'react';
import { lazy, ComponentType, Suspense } from 'react';
import { LoadingSpinner } from '@/components/ui/loading-spinner';

// Utility for creating lazy-loaded components with loading fallback
export function createLazyComponent<T extends ComponentType<any>>(
  importFunc: () => Promise<{ default: T }>,
  fallback: ComponentType = LoadingSpinner
) {
  const LazyComponent = lazy(importFunc);
  
  const withSuspense = (props: any) => {
    const FallbackComponent = fallback;
    return React.createElement(
      React.Suspense,
      { fallback: React.createElement(FallbackComponent, null) },
      React.createElement(LazyComponent, props)
    );
  };

  return {
    Component: LazyComponent,
    withSuspense,
    preload: importFunc,
  };
}

// Pre-configured lazy components for common heavy routes
export const LazyComponents = {
  // Booking related components
  BookingSearchExperiment: createLazyComponent(
    () => import('@/components/bookings/BookingSearchExperiment').then(module => ({ default: module.BookingSearchExperiment }))
  ),
  
  // UI components
  LoadingSpinner: createLazyComponent(
    () => import('@/components/ui/loading-spinner').then(module => ({ default: module.LoadingSpinner }))
  ),
};

// Preload function for critical routes
export function preloadComponent(componentName: keyof typeof LazyComponents) {
  const component = LazyComponents[componentName];
  if (component) {
    // Trigger the import to start loading
    component.preload();
  }
}

// Preload multiple components
export function preloadComponents(componentNames: (keyof typeof LazyComponents)[]) {
  componentNames.forEach(preloadComponent);
}
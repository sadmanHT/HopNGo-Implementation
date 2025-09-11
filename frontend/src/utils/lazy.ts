import { lazy, ComponentType, Suspense } from 'react';
import { LoadingSpinner } from '@/components/ui/loading-spinner';

// Utility for creating lazy-loaded components with loading fallback
export function createLazyComponent<T extends ComponentType<any>>(
  importFunc: () => Promise<{ default: T }>,
  fallback: ComponentType = LoadingSpinner
) {
  const LazyComponent = lazy(importFunc);
  
  return {
    Component: LazyComponent,
    withSuspense: (props: any) => (
      <Suspense fallback={<fallback />}>
        <LazyComponent {...props} />
      </Suspense>
    ),
  };
}

// Pre-configured lazy components for common heavy routes
export const LazyComponents = {
  // Booking related components
  BookingForm: createLazyComponent(
    () => import('@/components/booking/BookingForm')
  ),
  BookingHistory: createLazyComponent(
    () => import('@/components/booking/BookingHistory')
  ),
  
  // Social feed components
  SocialFeed: createLazyComponent(
    () => import('@/components/social/SocialFeed')
  ),
  PostEditor: createLazyComponent(
    () => import('@/components/social/PostEditor')
  ),
  
  // Map and location components
  MapView: createLazyComponent(
    () => import('@/components/map/MapView')
  ),
  LocationPicker: createLazyComponent(
    () => import('@/components/map/LocationPicker')
  ),
  
  // Chat components
  ChatInterface: createLazyComponent(
    () => import('@/components/chat/ChatInterface')
  ),
  
  // Admin dashboard
  AdminDashboard: createLazyComponent(
    () => import('@/components/admin/AdminDashboard')
  ),
};

// Preload function for critical routes
export function preloadComponent(componentName: keyof typeof LazyComponents) {
  const component = LazyComponents[componentName];
  if (component) {
    // Trigger the import to start loading
    component.Component.preload?.();
  }
}

// Preload multiple components
export function preloadComponents(componentNames: (keyof typeof LazyComponents)[]) {
  componentNames.forEach(preloadComponent);
}
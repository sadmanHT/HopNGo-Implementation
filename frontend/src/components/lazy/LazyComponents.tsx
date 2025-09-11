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
export const LazyItineraryPlanner = createLazyComponent(
  () => import('@/components/itinerary/ItineraryPlanner')
);

export const LazyBookingSystem = createLazyComponent(
  () => import('@/components/booking/BookingSystem')
);

export const LazyTicketManager = createLazyComponent(
  () => import('@/components/tickets/TicketManager')
);

export const LazyAnalyticsDashboard = createLazyComponent(
  () => import('@/components/analytics/AnalyticsDashboard')
);

export const LazyMapComponent = createLazyComponent(
  () => import('@/components/map/MapComponent')
);

export const LazyPaymentForm = createLazyComponent(
  () => import('@/components/payment/PaymentForm')
);

// Wrapper components with custom loading states
export function LazyItineraryPlannerWithFallback(props: any) {
  return (
    <Suspense fallback={
      <div className="space-y-4">
        <LoadingSkeleton className="h-8 w-64" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <LoadingCard />
          <LoadingCard />
        </div>
      </div>
    }>
      <LazyItineraryPlanner {...props} />
    </Suspense>
  );
}

export function LazyBookingSystemWithFallback(props: any) {
  return (
    <Suspense fallback={
      <div className="space-y-6">
        <LoadingSkeleton className="h-10 w-48" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <LoadingCard />
          </div>
          <div>
            <LoadingSkeleton className="h-64 w-full" />
          </div>
        </div>
      </div>
    }>
      <LazyBookingSystem {...props} />
    </Suspense>
  );
}

export function LazyMapComponentWithFallback(props: any) {
  return (
    <Suspense fallback={
      <div className="relative">
        <LoadingSkeleton className="h-96 w-full" />
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="bg-white rounded-lg p-4 shadow-lg">
            <LoadingSpinner />
            <p className="text-sm text-gray-600 mt-2">Loading map...</p>
          </div>
        </div>
      </div>
    }>
      <LazyMapComponent {...props} />
    </Suspense>
  );
}

export function LazyAnalyticsDashboardWithFallback(props: any) {
  return (
    <Suspense fallback={
      <div className="space-y-6">
        <LoadingSkeleton className="h-8 w-56" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <LoadingSkeleton key={i} className="h-24 w-full" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <LoadingSkeleton className="h-64 w-full" />
          <LoadingSkeleton className="h-64 w-full" />
        </div>
      </div>
    }>
      <LazyAnalyticsDashboard {...props} />
    </Suspense>
  );
}

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
  preloadComponent(() => import('@/components/itinerary/ItineraryPlanner'));
  preloadComponent(() => import('@/components/booking/BookingSystem'));
};
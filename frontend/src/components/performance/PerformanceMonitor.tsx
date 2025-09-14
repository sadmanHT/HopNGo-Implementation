'use client';

import { useEffect } from 'react';
import { trackWebVitals } from '@/lib/performance';

interface PerformanceMonitorProps {
  enableWebVitals?: boolean;
  enableBudgetCheck?: boolean;
  enableResourceHints?: boolean;
}

export function PerformanceMonitor({
  enableWebVitals = true,
  enableBudgetCheck = true,
  enableResourceHints = true,
}: PerformanceMonitorProps) {
  useEffect(() => {
    if (enableWebVitals) {
      trackWebVitals();
    }
    
    if (enableBudgetCheck) {
      // Simplified performance check
      const timer = setTimeout(() => {
        console.log('Performance budget check - simplified version');
      }, 1000);
      
      return () => clearTimeout(timer);
    }
  }, [enableWebVitals, enableBudgetCheck]);
  
  useEffect(() => {
    if (enableResourceHints) {
      // Simplified resource hints
      console.log('Resource hints initialized');
    }
  }, [enableResourceHints]);
  
  // This component doesn't render anything
  return null;
}

// HOC for performance monitoring
export function withPerformanceMonitoring<P extends object>(
  Component: React.ComponentType<P>
) {
  return function PerformanceWrappedComponent(props: P) {
    return (
      <>
        <PerformanceMonitor />
        <Component {...props} />
      </>
    );
  };
}

// Performance metrics display component (for development)
export function PerformanceMetrics() {
  useEffect(() => {
    const displayMetrics = () => {
      if (typeof window !== 'undefined' && 'performance' in window) {
        const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        
        const metrics = {
          'DOM Content Loaded': `${Math.round(navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart)}ms`,
          'Load Complete': `${Math.round(navigation.loadEventEnd - navigation.loadEventStart)}ms`,
          'First Paint': `${Math.round(performance.getEntriesByName('first-paint')[0]?.startTime || 0)}ms`,
          'First Contentful Paint': `${Math.round(performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0)}ms`,
        };
        
        console.table(metrics);
      }
    };
    
    // Display metrics after page load
    const timer = setTimeout(displayMetrics, 2000);
    return () => clearTimeout(timer);
  }, []);
  
  if (process.env.NODE_ENV !== 'development') {
    return null;
  }
  
  return (
    <div className="fixed bottom-4 right-4 bg-black text-white p-2 rounded text-xs font-mono z-50">
      Performance Monitor Active
    </div>
  );
}
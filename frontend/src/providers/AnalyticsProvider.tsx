'use client';

import { useEffect } from 'react';
import { usePathname } from 'next/navigation';
import analytics from '@/lib/analytics';

interface AnalyticsProviderProps {
  children: React.ReactNode;
}

export function AnalyticsProvider({ children }: AnalyticsProviderProps) {
  const pathname = usePathname();

  useEffect(() => {
    // Track page views on route changes
    analytics.trackPageView(pathname || undefined);
  }, [pathname]);

  useEffect(() => {
    // Track app initialization
    analytics.track('app_initialized', {
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString(),
      pathname
    });

    // Cleanup on unmount
    return () => {
      analytics.destroy();
    };
  }, []);

  return <>{children}</>;
}
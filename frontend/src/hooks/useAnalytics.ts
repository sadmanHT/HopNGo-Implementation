import { useCallback } from 'react';
import analytics from '@/lib/analytics';
import { useAuthStore } from '@/lib/state';

/**
 * Custom hook for analytics tracking with user context
 */
export function useAnalytics() {
  const { user } = useAuthStore();

  const trackEvent = useCallback((eventType: string, properties?: Record<string, any>, metadata?: Record<string, any>) => {
    const enrichedProperties = {
      ...properties,
      userId: user?.id,
      timestamp: new Date().toISOString()
    };
    
    analytics.track(eventType, enrichedProperties, metadata);
  }, [user?.id]);

  const trackPageView = useCallback((path?: string, title?: string) => {
    analytics.trackPageView(path, title);
  }, []);

  const trackAction = useCallback((action: string, target?: string, properties?: Record<string, any>) => {
    const enrichedProperties = {
      ...properties,
      userId: user?.id
    };
    
    analytics.trackAction(action, target, enrichedProperties);
  }, [user?.id]);

  const trackConversion = useCallback((conversionType: string, value?: number, properties?: Record<string, any>) => {
    const enrichedProperties = {
      ...properties,
      userId: user?.id
    };
    
    analytics.trackConversion(conversionType, value, enrichedProperties);
  }, [user?.id]);

  const trackError = useCallback((error: Error | string, context?: Record<string, any>) => {
    const enrichedContext = {
      ...context,
      userId: user?.id
    };
    
    analytics.trackError(error, enrichedContext);
  }, [user?.id]);

  const trackButtonClick = useCallback((buttonName: string, location?: string, properties?: Record<string, any>) => {
    trackAction('button_click', buttonName, {
      location,
      ...properties
    });
  }, [trackAction]);

  const trackFormSubmit = useCallback((formName: string, success: boolean, properties?: Record<string, any>) => {
    trackAction('form_submit', formName, {
      success,
      ...properties
    });
  }, [trackAction]);

  const trackNavigation = useCallback((from: string, to: string, method?: string) => {
    trackAction('navigation', 'route_change', {
      from,
      to,
      method: method || 'unknown'
    });
  }, [trackAction]);

  return {
    trackEvent,
    trackPageView,
    trackAction,
    trackConversion,
    trackError,
    trackButtonClick,
    trackFormSubmit,
    trackNavigation,
    // Direct access to analytics instance if needed
    analytics
  };
}

export default useAnalytics;
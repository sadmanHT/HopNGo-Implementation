'use client';

import React from 'react';
import { AlertTriangle, RefreshCw, Home, MessageCircle, Wifi, CreditCard, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useRouter } from 'next/navigation';
import { SentryService } from '@/lib/sentry';

interface FallbackUIProps {
  error?: Error;
  resetError?: () => void;
  feature?: 'payment' | 'search' | 'booking' | 'map' | 'general';
  title?: string;
  description?: string;
  showRetry?: boolean;
  showHome?: boolean;
  showSupport?: boolean;
  className?: string;
}

/**
 * Fallback UI component for critical feature errors
 */
export function FallbackUI({
  error,
  resetError,
  feature = 'general',
  title,
  description,
  showRetry = true,
  showHome = true,
  showSupport = true,
  className = '',
}: FallbackUIProps) {
  const router = useRouter();

  const handleRetry = () => {
    SentryService.addBreadcrumb(`User retried ${feature} feature`, 'user');
    resetError?.();
  };

  const handleGoHome = () => {
    SentryService.addBreadcrumb(`User navigated home from ${feature} error`, 'navigation');
    router.push('/');
  };

  const handleContactSupport = () => {
    SentryService.addBreadcrumb(`User contacted support from ${feature} error`, 'user');
    // You can integrate with your support system here
    window.open('mailto:support@hopngo.com?subject=Technical Issue', '_blank');
  };

  const getFeatureConfig = () => {
    switch (feature) {
      case 'payment':
        return {
          icon: <CreditCard className="h-12 w-12 text-red-500" />,
          title: title || 'Payment System Unavailable',
          description: description || 'We\'re experiencing issues with our payment system. Your booking is saved and you can complete payment later.',
          color: 'red',
          suggestions: [
            'Try refreshing the page',
            'Check your internet connection',
            'Try a different payment method',
            'Contact support if the issue persists',
          ],
        };
      
      case 'search':
        return {
          icon: <Search className="h-12 w-12 text-orange-500" />,
          title: title || 'Search Temporarily Unavailable',
          description: description || 'Our search service is currently experiencing issues. You can browse popular destinations or try again later.',
          color: 'orange',
          suggestions: [
            'Browse popular destinations',
            'Try a simpler search term',
            'Check your spelling',
            'Refresh the page and try again',
          ],
        };
      
      case 'booking':
        return {
          icon: <AlertTriangle className="h-12 w-12 text-yellow-500" />,
          title: title || 'Booking System Issue',
          description: description || 'We\'re having trouble processing your booking. Please try again or contact support.',
          color: 'yellow',
          suggestions: [
            'Try refreshing the page',
            'Check your booking details',
            'Ensure all required fields are filled',
            'Contact support for assistance',
          ],
        };
      
      case 'map':
        return {
          icon: <Wifi className="h-12 w-12 text-blue-500" />,
          title: title || 'Map Service Unavailable',
          description: description || 'We\'re unable to load the map right now. You can still view trip details and make bookings.',
          color: 'blue',
          suggestions: [
            'Check your internet connection',
            'Enable location services',
            'Try refreshing the page',
            'View trip details without map',
          ],
        };
      
      default:
        return {
          icon: <AlertTriangle className="h-12 w-12 text-gray-500" />,
          title: title || 'Something Went Wrong',
          description: description || 'We encountered an unexpected error. Please try again or contact support.',
          color: 'gray',
          suggestions: [
            'Refresh the page',
            'Check your internet connection',
            'Try again in a few minutes',
            'Contact support if the issue persists',
          ],
        };
    }
  };

  const config = getFeatureConfig();

  return (
    <div className={`flex items-center justify-center min-h-[400px] p-4 ${className}`}>
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="flex justify-center mb-4">
            {config.icon}
          </div>
          <CardTitle className="text-xl font-semibold">
            {config.title}
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground">
            {config.description}
          </CardDescription>
        </CardHeader>
        
        <CardContent className="space-y-4">
          {/* Error details for development */}
          {error && process.env.NODE_ENV === 'development' && (
            <Alert>
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription className="text-xs font-mono">
                {error.message}
              </AlertDescription>
            </Alert>
          )}

          {/* Suggestions */}
          <div className="space-y-2">
            <h4 className="text-sm font-medium">What you can try:</h4>
            <ul className="text-sm text-muted-foreground space-y-1">
              {config.suggestions.map((suggestion, index) => (
                <li key={index} className="flex items-start">
                  <span className="mr-2">•</span>
                  {suggestion}
                </li>
              ))}
            </ul>
          </div>

          {/* Action buttons */}
          <div className="flex flex-col gap-2 pt-4">
            {showRetry && resetError && (
              <Button 
                onClick={handleRetry}
                className="w-full"
                variant="default"
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Try Again
              </Button>
            )}
            
            {showHome && (
              <Button 
                onClick={handleGoHome}
                variant="outline"
                className="w-full"
              >
                <Home className="h-4 w-4 mr-2" />
                Go to Homepage
              </Button>
            )}
            
            {showSupport && (
              <Button 
                onClick={handleContactSupport}
                variant="ghost"
                className="w-full"
              >
                <MessageCircle className="h-4 w-4 mr-2" />
                Contact Support
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * Specific fallback components for critical features
 */
export function PaymentFallback({ error, resetError }: { error?: Error; resetError?: () => void }) {
  return (
    <FallbackUI
      error={error}
      resetError={resetError}
      feature="payment"
      showSupport={true}
    />
  );
}

export function SearchFallback({ error, resetError }: { error?: Error; resetError?: () => void }) {
  return (
    <FallbackUI
      error={error}
      resetError={resetError}
      feature="search"
      showRetry={true}
      showHome={false}
    />
  );
}

export function BookingFallback({ error, resetError }: { error?: Error; resetError?: () => void }) {
  return (
    <FallbackUI
      error={error}
      resetError={resetError}
      feature="booking"
      showSupport={true}
    />
  );
}

export function MapFallback({ error, resetError }: { error?: Error; resetError?: () => void }) {
  return (
    <FallbackUI
      error={error}
      resetError={resetError}
      feature="map"
      showRetry={true}
      showHome={false}
      showSupport={false}
    />
  );
}

/**
 * Higher-order component for wrapping components with fallback UI
 */
export function withFallbackUI<P extends object>(
  Component: React.ComponentType<P>,
  feature: FallbackUIProps['feature'] = 'general'
) {
  return function WrappedComponent(props: P) {
    return (
      <ErrorBoundary
        fallback={(error, resetError) => (
          <FallbackUI
            error={error}
            resetError={resetError}
            feature={feature}
          />
        )}
      >
        <Component {...props} />
      </ErrorBoundary>
    );
  };
}

/**
 * Simple error boundary for the fallback UI
 */
interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

class ErrorBoundary extends React.Component<
  {
    children: React.ReactNode;
    fallback: (error?: Error, resetError?: () => void) => React.ReactNode;
  },
  ErrorBoundaryState
> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    SentryService.captureException(error, {
      tags: { component: 'FallbackUI' },
      extra: { errorInfo },
    });
  }

  resetError = () => {
    this.setState({ hasError: false, error: undefined });
  };

  render() {
    if (this.state.hasError) {
      return this.props.fallback(this.state.error, this.resetError);
    }

    return this.props.children;
  }
}
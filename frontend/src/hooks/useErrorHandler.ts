import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { SentryService } from '@/lib/sentry';
import { toast } from 'sonner';

interface ErrorHandlerOptions {
  showToast?: boolean;
  redirectOnError?: string;
  logToConsole?: boolean;
  captureToSentry?: boolean;
  customErrorMessages?: Record<string, string>;
}

interface ErrorContext {
  component?: string;
  action?: string;
  userId?: string;
  additionalData?: Record<string, any>;
}

/**
 * Hook for handling errors with Sentry integration and user feedback
 */
export function useErrorHandler(options: ErrorHandlerOptions = {}) {
  const router = useRouter();
  const [isClient, setIsClient] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);
  
  const {
    showToast = true,
    redirectOnError,
    logToConsole = true,
    captureToSentry = true,
    customErrorMessages = {},
  } = options;

  // Handle different types of errors
  const handleError = useCallback(
    (error: Error | string, context?: ErrorContext) => {
      const errorObj = typeof error === 'string' ? new Error(error) : error;
      const errorMessage = errorObj.message || 'An unexpected error occurred';

      // Log to console if enabled
      if (logToConsole) {
        console.error('Error handled:', errorObj, context);
      }

      // Capture to Sentry if enabled
      if (captureToSentry) {
        SentryService.captureException(errorObj, {
          tags: {
            errorHandler: 'useErrorHandler',
            component: context?.component || 'unknown',
            action: context?.action || 'unknown',
          },
          extra: {
            context,
            userAgent: navigator.userAgent,
            url: window.location.href,
          },
        });
      }

      // Show user-friendly toast notification
      if (showToast) {
        const userMessage = customErrorMessages[errorMessage] || 
                           getDefaultErrorMessage(errorObj);
        
        toast.error(userMessage, {
          duration: 5000,
          action: {
            label: 'Dismiss',
            onClick: () => {},
          },
        });
      }

      // Redirect if specified
      if (isClient && redirectOnError) {
        setTimeout(() => {
          router.push(redirectOnError);
        }, 2000);
      }
    },
    [showToast, redirectOnError, logToConsole, captureToSentry, customErrorMessages, router]
  );

  // Handle API errors specifically
  const handleApiError = useCallback(
    (error: any, context?: ErrorContext & { endpoint?: string; method?: string }) => {
      let errorMessage = 'Network error occurred';
      let errorCode = 'UNKNOWN_ERROR';

      if (error.response) {
        // Server responded with error status
        const status = error.response.status;
        const data = error.response.data;

        errorCode = data?.code || `HTTP_${status}`;
        errorMessage = data?.message || getHttpErrorMessage(status);

        // Handle specific HTTP status codes
        switch (status) {
          case 401:
            // Unauthorized - redirect to login
            SentryService.addBreadcrumb('User unauthorized, redirecting to login', 'auth');
            if (isClient) {
              router.push('/login');
            }
            return;
          
          case 403:
            // Forbidden
            errorMessage = 'You do not have permission to perform this action';
            break;
          
          case 404:
            // Not found
            errorMessage = 'The requested resource was not found';
            break;
          
          case 429:
            // Rate limited
            errorMessage = 'Too many requests. Please try again later';
            break;
          
          case 500:
          case 502:
          case 503:
          case 504:
            // Server errors
            errorMessage = 'Server is temporarily unavailable. Please try again later';
            break;
        }
      } else if (error.request) {
        // Network error
        errorMessage = 'Unable to connect to server. Please check your internet connection';
        errorCode = 'NETWORK_ERROR';
      }

      // Create enhanced error object
      const enhancedError = new Error(errorMessage);
      enhancedError.name = errorCode;

      // Capture API error with additional context
      if (captureToSentry) {
        SentryService.captureApiError(enhancedError, {
          url: context?.endpoint || error.config?.url || 'unknown',
          method: context?.method || error.config?.method || 'unknown',
          status: error.response?.status,
          requestData: error.config?.data,
          responseData: error.response?.data,
        });
      }

      handleError(enhancedError, context);
    },
    [handleError, captureToSentry, router]
  );

  // Handle form validation errors
  const handleFormError = useCallback(
    (error: any, formName: string, formData?: any) => {
      if (captureToSentry) {
        SentryService.captureFormError(error, formName, formData);
      }

      handleError(error, {
        component: 'form',
        action: 'validation',
        additionalData: { formName },
      });
    },
    [handleError, captureToSentry]
  );

  // Handle navigation errors
  const handleNavigationError = useCallback(
    (error: Error, route: string, params?: any) => {
      if (captureToSentry) {
        SentryService.captureNavigationError(error, route, params);
      }

      handleError(error, {
        component: 'navigation',
        action: 'route_change',
        additionalData: { route, params },
      });
    },
    [handleError, captureToSentry]
  );

  // Handle async operation errors
  const handleAsyncError = useCallback(
    async <T>(operation: () => Promise<T>, context?: ErrorContext): Promise<T | null> => {
      try {
        return await operation();
      } catch (error) {
        handleError(error as Error, context);
        return null;
      }
    },
    [handleError]
  );

  // Set up global error handlers
  useEffect(() => {
    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      handleError(
        new Error(`Unhandled promise rejection: ${event.reason}`),
        { component: 'global', action: 'unhandled_rejection' }
      );
    };

    const handleGlobalError = (event: ErrorEvent) => {
      handleError(
        new Error(`Global error: ${event.message}`),
        { 
          component: 'global', 
          action: 'global_error',
          additionalData: {
            filename: event.filename,
            lineno: event.lineno,
            colno: event.colno,
          }
        }
      );
    };

    window.addEventListener('unhandledrejection', handleUnhandledRejection);
    window.addEventListener('error', handleGlobalError);

    return () => {
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      window.removeEventListener('error', handleGlobalError);
    };
  }, [handleError]);

  return {
    handleError,
    handleApiError,
    handleFormError,
    handleNavigationError,
    handleAsyncError,
  };
}

/**
 * Get user-friendly error message based on error type
 */
function getDefaultErrorMessage(error: Error): string {
  const message = error.message.toLowerCase();

  if (message.includes('network') || message.includes('fetch')) {
    return 'Connection problem. Please check your internet and try again.';
  }

  if (message.includes('timeout')) {
    return 'Request timed out. Please try again.';
  }

  if (message.includes('validation') || message.includes('invalid')) {
    return 'Please check your input and try again.';
  }

  if (message.includes('permission') || message.includes('unauthorized')) {
    return 'You do not have permission to perform this action.';
  }

  return 'Something went wrong. Please try again or contact support if the problem persists.';
}

/**
 * Get user-friendly message for HTTP status codes
 */
function getHttpErrorMessage(status: number): string {
  switch (status) {
    case 400:
      return 'Invalid request. Please check your input.';
    case 401:
      return 'Please log in to continue.';
    case 403:
      return 'You do not have permission to access this resource.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return 'This action conflicts with existing data.';
    case 422:
      return 'Please check your input and try again.';
    case 429:
      return 'Too many requests. Please wait a moment and try again.';
    case 500:
      return 'Server error. Please try again later.';
    case 502:
    case 503:
    case 504:
      return 'Service temporarily unavailable. Please try again later.';
    default:
      return 'An unexpected error occurred. Please try again.';
  }
}
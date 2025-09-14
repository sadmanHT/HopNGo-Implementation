import * as Sentry from '@sentry/nextjs';
import { User } from '@sentry/types';
import { RateLimitedLogger } from './error-rate-limiter';

/**
 * Sentry utility service for error monitoring and user context management
 */
export class SentryService {
  /**
   * Set user context for error tracking
   */
  static setUser(user: {
    id?: string;
    email?: string;
    username?: string;
    [key: string]: any;
  }) {
    const sentryUser: User = {
      id: user.id ? this.hashUserId(user.id) : undefined,
      email: user.email ? this.hashEmail(user.email) : undefined,
      username: user.username,
    };

    // Add non-PII user data
    const userData: Record<string, any> = {};
    Object.keys(user).forEach(key => {
      if (!['id', 'email', 'password', 'token'].includes(key)) {
        userData[key] = user[key];
      }
    });

    if (Object.keys(userData).length > 0) {
      sentryUser.data = userData;
    }

    Sentry.setUser(sentryUser);
  }

  /**
   * Clear user context
   */
  static clearUser() {
    Sentry.setUser(null);
  }

  /**
   * Set custom tags for error context
   */
  static setTags(tags: Record<string, string>) {
    Object.entries(tags).forEach(([key, value]) => {
      Sentry.setTag(key, value);
    });
  }

  /**
   * Set custom context data
   */
  static setContext(key: string, context: Record<string, any>) {
    Sentry.setContext(key, context);
  }

  /**
   * Add breadcrumb for debugging
   */
  static addBreadcrumb(message: string, category?: string, level?: Sentry.SeverityLevel, data?: any) {
    Sentry.addBreadcrumb({
      message,
      category: category || 'custom',
      level: level || 'info',
      data,
      timestamp: Date.now() / 1000,
    });
  }

  /**
   * Capture exception with additional context and rate limiting
   */
  static captureException(error: Error, context?: {
    tags?: Record<string, string>;
    extra?: Record<string, any>;
    level?: Sentry.SeverityLevel;
    fingerprint?: string[];
    skipRateLimit?: boolean;
  }) {
    // Check rate limiting unless explicitly skipped
    if (!context?.skipRateLimit) {
      const rateLimitContext = {
        component: context?.tags?.component,
        action: context?.tags?.action,
        url: typeof window !== 'undefined' ? window.location.href : undefined,
      };
      
      if (!RateLimitedLogger.logError(error, rateLimitContext, 'general')) {
        // Rate limited - add breadcrumb instead
        this.addBreadcrumb(
          `Rate limited error: ${error.message}`,
          'error',
          'info',
          { rateLimited: true, errorName: error.name }
        );
        return;
      }
    }

    return Sentry.captureException(error, {
      tags: context?.tags,
      extra: context?.extra,
      level: context?.level || 'error',
      fingerprint: context?.fingerprint,
    });
  }

  /**
   * Capture message with context and rate limiting
   */
  static captureMessage(message: string, level: Sentry.SeverityLevel = 'info', context?: {
    tags?: Record<string, string>;
    extra?: Record<string, any>;
    skipRateLimit?: boolean;
  }) {
    // Apply rate limiting for error and warning messages
    if ((level === 'error' || level === 'warning') && !context?.skipRateLimit) {
      const rateLimitContext = {
        component: context?.tags?.component,
        action: context?.tags?.action,
        url: typeof window !== 'undefined' ? window.location.href : undefined,
      };
      
      if (!RateLimitedLogger.logError(message, rateLimitContext, 'general')) {
        // Rate limited - skip logging
        return;
      }
    }

    return Sentry.captureMessage(message, {
      level,
      tags: context?.tags,
      extra: context?.extra,
    });
  }

  /**
   * Capture API error with request/response context and rate limiting
   */
  static captureApiError(error: Error, requestContext: {
    url: string;
    method: string;
    status?: number;
    requestData?: any;
    responseData?: any;
  }) {
    // Check rate limiting for API errors
    const rateLimitContext = {
      url: requestContext.url,
      method: requestContext.method,
      status: requestContext.status,
    };
    
    if (!RateLimitedLogger.logError(error, rateLimitContext, 'api')) {
      // Rate limited - add breadcrumb instead
      this.addBreadcrumb(
        `Rate limited API error: ${requestContext.method} ${requestContext.url} - ${error.message}`,
        'http',
        'info',
        { 
          rateLimited: true, 
          status: requestContext.status,
          method: requestContext.method 
        }
      );
      return;
    }

    const sanitizedContext = {
      ...requestContext,
      url: this.sanitizeUrl(requestContext.url),
      requestData: this.sanitizeData(requestContext.requestData),
      responseData: this.sanitizeData(requestContext.responseData),
    };

    return this.captureException(error, {
      tags: {
        errorType: 'api_error',
        httpMethod: requestContext.method,
        httpStatus: requestContext.status?.toString() || 'unknown',
      },
      extra: {
        apiRequest: sanitizedContext,
      },
      fingerprint: [
        'api_error',
        requestContext.method,
        requestContext.url.split('?')[0], // Remove query params for fingerprinting
        requestContext.status?.toString() || 'unknown',
      ],
      skipRateLimit: true, // Already checked above
    });
  }

  /**
   * Capture navigation error
   */
  static captureNavigationError(error: Error, route: string, params?: any) {
    return this.captureException(error, {
      tags: {
        errorType: 'navigation_error',
        route,
      },
      extra: {
        navigationParams: this.sanitizeData(params),
      },
      fingerprint: ['navigation_error', route],
    });
  }

  /**
   * Capture form validation error
   */
  static captureFormError(error: Error, formName: string, formData?: any) {
    return this.captureException(error, {
      tags: {
        errorType: 'form_error',
        formName,
      },
      extra: {
        formData: this.sanitizeData(formData),
      },
      fingerprint: ['form_error', formName],
    });
  }

  /**
   * Start a transaction for performance monitoring
   */
  static startTransaction(name: string, op: string, description?: string) {
    return Sentry.startTransaction({
      name,
      op,
      description,
    });
  }

  /**
   * Measure function execution time
   */
  static async measureAsync<T>(
    name: string,
    fn: () => Promise<T>,
    op: string = 'function'
  ): Promise<T> {
    const transaction = this.startTransaction(name, op);
    
    try {
      const result = await fn();
      transaction.setStatus('ok');
      return result;
    } catch (error) {
      transaction.setStatus('internal_error');
      this.captureException(error as Error, {
        tags: { measurementFunction: name },
      });
      throw error;
    } finally {
      transaction.finish();
    }
  }

  /**
   * Hash user ID for privacy
   */
  private static hashUserId(userId: string): string {
    // Simple hash function for client-side (not cryptographically secure)
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
      const char = userId.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return `user_${Math.abs(hash).toString(36)}`;
  }

  /**
   * Hash email for privacy
   */
  private static hashEmail(email: string): string {
    const [localPart, domain] = email.split('@');
    if (!domain) return 'invalid_email';
    
    // Hash local part, keep domain for debugging
    const hashedLocal = this.hashUserId(localPart);
    return `${hashedLocal}@${domain}`;
  }

  /**
   * Sanitize URL by removing sensitive parameters
   */
  private static sanitizeUrl(url: string): string {
    if (!url) return url;
    
    try {
      const urlObj = new URL(url);
      const sensitiveParams = ['password', 'token', 'key', 'secret', 'auth'];
      
      sensitiveParams.forEach(param => {
        if (urlObj.searchParams.has(param)) {
          urlObj.searchParams.set(param, '***');
        }
      });
      
      return urlObj.toString();
    } catch {
      // If URL parsing fails, use regex fallback
      return url.replace(
        /([?&])(password|token|key|secret|auth)=[^&]*/gi,
        '$1$2=***'
      );
    }
  }

  /**
   * Sanitize data by removing sensitive fields
   */
  private static sanitizeData(data: any): any {
    if (!data || typeof data !== 'object') {
      return data;
    }

    const sensitiveKeys = [
      'password', 'token', 'key', 'secret', 'auth', 'authorization',
      'cookie', 'session', 'csrf', 'api_key', 'access_token', 'refresh_token'
    ];

    const sanitized = { ...data };
    
    Object.keys(sanitized).forEach(key => {
      const lowerKey = key.toLowerCase();
      if (sensitiveKeys.some(sensitive => lowerKey.includes(sensitive))) {
        sanitized[key] = '***';
      } else if (typeof sanitized[key] === 'object' && sanitized[key] !== null) {
        sanitized[key] = this.sanitizeData(sanitized[key]);
      }
    });

    return sanitized;
  }
}

// Export convenience functions
export const {
  setUser,
  clearUser,
  setTags,
  setContext,
  addBreadcrumb,
  captureException,
  captureMessage,
  captureApiError,
  captureNavigationError,
  captureFormError,
  startTransaction,
  measureAsync,
} = SentryService;
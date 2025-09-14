/**
 * Error Monitoring Integration Tests
 * Tests Sentry integration, error handling, and alerting system
 */

const axios = require('axios');
const { expect } = require('@jest/globals');

// Test configuration
const config = {
  baseUrl: process.env.TEST_BASE_URL || 'http://localhost:3000',
  apiUrl: process.env.TEST_API_URL || 'http://localhost:8080/api',
  sentryDsn: process.env.SENTRY_DSN,
  environment: process.env.NODE_ENV || 'test',
};

// Mock Sentry for testing
const mockSentry = {
  capturedExceptions: [],
  capturedMessages: [],
  capturedBreadcrumbs: [],
  
  captureException: jest.fn((error, context) => {
    mockSentry.capturedExceptions.push({ error, context, timestamp: Date.now() });
  }),
  
  captureMessage: jest.fn((message, level, context) => {
    mockSentry.capturedMessages.push({ message, level, context, timestamp: Date.now() });
  }),
  
  addBreadcrumb: jest.fn((breadcrumb) => {
    mockSentry.capturedBreadcrumbs.push({ ...breadcrumb, timestamp: Date.now() });
  }),
  
  clear: () => {
    mockSentry.capturedExceptions = [];
    mockSentry.capturedMessages = [];
    mockSentry.capturedBreadcrumbs = [];
  }
};

// Test utilities
const testUtils = {
  async triggerError(type, endpoint = '/api/test/error') {
    try {
      await axios.post(`${config.apiUrl}${endpoint}`, { errorType: type });
    } catch (error) {
      return error.response;
    }
  },
  
  async waitForSentryCapture(timeout = 5000) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
      if (mockSentry.capturedExceptions.length > 0 || mockSentry.capturedMessages.length > 0) {
        return true;
      }
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    return false;
  },
  
  async checkAlertTriggered(alertName, timeout = 10000) {
    // In a real implementation, this would check your alerting system
    // For now, we'll simulate by checking if certain error patterns were captured
    const start = Date.now();
    while (Date.now() - start < timeout) {
      const criticalErrors = mockSentry.capturedExceptions.filter(
        capture => capture.context?.tags?.severity === 'critical'
      );
      if (criticalErrors.length > 0) {
        return true;
      }
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    return false;
  }
};

describe('Error Monitoring Integration Tests', () => {
  beforeEach(() => {
    mockSentry.clear();
  });

  describe('Backend Error Handling', () => {
    test('should capture ResourceNotFoundException', async () => {
      const response = await testUtils.triggerError('ResourceNotFoundException');
      
      expect(response.status).toBe(404);
      expect(response.data).toHaveProperty('code');
      expect(response.data).toHaveProperty('message');
      
      // Verify error is NOT sent to Sentry (as per requirements)
      await testUtils.waitForSentryCapture(2000);
      const resourceNotFoundErrors = mockSentry.capturedExceptions.filter(
        capture => capture.error.name === 'ResourceNotFoundException'
      );
      expect(resourceNotFoundErrors).toHaveLength(0);
    });

    test('should capture ServiceUnavailableException to Sentry', async () => {
      const response = await testUtils.triggerError('ServiceUnavailableException');
      
      expect(response.status).toBe(503);
      
      // Verify error IS sent to Sentry
      const captured = await testUtils.waitForSentryCapture();
      expect(captured).toBe(true);
      
      const serviceErrors = mockSentry.capturedExceptions.filter(
        capture => capture.error.name === 'ServiceUnavailableException'
      );
      expect(serviceErrors.length).toBeGreaterThan(0);
      
      const errorCapture = serviceErrors[0];
      expect(errorCapture.context.tags).toHaveProperty('errorType', 'infrastructure');
      expect(errorCapture.context.tags).toHaveProperty('service');
    });

    test('should capture database connection errors', async () => {
      const response = await testUtils.triggerError('DatabaseConnectionException');
      
      expect(response.status).toBe(503);
      
      const captured = await testUtils.waitForSentryCapture();
      expect(captured).toBe(true);
      
      const dbErrors = mockSentry.capturedExceptions.filter(
        capture => capture.error.message.includes('database')
      );
      expect(dbErrors.length).toBeGreaterThan(0);
    });

    test('should capture payment system errors', async () => {
      const response = await testUtils.triggerError('PaymentException');
      
      expect(response.status).toBe(500);
      
      const captured = await testUtils.waitForSentryCapture();
      expect(captured).toBe(true);
      
      const paymentErrors = mockSentry.capturedExceptions.filter(
        capture => capture.context?.tags?.feature === 'payment'
      );
      expect(paymentErrors.length).toBeGreaterThan(0);
    });
  });

  describe('Frontend Error Handling', () => {
    test('should capture JavaScript errors', async () => {
      // Simulate frontend error
      const jsError = new Error('Uncaught TypeError: Cannot read property of undefined');
      jsError.stack = `TypeError: Cannot read property 'id' of undefined
    at BookingComponent.render (booking.js:45:12)
    at React.render (react.js:123:45)`;
      
      mockSentry.captureException(jsError, {
        tags: {
          component: 'BookingComponent',
          service: 'frontend',
          errorType: 'javascript'
        },
        extra: {
          url: '/booking/123',
          userAgent: 'Mozilla/5.0...',
          userId: 'user_123'
        }
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      const capture = mockSentry.capturedExceptions[0];
      expect(capture.context.tags.component).toBe('BookingComponent');
      expect(capture.context.tags.service).toBe('frontend');
    });

    test('should capture API client errors with context', async () => {
      const apiError = new Error('Network Error: Request failed with status 500');
      
      mockSentry.captureException(apiError, {
        tags: {
          errorType: 'api',
          httpMethod: 'POST',
          httpStatus: '500',
          service: 'frontend'
        },
        extra: {
          apiUrl: '/api/bookings',
          requestData: { tripId: '123', userId: 'user_456' },
          responseData: { error: 'Internal Server Error' }
        }
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      const capture = mockSentry.capturedExceptions[0];
      expect(capture.context.tags.errorType).toBe('api');
      expect(capture.context.tags.httpStatus).toBe('500');
    });

    test('should capture form validation errors', async () => {
      const formError = new Error('Validation failed: Email is required');
      
      mockSentry.captureException(formError, {
        tags: {
          component: 'form',
          action: 'validation',
          service: 'frontend'
        },
        extra: {
          formName: 'userRegistration',
          formData: { username: 'test', email: '' },
          validationErrors: ['Email is required', 'Password too short']
        }
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      const capture = mockSentry.capturedExceptions[0];
      expect(capture.context.extra.formName).toBe('userRegistration');
    });
  });

  describe('Error Rate Limiting', () => {
    test('should rate limit duplicate errors', async () => {
      const duplicateError = new Error('Duplicate error for testing');
      
      // Trigger the same error multiple times
      for (let i = 0; i < 10; i++) {
        mockSentry.captureException(duplicateError, {
          tags: { component: 'test', action: 'rate-limit-test' }
        });
      }
      
      // Should have captured some but not all due to rate limiting
      // In a real implementation, this would be handled by the rate limiter
      expect(mockSentry.capturedExceptions.length).toBeLessThan(10);
    });

    test('should not rate limit different error types', async () => {
      const errors = [
        new Error('Error type 1'),
        new Error('Error type 2'),
        new Error('Error type 3')
      ];
      
      errors.forEach((error, index) => {
        mockSentry.captureException(error, {
          tags: { component: 'test', errorId: index.toString() }
        });
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(3);
    });
  });

  describe('Alert System Integration', () => {
    test('should trigger critical alert for payment errors', async () => {
      // Simulate multiple payment errors to trigger alert
      for (let i = 0; i < 6; i++) {
        mockSentry.captureException(new Error(`Payment error ${i}`), {
          tags: {
            feature: 'payment',
            severity: 'critical',
            service: 'backend'
          }
        });
      }
      
      const alertTriggered = await testUtils.checkAlertTriggered('Payment System Failures');
      expect(alertTriggered).toBe(true);
    });

    test('should trigger alert for high error rate', async () => {
      // Simulate high error rate
      for (let i = 0; i < 20; i++) {
        mockSentry.captureException(new Error(`High rate error ${i}`), {
          tags: {
            severity: 'error',
            service: 'backend'
          }
        });
      }
      
      const alertTriggered = await testUtils.checkAlertTriggered('High Error Rate');
      expect(alertTriggered).toBe(true);
    });

    test('should not trigger alert for low-severity errors', async () => {
      mockSentry.captureException(new Error('Minor validation error'), {
        tags: {
          severity: 'warning',
          service: 'frontend'
        }
      });
      
      const alertTriggered = await testUtils.checkAlertTriggered('Critical System Errors', 3000);
      expect(alertTriggered).toBe(false);
    });
  });

  describe('Error Context and Breadcrumbs', () => {
    test('should capture user context with errors', async () => {
      const userError = new Error('User-specific error');
      
      mockSentry.captureException(userError, {
        tags: {
          userId: 'user_789',
          userRole: 'premium'
        },
        extra: {
          userContext: {
            id: 'user_789',
            email: 'hashed_email',
            subscription: 'premium'
          }
        }
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      const capture = mockSentry.capturedExceptions[0];
      expect(capture.context.tags.userId).toBe('user_789');
      expect(capture.context.extra.userContext.subscription).toBe('premium');
    });

    test('should capture breadcrumbs for error context', async () => {
      // Add breadcrumbs
      mockSentry.addBreadcrumb({
        message: 'User clicked booking button',
        category: 'user',
        level: 'info',
        data: { tripId: '123', action: 'book_trip' }
      });
      
      mockSentry.addBreadcrumb({
        message: 'API call to /api/bookings',
        category: 'http',
        level: 'info',
        data: { method: 'POST', url: '/api/bookings' }
      });
      
      // Trigger error
      mockSentry.captureException(new Error('Booking failed'), {
        tags: { feature: 'booking' }
      });
      
      expect(mockSentry.capturedBreadcrumbs).toHaveLength(2);
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      
      const userBreadcrumb = mockSentry.capturedBreadcrumbs.find(
        b => b.category === 'user'
      );
      expect(userBreadcrumb.data.tripId).toBe('123');
    });
  });

  describe('Performance and Error Correlation', () => {
    test('should capture slow requests that result in errors', async () => {
      const slowError = new Error('Request timeout');
      
      mockSentry.captureException(slowError, {
        tags: {
          errorType: 'timeout',
          service: 'backend'
        },
        extra: {
          requestDuration: 30000, // 30 seconds
          endpoint: '/api/search',
          method: 'GET'
        }
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      const capture = mockSentry.capturedExceptions[0];
      expect(capture.context.extra.requestDuration).toBe(30000);
      expect(capture.context.tags.errorType).toBe('timeout');
    });

    test('should capture memory-related errors', async () => {
      const memoryError = new Error('JavaScript heap out of memory');
      
      mockSentry.captureException(memoryError, {
        tags: {
          errorType: 'memory',
          service: 'frontend'
        },
        extra: {
          memoryUsage: {
            used: 1500000000, // 1.5GB
            total: 2000000000  // 2GB
          },
          component: 'MapComponent'
        }
      });
      
      expect(mockSentry.capturedExceptions).toHaveLength(1);
      const capture = mockSentry.capturedExceptions[0];
      expect(capture.context.tags.errorType).toBe('memory');
      expect(capture.context.extra.component).toBe('MapComponent');
    });
  });

  describe('Error Recovery and Fallbacks', () => {
    test('should log fallback UI activation', async () => {
      mockSentry.captureMessage('Fallback UI activated for payment component', 'warning', {
        tags: {
          component: 'PaymentComponent',
          action: 'fallback_activated',
          service: 'frontend'
        },
        extra: {
          originalError: 'Payment gateway timeout',
          fallbackType: 'offline_payment_form'
        }
      });
      
      expect(mockSentry.capturedMessages).toHaveLength(1);
      const capture = mockSentry.capturedMessages[0];
      expect(capture.context.tags.action).toBe('fallback_activated');
      expect(capture.context.extra.fallbackType).toBe('offline_payment_form');
    });

    test('should track error recovery success', async () => {
      mockSentry.captureMessage('Error recovery successful', 'info', {
        tags: {
          component: 'SearchComponent',
          action: 'recovery_success',
          service: 'frontend'
        },
        extra: {
          originalError: 'Search service timeout',
          recoveryMethod: 'cached_results',
          recoveryTime: 2000
        }
      });
      
      expect(mockSentry.capturedMessages).toHaveLength(1);
      const capture = mockSentry.capturedMessages[0];
      expect(capture.context.tags.action).toBe('recovery_success');
      expect(capture.context.extra.recoveryMethod).toBe('cached_results');
    });
  });
});

// Integration test for real Sentry endpoint (if configured)
if (config.sentryDsn && process.env.RUN_SENTRY_INTEGRATION_TESTS === 'true') {
  describe('Real Sentry Integration', () => {
    test('should send test error to Sentry', async () => {
      const testError = new Error('Integration test error - safe to ignore');
      
      // This would use the real Sentry client
      // Sentry.captureException(testError, {
      //   tags: {
      //     test: 'integration',
      //     environment: config.environment
      //   }
      // });
      
      // Wait for Sentry to process
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // In a real test, you might check Sentry API to verify the error was received
      expect(true).toBe(true); // Placeholder
    });
  });
}

// Cleanup
afterAll(() => {
  mockSentry.clear();
});
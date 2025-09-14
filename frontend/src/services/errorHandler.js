// Error handling service for AI service and gateway timeouts

class ErrorHandler {
  constructor() {
    this.errorMessages = {
      // Network errors
      NETWORK_ERROR: 'Unable to connect to our servers. Please check your internet connection and try again.',
      TIMEOUT_ERROR: 'The request is taking longer than expected. Please try again in a moment.',
      
      // AI Service errors
      AI_SERVICE_TIMEOUT: 'Our AI travel assistant is currently busy. Please try generating your itinerary again in a few moments.',
      AI_SERVICE_UNAVAILABLE: 'Our AI travel planning service is temporarily unavailable. You can still browse and book individual items.',
      AI_GENERATION_FAILED: 'We encountered an issue generating your personalized itinerary. Please try adjusting your preferences and try again.',
      
      // Gateway errors
      GATEWAY_TIMEOUT: 'Our booking system is experiencing high traffic. Please wait a moment and try again.',
      GATEWAY_UNAVAILABLE: 'Our booking services are temporarily unavailable. Please try again later.',
      SERVICE_UNAVAILABLE: 'Some of our travel services are currently unavailable, but you can still plan your trip with available options.',
      
      // Booking errors
      BOOKING_FAILED: 'We couldn\'t complete your booking at this time. Please try again or contact support if the issue persists.',
      PAYMENT_FAILED: 'Payment processing failed. Please check your payment details and try again.',
      INVENTORY_UNAVAILABLE: 'This item is no longer available. Please select a different option.',
      
      // Authentication errors
      AUTH_EXPIRED: 'Your session has expired. Please log in again to continue.',
      AUTH_FAILED: 'Authentication failed. Please check your credentials and try again.',
      
      // Validation errors
      VALIDATION_ERROR: 'Please check your input and try again.',
      REQUIRED_FIELDS: 'Please fill in all required fields.',
      
      // Generic errors
      UNKNOWN_ERROR: 'Something went wrong. Please try again or contact support if the issue continues.',
      SERVER_ERROR: 'Our servers are experiencing issues. Please try again in a few minutes.'
    };
    
    this.retryableErrors = [
      'TIMEOUT_ERROR',
      'AI_SERVICE_TIMEOUT', 
      'GATEWAY_TIMEOUT',
      'NETWORK_ERROR'
    ];
  }

  // Main error handling method
  handleError(error, context = {}) {
    console.error('Error occurred:', error, 'Context:', context);
    
    const errorInfo = this.categorizeError(error, context);
    
    return {
      type: errorInfo.type,
      message: errorInfo.message,
      isRetryable: this.retryableErrors.includes(errorInfo.type),
      shouldShowNotification: true,
      severity: errorInfo.severity,
      context: context,
      timestamp: new Date().toISOString()
    };
  }

  // Categorize different types of errors
  categorizeError(error, context) {
    // Network timeout errors
    if (error.name === 'AbortError' || error.code === 'ECONNABORTED') {
      if (context.service === 'ai') {
        return { type: 'AI_SERVICE_TIMEOUT', message: this.errorMessages.AI_SERVICE_TIMEOUT, severity: 'warning' };
      } else if (context.service === 'gateway') {
        return { type: 'GATEWAY_TIMEOUT', message: this.errorMessages.GATEWAY_TIMEOUT, severity: 'warning' };
      }
      return { type: 'TIMEOUT_ERROR', message: this.errorMessages.TIMEOUT_ERROR, severity: 'warning' };
    }

    // HTTP status code errors
    if (error.response) {
      const status = error.response.status;
      
      switch (status) {
        case 401:
          return { type: 'AUTH_EXPIRED', message: this.errorMessages.AUTH_EXPIRED, severity: 'error' };
        case 403:
          return { type: 'AUTH_FAILED', message: this.errorMessages.AUTH_FAILED, severity: 'error' };
        case 404:
          if (context.action === 'booking') {
            return { type: 'INVENTORY_UNAVAILABLE', message: this.errorMessages.INVENTORY_UNAVAILABLE, severity: 'warning' };
          }
          return { type: 'SERVICE_UNAVAILABLE', message: this.errorMessages.SERVICE_UNAVAILABLE, severity: 'warning' };
        case 408:
        case 504:
          if (context.service === 'ai') {
            return { type: 'AI_SERVICE_TIMEOUT', message: this.errorMessages.AI_SERVICE_TIMEOUT, severity: 'warning' };
          }
          return { type: 'GATEWAY_TIMEOUT', message: this.errorMessages.GATEWAY_TIMEOUT, severity: 'warning' };
        case 422:
          return { type: 'VALIDATION_ERROR', message: this.errorMessages.VALIDATION_ERROR, severity: 'warning' };
        case 500:
        case 502:
        case 503:
          if (context.service === 'ai') {
            return { type: 'AI_SERVICE_UNAVAILABLE', message: this.errorMessages.AI_SERVICE_UNAVAILABLE, severity: 'error' };
          } else if (context.service === 'gateway') {
            return { type: 'GATEWAY_UNAVAILABLE', message: this.errorMessages.GATEWAY_UNAVAILABLE, severity: 'error' };
          }
          return { type: 'SERVER_ERROR', message: this.errorMessages.SERVER_ERROR, severity: 'error' };
        default:
          return { type: 'UNKNOWN_ERROR', message: this.errorMessages.UNKNOWN_ERROR, severity: 'error' };
      }
    }

    // Network connection errors
    if (error.message && error.message.toLowerCase().includes('network')) {
      return { type: 'NETWORK_ERROR', message: this.errorMessages.NETWORK_ERROR, severity: 'error' };
    }

    // Specific service errors based on context
    if (context.service === 'ai') {
      return { type: 'AI_GENERATION_FAILED', message: this.errorMessages.AI_GENERATION_FAILED, severity: 'warning' };
    }
    
    if (context.action === 'booking') {
      return { type: 'BOOKING_FAILED', message: this.errorMessages.BOOKING_FAILED, severity: 'error' };
    }
    
    if (context.action === 'payment') {
      return { type: 'PAYMENT_FAILED', message: this.errorMessages.PAYMENT_FAILED, severity: 'error' };
    }

    // Default unknown error
    return { type: 'UNKNOWN_ERROR', message: this.errorMessages.UNKNOWN_ERROR, severity: 'error' };
  }

  // Create timeout wrapper for API calls
  withTimeout(promise, timeoutMs = 30000, context = {}) {
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => {
        reject(new Error('Request timeout'));
      }, timeoutMs);
    });

    return Promise.race([promise, timeoutPromise])
      .catch(error => {
        throw this.handleError(error, context);
      });
  }

  // Retry mechanism for retryable errors
  async withRetry(asyncFn, maxRetries = 3, context = {}) {
    let lastError;
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await asyncFn();
      } catch (error) {
        lastError = error;
        const errorInfo = this.handleError(error, { ...context, attempt });
        
        // Don't retry if error is not retryable or it's the last attempt
        if (!errorInfo.isRetryable || attempt === maxRetries) {
          throw errorInfo;
        }
        
        // Exponential backoff
        const delay = Math.min(1000 * Math.pow(2, attempt - 1), 10000);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
    
    throw this.handleError(lastError, context);
  }

  // Format error for display in UI components
  formatErrorForDisplay(errorInfo) {
    return {
      title: this.getErrorTitle(errorInfo.type),
      message: errorInfo.message,
      actions: this.getErrorActions(errorInfo),
      severity: errorInfo.severity
    };
  }

  getErrorTitle(errorType) {
    const titles = {
      AI_SERVICE_TIMEOUT: 'AI Service Busy',
      AI_SERVICE_UNAVAILABLE: 'AI Service Unavailable',
      AI_GENERATION_FAILED: 'Generation Failed',
      GATEWAY_TIMEOUT: 'Service Timeout',
      GATEWAY_UNAVAILABLE: 'Service Unavailable',
      BOOKING_FAILED: 'Booking Failed',
      PAYMENT_FAILED: 'Payment Failed',
      AUTH_EXPIRED: 'Session Expired',
      NETWORK_ERROR: 'Connection Error',
      VALIDATION_ERROR: 'Invalid Input',
      UNKNOWN_ERROR: 'Unexpected Error'
    };
    
    return titles[errorType] || 'Error';
  }

  getErrorActions(errorInfo) {
    const actions = [];
    
    if (errorInfo.isRetryable) {
      actions.push({ label: 'Try Again', action: 'retry', variant: 'primary' });
    }
    
    if (errorInfo.type === 'AUTH_EXPIRED') {
      actions.push({ label: 'Log In', action: 'login', variant: 'primary' });
    }
    
    if (errorInfo.type === 'AI_SERVICE_UNAVAILABLE') {
      actions.push({ label: 'Browse Manually', action: 'browse', variant: 'secondary' });
    }
    
    actions.push({ label: 'Contact Support', action: 'support', variant: 'outline' });
    
    return actions;
  }

  // Log errors for monitoring (in production, this would send to a logging service)
  logError(errorInfo) {
    const logData = {
      timestamp: errorInfo.timestamp,
      type: errorInfo.type,
      message: errorInfo.message,
      context: errorInfo.context,
      userAgent: navigator.userAgent,
      url: window.location.href
    };
    
    // In production, send to logging service
    console.error('Error logged:', logData);
    
    // Store in localStorage for debugging (limit to last 50 errors)
    try {
      const errors = JSON.parse(localStorage.getItem('hopngo_errors') || '[]');
      errors.unshift(logData);
      errors.splice(50); // Keep only last 50 errors
      localStorage.setItem('hopngo_errors', JSON.stringify(errors));
    } catch (e) {
      console.warn('Could not store error in localStorage:', e);
    }
  }
}

// Create singleton instance
const errorHandler = new ErrorHandler();

// Export both the class and instance
export { ErrorHandler };
export default errorHandler;
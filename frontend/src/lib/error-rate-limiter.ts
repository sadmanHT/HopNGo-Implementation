/**
 * Rate limiter for error logging to prevent spam and reduce noise
 */

interface RateLimitConfig {
  maxErrors: number;
  timeWindow: number; // in milliseconds
  cooldownPeriod: number; // in milliseconds
}

interface ErrorEntry {
  timestamp: number;
  count: number;
  lastSeen: number;
  hash: string;
}

class ErrorRateLimiter {
  private errorCounts = new Map<string, ErrorEntry>();
  private config: RateLimitConfig;
  private cleanupInterval: NodeJS.Timeout | null = null;

  constructor(config: Partial<RateLimitConfig> = {}) {
    this.config = {
      maxErrors: config.maxErrors || 5, // Max 5 errors per time window
      timeWindow: config.timeWindow || 60000, // 1 minute window
      cooldownPeriod: config.cooldownPeriod || 300000, // 5 minute cooldown
    };

    // Clean up old entries every minute
    this.cleanupInterval = setInterval(() => {
      this.cleanup();
    }, 60000);
  }

  /**
   * Check if an error should be logged based on rate limiting rules
   */
  shouldLog(error: Error | string, context?: Record<string, any>): boolean {
    const errorHash = this.generateErrorHash(error, context);
    const now = Date.now();
    const entry = this.errorCounts.get(errorHash);

    if (!entry) {
      // First occurrence of this error
      this.errorCounts.set(errorHash, {
        timestamp: now,
        count: 1,
        lastSeen: now,
        hash: errorHash,
      });
      return true;
    }

    // Check if we're in cooldown period
    if (now - entry.lastSeen < this.config.cooldownPeriod && entry.count >= this.config.maxErrors) {
      return false; // Still in cooldown, don't log
    }

    // Check if we're in a new time window
    if (now - entry.timestamp > this.config.timeWindow) {
      // Reset the counter for new time window
      entry.timestamp = now;
      entry.count = 1;
      entry.lastSeen = now;
      return true;
    }

    // Within the same time window
    entry.count++;
    entry.lastSeen = now;

    // Check if we've exceeded the limit
    if (entry.count > this.config.maxErrors) {
      return false; // Rate limited
    }

    return true;
  }

  /**
   * Get rate limit status for an error
   */
  getRateLimitStatus(error: Error | string, context?: Record<string, any>) {
    const errorHash = this.generateErrorHash(error, context);
    const entry = this.errorCounts.get(errorHash);
    const now = Date.now();

    if (!entry) {
      return {
        isRateLimited: false,
        count: 0,
        timeUntilReset: 0,
        timeUntilCooldownEnd: 0,
      };
    }

    const timeUntilReset = Math.max(0, this.config.timeWindow - (now - entry.timestamp));
    const timeUntilCooldownEnd = entry.count >= this.config.maxErrors 
      ? Math.max(0, this.config.cooldownPeriod - (now - entry.lastSeen))
      : 0;

    return {
      isRateLimited: entry.count >= this.config.maxErrors && timeUntilCooldownEnd > 0,
      count: entry.count,
      timeUntilReset,
      timeUntilCooldownEnd,
    };
  }

  /**
   * Generate a hash for the error to group similar errors
   */
  private generateErrorHash(error: Error | string, context?: Record<string, any>): string {
    const errorMessage = typeof error === 'string' ? error : error.message;
    const errorName = typeof error === 'string' ? 'Error' : error.name;
    const stack = typeof error === 'string' ? '' : (error.stack || '');
    
    // Extract the first few lines of stack trace for grouping
    const stackLines = stack.split('\n').slice(0, 3).join('\n');
    
    // Include relevant context for grouping
    const contextString = context ? JSON.stringify({
      component: context.component,
      action: context.action,
      url: context.url,
      // Don't include user-specific data in hash
    }) : '';

    // Create a simple hash
    const hashInput = `${errorName}:${errorMessage}:${stackLines}:${contextString}`;
    return this.simpleHash(hashInput);
  }

  /**
   * Simple hash function for error grouping
   */
  private simpleHash(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash).toString(36);
  }

  /**
   * Clean up old entries to prevent memory leaks
   */
  private cleanup(): void {
    const now = Date.now();
    const cutoff = now - (this.config.timeWindow + this.config.cooldownPeriod);

    for (const [hash, entry] of this.errorCounts.entries()) {
      if (entry.lastSeen < cutoff) {
        this.errorCounts.delete(hash);
      }
    }
  }

  /**
   * Get statistics about rate limiting
   */
  getStats() {
    const now = Date.now();
    let activeErrors = 0;
    let rateLimitedErrors = 0;
    let totalErrors = 0;

    for (const entry of this.errorCounts.values()) {
      totalErrors += entry.count;
      
      if (now - entry.lastSeen < this.config.timeWindow) {
        activeErrors++;
      }
      
      if (entry.count >= this.config.maxErrors && 
          now - entry.lastSeen < this.config.cooldownPeriod) {
        rateLimitedErrors++;
      }
    }

    return {
      totalUniqueErrors: this.errorCounts.size,
      activeErrors,
      rateLimitedErrors,
      totalErrors,
      config: this.config,
    };
  }

  /**
   * Reset rate limiting for a specific error
   */
  resetError(error: Error | string, context?: Record<string, any>): void {
    const errorHash = this.generateErrorHash(error, context);
    this.errorCounts.delete(errorHash);
  }

  /**
   * Reset all rate limiting
   */
  resetAll(): void {
    this.errorCounts.clear();
  }

  /**
   * Update configuration
   */
  updateConfig(newConfig: Partial<RateLimitConfig>): void {
    this.config = { ...this.config, ...newConfig };
  }

  /**
   * Destroy the rate limiter and clean up resources
   */
  destroy(): void {
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval);
      this.cleanupInterval = null;
    }
    this.errorCounts.clear();
  }
}

// Global rate limiter instance
const globalRateLimiter = new ErrorRateLimiter({
  maxErrors: 5,
  timeWindow: 60000, // 1 minute
  cooldownPeriod: 300000, // 5 minutes
});

// Specific rate limiters for different error types
const apiErrorRateLimiter = new ErrorRateLimiter({
  maxErrors: 10,
  timeWindow: 60000, // 1 minute
  cooldownPeriod: 180000, // 3 minutes
});

const networkErrorRateLimiter = new ErrorRateLimiter({
  maxErrors: 3,
  timeWindow: 30000, // 30 seconds
  cooldownPeriod: 120000, // 2 minutes
});

const validationErrorRateLimiter = new ErrorRateLimiter({
  maxErrors: 15,
  timeWindow: 60000, // 1 minute
  cooldownPeriod: 60000, // 1 minute
});

/**
 * Rate-limited logging functions
 */
export const RateLimitedLogger = {
  /**
   * Log error with rate limiting
   */
  logError(error: Error | string, context?: Record<string, any>, type: 'general' | 'api' | 'network' | 'validation' = 'general'): boolean {
    let rateLimiter: ErrorRateLimiter;
    
    switch (type) {
      case 'api':
        rateLimiter = apiErrorRateLimiter;
        break;
      case 'network':
        rateLimiter = networkErrorRateLimiter;
        break;
      case 'validation':
        rateLimiter = validationErrorRateLimiter;
        break;
      default:
        rateLimiter = globalRateLimiter;
    }

    return rateLimiter.shouldLog(error, context);
  },

  /**
   * Get rate limit status for an error
   */
  getStatus(error: Error | string, context?: Record<string, any>, type: 'general' | 'api' | 'network' | 'validation' = 'general') {
    let rateLimiter: ErrorRateLimiter;
    
    switch (type) {
      case 'api':
        rateLimiter = apiErrorRateLimiter;
        break;
      case 'network':
        rateLimiter = networkErrorRateLimiter;
        break;
      case 'validation':
        rateLimiter = validationErrorRateLimiter;
        break;
      default:
        rateLimiter = globalRateLimiter;
    }

    return rateLimiter.getRateLimitStatus(error, context);
  },

  /**
   * Get statistics for all rate limiters
   */
  getAllStats() {
    return {
      general: globalRateLimiter.getStats(),
      api: apiErrorRateLimiter.getStats(),
      network: networkErrorRateLimiter.getStats(),
      validation: validationErrorRateLimiter.getStats(),
    };
  },

  /**
   * Reset rate limiting for specific error type
   */
  reset(type?: 'general' | 'api' | 'network' | 'validation') {
    if (type) {
      switch (type) {
        case 'api':
          apiErrorRateLimiter.resetAll();
          break;
        case 'network':
          networkErrorRateLimiter.resetAll();
          break;
        case 'validation':
          validationErrorRateLimiter.resetAll();
          break;
        default:
          globalRateLimiter.resetAll();
      }
    } else {
      // Reset all
      globalRateLimiter.resetAll();
      apiErrorRateLimiter.resetAll();
      networkErrorRateLimiter.resetAll();
      validationErrorRateLimiter.resetAll();
    }
  },
};

export { ErrorRateLimiter };
export default RateLimitedLogger;
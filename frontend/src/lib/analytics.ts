interface AnalyticsEvent {
  eventType: string;
  userId?: string;
  sessionId?: string;
  timestamp?: string;
  properties?: Record<string, any>;
  metadata?: Record<string, any>;
}

interface AnalyticsConfig {
  apiUrl?: string;
  batchSize?: number;
  flushInterval?: number;
  maxRetries?: number;
  debug?: boolean;
}

class AnalyticsSDK {
  private config: Required<AnalyticsConfig>;
  private eventQueue: AnalyticsEvent[] = [];
  private flushTimer: NodeJS.Timeout | null = null;
  private sessionId: string;
  private isOnline: boolean = true;

  constructor(config: AnalyticsConfig = {}) {
    this.config = {
      apiUrl: config.apiUrl || '/api/v1/analytics',
      batchSize: config.batchSize || 10,
      flushInterval: config.flushInterval || 30000, // 30 seconds
      maxRetries: config.maxRetries || 3,
      debug: config.debug || false
    };

    this.sessionId = this.generateSessionId();
    this.setupEventListeners();
    this.startAutoFlush();
  }

  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private isBrowser(): boolean {
    return typeof window !== 'undefined';
  }

  private setupEventListeners(): void {
    // Only run in browser environment
    if (typeof window === 'undefined') return;
    
    // Handle page visibility changes
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.flush();
      }
    });

    // Handle page unload
    window.addEventListener('beforeunload', () => {
      this.flush();
    });

    // Handle online/offline status
    window.addEventListener('online', () => {
      this.isOnline = true;
      this.flush();
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
    });
  }

  private startAutoFlush(): void {
    this.flushTimer = setInterval(() => {
      if (this.eventQueue.length > 0) {
        this.flush();
      }
    }, this.config.flushInterval);
  }

  private log(message: string, data?: any): void {
    if (this.config.debug) {
      console.log(`[Analytics SDK] ${message}`, data);
    }
  }

  /**
   * Track a single event
   */
  track(eventType: string, properties?: Record<string, any>, metadata?: Record<string, any>): void {
    if (!this.isBrowser()) return;
    
    const event: AnalyticsEvent = {
      eventType,
      sessionId: this.sessionId,
      timestamp: new Date().toISOString(),
      properties: properties || {},
      metadata: metadata || {}
    };

    // Add user ID if available
    const userId = this.getCurrentUserId();
    if (userId) {
      event.userId = userId;
    }

    this.eventQueue.push(event);
    this.log('Event queued', event);

    // Auto-flush if batch size reached
    if (this.eventQueue.length >= this.config.batchSize) {
      this.flush();
    }
  }

  /**
   * Track page view
   */
  trackPageView(path?: string, title?: string): void {
    this.track('page_view', {
      path: path || window.location.pathname,
      title: title || document.title,
      referrer: document.referrer,
      userAgent: navigator.userAgent
    });
  }

  /**
   * Track user action
   */
  trackAction(action: string, target?: string, properties?: Record<string, any>): void {
    this.track('user_action', {
      action,
      target,
      ...properties
    });
  }

  /**
   * Track conversion event
   */
  trackConversion(conversionType: string, value?: number, properties?: Record<string, any>): void {
    this.track('conversion', {
      conversionType,
      value,
      ...properties
    });
  }

  /**
   * Track error event
   */
  trackError(error: Error | string, context?: Record<string, any>): void {
    const errorData = typeof error === 'string' ? { message: error } : {
      message: error.message,
      stack: error.stack,
      name: error.name
    };

    this.track('error', {
      ...errorData,
      ...context
    });
  }

  /**
   * Flush events to server
   */
  async flush(): Promise<void> {
    if (!this.isBrowser() || this.eventQueue.length === 0 || !this.isOnline) {
      return;
    }

    const eventsToSend = [...this.eventQueue];
    this.eventQueue = [];

    try {
      await this.sendEvents(eventsToSend);
      this.log(`Successfully sent ${eventsToSend.length} events`);
    } catch (error) {
      this.log('Failed to send events', error);
      // Re-queue events for retry
      this.eventQueue.unshift(...eventsToSend);
    }
  }

  private async sendEvents(events: AnalyticsEvent[], retryCount = 0): Promise<void> {
    try {
      const response = await fetch(`${this.config.apiUrl}/events/batch`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify({ events })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
    } catch (error) {
      if (retryCount < this.config.maxRetries) {
        this.log(`Retrying send events (attempt ${retryCount + 1})`);
        await new Promise(resolve => setTimeout(resolve, Math.pow(2, retryCount) * 1000));
        return this.sendEvents(events, retryCount + 1);
      }
      throw error;
    }
  }

  private getCurrentUserId(): string | null {
    // Try to get user ID from various sources
    try {
      // From localStorage
      const userData = localStorage.getItem('user');
      if (userData) {
        const user = JSON.parse(userData);
        return user.id || user.userId;
      }

      // From sessionStorage
      const sessionData = sessionStorage.getItem('user');
      if (sessionData) {
        const user = JSON.parse(sessionData);
        return user.id || user.userId;
      }

      // From cookies (if using a cookie-based auth)
      const cookies = document.cookie.split(';');
      for (const cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'userId') {
          return value;
        }
      }
    } catch (error) {
      this.log('Error getting user ID', error);
    }

    return null;
  }

  private getAuthToken(): string | null {
    try {
      return localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
    } catch (error) {
      this.log('Error getting auth token', error);
      return null;
    }
  }

  /**
   * Set user ID manually
   */
  setUserId(userId: string): void {
    this.track('user_identified', { userId });
  }

  /**
   * Clear all queued events
   */
  clearQueue(): void {
    this.eventQueue = [];
    this.log('Event queue cleared');
  }

  /**
   * Get current queue size
   */
  getQueueSize(): number {
    return this.eventQueue.length;
  }

  /**
   * Destroy the SDK instance
   */
  destroy(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }
    this.flush();
  }
}

// Create singleton instance
const analytics = new AnalyticsSDK({
  debug: process.env.NODE_ENV === 'development'
});

// Export both the class and singleton
export { AnalyticsSDK, analytics };
export default analytics;

// Export common tracking functions for convenience
export const trackPageView = (path?: string, title?: string) => analytics.trackPageView(path, title);
export const trackAction = (action: string, target?: string, properties?: Record<string, any>) => 
  analytics.trackAction(action, target, properties);
export const trackConversion = (conversionType: string, value?: number, properties?: Record<string, any>) => 
  analytics.trackConversion(conversionType, value, properties);
export const trackError = (error: Error | string, context?: Record<string, any>) => 
  analytics.trackError(error, context);
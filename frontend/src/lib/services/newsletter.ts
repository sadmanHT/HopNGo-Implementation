import apiClient from '@/services/api';

// Types
export interface SubscribeRequest {
  email: string;
  source?: string;
  userId?: string;
  utmSource?: string;
  utmMedium?: string;
  utmCampaign?: string;
}

export interface FooterSubscribeRequest {
  email: string;
  userId?: string;
}

export interface PopupSubscribeRequest {
  email: string;
  userId?: string;
  utmSource?: string;
  utmMedium?: string;
  utmCampaign?: string;
}

export interface SubscriptionResponse {
  success: boolean;
  message: string;
  email?: string;
  status?: string;
  source?: string;
  unsubscribeToken?: string;
}

export interface SubscriberInfo {
  email: string;
  status: string;
  source: string;
  userId?: string;
  createdAt: string;
  tags?: string;
}

export interface SubscriptionStatus {
  subscribed: boolean;
  subscriber?: SubscriberInfo;
}

export interface SubscriptionStats {
  totalSubscribers: number;
  activeSubscribers: number;
  unsubscribedCount: number;
  todaySubscribers: number;
  weeklyGrowth: number;
  monthlyGrowth: number;
  topSources: Array<{ source: string; count: number }>;
  conversionRate: number;
}

// Newsletter Service Class
export class NewsletterService {
  private static instance: NewsletterService;
  private baseUrl = '/api/newsletter';

  static getInstance(): NewsletterService {
    if (!NewsletterService.instance) {
      NewsletterService.instance = new NewsletterService();
    }
    return NewsletterService.instance;
  }

  /**
   * Subscribe to newsletter
   */
  async subscribe(request: SubscribeRequest): Promise<SubscriptionResponse> {
    try {
      const response = await apiClient.post<SubscriptionResponse>(
        `${this.baseUrl}/subscribe`,
        request
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to subscribe');
    }
  }

  /**
   * Subscribe from footer
   */
  async subscribeFromFooter(request: FooterSubscribeRequest): Promise<SubscriptionResponse> {
    try {
      const response = await apiClient.post<SubscriptionResponse>(
        `${this.baseUrl}/subscribe/footer`,
        request
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to subscribe');
    }
  }

  /**
   * Subscribe from popup
   */
  async subscribeFromPopup(request: PopupSubscribeRequest): Promise<SubscriptionResponse> {
    try {
      const response = await apiClient.post<SubscriptionResponse>(
        `${this.baseUrl}/subscribe/popup`,
        request
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to subscribe');
    }
  }

  /**
   * Unsubscribe from newsletter
   */
  async unsubscribe(email: string): Promise<{ success: boolean; message: string }> {
    try {
      const response = await apiClient.post<{ success: boolean; message: string }>(
        `${this.baseUrl}/unsubscribe`,
        { email }
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to unsubscribe');
    }
  }

  /**
   * Check subscription status
   */
  async getSubscriptionStatus(email: string): Promise<SubscriptionStatus> {
    try {
      const response = await apiClient.get<SubscriptionStatus>(
        `${this.baseUrl}/status/${encodeURIComponent(email)}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to check subscription status');
    }
  }

  /**
   * Get subscription statistics (admin only)
   */
  async getSubscriptionStats(): Promise<SubscriptionStats> {
    try {
      const response = await apiClient.get<SubscriptionStats>(
        `${this.baseUrl}/stats`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to get subscription stats');
    }
  }

  /**
   * Get recent subscribers (admin only)
   */
  async getRecentSubscribers(limit: number = 10): Promise<SubscriberInfo[]> {
    try {
      const response = await apiClient.get<SubscriberInfo[]>(
        `${this.baseUrl}/recent?limit=${limit}`
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to get recent subscribers');
    }
  }

  /**
   * Add tag to subscriber
   */
  async addTag(email: string, tag: string): Promise<{ status: string; message: string }> {
    try {
      const response = await apiClient.post<{ status: string; message: string }>(
        `${this.baseUrl}/tag/add`,
        { email, tag }
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to add tag');
    }
  }

  /**
   * Remove tag from subscriber
   */
  async removeTag(email: string, tag: string): Promise<{ status: string; message: string }> {
    try {
      const response = await apiClient.post<{ status: string; message: string }>(
        `${this.baseUrl}/tag/remove`,
        { email, tag }
      );
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to remove tag');
    }
  }

  // Utility methods

  /**
   * Get UTM parameters from URL
   */
  getUtmParameters(): { utmSource?: string; utmMedium?: string; utmCampaign?: string } {
    if (typeof window === 'undefined') return {};
    
    const urlParams = new URLSearchParams(window.location.search);
    return {
      utmSource: urlParams.get('utm_source') || undefined,
      utmMedium: urlParams.get('utm_medium') || undefined,
      utmCampaign: urlParams.get('utm_campaign') || undefined,
    };
  }

  /**
   * Validate email format
   */
  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  /**
   * Store subscription in local storage for tracking
   */
  storeSubscription(email: string, source: string): void {
    if (typeof window === 'undefined') return;
    
    try {
      const subscription = {
        email,
        source,
        timestamp: Date.now(),
      };
      localStorage.setItem('newsletter_subscription', JSON.stringify(subscription));
    } catch (error) {
      console.warn('Failed to store subscription in localStorage:', error);
    }
  }

  /**
   * Get stored subscription from local storage
   */
  getStoredSubscription(): { email: string; source: string; timestamp: number } | null {
    if (typeof window === 'undefined') return null;
    
    try {
      const stored = localStorage.getItem('newsletter_subscription');
      return stored ? JSON.parse(stored) : null;
    } catch (error) {
      console.warn('Failed to get stored subscription from localStorage:', error);
      return null;
    }
  }

  /**
   * Clear stored subscription
   */
  clearStoredSubscription(): void {
    if (typeof window === 'undefined') return;
    
    try {
      localStorage.removeItem('newsletter_subscription');
    } catch (error) {
      console.warn('Failed to clear stored subscription from localStorage:', error);
    }
  }

  /**
   * Check if user has already subscribed (based on stored data)
   */
  hasSubscribed(): boolean {
    const stored = this.getStoredSubscription();
    return stored !== null;
  }

  /**
   * Generate unsubscribe URL
   */
  generateUnsubscribeUrl(token: string): string {
    const baseUrl = typeof window !== 'undefined' 
      ? window.location.origin 
      : 'https://hopngo.com';
    return `${baseUrl}/api/newsletter/unsubscribe/${token}`;
  }

  /**
   * Track newsletter signup event (for analytics)
   */
  trackSignup(email: string, source: string): void {
    if (typeof window !== 'undefined' && (window as any).gtag) {
      (window as any).gtag('event', 'newsletter_signup', {
        event_category: 'engagement',
        event_label: source,
        custom_parameter_1: email,
      });
    }

    // Track with other analytics services if available
    if (typeof window !== 'undefined' && (window as any).fbq) {
      (window as any).fbq('track', 'Subscribe', {
        content_name: 'Newsletter',
        content_category: source,
      });
    }
  }

  /**
   * Show subscription success message
   */
  showSuccessMessage(message: string = 'Successfully subscribed to newsletter!'): void {
    // This can be integrated with your toast/notification system
    if (typeof window !== 'undefined') {
      // Simple alert for now - replace with your notification system
      console.log('Newsletter subscription success:', message);
    }
  }

  /**
   * Show subscription error message
   */
  showErrorMessage(message: string = 'Failed to subscribe. Please try again.'): void {
    // This can be integrated with your toast/notification system
    if (typeof window !== 'undefined') {
      // Simple alert for now - replace with your notification system
      console.error('Newsletter subscription error:', message);
    }
  }
}

// Export singleton instance
export const newsletterService = NewsletterService.getInstance();

// Export default
export default newsletterService;
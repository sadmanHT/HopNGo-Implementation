import { ApiResponse } from '@/lib/types';

export interface Referral {
  id: string;
  userId: string;
  referralCode: string;
  campaign: string;
  status: 'ACTIVE' | 'COMPLETED' | 'EXPIRED' | 'DISABLED';
  clickCount: number;
  conversionCount: number;
  createdAt: string;
  expiresAt: string;
  lastClickedAt?: string;
  lastConvertedAt?: string;
}

export interface ReferralStats {
  totalReferrals: number;
  totalClicks: number;
  totalConversions: number;
  totalPointsEarned: number;
  conversionRate: number;
}

export interface CreateReferralRequest {
  userId: string;
  campaign?: string;
}

export interface ConversionRequest {
  referralCode: string;
  newUserId: string;
  conversionType: 'signup' | 'subscription' | 'booking' | 'purchase';
}

export interface SubscribeRequest {
  email: string;
  referralCode: string;
  userId?: string;
}

export interface ReferralResponse {
  success: boolean;
  message: string;
  referralCode?: string;
  userId?: string;
  campaign?: string;
  status?: string;
  clickCount?: number;
  conversionCount?: number;
}

export interface ValidationResponse {
  valid: boolean;
  message: string;
  referral?: ReferralResponse;
}

export interface SubscriberResponse {
  success: boolean;
  message: string;
  email?: string;
  status?: string;
  source?: string;
}

class ReferralService {
  private baseUrl: string;

  constructor() {
    this.baseUrl = process.env.NEXT_PUBLIC_ANALYTICS_API_URL || 'http://localhost:8083';
  }

  /**
   * Create a new referral code for a user
   */
  async createReferral(request: CreateReferralRequest): Promise<ApiResponse<ReferralResponse>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'Failed to create referral');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error creating referral:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Track a referral click/visit
   */
  async trackReferralClick(referralCode: string): Promise<ApiResponse<{ status: string; message: string }>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/track/${referralCode}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'Failed to track referral click');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error tracking referral click:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Process a referral conversion
   */
  async processConversion(request: ConversionRequest): Promise<ApiResponse<{ status: string; message: string }>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/convert`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'Failed to process conversion');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error processing conversion:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Validate a referral code
   */
  async validateReferralCode(referralCode: string): Promise<ApiResponse<ValidationResponse>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/validate/${referralCode}`);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'Failed to validate referral code');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error validating referral code:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Get referral statistics for a user
   */
  async getReferralStats(userId: string): Promise<ApiResponse<ReferralStats>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/stats/${userId}`);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error('Failed to fetch referral stats');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error fetching referral stats:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Get active referral codes for a user
   */
  async getUserReferrals(userId: string): Promise<ApiResponse<ReferralResponse[]>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/user/${userId}`);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error('Failed to fetch user referrals');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error fetching user referrals:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Subscribe via referral
   */
  async subscribeViaReferral(request: SubscribeRequest): Promise<ApiResponse<SubscriberResponse>> {
    try {
      const response = await fetch(`${this.baseUrl}/api/referrals/subscribe`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'Failed to subscribe via referral');
      }

      return {
        success: true,
        data,
      };
    } catch (error) {
      console.error('Error subscribing via referral:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred',
      };
    }
  }

  /**
   * Generate referral URL for sharing
   */
  generateReferralUrl(referralCode: string, baseUrl?: string): string {
    const domain = baseUrl || (typeof window !== 'undefined' ? window.location.origin : 'https://hopngo.com');
    return `${domain}?ref=${referralCode}`;
  }

  /**
   * Extract referral code from URL parameters
   */
  extractReferralCode(): string | null {
    if (typeof window === 'undefined') return null;
    
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('ref');
  }

  /**
   * Store referral code in localStorage for later use
   */
  storeReferralCode(referralCode: string): void {
    if (typeof window === 'undefined') return;
    
    localStorage.setItem('hopngo_referral_code', referralCode);
    localStorage.setItem('hopngo_referral_timestamp', Date.now().toString());
  }

  /**
   * Get stored referral code from localStorage
   */
  getStoredReferralCode(): string | null {
    if (typeof window === 'undefined') return null;
    
    const referralCode = localStorage.getItem('hopngo_referral_code');
    const timestamp = localStorage.getItem('hopngo_referral_timestamp');
    
    // Check if referral code is still valid (within 30 days)
    if (referralCode && timestamp) {
      const thirtyDaysInMs = 30 * 24 * 60 * 60 * 1000;
      const isExpired = Date.now() - parseInt(timestamp) > thirtyDaysInMs;
      
      if (isExpired) {
        this.clearStoredReferralCode();
        return null;
      }
      
      return referralCode;
    }
    
    return null;
  }

  /**
   * Clear stored referral code from localStorage
   */
  clearStoredReferralCode(): void {
    if (typeof window === 'undefined') return;
    
    localStorage.removeItem('hopngo_referral_code');
    localStorage.removeItem('hopngo_referral_timestamp');
  }

  /**
   * Initialize referral tracking on page load
   */
  async initializeReferralTracking(): Promise<void> {
    const referralCode = this.extractReferralCode();
    
    if (referralCode) {
      // Store the referral code
      this.storeReferralCode(referralCode);
      
      // Track the referral click
      await this.trackReferralClick(referralCode);
      
      // Clean up URL (remove ref parameter)
      if (typeof window !== 'undefined') {
        const url = new URL(window.location.href);
        url.searchParams.delete('ref');
        window.history.replaceState({}, '', url.toString());
      }
    }
  }
}

export const referralService = new ReferralService();
export default referralService;
/**
 * Cookie Consent Service
 * Manages cookie consent preferences and analytics tracking
 */

export interface CookiePreferences {
  essential: boolean; // Always true, cannot be disabled
  analytics: boolean;
  marketing: boolean;
  functional: boolean;
}

export interface ConsentState {
  hasConsented: boolean;
  consentDate: string;
  preferences: CookiePreferences;
  version: string; // Policy version
}

const CONSENT_STORAGE_KEY = 'hopngo_cookie_consent';
const CONSENT_VERSION = '1.0';
const CONSENT_EXPIRY_DAYS = 365;

class CookieConsentService {
  private static instance: CookieConsentService;
  private consentState: ConsentState | null = null;
  private listeners: Array<(state: ConsentState) => void> = [];

  private constructor() {
    this.loadConsentState();
  }

  static getInstance(): CookieConsentService {
    if (!CookieConsentService.instance) {
      CookieConsentService.instance = new CookieConsentService();
    }
    return CookieConsentService.instance;
  }

  /**
   * Load consent state from localStorage
   */
  private loadConsentState(): void {
    if (typeof window === 'undefined') return;

    try {
      const stored = localStorage.getItem(CONSENT_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        
        // Check if consent is still valid (not expired and same version)
        const consentDate = new Date(parsed.consentDate);
        const expiryDate = new Date(consentDate.getTime() + (CONSENT_EXPIRY_DAYS * 24 * 60 * 60 * 1000));
        
        if (new Date() < expiryDate && parsed.version === CONSENT_VERSION) {
          this.consentState = parsed;
        } else {
          // Consent expired or version changed, clear it
          this.clearConsent();
        }
      }
    } catch (error) {
      console.error('Error loading cookie consent state:', error);
      this.clearConsent();
    }
  }

  /**
   * Save consent state to localStorage
   */
  private saveConsentState(): void {
    if (typeof window === 'undefined' || !this.consentState) return;

    try {
      localStorage.setItem(CONSENT_STORAGE_KEY, JSON.stringify(this.consentState));
    } catch (error) {
      console.error('Error saving cookie consent state:', error);
    }
  }

  /**
   * Get current consent state
   */
  getConsentState(): ConsentState | null {
    return this.consentState;
  }

  /**
   * Check if user has given consent
   */
  hasConsented(): boolean {
    return this.consentState?.hasConsented ?? false;
  }

  /**
   * Check if specific cookie category is allowed
   */
  isCategoryAllowed(category: keyof CookiePreferences): boolean {
    if (!this.consentState) return false;
    return this.consentState.preferences[category];
  }

  /**
   * Set cookie preferences and save consent
   */
  setPreferences(preferences: Partial<CookiePreferences>): void {
    const newPreferences: CookiePreferences = {
      essential: true, // Always true
      analytics: preferences.analytics ?? false,
      marketing: preferences.marketing ?? false,
      functional: preferences.functional ?? false,
    };

    this.consentState = {
      hasConsented: true,
      consentDate: new Date().toISOString(),
      preferences: newPreferences,
      version: CONSENT_VERSION,
    };

    this.saveConsentState();
    this.notifyListeners();
    this.updateAnalyticsTracking();
  }

  /**
   * Accept all cookies
   */
  acceptAll(): void {
    this.setPreferences({
      analytics: true,
      marketing: true,
      functional: true,
    });
  }

  /**
   * Reject all non-essential cookies
   */
  rejectAll(): void {
    this.setPreferences({
      analytics: false,
      marketing: false,
      functional: false,
    });
  }

  /**
   * Clear consent state
   */
  clearConsent(): void {
    if (typeof window === 'undefined') return;
    
    localStorage.removeItem(CONSENT_STORAGE_KEY);
    this.consentState = null;
    this.notifyListeners();
  }

  /**
   * Subscribe to consent state changes
   */
  subscribe(listener: (state: ConsentState) => void): () => void {
    this.listeners.push(listener);
    
    // Return unsubscribe function
    return () => {
      const index = this.listeners.indexOf(listener);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    };
  }

  /**
   * Notify all listeners of state changes
   */
  private notifyListeners(): void {
    if (this.consentState) {
      this.listeners.forEach(listener => listener(this.consentState!));
    }
  }

  /**
   * Update analytics tracking based on consent
   */
  private updateAnalyticsTracking(): void {
    if (typeof window === 'undefined') return;

    const analyticsAllowed = this.isCategoryAllowed('analytics');
    
    // Google Analytics
    if (typeof window.gtag === 'function') {
      window.gtag('consent', 'update', {
        analytics_storage: analyticsAllowed ? 'granted' : 'denied',
        ad_storage: this.isCategoryAllowed('marketing') ? 'granted' : 'denied',
        functionality_storage: this.isCategoryAllowed('functional') ? 'granted' : 'denied',
      });
    }

    // Custom analytics events
    if (analyticsAllowed) {
      this.trackConsentEvent('consent_granted');
    } else {
      this.trackConsentEvent('consent_denied');
    }
  }

  /**
   * Track consent-related events
   */
  private trackConsentEvent(action: string): void {
    if (typeof window === 'undefined' || !this.isCategoryAllowed('analytics')) return;

    // Google Analytics event
    if (typeof window.gtag === 'function') {
      window.gtag('event', action, {
        event_category: 'cookie_consent',
        event_label: 'cookie_banner',
        custom_parameters: {
          consent_version: CONSENT_VERSION,
          preferences: JSON.stringify(this.consentState?.preferences),
        },
      });
    }

    // Custom tracking (if needed)
    console.log(`Cookie consent event: ${action}`, this.consentState?.preferences);
  }

  /**
   * Get consent summary for display
   */
  getConsentSummary(): string {
    if (!this.consentState) return 'No consent given';

    const { preferences } = this.consentState;
    const enabled = Object.entries(preferences)
      .filter(([key, value]) => key !== 'essential' && value)
      .map(([key]) => key);

    if (enabled.length === 0) {
      return 'Essential cookies only';
    }

    return `Essential + ${enabled.join(', ')}`;
  }

  /**
   * Check if consent banner should be shown
   */
  shouldShowBanner(): boolean {
    return !this.hasConsented();
  }

  /**
   * Get days until consent expires
   */
  getDaysUntilExpiry(): number {
    if (!this.consentState) return 0;

    const consentDate = new Date(this.consentState.consentDate);
    const expiryDate = new Date(consentDate.getTime() + (CONSENT_EXPIRY_DAYS * 24 * 60 * 60 * 1000));
    const now = new Date();
    const diffTime = expiryDate.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    return Math.max(0, diffDays);
  }

  /**
   * Export consent data (for GDPR compliance)
   */
  exportConsentData(): object {
    return {
      consentState: this.consentState,
      version: CONSENT_VERSION,
      expiryDays: CONSENT_EXPIRY_DAYS,
      daysUntilExpiry: this.getDaysUntilExpiry(),
      summary: this.getConsentSummary(),
    };
  }
}

// Global type declarations for analytics
declare global {
  interface Window {
    gtag?: (...args: any[]) => void;
    dataLayer?: any[];
  }
}

export const cookieConsentService = CookieConsentService.getInstance();
export default cookieConsentService;
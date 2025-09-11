'use client';

import { useState, useCallback, useEffect } from 'react';
import { useAuth } from './use-auth';
import { toast } from 'sonner';
import {
  newsletterService,
  type SubscribeRequest,
  type FooterSubscribeRequest,
  type PopupSubscribeRequest,
  type SubscriptionResponse,
  type SubscriptionStatus,
  type SubscriptionStats,
  type SubscriberInfo,
} from '@/lib/services/newsletter';

export interface UseNewsletterReturn {
  // State
  isLoading: boolean;
  isSubscribed: boolean;
  subscriptionStatus: SubscriptionStatus | null;
  stats: SubscriptionStats | null;
  recentSubscribers: SubscriberInfo[];
  
  // Actions
  subscribe: (email: string, source?: string) => Promise<boolean>;
  subscribeFromFooter: (email: string) => Promise<boolean>;
  subscribeFromPopup: (email: string) => Promise<boolean>;
  unsubscribe: (email: string) => Promise<boolean>;
  checkSubscriptionStatus: (email: string) => Promise<void>;
  loadStats: () => Promise<void>;
  loadRecentSubscribers: (limit?: number) => Promise<void>;
  addTag: (email: string, tag: string) => Promise<boolean>;
  removeTag: (email: string, tag: string) => Promise<boolean>;
  
  // Utilities
  validateEmail: (email: string) => boolean;
  hasSubscribed: () => boolean;
  clearStoredSubscription: () => void;
}

export function useNewsletter(): UseNewsletterReturn {
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [subscriptionStatus, setSubscriptionStatus] = useState<SubscriptionStatus | null>(null);
  const [stats, setStats] = useState<SubscriptionStats | null>(null);
  const [recentSubscribers, setRecentSubscribers] = useState<SubscriberInfo[]>([]);

  // Check if user has already subscribed on mount
  useEffect(() => {
    const hasSubscribed = newsletterService.hasSubscribed();
    setIsSubscribed(hasSubscribed);
    
    // If user is logged in and we have their email, check their subscription status
    if (user?.email) {
      checkSubscriptionStatus(user.email);
    }
  }, [user]);

  /**
   * Subscribe to newsletter
   */
  const subscribe = useCallback(async (email: string, source: string = 'general'): Promise<boolean> => {
    if (!newsletterService.isValidEmail(email)) {
      toast.error('Please enter a valid email address');
      return false;
    }

    setIsLoading(true);
    try {
      const utmParams = newsletterService.getUtmParameters();
      const request: SubscribeRequest = {
        email,
        source,
        userId: user?.id,
        ...utmParams,
      };

      const response: SubscriptionResponse = await newsletterService.subscribe(request);
      
      if (response.success) {
        setIsSubscribed(true);
        newsletterService.storeSubscription(email, source);
        newsletterService.trackSignup(email, source);
        toast.success(response.message || 'Successfully subscribed to newsletter!');
        return true;
      } else {
        toast.error(response.message || 'Failed to subscribe');
        return false;
      }
    } catch (error: any) {
      console.error('Newsletter subscription error:', error);
      toast.error(error.message || 'Failed to subscribe. Please try again.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  /**
   * Subscribe from footer
   */
  const subscribeFromFooter = useCallback(async (email: string): Promise<boolean> => {
    if (!newsletterService.isValidEmail(email)) {
      toast.error('Please enter a valid email address');
      return false;
    }

    setIsLoading(true);
    try {
      const request: FooterSubscribeRequest = {
        email,
        userId: user?.id,
      };

      const response: SubscriptionResponse = await newsletterService.subscribeFromFooter(request);
      
      if (response.success) {
        setIsSubscribed(true);
        newsletterService.storeSubscription(email, 'footer');
        newsletterService.trackSignup(email, 'footer');
        toast.success(response.message || 'Successfully subscribed to newsletter!');
        return true;
      } else {
        toast.error(response.message || 'Failed to subscribe');
        return false;
      }
    } catch (error: any) {
      console.error('Footer newsletter subscription error:', error);
      toast.error(error.message || 'Failed to subscribe. Please try again.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  /**
   * Subscribe from popup
   */
  const subscribeFromPopup = useCallback(async (email: string): Promise<boolean> => {
    if (!newsletterService.isValidEmail(email)) {
      toast.error('Please enter a valid email address');
      return false;
    }

    setIsLoading(true);
    try {
      const utmParams = newsletterService.getUtmParameters();
      const request: PopupSubscribeRequest = {
        email,
        userId: user?.id,
        ...utmParams,
      };

      const response: SubscriptionResponse = await newsletterService.subscribeFromPopup(request);
      
      if (response.success) {
        setIsSubscribed(true);
        newsletterService.storeSubscription(email, 'popup');
        newsletterService.trackSignup(email, 'popup');
        toast.success(response.message || 'Successfully subscribed to newsletter!');
        return true;
      } else {
        toast.error(response.message || 'Failed to subscribe');
        return false;
      }
    } catch (error: any) {
      console.error('Popup newsletter subscription error:', error);
      toast.error(error.message || 'Failed to subscribe. Please try again.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  /**
   * Unsubscribe from newsletter
   */
  const unsubscribe = useCallback(async (email: string): Promise<boolean> => {
    if (!newsletterService.isValidEmail(email)) {
      toast.error('Please enter a valid email address');
      return false;
    }

    setIsLoading(true);
    try {
      const response = await newsletterService.unsubscribe(email);
      
      if (response.success) {
        setIsSubscribed(false);
        newsletterService.clearStoredSubscription();
        toast.success(response.message || 'Successfully unsubscribed');
        return true;
      } else {
        toast.error(response.message || 'Failed to unsubscribe');
        return false;
      }
    } catch (error: any) {
      console.error('Newsletter unsubscribe error:', error);
      toast.error(error.message || 'Failed to unsubscribe. Please try again.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Check subscription status
   */
  const checkSubscriptionStatus = useCallback(async (email: string): Promise<void> => {
    if (!newsletterService.isValidEmail(email)) {
      return;
    }

    try {
      const status = await newsletterService.getSubscriptionStatus(email);
      setSubscriptionStatus(status);
      setIsSubscribed(status.subscribed);
    } catch (error: any) {
      console.error('Failed to check subscription status:', error);
    }
  }, []);

  /**
   * Load subscription statistics (admin only)
   */
  const loadStats = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    try {
      const statsData = await newsletterService.getSubscriptionStats();
      setStats(statsData);
    } catch (error: any) {
      console.error('Failed to load newsletter stats:', error);
      toast.error('Failed to load newsletter statistics');
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Load recent subscribers (admin only)
   */
  const loadRecentSubscribers = useCallback(async (limit: number = 10): Promise<void> => {
    setIsLoading(true);
    try {
      const subscribers = await newsletterService.getRecentSubscribers(limit);
      setRecentSubscribers(subscribers);
    } catch (error: any) {
      console.error('Failed to load recent subscribers:', error);
      toast.error('Failed to load recent subscribers');
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Add tag to subscriber
   */
  const addTag = useCallback(async (email: string, tag: string): Promise<boolean> => {
    if (!newsletterService.isValidEmail(email) || !tag.trim()) {
      toast.error('Please provide valid email and tag');
      return false;
    }

    setIsLoading(true);
    try {
      const response = await newsletterService.addTag(email, tag.trim());
      
      if (response.status === 'success') {
        toast.success(response.message || 'Tag added successfully');
        return true;
      } else {
        toast.error(response.message || 'Failed to add tag');
        return false;
      }
    } catch (error: any) {
      console.error('Failed to add tag:', error);
      toast.error(error.message || 'Failed to add tag');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Remove tag from subscriber
   */
  const removeTag = useCallback(async (email: string, tag: string): Promise<boolean> => {
    if (!newsletterService.isValidEmail(email) || !tag.trim()) {
      toast.error('Please provide valid email and tag');
      return false;
    }

    setIsLoading(true);
    try {
      const response = await newsletterService.removeTag(email, tag.trim());
      
      if (response.status === 'success') {
        toast.success(response.message || 'Tag removed successfully');
        return true;
      } else {
        toast.error(response.message || 'Failed to remove tag');
        return false;
      }
    } catch (error: any) {
      console.error('Failed to remove tag:', error);
      toast.error(error.message || 'Failed to remove tag');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Validate email format
   */
  const validateEmail = useCallback((email: string): boolean => {
    return newsletterService.isValidEmail(email);
  }, []);

  /**
   * Check if user has already subscribed
   */
  const hasSubscribed = useCallback((): boolean => {
    return newsletterService.hasSubscribed();
  }, []);

  /**
   * Clear stored subscription
   */
  const clearStoredSubscription = useCallback((): void => {
    newsletterService.clearStoredSubscription();
    setIsSubscribed(false);
  }, []);

  return {
    // State
    isLoading,
    isSubscribed,
    subscriptionStatus,
    stats,
    recentSubscribers,
    
    // Actions
    subscribe,
    subscribeFromFooter,
    subscribeFromPopup,
    unsubscribe,
    checkSubscriptionStatus,
    loadStats,
    loadRecentSubscribers,
    addTag,
    removeTag,
    
    // Utilities
    validateEmail,
    hasSubscribed,
    clearStoredSubscription,
  };
}

export default useNewsletter;
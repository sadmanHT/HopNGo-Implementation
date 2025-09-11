'use client';

import { useState, useEffect, useCallback } from 'react';
import { referralService, ReferralStats, ReferralResponse } from '@/lib/services/referral';
import { useAuth } from '@/hooks/use-auth';
import { toast } from '@/hooks/use-toast';

interface UseReferralReturn {
  // State
  stats: ReferralStats | null;
  referrals: ReferralResponse[];
  loading: boolean;
  
  // Actions
  createReferral: (campaign?: string) => Promise<ReferralResponse | null>;
  trackConversion: (conversionType: 'signup' | 'subscription' | 'booking' | 'purchase') => Promise<void>;
  refreshData: () => Promise<void>;
  
  // Utilities
  generateReferralUrl: (referralCode: string) => string;
  getStoredReferralCode: () => string | null;
  initializeTracking: () => Promise<void>;
}

export function useReferral(): UseReferralReturn {
  const { user } = useAuth();
  const [stats, setStats] = useState<ReferralStats | null>(null);
  const [referrals, setReferrals] = useState<ReferralResponse[]>([]);
  const [loading, setLoading] = useState(false);

  // Load referral data when user is available
  useEffect(() => {
    if (user?.id) {
      refreshData();
    }
  }, [user?.id]);

  // Initialize referral tracking on mount
  useEffect(() => {
    initializeTracking();
  }, []);

  const refreshData = useCallback(async () => {
    if (!user?.id) return;

    setLoading(true);
    try {
      const [statsResponse, referralsResponse] = await Promise.all([
        referralService.getReferralStats(user.id),
        referralService.getUserReferrals(user.id)
      ]);

      if (statsResponse.success) {
        setStats(statsResponse.data);
      } else {
        console.error('Failed to load referral stats:', statsResponse.error);
      }

      if (referralsResponse.success) {
        setReferrals(referralsResponse.data);
      } else {
        console.error('Failed to load referrals:', referralsResponse.error);
      }
    } catch (error) {
      console.error('Error refreshing referral data:', error);
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  const createReferral = useCallback(async (campaign = 'default'): Promise<ReferralResponse | null> => {
    if (!user?.id) {
      toast({
        title: 'Authentication Required',
        description: 'Please log in to create a referral code.',
        variant: 'destructive',
      });
      return null;
    }

    try {
      const response = await referralService.createReferral({
        userId: user.id,
        campaign
      });

      if (response.success) {
        toast({
          title: 'Success',
          description: 'Referral code created successfully!',
        });
        
        // Refresh data to include the new referral
        await refreshData();
        
        return response.data;
      } else {
        toast({
          title: 'Error',
          description: response.error || 'Failed to create referral code',
          variant: 'destructive',
        });
        return null;
      }
    } catch (error) {
      console.error('Error creating referral:', error);
      toast({
        title: 'Error',
        description: 'An unexpected error occurred',
        variant: 'destructive',
      });
      return null;
    }
  }, [user?.id, refreshData]);

  const trackConversion = useCallback(async (conversionType: 'signup' | 'subscription' | 'booking' | 'purchase') => {
    if (!user?.id) return;

    // Get stored referral code
    const referralCode = referralService.getStoredReferralCode();
    if (!referralCode) return;

    try {
      const response = await referralService.processConversion({
        referralCode,
        newUserId: user.id,
        conversionType
      });

      if (response.success) {
        // Clear stored referral code after successful conversion
        referralService.clearStoredReferralCode();
        
        // Show success message
        toast({
          title: 'Referral Bonus!',
          description: `Thanks for using a referral link! You and your friend have earned rewards.`,
        });
        
        // Refresh data if this user also has referrals
        await refreshData();
      }
    } catch (error) {
      console.error('Error tracking conversion:', error);
    }
  }, [user?.id, refreshData]);

  const generateReferralUrl = useCallback((referralCode: string): string => {
    return referralService.generateReferralUrl(referralCode);
  }, []);

  const getStoredReferralCode = useCallback((): string | null => {
    return referralService.getStoredReferralCode();
  }, []);

  const initializeTracking = useCallback(async (): Promise<void> => {
    try {
      await referralService.initializeReferralTracking();
    } catch (error) {
      console.error('Error initializing referral tracking:', error);
    }
  }, []);

  return {
    // State
    stats,
    referrals,
    loading,
    
    // Actions
    createReferral,
    trackConversion,
    refreshData,
    
    // Utilities
    generateReferralUrl,
    getStoredReferralCode,
    initializeTracking,
  };
}

export default useReferral;
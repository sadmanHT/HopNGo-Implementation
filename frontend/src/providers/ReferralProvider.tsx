'use client';

import React, { createContext, useContext, useEffect, ReactNode } from 'react';
import { useReferral } from '@/hooks/use-referral';
import { useAuth } from '@/hooks/use-auth';
import { ReferralStats, ReferralResponse } from '@/lib/services/referral';

interface ReferralContextType {
  stats: ReferralStats | null;
  referrals: ReferralResponse[];
  loading: boolean;
  createReferral: (campaign?: string) => Promise<ReferralResponse | null>;
  trackConversion: (conversionType: 'signup' | 'subscription' | 'booking' | 'purchase') => Promise<void>;
  refreshData: () => Promise<void>;
  generateReferralUrl: (referralCode: string) => string;
  getStoredReferralCode: () => string | null;
}

const ReferralContext = createContext<ReferralContextType | undefined>(undefined);

interface ReferralProviderProps {
  children: ReactNode;
}

export function ReferralProvider({ children }: ReferralProviderProps) {
  const { user, isAuthenticated } = useAuth();
  const referralHook = useReferral();

  // Auto-track signup conversion when user first authenticates
  useEffect(() => {
    if (isAuthenticated && user?.id) {
      // Check if this is a new user (you might want to add a flag for this)
      const hasTrackedSignup = localStorage.getItem('hopngo_signup_tracked');
      
      if (!hasTrackedSignup) {
        // Track signup conversion
        referralHook.trackConversion('signup');
        localStorage.setItem('hopngo_signup_tracked', 'true');
      }
    }
  }, [isAuthenticated, user?.id, referralHook]);

  return (
    <ReferralContext.Provider value={referralHook}>
      {children}
    </ReferralContext.Provider>
  );
}

export function useReferralContext(): ReferralContextType {
  const context = useContext(ReferralContext);
  if (context === undefined) {
    throw new Error('useReferralContext must be used within a ReferralProvider');
  }
  return context;
}

export default ReferralProvider;
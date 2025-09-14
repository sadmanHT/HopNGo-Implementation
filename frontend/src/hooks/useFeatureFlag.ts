import { useState, useEffect } from 'react';

// Feature flags configuration
const FEATURE_FLAGS = {
  ADMIN_PANEL: true,
  GEAR_MANAGEMENT: true,
  PAYMENT_PROCESSING: true,
  ANALYTICS: true,
  NOTIFICATIONS: true,
  DARK_MODE: true,
  BETA_FEATURES: false,
} as const;

type FeatureFlag = keyof typeof FEATURE_FLAGS;

/**
 * Hook to check if a feature flag is enabled
 * @param flag - The feature flag to check
 * @returns boolean indicating if the feature is enabled or object with initializeFlags and flags
 */
export function useFeatureFlag(): { initializeFlags: (userId: string) => void; flags: typeof FEATURE_FLAGS };
export function useFeatureFlag(flag: FeatureFlag): boolean;
export function useFeatureFlag(flag?: FeatureFlag) {
  const [flags, setFlags] = useState(FEATURE_FLAGS);
  
  const initializeFlags = (userId: string) => {
    // In a real application, you might fetch user-specific feature flags from an API
    console.log(`Initializing feature flags for user: ${userId}`);
    setFlags(FEATURE_FLAGS);
  };
  
  if (flag) {
    return flags[flag];
  }
  
  return { initializeFlags, flags };
}

/**
 * Hook to get all feature flags
 * @returns object with all feature flags and their states
 */
export const useFeatureFlags = () => {
  const [flags, setFlags] = useState(FEATURE_FLAGS);

  useEffect(() => {
    // In a real application, you might fetch all flags from an API
    setFlags(FEATURE_FLAGS);
  }, []);

  return flags;
};

/**
 * Hook to check multiple feature flags at once
 * @param flagList - Array of feature flags to check
 * @returns object with flag names as keys and boolean values
 */
export const useMultipleFeatureFlags = (flagList: FeatureFlag[]) => {
  const [flagStates, setFlagStates] = useState<Record<FeatureFlag, boolean>>({} as Record<FeatureFlag, boolean>);

  useEffect(() => {
    const states = flagList.reduce((acc, flag) => {
      acc[flag] = FEATURE_FLAGS[flag];
      return acc;
    }, {} as Record<FeatureFlag, boolean>);
    
    setFlagStates(states);
  }, [flagList]);

  return flagStates;
};
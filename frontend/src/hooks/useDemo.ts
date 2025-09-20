import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { useAuthStore } from '../lib/state/auth';
import { User } from '../lib/api/types';

// Demo user accounts
export const DEMO_USERS = {
  traveler: {
    id: 'demo-traveler-001',
    email: 'demo.traveler@hopngo.com',
    firstName: 'Alex',
    lastName: 'Explorer',
    name: 'Alex Explorer',
    role: 'CUSTOMER' as const,
    avatar: '/images/demo/traveler-avatar.jpg',
    verified: true,
    isVerified: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    preferences: {
      currency: 'BDT',
      language: 'en',
      notifications: true,
      theme: 'light'
    },
    stats: {
      tripsCompleted: 12,
      reviewsGiven: 8,
      wishlistItems: 24,
      loyaltyPoints: 1250
    }
  },
  provider: {
    id: 'demo-provider-001',
    email: 'demo.provider@hopngo.com',
    firstName: 'Maya',
    lastName: 'Host',
    name: 'Maya Host',
    role: 'VENDOR' as const,
    avatar: '/images/demo/provider-avatar.jpg',
    verified: true,
    isVerified: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    businessInfo: {
      businessName: 'Srimangal Tea Garden Resort',
      businessType: 'Accommodation',
      location: 'Srimangal, Sylhet, Bangladesh',
      rating: 4.8,
      totalReviews: 156
    },
    stats: {
      totalBookings: 342,
      revenue: 125000,
      averageRating: 4.8,
      responseTime: '< 1 hour'
    }
  }
};

// Demo tokens (mock JWT-like tokens for demo purposes)
const DEMO_TOKENS = {
  traveler: 'demo.traveler.token.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vLXRyYXZlbGVyLTAwMSIsImVtYWlsIjoiZGVtby50cmF2ZWxlckBob3BuZ28uY29tIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjk5OTk5OTk5OTl9',
  provider: 'demo.provider.token.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZW1vLXByb3ZpZGVyLTAwMSIsImVtYWlsIjoiZGVtby5wcm92aWRlckBob3BuZ28uY29tIiwicm9sZSI6IlZFTkRPUiIsImlhdCI6MTcwNDA2NzIwMCwiZXhwIjo5OTk5OTk5OTk5fQ'
};

const DEMO_REFRESH_TOKENS = {
  traveler: 'demo.traveler.refresh.token.12345',
  provider: 'demo.provider.refresh.token.67890'
};

export interface DemoState {
  isDemoMode: boolean;
  demoUser: 'traveler' | 'provider' | null;
  isInitialized: boolean;
}

export const useDemo = () => {
  const router = useRouter();
  const { setAuth, clearAuth, isAuthenticated } = useAuthStore();
  const [isClient, setIsClient] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);
  const [demoState, setDemoState] = useState<DemoState>({
    isDemoMode: false,
    demoUser: null,
    isInitialized: false
  });

  // Check for demo mode from URL params or headers
  const checkDemoMode = () => {
    // Check URL query parameter
    const urlParams = new URLSearchParams(window.location.search);
    const demoParam = urlParams.get('demo');
    
    // Check for demo header (useful for API testing)
    const demoHeader = document.querySelector('meta[name="demo-mode"]')?.getAttribute('content');
    
    // Check localStorage for persistent demo mode
    const storedDemoMode = localStorage.getItem('hopngo-demo-mode');
    
    return demoParam === '1' || demoHeader === '1' || storedDemoMode === 'true';
  };

  // Get demo user type from URL or default to traveler
  const getDemoUserType = (): 'traveler' | 'provider' => {
    const urlParams = new URLSearchParams(window.location.search);
    const userType = urlParams.get('demo-user');
    return userType === 'provider' ? 'provider' : 'traveler';
  };

  // Initialize demo mode
  const initializeDemoMode = () => {
    const isDemoMode = checkDemoMode();
    
    if (isDemoMode) {
      const demoUserType = getDemoUserType();
      const demoUser = DEMO_USERS[demoUserType];
      const demoToken = DEMO_TOKENS[demoUserType];
      const demoRefreshToken = DEMO_REFRESH_TOKENS[demoUserType];

      // Set demo mode in localStorage for persistence
      localStorage.setItem('hopngo-demo-mode', 'true');
      localStorage.setItem('hopngo-demo-user-type', demoUserType);
      
      // Auto-authenticate with demo user
      setAuth(demoUser as User, demoToken, demoRefreshToken);
      
      setDemoState({
        isDemoMode: true,
        demoUser: demoUserType,
        isInitialized: true
      });

      // Add demo indicator to document
      document.body.classList.add('demo-mode');
      
      // Show demo notification
      if (typeof window !== 'undefined' && window.location.search.includes('demo=1')) {
        setTimeout(() => {
          console.log(`🎭 Demo Mode Active - Logged in as ${demoUser.name} (${demoUserType})`);
        }, 1000);
      }
    } else {
      // Clear demo mode
      localStorage.removeItem('hopngo-demo-mode');
      localStorage.removeItem('hopngo-demo-user-type');
      document.body.classList.remove('demo-mode');
      
      setDemoState({
        isDemoMode: false,
        demoUser: null,
        isInitialized: true
      });
    }
  };

  // Exit demo mode
  const exitDemoMode = () => {
    localStorage.removeItem('hopngo-demo-mode');
    localStorage.removeItem('hopngo-demo-user-type');
    document.body.classList.remove('demo-mode');
    clearAuth();
    
    setDemoState({
      isDemoMode: false,
      demoUser: null,
      isInitialized: true
    });

    // Remove demo params from URL
    if (isClient) {
      const url = new URL(window.location.href);
      url.searchParams.delete('demo');
      url.searchParams.delete('demo-user');
      router.replace(url.pathname + url.search, undefined, { shallow: true });
    }
  };

  // Switch demo user type
  const switchDemoUser = (userType: 'traveler' | 'provider') => {
    if (!demoState.isDemoMode) return;
    
    const demoUser = DEMO_USERS[userType];
    const demoToken = DEMO_TOKENS[userType];
    const demoRefreshToken = DEMO_REFRESH_TOKENS[userType];
    
    localStorage.setItem('hopngo-demo-user-type', userType);
    setAuth(demoUser as User, demoToken, demoRefreshToken);
    
    setDemoState(prev => ({
      ...prev,
      demoUser: userType
    }));
  };

  // Initialize on mount and URL changes
  useEffect(() => {
    initializeDemoMode();
  }, [router.asPath]);

  // Listen for demo mode changes
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'hopngo-demo-mode') {
        initializeDemoMode();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  return {
    ...demoState,
    exitDemoMode,
    switchDemoUser,
    initializeDemoMode,
    demoUsers: DEMO_USERS
  };
};

// Demo mode utilities
export const isDemoMode = () => {
  if (typeof window === 'undefined') return false;
  return localStorage.getItem('hopngo-demo-mode') === 'true' || 
         new URLSearchParams(window.location.search).get('demo') === '1';
};

export const getDemoUserType = (): 'traveler' | 'provider' | null => {
  if (!isDemoMode()) return null;
  return (localStorage.getItem('hopngo-demo-user-type') as 'traveler' | 'provider') || 'traveler';
};

// Demo data flag for API calls
export const shouldUseDemoData = () => {
  return isDemoMode();
};
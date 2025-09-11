import { useState, useEffect, useCallback } from 'react';
import { offlineWallet } from '@/services/offlineWallet';
import { useQuery, useQueryClient } from '@tanstack/react-query';

interface OfflineStats {
  itineraries: number;
  bookings: number;
  tickets: number;
  lastSync: string | null;
}

interface UseOfflineWalletReturn {
  // Data
  itineraries: any[];
  bookings: any[];
  tickets: any[];
  stats: OfflineStats | null;
  
  // State
  isLoading: boolean;
  isSyncing: boolean;
  isOnline: boolean;
  
  // Methods
  syncWithServer: (apiClient: any, userId: string) => Promise<void>;
  getItinerary: (id: string) => Promise<any | undefined>;
  getBooking: (id: string) => Promise<any | undefined>;
  getTicket: (id: string) => Promise<any | undefined>;
  getBookingsForItinerary: (itineraryId: string) => Promise<any[]>;
  getTicketsForBooking: (bookingId: string) => Promise<any[]>;
  clearAllData: () => Promise<void>;
  refreshData: () => void;
}

export function useOfflineWallet(): UseOfflineWalletReturn {
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [isOnline, setIsOnline] = useState(typeof navigator !== 'undefined' ? navigator.onLine : true);
  const queryClient = useQueryClient();

  // Monitor online/offline status
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Fetch offline data
  const { data: itineraries = [], refetch: refetchItineraries } = useQuery({
    queryKey: ['offline-itineraries'],
    queryFn: async () => {
      try {
        return await offlineWallet.getItineraries();
      } catch (error) {
        console.error('Failed to fetch offline itineraries:', error);
        return [];
      }
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  const { data: bookings = [], refetch: refetchBookings } = useQuery({
    queryKey: ['offline-bookings'],
    queryFn: async () => {
      try {
        return await offlineWallet.getBookings();
      } catch (error) {
        console.error('Failed to fetch offline bookings:', error);
        return [];
      }
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  const { data: tickets = [], refetch: refetchTickets } = useQuery({
    queryKey: ['offline-tickets'],
    queryFn: async () => {
      try {
        return await offlineWallet.getTickets();
      } catch (error) {
        console.error('Failed to fetch offline tickets:', error);
        return [];
      }
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  const { data: stats = null, refetch: refetchStats } = useQuery({
    queryKey: ['offline-stats'],
    queryFn: async () => {
      try {
        return await offlineWallet.getOfflineStats();
      } catch (error) {
        console.error('Failed to fetch offline stats:', error);
        return null;
      }
    },
    staleTime: 1000 * 60 * 1, // 1 minute
  });

  // Initialize offline wallet
  useEffect(() => {
    const initWallet = async () => {
      try {
        await offlineWallet.init();
        setIsLoading(false);
      } catch (error) {
        console.error('Failed to initialize offline wallet:', error);
        setIsLoading(false);
      }
    };

    initWallet();
  }, []);

  // Sync with server
  const syncWithServer = useCallback(async (apiClient: any, userId: string) => {
    if (!isOnline) {
      throw new Error('Cannot sync while offline');
    }

    setIsSyncing(true);
    try {
      await offlineWallet.syncWithServer(apiClient, userId);
      
      // Refresh all queries after sync
      await Promise.all([
        refetchItineraries(),
        refetchBookings(),
        refetchTickets(),
        refetchStats(),
      ]);
      
      // Also invalidate related queries in the main cache
      queryClient.invalidateQueries({ queryKey: ['itineraries'] });
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      
    } catch (error) {
      console.error('Sync failed:', error);
      throw error;
    } finally {
      setIsSyncing(false);
    }
  }, [isOnline, refetchItineraries, refetchBookings, refetchTickets, refetchStats, queryClient]);

  // Individual item getters
  const getItinerary = useCallback(async (id: string) => {
    return await offlineWallet.getItinerary(id);
  }, []);

  const getBooking = useCallback(async (id: string) => {
    return await offlineWallet.getBooking(id);
  }, []);

  const getTicket = useCallback(async (id: string) => {
    return await offlineWallet.getTicket(id);
  }, []);

  const getBookingsForItinerary = useCallback(async (itineraryId: string) => {
    return await offlineWallet.getBookings(itineraryId);
  }, []);

  const getTicketsForBooking = useCallback(async (bookingId: string) => {
    return await offlineWallet.getTickets(bookingId);
  }, []);

  // Clear all data
  const clearAllData = useCallback(async () => {
    try {
      await offlineWallet.clearAllData();
      
      // Refresh all queries after clearing
      await Promise.all([
        refetchItineraries(),
        refetchBookings(),
        refetchTickets(),
        refetchStats(),
      ]);
    } catch (error) {
      console.error('Failed to clear offline data:', error);
      throw error;
    }
  }, [refetchItineraries, refetchBookings, refetchTickets, refetchStats]);

  // Refresh all data
  const refreshData = useCallback(() => {
    refetchItineraries();
    refetchBookings();
    refetchTickets();
    refetchStats();
  }, [refetchItineraries, refetchBookings, refetchTickets, refetchStats]);

  return {
    // Data
    itineraries,
    bookings,
    tickets,
    stats,
    
    // State
    isLoading,
    isSyncing,
    isOnline,
    
    // Methods
    syncWithServer,
    getItinerary,
    getBooking,
    getTicket,
    getBookingsForItinerary,
    getTicketsForBooking,
    clearAllData,
    refreshData,
  };
}

export default useOfflineWallet;
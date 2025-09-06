import { useState, useEffect, useRef, useCallback } from 'react';
import { bookingsApi, BookingStatusResponse } from '@/lib/api/bookings';

export interface BookingPollingOptions {
  bookingId: string;
  enabled?: boolean;
  interval?: number; // milliseconds
  maxAttempts?: number;
  onStatusChange?: (status: BookingStatusResponse) => void;
  onSuccess?: (status: BookingStatusResponse) => void;
  onFailure?: (status: BookingStatusResponse) => void;
  onTimeout?: () => void;
}

export interface BookingPollingState {
  status: BookingStatusResponse | null;
  isPolling: boolean;
  error: string | null;
  attempts: number;
}

export function useBookingStatusPolling(options: BookingPollingOptions) {
  const {
    bookingId,
    enabled = true,
    interval = 2000, // 2 seconds
    maxAttempts = 150, // 5 minutes with 2s interval
    onStatusChange,
    onSuccess,
    onFailure,
    onTimeout
  } = options;

  const [state, setState] = useState<BookingPollingState>({
    status: null,
    isPolling: false,
    error: null,
    attempts: 0
  });

  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const attemptsRef = useRef(0);

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setState(prev => ({ ...prev, isPolling: false }));
  }, []);

  const startPolling = useCallback(async () => {
    if (!enabled || !bookingId) return;

    setState(prev => ({ ...prev, isPolling: true, error: null }));
    attemptsRef.current = 0;

    const poll = async () => {
      try {
        attemptsRef.current += 1;
        setState(prev => ({ ...prev, attempts: attemptsRef.current }));

        const response = await bookingsApi.getBookingStatus(bookingId);
        
        if (response.success && response.data) {
          const status = response.data;
          setState(prev => ({ ...prev, status, error: null }));
          
          // Call status change callback
          onStatusChange?.(status);

          // Check if we should stop polling
          if (status.status === 'confirmed') {
            onSuccess?.(status);
            stopPolling();
            return;
          }
          
          if (status.status === 'cancelled') {
            onFailure?.(status);
            stopPolling();
            return;
          }

          // Check if we've reached max attempts
          if (attemptsRef.current >= maxAttempts) {
            onTimeout?.();
            stopPolling();
            return;
          }
        } else {
          throw new Error(response.message || 'Failed to fetch booking status');
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Unknown error';
        setState(prev => ({ ...prev, error: errorMessage }));
        
        // Continue polling on error unless we've reached max attempts
        if (attemptsRef.current >= maxAttempts) {
          onTimeout?.();
          stopPolling();
        }
      }
    };

    // Initial poll
    await poll();

    // Set up interval for subsequent polls
    intervalRef.current = setInterval(poll, interval);
  }, [bookingId, enabled, interval, maxAttempts, onStatusChange, onSuccess, onFailure, onTimeout, stopPolling]);

  // Start polling when enabled and bookingId changes
  useEffect(() => {
    if (enabled && bookingId) {
      startPolling();
    } else {
      stopPolling();
    }

    return () => {
      stopPolling();
    };
  }, [enabled, bookingId, startPolling, stopPolling]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, [stopPolling]);

  return {
    ...state,
    startPolling,
    stopPolling,
    isComplete: state.status?.status === 'confirmed' || state.status?.status === 'cancelled',
    isSuccess: state.status?.status === 'confirmed',
    isFailure: state.status?.status === 'cancelled'
  };
}
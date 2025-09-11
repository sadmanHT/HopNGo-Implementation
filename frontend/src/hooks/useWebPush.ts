import { useState, useEffect, useCallback } from 'react';
import { webPushService, type NotificationPayload } from '@/services/webPush';

// VAPID public key - in production, this should come from environment variables
const VAPID_PUBLIC_KEY = process.env.NEXT_PUBLIC_VAPID_KEY || 'BEl62iUYgUivxIkv69yViEuiBIa40HcCWLWw-ixdPiAahsh_JnXRbNfQ-FYXkddkFz-VYBtxaLwGH2_L88G18xQ';

interface UseWebPushReturn {
  // State
  isSupported: boolean;
  isSubscribed: boolean;
  permission: NotificationPermission;
  isLoading: boolean;
  error: string | null;
  
  // Methods
  requestPermission: () => Promise<NotificationPermission>;
  subscribe: (userId: string) => Promise<void>;
  unsubscribe: (userId: string) => Promise<void>;
  showTestNotification: (userId: string) => Promise<void>;
  registerWithBackend: (apiClient: any, userId: string) => Promise<void>;
  unregisterFromBackend: (apiClient: any, userId: string) => Promise<void>;
}

export function useWebPush(): UseWebPushReturn {
  const [isSupported, setIsSupported] = useState(false);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [permission, setPermission] = useState<NotificationPermission>('default');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Check browser support and initialize
  useEffect(() => {
    const checkSupport = async () => {
      try {
        const supported = 'serviceWorker' in navigator && 
                         'PushManager' in window && 
                         'Notification' in window;
        
        setIsSupported(supported);
        
        if (supported) {
          // Initialize web push service
          await webPushService.init();
          
          // Check current permission
          setPermission(Notification.permission);
          
          // Check if already subscribed
          const subscribed = await webPushService.isSubscribed();
          setIsSubscribed(subscribed);
        }
      } catch (err) {
        console.error('Failed to initialize web push:', err);
        setError(err instanceof Error ? err.message : 'Failed to initialize web push');
      } finally {
        setIsLoading(false);
      }
    };

    checkSupport();
  }, []);

  // Request notification permission
  const requestPermission = useCallback(async (): Promise<NotificationPermission> => {
    try {
      setError(null);
      const newPermission = await webPushService.requestPermission();
      setPermission(newPermission);
      
      // Initialize web push service after permission granted
       if (newPermission === 'granted') {
         await webPushService.init();
       }
      
      return newPermission;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to request permission';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  }, []);

  // Subscribe to push notifications
  const subscribe = useCallback(async (userId: string): Promise<void> => {
    try {
      setError(null);
      setIsLoading(true);
      
      const subscription = await webPushService.subscribe();
       if (subscription) {
         await webPushService.registerSubscription(userId);
         setIsSubscribed(true);
         setPermission('granted');
       }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to subscribe';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Unsubscribe from push notifications
  const unsubscribe = useCallback(async (userId: string): Promise<void> => {
    try {
      setError(null);
      setIsLoading(true);
      
      await webPushService.unregisterSubscription(userId);
      setIsSubscribed(false);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to unsubscribe';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Show test notification
  const showTestNotification = useCallback(async (userId: string): Promise<void> => {
    try {
      setError(null);
      setIsLoading(true);
      
      const response = await fetch('/api/v1/notify/webpush/test', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId })
      });
      
      if (!response.ok) {
        throw new Error('Failed to send test notification');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to show test notification';
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);



  // Register with backend (placeholder implementation)
  const registerWithBackend = useCallback(async (apiClient: any, userId: string): Promise<void> => {
    // Implementation would use apiClient to register with backend
    console.log('Register with backend:', userId);
  }, []);

  // Unregister from backend (placeholder implementation)
  const unregisterFromBackend = useCallback(async (apiClient: any, userId: string): Promise<void> => {
    // Implementation would use apiClient to unregister from backend
    console.log('Unregister from backend:', userId);
  }, []);

  return {
    // State
    isSupported,
    isSubscribed,
    permission,
    isLoading,
    error,
    
    // Methods
    requestPermission,
    subscribe,
    unsubscribe,
    showTestNotification,
    registerWithBackend,
    unregisterFromBackend,
  };
}

export default useWebPush;
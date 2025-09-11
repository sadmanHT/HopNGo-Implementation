interface PushSubscriptionData {
  endpoint: string;
  keys: {
    p256dh: string;
    auth: string;
  };
}

interface NotificationPayload {
  title: string;
  body: string;
  icon?: string;
  badge?: string;
  image?: string;
  tag?: string;
  data?: any;
  actions?: Array<{
    action: string;
    title: string;
    icon?: string;
  }>;
}

class WebPushService {
  private vapidPublicKey: string = '';
  private serviceWorkerRegistration: ServiceWorkerRegistration | null = null;

  /**
   * Get VAPID public key from backend
   */
  private async getVapidPublicKey(): Promise<string> {
    try {
      const response = await fetch('/api/v1/notify/webpush/vapid-public-key');
      const data = await response.json();
      return data.publicKey;
    } catch (error) {
      console.error('Failed to get VAPID public key:', error);
      // Fallback key for development
      return 'BKxTk7ZbIjkldBt-zxekFWBD-XiPSQHlIDQyNjnW9NJ8QFlFNlBxv_77VJMLcv8Gb9c_8zXcmBqPvVaOq0-Ck8s';
    }
  }

  async init(): Promise<void> {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      throw new Error('Web Push is not supported in this browser');
    }

    try {
      // Get VAPID public key from backend
      this.vapidPublicKey = await this.getVapidPublicKey();
      
      this.serviceWorkerRegistration = await navigator.serviceWorker.ready;
      console.log('Service Worker ready for push notifications');
    } catch (error) {
      console.error('Service Worker not available:', error);
      throw new Error('Service Worker required for push notifications');
    }
  }

  async requestPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      throw new Error('This browser does not support notifications');
    }

    let permission = Notification.permission;

    if (permission === 'default') {
      permission = await Notification.requestPermission();
    }

    return permission;
  }

  async subscribe(): Promise<PushSubscriptionData | null> {
    if (!this.serviceWorkerRegistration) {
      throw new Error('Service Worker not initialized');
    }

    const permission = await this.requestPermission();
    if (permission !== 'granted') {
      throw new Error('Push notification permission denied');
    }

    try {
      const subscription = await this.serviceWorkerRegistration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(this.vapidPublicKey) as BufferSource,
      });

      const subscriptionData: PushSubscriptionData = {
        endpoint: subscription.endpoint,
        keys: {
          p256dh: this.arrayBufferToBase64(subscription.getKey('p256dh')!),
          auth: this.arrayBufferToBase64(subscription.getKey('auth')!),
        },
      };

      return subscriptionData;
    } catch (error) {
      console.error('Failed to subscribe to push notifications:', error);
      throw error;
    }
  }

  async unsubscribe(): Promise<boolean> {
    if (!this.serviceWorkerRegistration) {
      return false;
    }

    try {
      const subscription = await this.serviceWorkerRegistration.pushManager.getSubscription();
      if (subscription) {
        return await subscription.unsubscribe();
      }
      return true;
    } catch (error) {
      console.error('Failed to unsubscribe from push notifications:', error);
      return false;
    }
  }

  async getSubscription(): Promise<PushSubscriptionData | null> {
    if (!this.serviceWorkerRegistration) {
      return null;
    }

    try {
      const subscription = await this.serviceWorkerRegistration.pushManager.getSubscription();
      if (!subscription) {
        return null;
      }

      return {
        endpoint: subscription.endpoint,
        keys: {
          p256dh: this.arrayBufferToBase64(subscription.getKey('p256dh')!),
          auth: this.arrayBufferToBase64(subscription.getKey('auth')!),
        },
      };
    } catch (error) {
      console.error('Failed to get push subscription:', error);
      return null;
    }
  }

  async isSubscribed(): Promise<boolean> {
    const subscription = await this.getSubscription();
    return subscription !== null;
  }

  // Show local notification (for testing)
  async showNotification(payload: NotificationPayload): Promise<void> {
    if (!this.serviceWorkerRegistration) {
      throw new Error('Service Worker not initialized');
    }

    const permission = await this.requestPermission();
    if (permission !== 'granted') {
      throw new Error('Notification permission not granted');
    }

    const options: NotificationOptions = {
      body: payload.body,
      icon: payload.icon || '/icons/icon-192x192.svg',
      badge: payload.badge || '/icons/icon-72x72.svg',
      tag: payload.tag,
      data: payload.data,
      requireInteraction: true,
      silent: false,
    };

    await this.serviceWorkerRegistration.showNotification(payload.title, options);
  }

  // Register push subscription with backend
  async registerSubscription(userId: string): Promise<void> {
    const subscriptionData = await this.subscribe();
    if (!subscriptionData) {
      throw new Error('Failed to create push subscription');
    }

    try {
      const response = await fetch('/api/v1/notify/webpush/subscribe', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId,
          endpoint: subscriptionData.endpoint,
          p256dh: subscriptionData.keys.p256dh,
          auth: subscriptionData.keys.auth,
        })
      });

      if (!response.ok) {
        throw new Error('Failed to register subscription with backend');
      }

      console.log('Push subscription registered successfully');
    } catch (error) {
      console.error('Failed to register push subscription:', error);
      throw error;
    }
  }

  // Unregister push subscription from backend
  async unregisterSubscription(userId: string): Promise<void> {
    const subscriptionData = await this.getSubscription();
    if (!subscriptionData) {
      return; // Already unsubscribed
    }

    try {
      const params = new URLSearchParams({ 
        userId, 
        endpoint: subscriptionData.endpoint 
      });
      
      const response = await fetch(`/api/v1/notify/webpush/unsubscribe?${params}`, {
        method: 'POST'
      });

      if (!response.ok) {
        console.warn('Failed to unregister subscription from backend');
      }

      await this.unsubscribe();
      console.log('Push subscription unregistered successfully');
    } catch (error) {
      console.error('Failed to unregister push subscription:', error);
      throw error;
    }
  }

  // Utility methods
  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const buffer = new ArrayBuffer(rawData.length);
    const outputArray = new Uint8Array(buffer);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }

    return outputArray;
  }

  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }
}

// Export singleton instance
export const webPushService = new WebPushService();
export default webPushService;
export type { PushSubscriptionData, NotificationPayload };
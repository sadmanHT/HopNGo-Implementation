import { webPushService } from './webPush';

/**
 * Service to integrate web push notifications with app events
 */
export class PushNotificationIntegration {
  private static instance: PushNotificationIntegration;
  private isOnline: boolean = navigator.onLine;
  private userId: string | null = null;

  private constructor() {
    this.setupOnlineStatusListeners();
  }

  static getInstance(): PushNotificationIntegration {
    if (!PushNotificationIntegration.instance) {
      PushNotificationIntegration.instance = new PushNotificationIntegration();
    }
    return PushNotificationIntegration.instance;
  }

  /**
   * Initialize the integration with user ID
   */
  init(userId: string): void {
    this.userId = userId;
  }

  /**
   * Setup listeners for online/offline status
   */
  private setupOnlineStatusListeners(): void {
    window.addEventListener('online', () => {
      this.isOnline = true;
      console.log('App is online - push notifications may be less critical');
    });

    window.addEventListener('offline', () => {
      this.isOnline = false;
      console.log('App is offline - push notifications are critical');
    });
  }

  /**
   * Handle chat message events
   */
  async handleChatMessage(data: {
    senderId: string;
    senderName: string;
    message: string;
    chatId: string;
  }): Promise<void> {
    // Only send push notification if user is offline or app is in background
    if (this.shouldSendPushNotification()) {
      try {
        await this.sendChatNotification(data);
      } catch (error) {
        console.error('Failed to send chat push notification:', error);
      }
    }
  }

  /**
   * Handle booking confirmation events
   */
  async handleBookingConfirmed(data: {
    bookingId: string;
    tripName: string;
    departureDate: string;
    totalAmount: number;
  }): Promise<void> {
    // Always send booking confirmations as they are critical
    try {
      await this.sendBookingNotification(data);
    } catch (error) {
      console.error('Failed to send booking push notification:', error);
    }
  }

  /**
   * Send chat notification via backend
   */
  private async sendChatNotification(data: {
    senderId: string;
    senderName: string;
    message: string;
    chatId: string;
  }): Promise<void> {
    if (!this.userId) {
      console.warn('User ID not set for push notifications');
      return;
    }

    const response = await fetch('/api/v1/notify/webpush/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: this.userId,
        title: `New message from ${data.senderName}`,
        body: data.message.length > 100 ? 
          `${data.message.substring(0, 100)}...` : 
          data.message,
        icon: '/icons/icon-192x192.svg',
        badge: '/icons/badge-72x72.svg',
        url: `/chat/${data.chatId}`,
        tag: `chat-${data.chatId}`,
        data: {
          type: 'chat',
          chatId: data.chatId,
          senderId: data.senderId
        }
      })
    });

    if (!response.ok) {
      throw new Error('Failed to send chat notification');
    }
  }

  /**
   * Send booking notification via backend
   */
  private async sendBookingNotification(data: {
    bookingId: string;
    tripName: string;
    departureDate: string;
    totalAmount: number;
  }): Promise<void> {
    if (!this.userId) {
      console.warn('User ID not set for push notifications');
      return;
    }

    const response = await fetch('/api/v1/notify/webpush/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: this.userId,
        title: 'Booking Confirmed! 🎉',
        body: `Your trip "${data.tripName}" has been confirmed for ${new Date(data.departureDate).toLocaleDateString()}`,
        icon: '/icons/icon-192x192.svg',
        badge: '/icons/badge-72x72.svg',
        url: `/bookings/${data.bookingId}`,
        tag: `booking-${data.bookingId}`,
        data: {
          type: 'booking',
          bookingId: data.bookingId,
          amount: data.totalAmount
        }
      })
    });

    if (!response.ok) {
      throw new Error('Failed to send booking notification');
    }
  }

  /**
   * Determine if push notification should be sent
   */
  private shouldSendPushNotification(): boolean {
    // Send if offline
    if (!this.isOnline) {
      return true;
    }

    // Send if document is hidden (app in background)
    if (document.hidden) {
      return true;
    }

    // Don't send if app is active and online
    return false;
  }

  /**
   * Setup WebSocket listeners for real-time events
   */
  setupWebSocketListeners(socket: WebSocket): void {
    socket.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(event.data);
        
        switch (data.type) {
          case 'chat.message.created':
            this.handleChatMessage(data.payload);
            break;
          case 'booking.confirmed':
            this.handleBookingConfirmed(data.payload);
            break;
          default:
            // Ignore other message types
            break;
        }
      } catch (error) {
        console.error('Error processing WebSocket message:', error);
      }
    });
  }

  /**
   * Setup event listeners for custom events
   */
  setupEventListeners(): void {
    // Listen for custom chat events
    window.addEventListener('hopngo:chat:message', (event: any) => {
      this.handleChatMessage(event.detail);
    });

    // Listen for custom booking events
    window.addEventListener('hopngo:booking:confirmed', (event: any) => {
      this.handleBookingConfirmed(event.detail);
    });
  }
}

// Export singleton instance
export const pushNotificationIntegration = PushNotificationIntegration.getInstance();
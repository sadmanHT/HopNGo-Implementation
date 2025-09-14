// Notification service for handling user notifications

class NotificationService {
  constructor() {
    this.notifications = [];
    this.listeners = [];
    this.maxNotifications = 50;
    
    // Load persisted notifications from localStorage
    this.loadNotifications();
  }

  // Add a new notification
  addNotification(notification) {
    const newNotification = {
      id: this.generateId(),
      timestamp: new Date().toISOString(),
      read: false,
      ...notification
    };
    
    this.notifications.unshift(newNotification);
    
    // Keep only the latest notifications
    if (this.notifications.length > this.maxNotifications) {
      this.notifications = this.notifications.slice(0, this.maxNotifications);
    }
    
    this.saveNotifications();
    this.notifyListeners();
    
    return newNotification;
  }

  // Booking confirmation notification
  notifyBookingConfirmed(bookingData) {
    return this.addNotification({
      type: 'booking_confirmed',
      title: 'Booking Confirmed!',
      message: `Your ${bookingData.type} booking for "${bookingData.itemName}" has been confirmed.`,
      data: {
        bookingId: bookingData.id,
        confirmationNumber: bookingData.confirmationNumber,
        type: bookingData.type,
        itemName: bookingData.itemName,
        amount: bookingData.amount
      },
      priority: 'high',
      category: 'booking',
      actions: [
        {
          label: 'View Details',
          action: 'view_booking',
          url: `/booking-confirmation?bookingId=${bookingData.id}`
        }
      ]
    });
  }

  // Payment success notification
  notifyPaymentSuccess(paymentData) {
    return this.addNotification({
      type: 'payment_success',
      title: 'Payment Successful',
      message: `Payment of $${paymentData.amount} has been processed successfully.`,
      data: {
        paymentId: paymentData.paymentId,
        amount: paymentData.amount,
        bookingId: paymentData.bookingId
      },
      priority: 'medium',
      category: 'payment'
    });
  }

  // Trip planning notification
  notifyTripPlanGenerated(tripData) {
    return this.addNotification({
      type: 'trip_plan_generated',
      title: 'Trip Itinerary Ready!',
      message: `Your personalized itinerary for ${tripData.destination} is ready to view.`,
      data: {
        destination: tripData.destination,
        startDate: tripData.startDate,
        endDate: tripData.endDate,
        budget: tripData.budget
      },
      priority: 'medium',
      category: 'trip_planning',
      actions: [
        {
          label: 'View Itinerary',
          action: 'view_itinerary',
          url: '/trip-planning'
        }
      ]
    });
  }

  // Service error notification
  notifyServiceError(errorData) {
    return this.addNotification({
      type: 'service_error',
      title: 'Service Issue',
      message: errorData.message || 'We encountered an issue with one of our services.',
      data: {
        service: errorData.service,
        errorType: errorData.type,
        context: errorData.context
      },
      priority: 'low',
      category: 'system',
      autoHide: true,
      hideAfter: 10000 // Hide after 10 seconds
    });
  }

  // Welcome notification for new users
  notifyWelcome(userData) {
    return this.addNotification({
      type: 'welcome',
      title: 'Welcome to HopNGo!',
      message: `Hi ${userData.name || 'there'}! Start planning your next adventure with our AI-powered travel assistant.`,
      data: {
        userId: userData.id,
        isNewUser: true
      },
      priority: 'medium',
      category: 'onboarding',
      actions: [
        {
          label: 'Plan Your First Trip',
          action: 'start_planning',
          url: '/trip-planning'
        }
      ]
    });
  }

  // System maintenance notification
  notifyMaintenance(maintenanceData) {
    return this.addNotification({
      type: 'maintenance',
      title: 'Scheduled Maintenance',
      message: `Some services will be temporarily unavailable on ${new Date(maintenanceData.scheduledTime).toLocaleDateString()}.`,
      data: {
        scheduledTime: maintenanceData.scheduledTime,
        duration: maintenanceData.duration,
        affectedServices: maintenanceData.affectedServices
      },
      priority: 'medium',
      category: 'system',
      persistent: true // Don't auto-hide
    });
  }

  // Mark notification as read
  markAsRead(notificationId) {
    const notification = this.notifications.find(n => n.id === notificationId);
    if (notification) {
      notification.read = true;
      this.saveNotifications();
      this.notifyListeners();
    }
  }

  // Mark all notifications as read
  markAllAsRead() {
    this.notifications.forEach(notification => {
      notification.read = true;
    });
    this.saveNotifications();
    this.notifyListeners();
  }

  // Remove a notification
  removeNotification(notificationId) {
    this.notifications = this.notifications.filter(n => n.id !== notificationId);
    this.saveNotifications();
    this.notifyListeners();
  }

  // Clear all notifications
  clearAll() {
    this.notifications = [];
    this.saveNotifications();
    this.notifyListeners();
  }

  // Get all notifications
  getNotifications() {
    return [...this.notifications];
  }

  // Get unread notifications
  getUnreadNotifications() {
    return this.notifications.filter(n => !n.read);
  }

  // Get notifications by category
  getNotificationsByCategory(category) {
    return this.notifications.filter(n => n.category === category);
  }

  // Get unread count
  getUnreadCount() {
    return this.notifications.filter(n => !n.read).length;
  }

  // Subscribe to notification changes
  subscribe(listener) {
    this.listeners.push(listener);
    
    // Return unsubscribe function
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  // Notify all listeners of changes
  notifyListeners() {
    this.listeners.forEach(listener => {
      try {
        listener({
          notifications: this.getNotifications(),
          unreadCount: this.getUnreadCount()
        });
      } catch (error) {
        console.error('Error notifying listener:', error);
      }
    });
  }

  // Generate unique ID
  generateId() {
    return `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  // Save notifications to localStorage
  saveNotifications() {
    try {
      localStorage.setItem('hopngo_notifications', JSON.stringify(this.notifications));
    } catch (error) {
      console.warn('Could not save notifications to localStorage:', error);
    }
  }

  // Load notifications from localStorage
  loadNotifications() {
    try {
      const saved = localStorage.getItem('hopngo_notifications');
      if (saved) {
        this.notifications = JSON.parse(saved);
        
        // Clean up old notifications (older than 30 days)
        const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
        this.notifications = this.notifications.filter(n => 
          new Date(n.timestamp) > thirtyDaysAgo
        );
      }
    } catch (error) {
      console.warn('Could not load notifications from localStorage:', error);
      this.notifications = [];
    }
  }

  // Request browser notification permission
  async requestPermission() {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }
    return false;
  }

  // Show browser notification
  showBrowserNotification(notification) {
    if ('Notification' in window && Notification.permission === 'granted') {
      const browserNotification = new Notification(notification.title, {
        body: notification.message,
        icon: '/favicon.ico',
        badge: '/favicon.ico',
        tag: notification.id,
        requireInteraction: notification.priority === 'high'
      });

      // Auto-close after 5 seconds unless it's high priority
      if (notification.priority !== 'high') {
        setTimeout(() => {
          browserNotification.close();
        }, 5000);
      }

      // Handle click events
      browserNotification.onclick = () => {
        window.focus();
        if (notification.actions && notification.actions[0]) {
          const action = notification.actions[0];
          if (action.url) {
            window.location.href = action.url;
          }
        }
        browserNotification.close();
      };

      return browserNotification;
    }
    return null;
  }

  // Show toast notification (for in-app display)
  showToast(notification) {
    // This would integrate with a toast component
    const event = new CustomEvent('show-toast', {
      detail: {
        id: notification.id,
        title: notification.title,
        message: notification.message,
        type: this.getToastType(notification.priority),
        duration: notification.autoHide ? (notification.hideAfter || 5000) : null,
        actions: notification.actions
      }
    });
    
    window.dispatchEvent(event);
  }

  // Get toast type based on priority
  getToastType(priority) {
    switch (priority) {
      case 'high': return 'error';
      case 'medium': return 'info';
      case 'low': return 'warning';
      default: return 'info';
    }
  }

  // Process notification for display
  processNotification(notification, showBrowser = false, showToast = true) {
    if (showBrowser) {
      this.showBrowserNotification(notification);
    }
    
    if (showToast) {
      this.showToast(notification);
    }
    
    return notification;
  }
}

// Create singleton instance
const notificationService = new NotificationService();

// Export both the class and instance
export { NotificationService };
export default notificationService;
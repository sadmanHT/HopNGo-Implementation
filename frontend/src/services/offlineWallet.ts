import { openDB, DBSchema, IDBPDatabase } from 'idb';

// Define the database schema
interface OfflineWalletDB extends DBSchema {
  itineraries: {
    key: string;
    value: {
      id: string;
      title: string;
      description: string;
      destinations: any[];
      startDate: string;
      endDate: string;
      totalCost: number;
      currency: string;
      status: string;
      createdAt: string;
      updatedAt: string;
      syncedAt: string;
    };
    indexes: { 'by-status': string; 'by-date': string };
  };
  bookings: {
    key: string;
    value: {
      id: string;
      itineraryId: string;
      type: 'accommodation' | 'transport' | 'activity';
      title: string;
      description: string;
      startDate: string;
      endDate: string;
      cost: number;
      currency: string;
      status: 'pending' | 'confirmed' | 'cancelled';
      confirmationCode?: string;
      vendorInfo: any;
      createdAt: string;
      updatedAt: string;
      syncedAt: string;
    };
    indexes: { 'by-itinerary': string; 'by-status': string; 'by-type': string };
  };
  tickets: {
    key: string;
    value: {
      id: string;
      bookingId: string;
      ticketNumber: string;
      qrCode?: string;
      barcode?: string;
      passengerName: string;
      seatNumber?: string;
      gateNumber?: string;
      departureTime: string;
      arrivalTime?: string;
      status: 'valid' | 'used' | 'expired' | 'cancelled';
      metadata: any;
      createdAt: string;
      updatedAt: string;
      syncedAt: string;
    };
    indexes: { 'by-booking': string; 'by-status': string; 'by-passenger': string };
  };
  syncQueue: {
    key: string;
    value: {
      id: string;
      operation: 'create' | 'update' | 'delete';
      table: 'itineraries' | 'bookings' | 'tickets';
      recordId: string;
      data: any;
      timestamp: string;
      retryCount: number;
    };
    indexes: { 'by-table': string; 'by-timestamp': string };
  };
}

class OfflineWalletService {
  private db: IDBPDatabase<OfflineWalletDB> | null = null;
  private readonly DB_NAME = 'HopNGoOfflineWallet';
  private readonly DB_VERSION = 1;

  async init(): Promise<void> {
    if (this.db) return;

    this.db = await openDB<OfflineWalletDB>(this.DB_NAME, this.DB_VERSION, {
      upgrade(db) {
        // Create itineraries store
        const itinerariesStore = db.createObjectStore('itineraries', {
          keyPath: 'id',
        });
        itinerariesStore.createIndex('by-status', 'status');
        itinerariesStore.createIndex('by-date', 'startDate');

        // Create bookings store
        const bookingsStore = db.createObjectStore('bookings', {
          keyPath: 'id',
        });
        bookingsStore.createIndex('by-itinerary', 'itineraryId');
        bookingsStore.createIndex('by-status', 'status');
        bookingsStore.createIndex('by-type', 'type');

        // Create tickets store
        const ticketsStore = db.createObjectStore('tickets', {
          keyPath: 'id',
        });
        ticketsStore.createIndex('by-booking', 'bookingId');
        ticketsStore.createIndex('by-status', 'status');
        ticketsStore.createIndex('by-passenger', 'passengerName');

        // Create sync queue store
        const syncQueueStore = db.createObjectStore('syncQueue', {
          keyPath: 'id',
        });
        syncQueueStore.createIndex('by-table', 'table');
        syncQueueStore.createIndex('by-timestamp', 'timestamp');
      },
    });
  }

  // Itineraries methods
  async saveItinerary(itinerary: any): Promise<void> {
    await this.init();
    const now = new Date().toISOString();
    const data = {
      ...itinerary,
      syncedAt: now,
      updatedAt: itinerary.updatedAt || now,
    };
    await this.db!.put('itineraries', data);
  }

  async getItineraries(): Promise<any[]> {
    await this.init();
    return await this.db!.getAll('itineraries');
  }

  async getItinerary(id: string): Promise<any | undefined> {
    await this.init();
    return await this.db!.get('itineraries', id);
  }

  async deleteItinerary(id: string): Promise<void> {
    await this.init();
    await this.db!.delete('itineraries', id);
  }

  // Bookings methods
  async saveBooking(booking: any): Promise<void> {
    await this.init();
    const now = new Date().toISOString();
    const data = {
      ...booking,
      syncedAt: now,
      updatedAt: booking.updatedAt || now,
    };
    await this.db!.put('bookings', data);
  }

  async getBookings(itineraryId?: string): Promise<any[]> {
    await this.init();
    if (itineraryId) {
      return await this.db!.getAllFromIndex('bookings', 'by-itinerary', itineraryId);
    }
    return await this.db!.getAll('bookings');
  }

  async getBooking(id: string): Promise<any | undefined> {
    await this.init();
    return await this.db!.get('bookings', id);
  }

  async deleteBooking(id: string): Promise<void> {
    await this.init();
    await this.db!.delete('bookings', id);
  }

  // Tickets methods
  async saveTicket(ticket: any): Promise<void> {
    await this.init();
    const now = new Date().toISOString();
    const data = {
      ...ticket,
      syncedAt: now,
      updatedAt: ticket.updatedAt || now,
    };
    await this.db!.put('tickets', data);
  }

  async getTickets(bookingId?: string): Promise<any[]> {
    await this.init();
    if (bookingId) {
      return await this.db!.getAllFromIndex('tickets', 'by-booking', bookingId);
    }
    return await this.db!.getAll('tickets');
  }

  async getTicket(id: string): Promise<any | undefined> {
    await this.init();
    return await this.db!.get('tickets', id);
  }

  async deleteTicket(id: string): Promise<void> {
    await this.init();
    await this.db!.delete('tickets', id);
  }

  // Sync methods
  async syncWithServer(apiClient: any, userId: string): Promise<void> {
    await this.init();
    
    try {
      // Fetch recent data from server
      const [itineraries, bookings, tickets] = await Promise.all([
        apiClient.get(`/api/v1/users/${userId}/itineraries?limit=50`),
        apiClient.get(`/api/v1/users/${userId}/bookings?limit=100`),
        apiClient.get(`/api/v1/users/${userId}/tickets?limit=100`),
      ]);

      // Store in IndexedDB
      const tx = this.db!.transaction(['itineraries', 'bookings', 'tickets'], 'readwrite');
      
      // Clear existing data
      await Promise.all([
        tx.objectStore('itineraries').clear(),
        tx.objectStore('bookings').clear(),
        tx.objectStore('tickets').clear(),
      ]);

      // Save new data
      const now = new Date().toISOString();
      
      for (const itinerary of itineraries.data || []) {
        await tx.objectStore('itineraries').put({ ...itinerary, syncedAt: now });
      }
      
      for (const booking of bookings.data || []) {
        await tx.objectStore('bookings').put({ ...booking, syncedAt: now });
      }
      
      for (const ticket of tickets.data || []) {
        await tx.objectStore('tickets').put({ ...ticket, syncedAt: now });
      }

      await tx.done;
      
      console.log('Offline wallet synced successfully');
    } catch (error) {
      console.error('Failed to sync offline wallet:', error);
      throw error;
    }
  }

  async getOfflineStats(): Promise<{
    itineraries: number;
    bookings: number;
    tickets: number;
    lastSync: string | null;
  }> {
    await this.init();
    
    const [itineraries, bookings, tickets] = await Promise.all([
      this.db!.getAll('itineraries'),
      this.db!.getAll('bookings'),
      this.db!.getAll('tickets'),
    ]);

    // Find the most recent sync time
    const allItems = [...itineraries, ...bookings, ...tickets];
    const lastSync = allItems.reduce((latest, item) => {
      const syncTime = item.syncedAt;
      return !latest || (syncTime && syncTime > latest) ? syncTime : latest;
    }, null as string | null);

    return {
      itineraries: itineraries.length,
      bookings: bookings.length,
      tickets: tickets.length,
      lastSync,
    };
  }

  async clearAllData(): Promise<void> {
    await this.init();
    const tx = this.db!.transaction(['itineraries', 'bookings', 'tickets', 'syncQueue'], 'readwrite');
    await Promise.all([
      tx.objectStore('itineraries').clear(),
      tx.objectStore('bookings').clear(),
      tx.objectStore('tickets').clear(),
      tx.objectStore('syncQueue').clear(),
    ]);
    await tx.done;
  }
}

// Export singleton instance
export const offlineWallet = new OfflineWalletService();
export default offlineWallet;
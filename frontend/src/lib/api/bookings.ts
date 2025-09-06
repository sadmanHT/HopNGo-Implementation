import { apiClient } from './client';
import { ApiResponse, Booking } from './types';

export interface CreateBookingRequest {
  serviceType: 'transport' | 'accommodation' | 'activity';
  title: string;
  description: string;
  price: number;
  currency: string;
  startDate: string;
  endDate: string;
  metadata?: Record<string, any>;
}

export interface BookingStatusResponse {
  id: string;
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed';
  paymentStatus?: 'pending' | 'processing' | 'succeeded' | 'failed';
  updatedAt: string;
}

class BookingsApi {
  async createBooking(request: CreateBookingRequest): Promise<ApiResponse<Booking>> {
    return apiClient.post('/bookings', request);
  }

  async getBooking(id: string): Promise<ApiResponse<Booking>> {
    return apiClient.get(`/bookings/${id}`);
  }

  async getBookingStatus(id: string): Promise<ApiResponse<BookingStatusResponse>> {
    return apiClient.get(`/bookings/${id}/status`);
  }

  async getUserBookings(page = 0, size = 20): Promise<ApiResponse<Booking[]>> {
    return apiClient.get(`/bookings?page=${page}&size=${size}`);
  }

  async cancelBooking(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete(`/bookings/${id}`);
  }
}

export const bookingsApi = new BookingsApi();
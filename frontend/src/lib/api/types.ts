// Common API response types
export interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  message?: string;
  errors?: string[];
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

// Auth types
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  isVerified?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  user: User;
  token: string;
  refreshToken: string;
}

// Booking types
export interface Booking {
  id: string;
  userId: string;
  serviceType: 'transport' | 'accommodation' | 'activity';
  title: string;
  description: string;
  price: number;
  currency: string;
  startDate: string;
  endDate: string;
  status: 'pending' | 'confirmed' | 'cancelled' | 'completed';
  createdAt: string;
  updatedAt: string;
}

export interface CancellationRequest {
  reason: string;
}

export interface CancellationResponse {
  bookingId: string;
  bookingReference: string;
  status: string;
  refundAmount: number;
  refundStatus: string;
  cancellationReason: string;
  cancelledAt: string;
}

// Social types
export interface Post {
  id: string;
  userId: string;
  user: User;
  content: string;
  images?: string[];
  location?: {
    name: string;
    coordinates: [number, number];
  };
  likes: number;
  comments: number;
  createdAt: string;
  updatedAt: string;
}

// Market types
export interface MarketItem {
  id: string;
  sellerId: string;
  seller: User;
  title: string;
  description: string;
  price: number;
  currency: string;
  category: string;
  images: string[];
  condition: 'new' | 'used' | 'refurbished';
  location: string;
  status: 'available' | 'sold' | 'reserved';
  createdAt: string;
  updatedAt: string;
}

// Chat types
export interface ChatRoom {
  id: string;
  participants: User[];
  lastMessage?: Message;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  chatRoomId: string;
  senderId: string;
  sender: User;
  content: string;
  type: 'text' | 'image' | 'file';
  createdAt: string;
}

// Trip types
export interface Trip {
  id: string;
  userId: string;
  user: User;
  title: string;
  description: string;
  destination: string;
  startDate: string;
  endDate: string;
  budget?: number;
  currency?: string;
  participants: User[];
  itinerary: TripDay[];
  status: 'planning' | 'active' | 'completed' | 'cancelled';
  createdAt: string;
  updatedAt: string;
}

export interface TripDay {
  id: string;
  tripId: string;
  date: string;
  activities: Activity[];
}

export interface Activity {
  id: string;
  title: string;
  description: string;
  startTime: string;
  endTime: string;
  location: string;
  cost?: number;
}
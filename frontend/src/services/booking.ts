export interface SearchParams extends Record<string, string> {
  location: string;
  checkIn: string;
  checkOut: string;
  guests: string;
  category: string;
}

export interface Vendor {
  id: string;
  businessName: string;
  rating: number;
}

export interface Availability {
  available: boolean;
  pricePerNight: number;
  totalPrice: number;
}

export interface Listing {
  id: string;
  title: string;
  description: string;
  category: string;
  basePrice: number;
  currency: string;
  maxGuests: number;
  address: string;
  latitude: number;
  longitude: number;
  amenities: string[];
  images: string[];
  vendor: Vendor;
  availability: Availability;
}

export interface SearchResponse {
  content: Listing[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface BookingRequest {
  listingId: string;
  startDate: string;
  endDate: string;
  numberOfGuests: number;
  specialRequests?: string | null;
}

export interface Booking {
  id: string;
  listingId: string;
  userId: string;
  startDate: string;
  endDate: string;
  numberOfGuests: number;
  totalPrice: number;
  status: string;
  specialRequests?: string;
  createdAt: string;
}

export class BookingService {
  constructor(private baseUrl: string) {}

  async searchListings(params: SearchParams): Promise<SearchResponse> {
    const queryString = new URLSearchParams(params).toString();
    const response = await fetch(`${this.baseUrl}/api/v1/listings/search?${queryString}`, {
      method: 'GET',
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to search listings');
    }

    return response.json();
  }

  async createBooking(request: BookingRequest, userId: string): Promise<Booking> {
    const response = await fetch(`${this.baseUrl}/api/v1/bookings`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-ID': userId,
        'X-User-Role': 'CUSTOMER',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create booking');
    }

    return response.json();
  }
}
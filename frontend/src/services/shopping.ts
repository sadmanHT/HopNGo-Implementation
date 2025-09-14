import axios, { AxiosResponse } from 'axios';

// Types for marketplace functionality
export interface TravelGear {
  id: string;
  title: string;
  description: string;
  category: 'gear' | 'equipment' | 'accessories';
  type: 'rent' | 'sale' | 'both';
  price: number;
  rentPrice?: number;
  currency: string;
  brand: string;
  condition: 'new' | 'like-new' | 'good' | 'fair';
  images: string[];
  specifications: Record<string, any>;
  availability: {
    inStock: boolean;
    quantity: number;
    availableDates?: string[];
  };
  location: {
    city: string;
    country: string;
    coordinates?: { lat: number; lng: number };
  };
  seller: {
    id: string;
    name: string;
    avatar?: string;
    rating: number;
    reviewCount: number;
  };
  ratings: {
    average: number;
    count: number;
    breakdown: { [key: number]: number };
  };
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CartItem {
  id: string;
  gearId: string;
  gear: TravelGear;
  quantity: number;
  type: 'rent' | 'purchase';
  rentDuration?: {
    startDate: string;
    endDate: string;
    days: number;
  };
  price: number;
  totalPrice: number;
  addedAt: string;
}

export interface Cart {
  id: string;
  userId: string;
  items: CartItem[];
  totalItems: number;
  subtotal: number;
  tax: number;
  shipping: number;
  total: number;
  currency: string;
  updatedAt: string;
}

export interface Order {
  id: string;
  userId: string;
  items: CartItem[];
  status: 'pending' | 'confirmed' | 'processing' | 'shipped' | 'delivered' | 'cancelled' | 'returned';
  payment: {
    method: string;
    status: 'pending' | 'completed' | 'failed' | 'refunded';
    transactionId?: string;
    amount: number;
  };
  shipping: {
    address: {
      street: string;
      city: string;
      state: string;
      zipCode: string;
      country: string;
    };
    method: string;
    trackingNumber?: string;
    estimatedDelivery?: string;
  };
  totals: {
    subtotal: number;
    tax: number;
    shipping: number;
    total: number;
  };
  createdAt: string;
  updatedAt: string;
}

export interface Review {
  id: string;
  userId: string;
  gearId: string;
  orderId: string;
  rating: number;
  title: string;
  comment: string;
  images?: string[];
  helpful: number;
  verified: boolean;
  createdAt: string;
  user: {
    name: string;
    avatar?: string;
  };
}

export interface MarketplaceFilters {
  category?: string[];
  type?: 'rent' | 'sale' | 'both';
  priceRange?: { min: number; max: number };
  condition?: string[];
  location?: string;
  availability?: boolean;
  rating?: number;
  tags?: string[];
  sortBy?: 'price' | 'rating' | 'newest' | 'popular';
  sortOrder?: 'asc' | 'desc';
}

export interface SearchResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  hasMore: boolean;
  filters: {
    categories: { name: string; count: number }[];
    priceRange: { min: number; max: number };
    conditions: { name: string; count: number }[];
    locations: { name: string; count: number }[];
  };
}

class ShoppingService {
  private baseURL: string;
  private axiosInstance;

  constructor(baseURL: string = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080') {
    this.baseURL = baseURL;
    this.axiosInstance = axios.create({
      baseURL: `${baseURL}/api/v1`,
      timeout: 10000,
    });

    // Add auth token to requests
    this.axiosInstance.interceptors.request.use((config) => {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }

  // Travel Gear Listings
  async searchGear(
    query?: string,
    filters?: MarketplaceFilters,
    page: number = 0,
    limit: number = 20
  ): Promise<SearchResponse<TravelGear>> {
    const params = {
      q: query,
      page,
      limit,
      ...filters,
    };

    const response: AxiosResponse<SearchResponse<TravelGear>> = await this.axiosInstance.get(
      '/marketplace/gear/search',
      { params }
    );
    return response.data;
  }

  async getGearById(id: string): Promise<TravelGear> {
    try {
      const response: AxiosResponse<TravelGear> = await this.axiosInstance.get(
        `/marketplace/gear/${id}`
      );
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        throw new Error('Gear item not found');
      }
      throw error;
    }
  }

  // Admin: Create new gear
  async createGear(gearData: Partial<TravelGear>): Promise<TravelGear> {
    const response: AxiosResponse<TravelGear> = await this.axiosInstance.post(
      '/marketplace/admin/gear',
      gearData
    );
    return response.data;
  }

  // Admin: Update existing gear
  async updateGear(id: string, gearData: Partial<TravelGear>): Promise<TravelGear> {
    const response: AxiosResponse<TravelGear> = await this.axiosInstance.put(
      `/marketplace/admin/gear/${id}`,
      gearData
    );
    return response.data;
  }

  // Admin: Delete gear
  async deleteGear(id: string): Promise<void> {
    await this.axiosInstance.delete(`/marketplace/admin/gear/${id}`);
  }

  async getFeaturedGear(limit: number = 10): Promise<TravelGear[]> {
    const response: AxiosResponse<TravelGear[]> = await this.axiosInstance.get(
      '/marketplace/gear/featured',
      { params: { limit } }
    );
    return response.data;
  }

  async getGearByCategory(category: string, limit: number = 20): Promise<TravelGear[]> {
    const response: AxiosResponse<TravelGear[]> = await this.axiosInstance.get(
      `/marketplace/gear/category/${category}`,
      { params: { limit } }
    );
    return response.data;
  }

  // Cart Management
  async getCart(): Promise<Cart> {
    const response: AxiosResponse<Cart> = await this.axiosInstance.get('/marketplace/cart');
    return response.data;
  }

  async addToCart(item: {
    gearId: string;
    quantity: number;
    type: 'rent' | 'purchase';
    rentDuration?: { startDate: string; endDate: string };
  }): Promise<Cart> {
    try {
      const response: AxiosResponse<Cart> = await this.axiosInstance.post(
        '/marketplace/cart/items',
        item
      );
      return response.data;
    } catch (error: any) {
      // Handle inventory conflicts
      if (error.response?.status === 409) {
        throw new Error(error.response.data?.message || 'Item is no longer available in the requested quantity');
      }
      
      // Handle validation errors
      if (error.response?.status === 400) {
        throw new Error(error.response.data?.message || 'Invalid item configuration');
      }
      
      // Handle authentication errors
      if (error.response?.status === 401) {
        throw new Error('Please log in to add items to cart');
      }
      
      throw error;
    }
  }

  async updateCartItem(itemId: string, updates: {
    quantity?: number;
    rentDuration?: { startDate: string; endDate: string };
  }): Promise<Cart> {
    try {
      const response: AxiosResponse<Cart> = await this.axiosInstance.patch(
        `/marketplace/cart/items/${itemId}`,
        updates
      );
      return response.data;
    } catch (error: any) {
      // Handle inventory conflicts
      if (error.response?.status === 409) {
        throw new Error(error.response.data?.message || 'Requested quantity is no longer available');
      }
      
      // Handle validation errors
      if (error.response?.status === 400) {
        throw new Error(error.response.data?.message || 'Invalid update parameters');
      }
      
      // Handle not found errors
      if (error.response?.status === 404) {
        throw new Error('Cart item not found');
      }
      
      throw error;
    }
  }

  async removeFromCart(itemId: string): Promise<Cart> {
    const response: AxiosResponse<Cart> = await this.axiosInstance.delete(
      `/marketplace/cart/items/${itemId}`
    );
    return response.data;
  }

  async clearCart(): Promise<void> {
    await this.axiosInstance.delete('/marketplace/cart');
  }

  // Order Management
  async createOrder(orderData: {
    cartId: string;
    shipping: {
      address: {
        street: string;
        city: string;
        state: string;
        zipCode: string;
        country: string;
      };
      method: string;
    };
    payment: {
      method: string;
      cardToken?: string;
    };
  }): Promise<Order> {
    const response: AxiosResponse<Order> = await this.axiosInstance.post(
      '/marketplace/orders',
      orderData
    );
    return response.data;
  }

  async getOrder(orderId: string): Promise<Order> {
    const response: AxiosResponse<Order> = await this.axiosInstance.get(
      `/marketplace/orders/${orderId}`
    );
    return response.data;
  }

  async getUserOrders(page: number = 0, limit: number = 20): Promise<SearchResponse<Order>> {
    const response: AxiosResponse<SearchResponse<Order>> = await this.axiosInstance.get(
      '/marketplace/orders',
      { params: { page, limit } }
    );
    return response.data;
  }

  async cancelOrder(orderId: string, reason?: string): Promise<Order> {
    const response: AxiosResponse<Order> = await this.axiosInstance.patch(
      `/marketplace/orders/${orderId}/cancel`,
      { reason }
    );
    return response.data;
  }

  // Reviews and Ratings
  async getGearReviews(gearId: string, page: number = 0, limit: number = 10): Promise<SearchResponse<Review>> {
    const response: AxiosResponse<SearchResponse<Review>> = await this.axiosInstance.get(
      `/marketplace/gear/${gearId}/reviews`,
      { params: { page, limit } }
    );
    return response.data;
  }

  async createReview(reviewData: {
    gearId: string;
    orderId: string;
    rating: number;
    title: string;
    comment: string;
    images?: string[];
  }): Promise<Review> {
    const response: AxiosResponse<Review> = await this.axiosInstance.post(
      '/marketplace/reviews',
      reviewData
    );
    return response.data;
  }

  async updateReview(reviewId: string, updates: {
    rating?: number;
    title?: string;
    comment?: string;
    images?: string[];
  }): Promise<Review> {
    const response: AxiosResponse<Review> = await this.axiosInstance.patch(
      `/marketplace/reviews/${reviewId}`,
      updates
    );
    return response.data;
  }

  async deleteReview(reviewId: string): Promise<void> {
    await this.axiosInstance.delete(`/marketplace/reviews/${reviewId}`);
  }

  async markReviewHelpful(reviewId: string): Promise<Review> {
    const response: AxiosResponse<Review> = await this.axiosInstance.post(
      `/marketplace/reviews/${reviewId}/helpful`
    );
    return response.data;
  }

  // Inventory and Availability
  async checkAvailability(gearId: string, dates?: { startDate: string; endDate: string }): Promise<{
    available: boolean;
    quantity: number;
    nextAvailableDate?: string;
  }> {
    const response = await this.axiosInstance.get(
      `/marketplace/gear/${gearId}/availability`,
      { params: dates }
    );
    return response.data;
  }

  // Payment Processing
  async processPayment(paymentData: {
    orderId: string;
    amount: number;
    paymentMethod: {
      type: 'card' | 'paypal' | 'bank';
      cardNumber?: string;
      expiryDate?: string;
      cardholderName?: string;
      paypalEmail?: string;
    };
    billingAddress: {
      street: string;
      city: string;
      state: string;
      zipCode: string;
      country: string;
    };
  }): Promise<{
    success: boolean;
    paymentId: string;
    transactionId?: string;
    error?: string;
  }> {
    const response = await this.axiosInstance.post('/marketplace/payments/process', paymentData);
    return response.data;
  }

  // Analytics and Recommendations
  async getRecommendations(gearId?: string, limit: number = 10): Promise<TravelGear[]> {
    const response: AxiosResponse<TravelGear[]> = await this.axiosInstance.get(
      '/marketplace/recommendations',
      { params: { gearId, limit } }
    );
    return response.data;
  }

  async trackView(gearId: string): Promise<void> {
    await this.axiosInstance.post(`/marketplace/gear/${gearId}/view`);
  }

  async getPopularGear(timeframe: 'day' | 'week' | 'month' = 'week', limit: number = 10): Promise<TravelGear[]> {
    const response: AxiosResponse<TravelGear[]> = await this.axiosInstance.get(
      '/marketplace/gear/popular',
      { params: { timeframe, limit } }
    );
    return response.data;
  }
}

// Create singleton instance
export const shoppingService = new ShoppingService();

export default ShoppingService;
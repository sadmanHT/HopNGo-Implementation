import api from '@/services/api';

interface ItineraryItem {
  id: string;
  type: 'stay' | 'activity' | 'transport' | 'meal';
  title: string;
  description: string;
  location: {
    name: string;
    address?: string;
    coordinates?: {
      lat: number;
      lng: number;
    };
  };
  startTime?: string;
  endTime?: string;
  duration?: number; // in minutes
  price?: number;
  currency?: string;
  bookingUrl?: string;
  notes?: string;
}

interface Itinerary {
  id: string;
  title: string;
  description: string;
  destination: {
    city: string;
    country: string;
    region?: string;
  };
  duration: number; // in days
  difficulty: 'easy' | 'moderate' | 'challenging';
  budget: {
    min: number;
    max: number;
    currency: string;
  };
  tags: string[];
  categories: string[];
  featuredImage?: string;
  images: string[];
  items: ItineraryItem[];
  author: {
    id: string;
    name: string;
    avatar: string;
    isVerified: boolean;
  };
  status: 'PUBLISHED' | 'DRAFT' | 'ARCHIVED';
  isPublic: boolean;
  likes: number;
  saves: number;
  views: number;
  createdAt: string;
  updatedAt?: string;
}

interface ItineraryFilters {
  destination?: string;
  duration?: number;
  minDuration?: number;
  maxDuration?: number;
  difficulty?: string;
  minBudget?: number;
  maxBudget?: number;
  category?: string;
  tag?: string;
  status?: string;
  limit?: number;
  offset?: number;
}

class ItineraryService {
  async getPublicItineraries(filters: ItineraryFilters = {}): Promise<Itinerary[]> {
    try {
      const params = new URLSearchParams();
      
      // Only include published and public itineraries
      params.append('status', 'PUBLISHED');
      params.append('isPublic', 'true');
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/itineraries/public?${params.toString()}`);
      return response.data.itineraries || [];
    } catch (error) {
      console.error('Failed to fetch public itineraries:', error);
      return [];
    }
  }

  async getItinerary(id: string): Promise<Itinerary | null> {
    try {
      const response = await api.get(`/api/itineraries/${id}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch itinerary:', error);
      return null;
    }
  }

  async searchItineraries(query: string, filters: ItineraryFilters = {}): Promise<Itinerary[]> {
    try {
      const params = new URLSearchParams();
      params.append('q', query);
      params.append('status', 'PUBLISHED');
      params.append('isPublic', 'true');
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/itineraries/search?${params.toString()}`);
      return response.data.itineraries || [];
    } catch (error) {
      console.error('Failed to search itineraries:', error);
      return [];
    }
  }

  async getItinerariesByDestination(destination: string, limit: number = 20): Promise<Itinerary[]> {
    return this.getPublicItineraries({ destination, limit });
  }

  async getItinerariesByDuration(duration: number, limit: number = 20): Promise<Itinerary[]> {
    return this.getPublicItineraries({ duration, limit });
  }

  async getItinerariesByCategory(category: string, limit: number = 20): Promise<Itinerary[]> {
    return this.getPublicItineraries({ category, limit });
  }

  async getFeaturedItineraries(limit: number = 10): Promise<Itinerary[]> {
    try {
      const response = await api.get(`/api/itineraries/featured?limit=${limit}`);
      return response.data.itineraries || [];
    } catch (error) {
      console.error('Failed to fetch featured itineraries:', error);
      return [];
    }
  }

  async getPopularItineraries(limit: number = 10): Promise<Itinerary[]> {
    try {
      const response = await api.get(`/api/itineraries/popular?limit=${limit}`);
      return response.data.itineraries || [];
    } catch (error) {
      console.error('Failed to fetch popular itineraries:', error);
      return [];
    }
  }

  async getRecentItineraries(limit: number = 10): Promise<Itinerary[]> {
    return this.getPublicItineraries({ limit });
  }

  async getSimilarItineraries(itineraryId: string, limit: number = 5): Promise<Itinerary[]> {
    try {
      const response = await api.get(`/api/itineraries/${itineraryId}/similar?limit=${limit}`);
      return response.data.itineraries || [];
    } catch (error) {
      console.error('Failed to fetch similar itineraries:', error);
      return [];
    }
  }

  async incrementViews(itineraryId: string): Promise<void> {
    try {
      await api.post(`/api/itineraries/${itineraryId}/views`);
    } catch (error) {
      console.error('Failed to increment itinerary views:', error);
    }
  }

  async likeItinerary(itineraryId: string): Promise<void> {
    try {
      await api.post(`/api/itineraries/${itineraryId}/like`);
    } catch (error) {
      console.error('Failed to like itinerary:', error);
    }
  }

  async saveItinerary(itineraryId: string): Promise<void> {
    try {
      await api.post(`/api/itineraries/${itineraryId}/save`);
    } catch (error) {
      console.error('Failed to save itinerary:', error);
    }
  }

  async getDestinations(): Promise<string[]> {
    try {
      const response = await api.get('/api/itineraries/destinations');
      return response.data.destinations || [];
    } catch (error) {
      console.error('Failed to fetch destinations:', error);
      return [];
    }
  }

  async getCategories(): Promise<string[]> {
    try {
      const response = await api.get('/api/itineraries/categories');
      return response.data.categories || [];
    } catch (error) {
      console.error('Failed to fetch itinerary categories:', error);
      return [];
    }
  }
}

export const itineraryService = new ItineraryService();
export default itineraryService;

export type { Itinerary, ItineraryItem, ItineraryFilters };
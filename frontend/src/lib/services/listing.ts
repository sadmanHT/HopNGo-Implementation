import api from '@/services/api';

interface Listing {
  id: string;
  title: string;
  description: string;
  price: number;
  currency: string;
  category: string;
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT';
  location: {
    city: string;
    country: string;
    address?: string;
  };
  images: string[];
  tags: string[];
  amenities: string[];
  author: {
    id: string;
    name: string;
    avatar: string;
    isVerified: boolean;
  };
  createdAt: string;
  updatedAt?: string;
}

interface ListingFilters {
  category?: string;
  location?: string;
  minPrice?: number;
  maxPrice?: number;
  status?: string;
  limit?: number;
  offset?: number;
}

class ListingService {
  async getPublicListings(filters: ListingFilters = {}): Promise<Listing[]> {
    try {
      const params = new URLSearchParams();
      
      // Only include active listings for public access
      params.append('status', 'ACTIVE');
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/listings/public?${params.toString()}`);
      return response.data.listings || [];
    } catch (error) {
      console.error('Failed to fetch public listings:', error);
      return [];
    }
  }

  async getListing(id: string): Promise<Listing | null> {
    try {
      const response = await api.get(`/api/listings/${id}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch listing:', error);
      return null;
    }
  }

  async searchListings(query: string, filters: ListingFilters = {}): Promise<Listing[]> {
    try {
      const params = new URLSearchParams();
      params.append('q', query);
      params.append('status', 'ACTIVE');
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/listings/search?${params.toString()}`);
      return response.data.listings || [];
    } catch (error) {
      console.error('Failed to search listings:', error);
      return [];
    }
  }

  async getListingsByCategory(category: string, limit: number = 20): Promise<Listing[]> {
    return this.getPublicListings({ category, limit });
  }

  async getListingsByLocation(location: string, limit: number = 20): Promise<Listing[]> {
    return this.getPublicListings({ location, limit });
  }

  async getFeaturedListings(limit: number = 10): Promise<Listing[]> {
    try {
      const response = await api.get(`/api/listings/featured?limit=${limit}`);
      return response.data.listings || [];
    } catch (error) {
      console.error('Failed to fetch featured listings:', error);
      return [];
    }
  }
}

export const listingService = new ListingService();
export default listingService;

export type { Listing, ListingFilters };
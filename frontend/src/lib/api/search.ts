import { apiClient } from './client';
import { SearchResult, SearchFilters } from '../state/search';

export interface SearchResponse {
  results: SearchResult[];
  total: number;
  hasMore: boolean;
  page: number;
  size: number;
}

export interface SearchSuggestionsResponse {
  suggestions: string[];
}

export interface SearchParams extends SearchFilters {
  q: string;
  page?: number;
  size?: number;
}

export class SearchAPI {
  /**
   * Perform a unified search across posts and listings
   */
  static async search(params: SearchParams): Promise<SearchResponse> {
    const searchParams = new URLSearchParams({
      q: params.q,
      page: (params.page || 1).toString(),
      size: (params.size || 20).toString()
    });
    
    if (params.type && params.type !== 'all') {
      searchParams.append('type', params.type);
    }
    
    if (params.location) {
      searchParams.append('location', params.location);
    }
    
    if (params.priceRange?.min !== undefined) {
      searchParams.append('minPrice', params.priceRange.min.toString());
    }
    
    if (params.priceRange?.max !== undefined) {
      searchParams.append('maxPrice', params.priceRange.max.toString());
    }
    
    if (params.dateRange?.from) {
      searchParams.append('fromDate', params.dateRange.from);
    }
    
    if (params.dateRange?.to) {
      searchParams.append('toDate', params.dateRange.to);
    }

    const response = await apiClient.get(`/search?${searchParams.toString()}`);
    return response.data;
  }

  /**
   * Search only posts
   */
  static async searchPosts(params: Omit<SearchParams, 'type'>): Promise<SearchResponse> {
    const searchParams = new URLSearchParams({
      q: params.q,
      page: (params.page || 1).toString(),
      size: (params.size || 20).toString()
    });
    
    if (params.location) {
      searchParams.append('location', params.location);
    }
    
    if (params.dateRange?.from) {
      searchParams.append('fromDate', params.dateRange.from);
    }
    
    if (params.dateRange?.to) {
      searchParams.append('toDate', params.dateRange.to);
    }

    const response = await apiClient.get(`/search/posts?${searchParams.toString()}`);
    return {
      results: response.data.results.map((result: any) => ({
        ...result,
        type: 'post' as const
      })),
      total: response.data.total,
      hasMore: response.data.hasMore,
      page: response.data.page,
      size: response.data.size
    };
  }

  /**
   * Search only listings
   */
  static async searchListings(params: Omit<SearchParams, 'type'>): Promise<SearchResponse> {
    const searchParams = new URLSearchParams({
      q: params.q,
      page: (params.page || 1).toString(),
      size: (params.size || 20).toString()
    });
    
    if (params.location) {
      searchParams.append('location', params.location);
    }
    
    if (params.priceRange?.min !== undefined) {
      searchParams.append('minPrice', params.priceRange.min.toString());
    }
    
    if (params.priceRange?.max !== undefined) {
      searchParams.append('maxPrice', params.priceRange.max.toString());
    }
    
    if (params.dateRange?.from) {
      searchParams.append('fromDate', params.dateRange.from);
    }
    
    if (params.dateRange?.to) {
      searchParams.append('toDate', params.dateRange.to);
    }

    const response = await apiClient.get(`/search/listings?${searchParams.toString()}`);
    return {
      results: response.data.results.map((result: any) => ({
        ...result,
        type: 'listing' as const
      })),
      total: response.data.total,
      hasMore: response.data.hasMore,
      page: response.data.page,
      size: response.data.size
    };
  }

  /**
   * Get search suggestions
   */
  static async getSuggestions(query: string): Promise<SearchSuggestionsResponse> {
    const response = await apiClient.get(`/search/suggestions?q=${encodeURIComponent(query)}`);
    return response.data;
  }

  /**
    * Get post-specific suggestions
    */
   static async getPostSuggestions(query: string): Promise<SearchSuggestionsResponse> {
     const response = await apiClient.get(`/social/search/suggestions?q=${encodeURIComponent(query)}`);
     return response.data;
   }
 
   /**
    * Get listing-specific suggestions
    */
   static async getListingSuggestions(query: string): Promise<SearchSuggestionsResponse> {
     const response = await apiClient.get(`/bookings/search/suggestions?q=${encodeURIComponent(query)}`);
     return response.data;
   }
 
   /**
    * Reindex posts (admin only)
    */
   static async reindexPosts(): Promise<void> {
     await apiClient.post('/social/search/reindex');
   }
 
   /**
    * Reindex listings (admin only)
    */
   static async reindexListings(): Promise<void> {
     await apiClient.post('/bookings/search/reindex');
   }
  }

  /**
   * Trigger full reindexing (admin only)
   */
  static async reindexAll(): Promise<void> {
    await Promise.all([
      this.reindexPosts(),
      this.reindexListings()
    ]);
  }
}
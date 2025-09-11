import { ApiResponse } from '@/lib/api/types';

// Response wrapper for recommendations
export interface RecommendationResponse<T> {
  recommendations: T[];
  total: number;
  algorithm?: string;
  context?: string;
}

export interface RecommendedItem {
  id: string;
  title: string;
  description: string;
  imageUrl: string;
  price?: number;
  rating?: number;
  location?: string;
  type: 'stay' | 'tour' | 'experience';
}

export interface RecommendedUser {
  id: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  bio?: string;
  followersCount: number;
  isVerified: boolean;
}

export interface HomeRecommendationsResponse {
  recommendations: RecommendedItem[];
  algorithm: string;
  cached: boolean;
  processingTimeMs: number;
}

export interface UserRecommendationsResponse {
  recommendations: RecommendedUser[];
  algorithm: string;
  cached: boolean;
  processingTimeMs: number;
}

export interface ItemRecommendationsResponse {
  recommendations: RecommendedItem[];
  algorithm: string;
  cached: boolean;
  processingTimeMs: number;
}

class RecommendationService {
  private baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  private async fetchWithAuth<T>(url: string, options: RequestInit = {}): Promise<T> {
    const token = localStorage.getItem('authToken');
    
    const response = await fetch(`${this.baseUrl}${url}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options.headers,
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Get personalized home recommendations
   */
  async getHomeRecommendations(params: {
    limit?: number;
    excludeIds?: string[];
    location?: string;
    maxDistance?: number;
  } = {}): Promise<HomeRecommendationsResponse> {
    const searchParams = new URLSearchParams();
    
    if (params.limit) searchParams.append('limit', params.limit.toString());
    if (params.excludeIds?.length) {
      params.excludeIds.forEach(id => searchParams.append('excludeIds', id));
    }
    if (params.location) searchParams.append('location', params.location);
    if (params.maxDistance) searchParams.append('maxDistance', params.maxDistance.toString());

    const url = `/api/ai/similarity/home${searchParams.toString() ? `?${searchParams.toString()}` : ''}`;
    return this.fetchWithAuth<HomeRecommendationsResponse>(url);
  }

  /**
   * Get user recommendations (travel buddies to follow)
   */
  async getUserRecommendations(params: {
    limit?: number;
    excludeIds?: string[];
  } = {}): Promise<UserRecommendationsResponse> {
    const searchParams = new URLSearchParams();
    
    if (params.limit) searchParams.append('limit', params.limit.toString());
    if (params.excludeIds?.length) {
      params.excludeIds.forEach(id => searchParams.append('excludeIds', id));
    }

    const url = `/api/social/recommendations/users${searchParams.toString() ? `?${searchParams.toString()}` : ''}`;
    return this.fetchWithAuth<UserRecommendationsResponse>(url);
  }

  /**
   * Get item recommendations (similar stays/tours)
   */
  async getItemRecommendations(params: {
    itemId?: string;
    type?: 'stay' | 'tour' | 'experience' | 'trending';
    limit?: number;
    excludeIds?: string[];
    location?: string;
    maxDistance?: number;
  } = {}): Promise<ItemRecommendationsResponse> {
    const searchParams = new URLSearchParams();
    
    if (params.itemId) searchParams.append('itemId', params.itemId);
    if (params.type) searchParams.append('type', params.type);
    if (params.limit) searchParams.append('limit', params.limit.toString());
    if (params.excludeIds?.length) {
      params.excludeIds.forEach(id => searchParams.append('excludeIds', id));
    }
    if (params.location) searchParams.append('location', params.location);
    if (params.maxDistance) searchParams.append('maxDistance', params.maxDistance.toString());

    const url = `/api/ai/similarity/items${searchParams.toString() ? `?${searchParams.toString()}` : ''}`;
    return this.fetchWithAuth<ItemRecommendationsResponse>(url);
  }

  /**
   * Get similar items for a specific item (for listing page)
   */
  async getSimilarItems(
    itemId: string,
    params: {
      limit?: number;
      excludeIds?: string[];
    } = {}
  ): Promise<RecommendationResponse<RecommendedItem>> {
    try {
      const searchParams = new URLSearchParams();
      
      if (params.limit) searchParams.append('limit', params.limit.toString());
      if (params.excludeIds?.length) {
        params.excludeIds.forEach(id => searchParams.append('excludeIds', id));
      }

      const url = `/api/ai/recommendations/items/${itemId}/similar${searchParams.toString() ? `?${searchParams.toString()}` : ''}`;
       const response = await this.fetchWithAuth<RecommendationResponse<RecommendedItem>>(url);
       return response;
    } catch (error) {
      console.error('Failed to fetch similar items:', error);
      throw error;
    }
  }

  /**
   * Get trending items near user's location
   */
  async getTrendingNearby(params: {
    location?: string;
    maxDistance?: number;
    limit?: number;
  } = {}): Promise<ItemRecommendationsResponse> {
    return this.getItemRecommendations({
      type: 'trending',
      ...params
    });
  }

  /**
   * Track recommendation impression for analytics
   */
  async trackImpression(data: {
    recommendationId: string;
    algorithm: string;
    position: number;
    context: 'discover' | 'listing' | 'home';
  }): Promise<void> {
    try {
      await this.fetchWithAuth('/api/analytics/events', {
        method: 'POST',
        body: JSON.stringify({
          eventType: 'rec_impression',
          properties: data,
          timestamp: new Date().toISOString()
        })
      });
    } catch (error) {
      console.error('Failed to track recommendation impression:', error);
    }
  }

  /**
   * Track recommendation click for analytics
   */
  async trackClick(data: {
    recommendationId: string;
    algorithm: string;
    position: number;
    context: 'discover' | 'listing' | 'home';
  }): Promise<void> {
    try {
      await this.fetchWithAuth('/api/analytics/events', {
        method: 'POST',
        body: JSON.stringify({
          eventType: 'rec_click',
          properties: data,
          timestamp: new Date().toISOString()
        })
      });
    } catch (error) {
      console.error('Failed to track recommendation click:', error);
    }
  }
}

export const recommendationService = new RecommendationService();
export default recommendationService;
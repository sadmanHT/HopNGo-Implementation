import { Post, Comment, UserProfile } from '../lib/state/social';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

// API Response types
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  error?: string;
}

interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  hasMore: boolean;
}

// Request types
interface CreatePostRequest {
  content?: string;
  mediaUrls?: string[];
  location?: {
    name: string;
    coordinates: [number, number];
    address?: string;
  };
  tags?: string[];
}

interface UpdatePostRequest {
  content?: string;
  tags?: string[];
}

interface CreateCommentRequest {
  content: string;
  parentId?: string;
}

interface UpdateProfileRequest {
  name?: string;
  bio?: string;
  avatar?: string;
  location?: string;
  website?: string;
}

class SocialApiService {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const defaultHeaders: { [key: string]: string } = {
      'Content-Type': 'application/json',
    };

    // Add auth token if available
    const token = localStorage.getItem('auth_token');
    if (token) {
      defaultHeaders['Authorization'] = `Bearer ${token}`;
    }

    const config: RequestInit = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers,
      },
    };

    try {
      const response = await fetch(url, config);
      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || `HTTP error! status: ${response.status}`);
      }

      return data;
    } catch (error) {
      console.error(`API request failed: ${endpoint}`, error);
      throw error;
    }
  }

  // Posts API
  async getPosts(
    page: number = 1,
    limit: number = 20,
    userId?: string
  ): Promise<PaginatedResponse<Post>> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });

    if (userId) {
      params.append('userId', userId);
    }

    const response = await this.request<PaginatedResponse<Post>>(
      `/social/posts?${params.toString()}`
    );
    return response.data;
  }

  async getPost(postId: string): Promise<Post> {
    const response = await this.request<Post>(`/social/posts/${postId}`);
    return response.data;
  }

  async createPost(data: CreatePostRequest): Promise<Post> {
    const response = await this.request<Post>('/social/posts', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return response.data;
  }

  async updatePost(postId: string, data: UpdatePostRequest): Promise<Post> {
    const response = await this.request<Post>(`/social/posts/${postId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    return response.data;
  }

  async deletePost(postId: string): Promise<void> {
    await this.request(`/social/posts/${postId}`, {
      method: 'DELETE',
    });
  }

  async likePost(postId: string): Promise<{ liked: boolean; likesCount: number }> {
    const response = await this.request<{ liked: boolean; likesCount: number }>(
      `/social/posts/${postId}/like`,
      { method: 'POST' }
    );
    return response.data;
  }

  async bookmarkPost(postId: string): Promise<{ bookmarked: boolean }> {
    const response = await this.request<{ bookmarked: boolean }>(
      `/social/posts/${postId}/bookmark`,
      { method: 'POST' }
    );
    return response.data;
  }

  // Comments API
  async getComments(
    postId: string,
    page: number = 1,
    limit: number = 20
  ): Promise<PaginatedResponse<Comment>> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });

    const response = await this.request<PaginatedResponse<Comment>>(
      `/social/posts/${postId}/comments?${params.toString()}`
    );
    return response.data;
  }

  async createComment(postId: string, data: CreateCommentRequest): Promise<Comment> {
    const response = await this.request<Comment>(
      `/social/posts/${postId}/comments`,
      {
        method: 'POST',
        body: JSON.stringify(data),
      }
    );
    return response.data;
  }

  async updateComment(commentId: string, content: string): Promise<Comment> {
    const response = await this.request<Comment>(`/social/comments/${commentId}`, {
      method: 'PUT',
      body: JSON.stringify({ content }),
    });
    return response.data;
  }

  async deleteComment(commentId: string): Promise<void> {
    await this.request(`/social/comments/${commentId}`, {
      method: 'DELETE',
    });
  }

  async likeComment(commentId: string): Promise<{ liked: boolean; likesCount: number }> {
    const response = await this.request<{ liked: boolean; likesCount: number }>(
      `/social/comments/${commentId}/like`,
      { method: 'POST' }
    );
    return response.data;
  }

  // Users API
  async getUserProfile(userId: string): Promise<UserProfile> {
    const response = await this.request<UserProfile>(`/social/users/${userId}`);
    return response.data;
  }

  async updateProfile(data: UpdateProfileRequest): Promise<UserProfile> {
    const response = await this.request<UserProfile>('/social/users/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    return response.data;
  }

  async followUser(userId: string): Promise<{ following: boolean; followersCount: number }> {
    const response = await this.request<{ following: boolean; followersCount: number }>(
      `/social/users/${userId}/follow`,
      { method: 'POST' }
    );
    return response.data;
  }

  async unfollowUser(userId: string): Promise<{ following: boolean; followersCount: number }> {
    const response = await this.request<{ following: boolean; followersCount: number }>(
      `/social/users/${userId}/unfollow`,
      { method: 'POST' }
    );
    return response.data;
  }

  async getFollowers(
    userId: string,
    page: number = 1,
    limit: number = 20
  ): Promise<PaginatedResponse<UserProfile>> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });

    const response = await this.request<PaginatedResponse<UserProfile>>(
      `/social/users/${userId}/followers?${params.toString()}`
    );
    return response.data;
  }

  async getFollowing(
    userId: string,
    page: number = 1,
    limit: number = 20
  ): Promise<PaginatedResponse<UserProfile>> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
    });

    const response = await this.request<PaginatedResponse<UserProfile>>(
      `/social/users/${userId}/following?${params.toString()}`
    );
    return response.data;
  }

  // Feed API
  async getFeed(
    page: number = 1,
    limit: number = 20,
    type: 'following' | 'trending' | 'nearby' = 'following'
  ): Promise<PaginatedResponse<Post>> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
      type,
    });

    const response = await this.request<PaginatedResponse<Post>>(
      `/social/feed?${params.toString()}`
    );
    return response.data;
  }

  // Location-based API
  async getNearbyPosts(
    latitude: number,
    longitude: number,
    radius: number = 10000, // meters
    page: number = 1,
    limit: number = 20
  ): Promise<PaginatedResponse<Post>> {
    const params = new URLSearchParams({
      lat: latitude.toString(),
      lng: longitude.toString(),
      radius: radius.toString(),
      page: page.toString(),
      limit: limit.toString(),
    });

    const response = await this.request<PaginatedResponse<Post>>(
      `/social/posts/nearby?${params.toString()}`
    );
    return response.data;
  }

  async getLocationHeatmap(
    bounds?: {
      north: number;
      south: number;
      east: number;
      west: number;
    },
    timeRange?: '24h' | '7d' | '30d' | 'all'
  ): Promise<Array<{
    location: {
      name: string;
      coordinates: [number, number];
    };
    count: number;
    weight: number;
  }>> {
    const params = new URLSearchParams();
    
    if (bounds) {
      params.append('north', bounds.north.toString());
      params.append('south', bounds.south.toString());
      params.append('east', bounds.east.toString());
      params.append('west', bounds.west.toString());
    }
    
    if (timeRange) {
      params.append('timeRange', timeRange);
    }

    const response = await this.request<any>(
      `/social/analytics/heatmap?${params.toString()}`
    );
    return response.data;
  }
}

// Export singleton instance
export const socialApi = new SocialApiService();

// Export types
export type {
  CreatePostRequest,
  UpdatePostRequest,
  CreateCommentRequest,
  UpdateProfileRequest,
  PaginatedResponse,
  ApiResponse,
};
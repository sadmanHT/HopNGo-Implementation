export interface Location {
  latitude: number;
  longitude: number;
  address: string;
}

export interface PostRequest {
  content: string;
  location?: Location | null;
  mediaUrls: string[];
  tags: string[];
}

export interface Post {
  id: string;
  userId: string;
  content: string;
  location?: Location;
  mediaUrls: string[];
  tags: string[];
  likes: number;
  comments: number;
  createdAt: string;
  updatedAt?: string;
}

export interface FeedResponse {
  content: Post[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
  };
}

export class SocialService {
  constructor(private baseUrl: string) {}

  async createPost(request: PostRequest, token: string, userId: string): Promise<Post> {
    const response = await fetch(`${this.baseUrl}/api/v1/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token,
        'X-User-ID': userId,
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create post');
    }

    return response.json();
  }

  async getFeed(token: string, userId: string, page: number = 0, size: number = 20): Promise<FeedResponse> {
    const response = await fetch(`${this.baseUrl}/api/v1/posts/feed?page=${page}&size=${size}`, {
      method: 'GET',
      headers: {
        'Authorization': token,
        'X-User-ID': userId,
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to fetch feed');
    }

    return response.json();
  }
}
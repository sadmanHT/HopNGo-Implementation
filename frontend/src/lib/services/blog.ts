import api from '@/services/api';

interface BlogPost {
  id: string;
  title: string;
  slug: string;
  excerpt: string;
  content: string;
  featuredImage?: string;
  author: {
    id: string;
    name: string;
    avatar: string;
  };
  category: string;
  tags: string[];
  status: 'PUBLISHED' | 'DRAFT' | 'ARCHIVED';
  publishedAt?: string;
  createdAt: string;
  updatedAt?: string;
  readTime: number; // in minutes
  views: number;
}

interface BlogFilters {
  category?: string;
  tag?: string;
  author?: string;
  status?: string;
  limit?: number;
  offset?: number;
}

class BlogService {
  async getPublicPosts(filters: BlogFilters = {}): Promise<BlogPost[]> {
    try {
      const params = new URLSearchParams();
      
      // Only include published posts for public access
      params.append('status', 'PUBLISHED');
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/blog/posts/public?${params.toString()}`);
      return response.data.posts || [];
    } catch (error) {
      console.error('Failed to fetch public blog posts:', error);
      return [];
    }
  }

  async getPost(slug: string): Promise<BlogPost | null> {
    try {
      const response = await api.get(`/api/blog/posts/${slug}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch blog post:', error);
      return null;
    }
  }

  async getPostById(id: string): Promise<BlogPost | null> {
    try {
      const response = await api.get(`/api/blog/posts/id/${id}`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch blog post by ID:', error);
      return null;
    }
  }

  async searchPosts(query: string, filters: BlogFilters = {}): Promise<BlogPost[]> {
    try {
      const params = new URLSearchParams();
      params.append('q', query);
      params.append('status', 'PUBLISHED');
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get(`/api/blog/posts/search?${params.toString()}`);
      return response.data.posts || [];
    } catch (error) {
      console.error('Failed to search blog posts:', error);
      return [];
    }
  }

  async getPostsByCategory(category: string, limit: number = 20): Promise<BlogPost[]> {
    return this.getPublicPosts({ category, limit });
  }

  async getPostsByTag(tag: string, limit: number = 20): Promise<BlogPost[]> {
    return this.getPublicPosts({ tag, limit });
  }

  async getFeaturedPosts(limit: number = 5): Promise<BlogPost[]> {
    try {
      const response = await api.get(`/api/blog/posts/featured?limit=${limit}`);
      return response.data.posts || [];
    } catch (error) {
      console.error('Failed to fetch featured blog posts:', error);
      return [];
    }
  }

  async getRecentPosts(limit: number = 10): Promise<BlogPost[]> {
    return this.getPublicPosts({ limit });
  }

  async getRelatedPosts(postId: string, limit: number = 3): Promise<BlogPost[]> {
    try {
      const response = await api.get(`/api/blog/posts/${postId}/related?limit=${limit}`);
      return response.data.posts || [];
    } catch (error) {
      console.error('Failed to fetch related blog posts:', error);
      return [];
    }
  }

  async incrementViews(postId: string): Promise<void> {
    try {
      await api.post(`/api/blog/posts/${postId}/views`);
    } catch (error) {
      console.error('Failed to increment post views:', error);
    }
  }

  async getCategories(): Promise<string[]> {
    try {
      const response = await api.get('/api/blog/categories');
      return response.data.categories || [];
    } catch (error) {
      console.error('Failed to fetch blog categories:', error);
      return [];
    }
  }

  async getTags(): Promise<string[]> {
    try {
      const response = await api.get('/api/blog/tags');
      return response.data.tags || [];
    } catch (error) {
      console.error('Failed to fetch blog tags:', error);
      return [];
    }
  }
}

export const blogService = new BlogService();
export default blogService;

export type { BlogPost, BlogFilters };
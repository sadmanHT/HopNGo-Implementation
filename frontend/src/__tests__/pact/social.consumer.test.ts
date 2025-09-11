import { Pact, Matchers } from '@pact-foundation/pact';
import path from 'path';
import { SocialService } from '../../services/social';

const { like, eachLike, term } = Matchers;

describe('Social Service Consumer Tests', () => {
  const provider = new Pact({
    consumer: 'frontend',
    provider: 'social-service',
    port: 1235,
    log: path.resolve(process.cwd(), 'logs', 'pact.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'INFO',
  });

  beforeAll(() => provider.setup());
  afterEach(() => provider.verify());
  afterAll(() => provider.finalize());

  describe('POST /api/v1/posts', () => {
    it('should create a new post successfully', async () => {
      // Arrange
      const postRequest = {
        content: 'Just visited an amazing place in Paris! #travel #paris',
        location: {
          latitude: 48.8566,
          longitude: 2.3522,
          address: 'Paris, France'
        },
        mediaUrls: ['https://example.com/image1.jpg'],
        tags: ['travel', 'paris']
      };

      const expectedResponse = {
        id: like('post-123'),
        userId: like('user-456'),
        content: like('Just visited an amazing place in Paris! #travel #paris'),
        location: {
          latitude: like(48.8566),
          longitude: like(2.3522),
          address: like('Paris, France')
        },
        mediaUrls: eachLike('https://example.com/image1.jpg'),
        tags: eachLike('travel'),
        likes: like(0),
        comments: like(0),
        createdAt: term({
          matcher: '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*',
          generate: '2024-01-15T10:30:00Z'
        }),
        updatedAt: term({
          matcher: '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*',
          generate: '2024-01-15T10:30:00Z'
        })
      };

      await provider.addInteraction({
        state: 'user is authenticated and has valid permissions',
        uponReceiving: 'a request to create a new post',
        withRequest: {
          method: 'POST',
          path: '/api/v1/posts',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'),
            'X-User-ID': like('user-456')
          },
          body: postRequest
        },
        willRespondWith: {
          status: 201,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      // Act
      const socialService = new SocialService('http://localhost:1235');
      const response = await socialService.createPost(postRequest, 'Bearer token', 'user-456');

      // Assert
      expect(response.id).toBeDefined();
      expect(response.content).toBe('Just visited an amazing place in Paris! #travel #paris');
      expect(response.location.address).toBe('Paris, France');
      expect(response.tags).toContain('travel');
    });

    it('should return 400 for invalid post content', async () => {
      // Arrange
      const invalidPostRequest = {
        content: '', // Empty content
        location: null,
        mediaUrls: [],
        tags: []
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create a post with invalid content',
        withRequest: {
          method: 'POST',
          path: '/api/v1/posts',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'),
            'X-User-ID': like('user-456')
          },
          body: invalidPostRequest
        },
        willRespondWith: {
          status: 400,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            error: like('Validation failed'),
            message: like('Post content cannot be empty'),
            details: eachLike({
              field: like('content'),
              message: like('Content is required')
            })
          }
        }
      });

      // Act & Assert
      const socialService = new SocialService('http://localhost:1235');
      await expect(socialService.createPost(invalidPostRequest, 'Bearer token', 'user-456'))
        .rejects.toThrow('Validation failed');
    });
  });

  describe('GET /api/v1/posts/feed', () => {
    it('should retrieve user feed successfully', async () => {
      // Arrange
      const expectedResponse = {
        content: eachLike({
          id: like('post-123'),
          userId: like('user-789'),
          content: like('Amazing sunset at the beach! 🌅'),
          location: {
            latitude: like(34.0522),
            longitude: like(-118.2437),
            address: like('Los Angeles, CA')
          },
          mediaUrls: eachLike('https://example.com/sunset.jpg'),
          tags: eachLike('sunset'),
          likes: like(15),
          comments: like(3),
          createdAt: term({
            matcher: '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*',
            generate: '2024-01-15T18:30:00Z'
          })
        }),
        pageable: {
          pageNumber: like(0),
          pageSize: like(20),
          totalElements: like(50),
          totalPages: like(3)
        }
      };

      await provider.addInteraction({
        state: 'user has posts in their feed',
        uponReceiving: 'a request to get user feed',
        withRequest: {
          method: 'GET',
          path: '/api/v1/posts/feed',
          query: {
            page: '0',
            size: '20'
          },
          headers: {
            'Authorization': like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'),
            'X-User-ID': like('user-456')
          }
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      // Act
      const socialService = new SocialService('http://localhost:1235');
      const response = await socialService.getFeed('Bearer token', 'user-456', 0, 20);

      // Assert
      expect(response.content).toBeDefined();
      expect(response.content.length).toBeGreaterThan(0);
      expect(response.pageable.pageNumber).toBe(0);
      expect(response.pageable.pageSize).toBe(20);
    });
  });
});
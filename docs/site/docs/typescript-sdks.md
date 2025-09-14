---
sidebar_position: 8
---

# TypeScript SDKs

Comprehensive TypeScript client libraries for all HopNGo services, auto-generated from OpenAPI specifications.

## Overview

The HopNGo TypeScript SDKs provide type-safe, easy-to-use client libraries for interacting with all HopNGo microservices. These SDKs are automatically generated from OpenAPI specifications and include:

- **Type Safety**: Full TypeScript support with auto-generated types
- **Authentication**: Built-in JWT token management
- **Error Handling**: Structured error responses
- **Request/Response Types**: Complete type definitions for all API endpoints
- **Unified Interface**: Single SDK object for all services

## Installation

The SDKs are located in the frontend project and can be imported directly:

```typescript
import { HopNGoSDK } from './lib/sdk';
// Or import individual clients
import { authClient, bookingClient } from './lib/sdk';
```

## Quick Start

### Basic Usage

```typescript
import { HopNGoSDK } from './lib/sdk';

// Set authentication token for all services
HopNGoSDK.setAuthToken('your-jwt-token');

// Make API calls
try {
  const user = await HopNGoSDK.auth.get('/users/me');
  console.log('Current user:', user.data);
  
  const bookings = await HopNGoSDK.booking.get('/bookings');
  console.log('User bookings:', bookings.data);
} catch (error) {
  console.error('API Error:', error);
}
```

### Individual Service Clients

```typescript
import { AuthServiceClient, BookingServiceClient } from './lib/sdk';

// Create individual clients with custom configuration
const authClient = new AuthServiceClient({
  baseUrl: 'https://api.hopngo.com/auth',
  timeout: 15000,
  headers: {
    'X-API-Version': '1.0'
  }
});

const bookingClient = new BookingServiceClient({
  baseUrl: 'https://api.hopngo.com/booking'
});
```

## Available Services

### Authentication Service
```typescript
// User authentication and management
const loginResponse = await HopNGoSDK.auth.post('/auth/login', {
  email: 'user@example.com',
  password: 'password'
});

const profile = await HopNGoSDK.auth.get('/users/profile');
const updateProfile = await HopNGoSDK.auth.put('/users/profile', {
  name: 'Updated Name',
  preferences: { theme: 'dark' }
});
```

### Social Service
```typescript
// Social media and posts
const posts = await HopNGoSDK.social.get('/posts', {
  params: { limit: 10, offset: 0 }
});

const newPost = await HopNGoSDK.social.post('/posts', {
  content: 'Amazing trip to Paris!',
  images: ['image1.jpg', 'image2.jpg'],
  location: 'Paris, France'
});

const likes = await HopNGoSDK.social.post(`/posts/${postId}/like`);
```

### Booking Service
```typescript
// Booking and vendor management
const vendors = await HopNGoSDK.booking.get('/vendors', {
  params: { location: 'Paris', category: 'hotel' }
});

const booking = await HopNGoSDK.booking.post('/bookings', {
  vendorId: 'vendor-123',
  checkIn: '2024-06-01',
  checkOut: '2024-06-05',
  guests: 2
});

const bookingStatus = await HopNGoSDK.booking.get(`/bookings/${bookingId}`);
```

### Chat Service
```typescript
// Real-time messaging
const conversations = await HopNGoSDK.chat.get('/conversations');

const messages = await HopNGoSDK.chat.get(`/conversations/${conversationId}/messages`);

const newMessage = await HopNGoSDK.chat.post(`/conversations/${conversationId}/messages`, {
  content: 'Hello there!',
  type: 'text'
});
```

### Market Service
```typescript
// Marketplace and vendor listings
const listings = await HopNGoSDK.market.get('/listings', {
  params: { category: 'accommodation', location: 'Tokyo' }
});

const listing = await HopNGoSDK.market.get(`/listings/${listingId}`);

const reviews = await HopNGoSDK.market.get(`/listings/${listingId}/reviews`);
```

### Search Service
```typescript
// Search and discovery
const searchResults = await HopNGoSDK.search.get('/search', {
  params: {
    query: 'luxury hotels Paris',
    filters: { priceRange: '100-300', rating: '4+' }
  }
});

const suggestions = await HopNGoSDK.search.get('/suggestions', {
  params: { query: 'par' }
});
```

### AI Service
```typescript
// AI-powered recommendations
const recommendations = await HopNGoSDK.ai.post('/recommendations', {
  userId: 'user-123',
  preferences: ['culture', 'food', 'adventure'],
  location: 'Barcelona'
});

const insights = await HopNGoSDK.ai.post('/insights/travel-patterns', {
  userId: 'user-123',
  timeframe: '6months'
});
```

### Trip Planning Service
```typescript
// Trip planning and itineraries
const itinerary = await HopNGoSDK.tripPlanning.post('/itineraries', {
  destination: 'Rome',
  duration: 5,
  interests: ['history', 'food', 'art'],
  budget: 2000
});

const trips = await HopNGoSDK.tripPlanning.get('/trips');

const tripDetails = await HopNGoSDK.tripPlanning.get(`/trips/${tripId}`);
```

### Admin Service
```typescript
// Administrative operations (requires admin role)
const users = await HopNGoSDK.admin.get('/users', {
  params: { page: 1, limit: 50 }
});

const systemStats = await HopNGoSDK.admin.get('/system/stats');

const moderateContent = await HopNGoSDK.admin.post('/moderation/content', {
  contentId: 'content-123',
  action: 'approve'
});
```

### Analytics Service
```typescript
// Analytics and reporting
const userMetrics = await HopNGoSDK.analytics.get('/metrics/users', {
  params: { timeframe: '30d' }
});

const bookingTrends = await HopNGoSDK.analytics.get('/trends/bookings');

const customReport = await HopNGoSDK.analytics.post('/reports/custom', {
  metrics: ['bookings', 'revenue', 'users'],
  filters: { dateRange: '2024-01-01:2024-03-31' }
});
```

### Emergency Service
```typescript
// Emergency assistance
const emergencyContacts = await HopNGoSDK.emergency.get('/contacts', {
  params: { location: 'current' }
});

const alert = await HopNGoSDK.emergency.post('/alerts', {
  type: 'medical',
  location: { lat: 48.8566, lng: 2.3522 },
  message: 'Need immediate assistance'
});

const safetyTips = await HopNGoSDK.emergency.get('/safety-tips', {
  params: { destination: 'Thailand' }
});
```

## Configuration

### Client Configuration

```typescript
import { AuthServiceClient } from './lib/sdk';

const client = new AuthServiceClient({
  baseUrl: 'https://api.hopngo.com/auth',
  timeout: 10000,
  headers: {
    'X-API-Version': '1.0',
    'X-Client-ID': 'web-app'
  }
});
```

### Authentication Management

```typescript
// Set token for all services
HopNGoSDK.setAuthToken('jwt-token-here');

// Set token for individual service
HopNGoSDK.auth.setAuthToken('jwt-token-here');

// Clear authentication
HopNGoSDK.clearAuthToken();

// Update base URLs (for different environments)
HopNGoSDK.auth.setBaseUrl('https://staging-api.hopngo.com/auth');

// Update headers
HopNGoSDK.auth.setHeaders({
  'X-Environment': 'staging'
});
```

## Type Definitions

### API Response Type

```typescript
interface ApiResponse<T> {
  data: T;
  status: number;
  statusText: string;
  headers: Record<string, string>;
}
```

### API Error Type

```typescript
interface ApiError {
  message: string;
  status: number;
  statusText: string;
  data?: any;
}
```

### Using Generated Types

```typescript
import type { 
  AuthPaths, 
  BookingPaths, 
  SocialPaths 
} from './lib/sdk';

// Use path-specific types
type LoginRequest = AuthPaths['/auth/login']['post']['requestBody']['content']['application/json'];
type UserProfile = AuthPaths['/users/profile']['get']['responses']['200']['content']['application/json'];

type BookingRequest = BookingPaths['/bookings']['post']['requestBody']['content']['application/json'];
type BookingResponse = BookingPaths['/bookings']['post']['responses']['201']['content']['application/json'];
```

## Error Handling

### Basic Error Handling

```typescript
try {
  const response = await HopNGoSDK.auth.get('/users/profile');
  console.log(response.data);
} catch (error) {
  if (error.status === 401) {
    // Handle unauthorized
    console.log('Please log in');
  } else if (error.status === 404) {
    // Handle not found
    console.log('Profile not found');
  } else {
    // Handle other errors
    console.error('API Error:', error.message);
  }
}
```

### Advanced Error Handling

```typescript
import type { ApiError } from './lib/sdk';

function handleApiError(error: ApiError) {
  switch (error.status) {
    case 400:
      console.error('Bad Request:', error.data?.message);
      break;
    case 401:
      // Redirect to login
      window.location.href = '/login';
      break;
    case 403:
      console.error('Forbidden:', error.message);
      break;
    case 429:
      console.error('Rate limited. Please try again later.');
      break;
    case 500:
      console.error('Server error. Please try again.');
      break;
    default:
      console.error('Unexpected error:', error.message);
  }
}

try {
  const response = await HopNGoSDK.booking.post('/bookings', bookingData);
} catch (error) {
  handleApiError(error as ApiError);
}
```

## Best Practices

### 1. Environment Configuration

```typescript
// config/api.ts
const API_CONFIG = {
  development: {
    auth: 'http://localhost:8081',
    booking: 'http://localhost:8083',
    social: 'http://localhost:8082'
  },
  production: {
    auth: 'https://api.hopngo.com/auth',
    booking: 'https://api.hopngo.com/booking',
    social: 'https://api.hopngo.com/social'
  }
};

const env = process.env.NODE_ENV || 'development';
const config = API_CONFIG[env];

// Configure base URLs
HopNGoSDK.auth.setBaseUrl(config.auth);
HopNGoSDK.booking.setBaseUrl(config.booking);
HopNGoSDK.social.setBaseUrl(config.social);
```

### 2. Request Interceptors

```typescript
// Create a wrapper for common functionality
class ApiClient {
  private static instance: ApiClient;
  
  static getInstance() {
    if (!ApiClient.instance) {
      ApiClient.instance = new ApiClient();
    }
    return ApiClient.instance;
  }
  
  async request<T>(serviceCall: () => Promise<ApiResponse<T>>): Promise<T> {
    try {
      const response = await serviceCall();
      return response.data;
    } catch (error) {
      // Log error
      console.error('API Request failed:', error);
      
      // Handle token refresh
      if (error.status === 401) {
        await this.refreshToken();
        // Retry request
        const response = await serviceCall();
        return response.data;
      }
      
      throw error;
    }
  }
  
  private async refreshToken() {
    // Implement token refresh logic
  }
}

// Usage
const apiClient = ApiClient.getInstance();
const userData = await apiClient.request(() => 
  HopNGoSDK.auth.get('/users/profile')
);
```

### 3. Type-Safe API Calls

```typescript
// Create typed wrapper functions
class UserService {
  static async getCurrentUser() {
    const response = await HopNGoSDK.auth.get('/users/me');
    return response.data;
  }
  
  static async updateProfile(data: Partial<UserProfile>) {
    const response = await HopNGoSDK.auth.put('/users/profile', data);
    return response.data;
  }
  
  static async getUserBookings(userId: string) {
    const response = await HopNGoSDK.booking.get('/bookings', {
      params: { userId }
    });
    return response.data;
  }
}
```

## Development Workflow

### Regenerating SDKs

1. **Update OpenAPI specs** (when services are running):
   ```bash
   cd tools/openapi-aggregator
   npm run start
   ```

2. **Generate TypeScript SDKs**:
   ```bash
   cd tools/sdk-generator
   npm run start
   ```

3. **Use in your application**:
   ```typescript
   import { HopNGoSDK } from './lib/sdk';
   ```

### Using with React

```typescript
// hooks/useApi.ts
import { useState, useEffect } from 'react';
import { HopNGoSDK } from '../lib/sdk';

export function useUserProfile() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    async function fetchProfile() {
      try {
        const response = await HopNGoSDK.auth.get('/users/profile');
        setProfile(response.data);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    }
    
    fetchProfile();
  }, []);
  
  return { profile, loading, error };
}
```

## Troubleshooting

### Common Issues

1. **Import Errors**
   ```typescript
   // ❌ Wrong
   import HopNGoSDK from './lib/sdk';
   
   // ✅ Correct
   import { HopNGoSDK } from './lib/sdk';
   ```

2. **Type Errors**
   ```typescript
   // Ensure you're using the correct types
   import type { ApiResponse, ApiError } from './lib/sdk';
   ```

3. **Authentication Issues**
   ```typescript
   // Make sure to set the token before making requests
   HopNGoSDK.setAuthToken(localStorage.getItem('authToken'));
   ```

### Debug Mode

```typescript
// Enable request logging
HopNGoSDK.auth.setHeaders({
  'X-Debug': 'true'
});

// Log all requests
const originalRequest = HopNGoSDK.auth.request;
HopNGoSDK.auth.request = async function(...args) {
  console.log('API Request:', args);
  const result = await originalRequest.apply(this, args);
  console.log('API Response:', result);
  return result;
};
```

## Support

For issues with the TypeScript SDKs:

1. Check the [API Reference](./api-reference) for endpoint documentation
2. Verify service availability in your environment
3. Ensure OpenAPI specs are up to date
4. Check the generated types in `frontend/src/lib/sdk/`

The SDKs are automatically generated and should be regenerated whenever the OpenAPI specifications change.
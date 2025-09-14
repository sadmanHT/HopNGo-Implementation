---
sidebar_position: 9
---

# Code Samples

Comprehensive code examples for all HopNGo API endpoints across different programming languages and frameworks.

## Overview

This guide provides practical code samples for integrating with HopNGo APIs. Examples are provided in:

- **TypeScript/JavaScript** (using HopNGo SDK)
- **Python** (using requests)
- **Java** (using Spring WebClient)
- **cURL** (for testing)

## Authentication

### Obtaining JWT Token

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
import { HopNGoSDK } from './lib/sdk';

// Login and get JWT token
const loginResponse = await HopNGoSDK.auth.post('/auth/login', {
  email: 'user@example.com',
  password: 'securePassword123'
});

const { token, refreshToken, user } = loginResponse.data;

// Set token for all subsequent requests
HopNGoSDK.setAuthToken(token);

// Store tokens securely
localStorage.setItem('authToken', token);
localStorage.setItem('refreshToken', refreshToken);
```

</TabItem>
<TabItem value="python" label="Python">

```python
import requests
import json

# Login and get JWT token
login_data = {
    'email': 'user@example.com',
    'password': 'securePassword123'
}

response = requests.post(
    'http://localhost:8081/auth/login',
    json=login_data,
    headers={'Content-Type': 'application/json'}
)

if response.status_code == 200:
    auth_data = response.json()
    token = auth_data['token']
    
    # Use token in subsequent requests
    headers = {
        'Authorization': f'Bearer {token}',
        'Content-Type': 'application/json'
    }
else:
    print(f'Login failed: {response.status_code}')
```

</TabItem>
<TabItem value="java" label="Java">

```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HopNGoClient {
    private final WebClient webClient;
    private String authToken;
    
    public HopNGoClient() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:8081")
            .build();
    }
    
    public Mono<String> login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        
        return webClient.post()
            .uri("/auth/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(LoginResponse.class)
            .map(response -> {
                this.authToken = response.getToken();
                return response.getToken();
            });
    }
    
    private WebClient.RequestHeadersSpec<?> authenticatedRequest() {
        return webClient.get()
            .header("Authorization", "Bearer " + authToken);
    }
}
```

</TabItem>
<TabItem value="curl" label="cURL">

```bash
# Login and get JWT token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'

# Response:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "refresh_token_here",
#   "user": { ... }
# }

# Use token in subsequent requests
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

</TabItem>
</Tabs>

## User Management

### Get User Profile

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Get current user profile
const profile = await HopNGoSDK.auth.get('/users/profile');
console.log('User profile:', profile.data);

// Update user profile
const updatedProfile = await HopNGoSDK.auth.put('/users/profile', {
  name: 'John Doe',
  bio: 'Travel enthusiast',
  preferences: {
    currency: 'USD',
    language: 'en',
    notifications: {
      email: true,
      push: false
    }
  }
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Get current user profile
response = requests.get(
    'http://localhost:8081/users/profile',
    headers=headers
)

if response.status_code == 200:
    profile = response.json()
    print(f"User: {profile['name']} ({profile['email']})")

# Update user profile
update_data = {
    'name': 'John Doe',
    'bio': 'Travel enthusiast',
    'preferences': {
        'currency': 'USD',
        'language': 'en'
    }
}

response = requests.put(
    'http://localhost:8081/users/profile',
    json=update_data,
    headers=headers
)
```

</TabItem>
<TabItem value="curl" label="cURL">

```bash
# Get user profile
curl -X GET http://localhost:8081/users/profile \
  -H "Authorization: Bearer $TOKEN"

# Update user profile
curl -X PUT http://localhost:8081/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "bio": "Travel enthusiast",
    "preferences": {
      "currency": "USD",
      "language": "en"
    }
  }'
```

</TabItem>
</Tabs>

## Social Features

### Create and Manage Posts

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Create a new post
const newPost = await HopNGoSDK.social.post('/posts', {
  content: 'Just had an amazing dinner in Tokyo! 🍣',
  images: [
    'https://example.com/image1.jpg',
    'https://example.com/image2.jpg'
  ],
  location: {
    name: 'Tokyo, Japan',
    coordinates: {
      lat: 35.6762,
      lng: 139.6503
    }
  },
  tags: ['tokyo', 'food', 'sushi', 'travel']
});

// Get user's posts
const userPosts = await HopNGoSDK.social.get('/posts', {
  params: {
    userId: 'current',
    limit: 20,
    offset: 0
  }
});

// Like a post
const likeResponse = await HopNGoSDK.social.post(`/posts/${postId}/like`);

// Add comment
const comment = await HopNGoSDK.social.post(`/posts/${postId}/comments`, {
  content: 'Looks delicious! What restaurant was this?'
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Create a new post
post_data = {
    'content': 'Just had an amazing dinner in Tokyo! 🍣',
    'images': [
        'https://example.com/image1.jpg',
        'https://example.com/image2.jpg'
    ],
    'location': {
        'name': 'Tokyo, Japan',
        'coordinates': {
            'lat': 35.6762,
            'lng': 139.6503
        }
    },
    'tags': ['tokyo', 'food', 'sushi', 'travel']
}

response = requests.post(
    'http://localhost:8082/posts',
    json=post_data,
    headers=headers
)

if response.status_code == 201:
    post = response.json()
    post_id = post['id']
    print(f'Post created: {post_id}')

# Get user's posts
response = requests.get(
    'http://localhost:8082/posts',
    params={'userId': 'current', 'limit': 20},
    headers=headers
)

posts = response.json()
for post in posts['data']:
    print(f"Post: {post['content'][:50]}...")
```

</TabItem>
</Tabs>

## Booking Management

### Search and Book Accommodations

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Search for accommodations
const searchResults = await HopNGoSDK.booking.get('/vendors', {
  params: {
    location: 'Paris, France',
    category: 'hotel',
    checkIn: '2024-06-01',
    checkOut: '2024-06-05',
    guests: 2,
    priceRange: '100-300',
    amenities: ['wifi', 'breakfast', 'parking']
  }
});

// Get detailed vendor information
const vendorDetails = await HopNGoSDK.booking.get(`/vendors/${vendorId}`);

// Create a booking
const booking = await HopNGoSDK.booking.post('/bookings', {
  vendorId: 'vendor-123',
  checkIn: '2024-06-01T15:00:00Z',
  checkOut: '2024-06-05T11:00:00Z',
  guests: {
    adults: 2,
    children: 0
  },
  rooms: [
    {
      type: 'deluxe',
      quantity: 1
    }
  ],
  specialRequests: 'High floor, city view if possible',
  contactInfo: {
    name: 'John Doe',
    email: 'john@example.com',
    phone: '+1-555-0123'
  }
});

// Get booking status
const bookingStatus = await HopNGoSDK.booking.get(`/bookings/${booking.data.id}`);

// Cancel booking
const cancellation = await HopNGoSDK.booking.delete(`/bookings/${bookingId}`, {
  params: {
    reason: 'Change of plans'
  }
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Search for accommodations
search_params = {
    'location': 'Paris, France',
    'category': 'hotel',
    'checkIn': '2024-06-01',
    'checkOut': '2024-06-05',
    'guests': 2,
    'priceRange': '100-300'
}

response = requests.get(
    'http://localhost:8083/vendors',
    params=search_params,
    headers=headers
)

vendors = response.json()
for vendor in vendors['data']:
    print(f"Hotel: {vendor['name']} - ${vendor['pricePerNight']}/night")

# Create a booking
booking_data = {
    'vendorId': 'vendor-123',
    'checkIn': '2024-06-01T15:00:00Z',
    'checkOut': '2024-06-05T11:00:00Z',
    'guests': {
        'adults': 2,
        'children': 0
    },
    'rooms': [{
        'type': 'deluxe',
        'quantity': 1
    }],
    'contactInfo': {
        'name': 'John Doe',
        'email': 'john@example.com',
        'phone': '+1-555-0123'
    }
}

response = requests.post(
    'http://localhost:8083/bookings',
    json=booking_data,
    headers=headers
)

if response.status_code == 201:
    booking = response.json()
    print(f"Booking confirmed: {booking['confirmationNumber']}")
```

</TabItem>
</Tabs>

## Chat and Messaging

### Real-time Communication

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Get user's conversations
const conversations = await HopNGoSDK.chat.get('/conversations');

// Start a new conversation
const newConversation = await HopNGoSDK.chat.post('/conversations', {
  participants: ['user-456'],
  type: 'direct',
  title: 'Trip Planning Discussion'
});

// Send a message
const message = await HopNGoSDK.chat.post(`/conversations/${conversationId}/messages`, {
  content: 'Hey! Are you still interested in that Paris trip?',
  type: 'text',
  metadata: {
    replyTo: 'message-123'
  }
});

// Send image message
const imageMessage = await HopNGoSDK.chat.post(`/conversations/${conversationId}/messages`, {
  content: 'Check out this hotel!',
  type: 'image',
  attachments: [{
    url: 'https://example.com/hotel-image.jpg',
    type: 'image/jpeg',
    size: 1024000
  }]
});

// Get conversation messages
const messages = await HopNGoSDK.chat.get(`/conversations/${conversationId}/messages`, {
  params: {
    limit: 50,
    before: 'message-id-cursor'
  }
});

// Mark messages as read
const readReceipt = await HopNGoSDK.chat.post(`/conversations/${conversationId}/read`, {
  lastReadMessageId: 'message-789'
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Get user's conversations
response = requests.get(
    'http://localhost:8085/conversations',
    headers=headers
)

conversations = response.json()
for conv in conversations['data']:
    print(f"Conversation: {conv['title']} ({conv['unreadCount']} unread)")

# Send a message
message_data = {
    'content': 'Hey! Are you still interested in that Paris trip?',
    'type': 'text'
}

response = requests.post(
    f'http://localhost:8085/conversations/{conversation_id}/messages',
    json=message_data,
    headers=headers
)

if response.status_code == 201:
    message = response.json()
    print(f"Message sent: {message['id']}")

# Get messages
response = requests.get(
    f'http://localhost:8085/conversations/{conversation_id}/messages',
    params={'limit': 50},
    headers=headers
)

messages = response.json()
for msg in messages['data']:
    print(f"{msg['sender']['name']}: {msg['content']}")
```

</TabItem>
</Tabs>

## Search and Discovery

### Advanced Search

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// General search
const searchResults = await HopNGoSDK.search.get('/search', {
  params: {
    query: 'luxury hotels Paris',
    filters: {
      category: 'accommodation',
      location: 'Paris, France',
      priceRange: '200-500',
      rating: '4+',
      amenities: ['spa', 'pool', 'restaurant']
    },
    sort: 'rating',
    limit: 20,
    offset: 0
  }
});

// Location-based search
const nearbyResults = await HopNGoSDK.search.get('/search/nearby', {
  params: {
    lat: 48.8566,
    lng: 2.3522,
    radius: 5000, // 5km
    category: 'restaurant',
    cuisine: 'french'
  }
});

// Get search suggestions
const suggestions = await HopNGoSDK.search.get('/suggestions', {
  params: {
    query: 'par',
    type: 'location'
  }
});

// Advanced filters search
const filteredResults = await HopNGoSDK.search.post('/search/advanced', {
  query: 'romantic getaway',
  filters: {
    dateRange: {
      start: '2024-06-01',
      end: '2024-06-07'
    },
    budget: {
      min: 1000,
      max: 3000,
      currency: 'USD'
    },
    preferences: {
      atmosphere: ['romantic', 'quiet'],
      activities: ['dining', 'spa', 'sightseeing']
    }
  }
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# General search
search_params = {
    'query': 'luxury hotels Paris',
    'filters': json.dumps({
        'category': 'accommodation',
        'location': 'Paris, France',
        'priceRange': '200-500',
        'rating': '4+'
    }),
    'sort': 'rating',
    'limit': 20
}

response = requests.get(
    'http://localhost:8087/search',
    params=search_params,
    headers=headers
)

results = response.json()
for result in results['data']:
    print(f"{result['name']} - {result['rating']}⭐ - ${result['price']}")

# Location-based search
nearby_params = {
    'lat': 48.8566,
    'lng': 2.3522,
    'radius': 5000,
    'category': 'restaurant'
}

response = requests.get(
    'http://localhost:8087/search/nearby',
    params=nearby_params,
    headers=headers
)

nearby_results = response.json()
for place in nearby_results['data']:
    distance = place['distance']
    print(f"{place['name']} - {distance}m away")
```

</TabItem>
</Tabs>

## AI-Powered Features

### Recommendations and Insights

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Get personalized recommendations
const recommendations = await HopNGoSDK.ai.post('/recommendations', {
  userId: 'current',
  context: {
    location: 'Barcelona, Spain',
    travelDates: {
      start: '2024-07-15',
      end: '2024-07-22'
    },
    budget: 2500,
    currency: 'EUR'
  },
  preferences: {
    interests: ['culture', 'food', 'nightlife', 'architecture'],
    travelStyle: 'explorer',
    groupSize: 2,
    accommodation: 'boutique-hotel'
  }
});

// Get travel insights
const insights = await HopNGoSDK.ai.post('/insights/travel-patterns', {
  userId: 'current',
  timeframe: '12months',
  analysisType: 'spending-patterns'
});

// Smart itinerary generation
const itinerary = await HopNGoSDK.ai.post('/itinerary/generate', {
  destination: 'Rome, Italy',
  duration: 4,
  interests: ['history', 'art', 'food'],
  budget: 1500,
  travelStyle: 'cultural',
  constraints: {
    mobility: 'walking',
    dietary: ['vegetarian']
  }
});

// Price prediction
const pricePrediction = await HopNGoSDK.ai.post('/predictions/price', {
  destination: 'Tokyo, Japan',
  travelDates: {
    start: '2024-10-01',
    end: '2024-10-10'
  },
  category: 'accommodation',
  type: 'hotel'
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Get personalized recommendations
recommendation_data = {
    'userId': 'current',
    'context': {
        'location': 'Barcelona, Spain',
        'travelDates': {
            'start': '2024-07-15',
            'end': '2024-07-22'
        },
        'budget': 2500
    },
    'preferences': {
        'interests': ['culture', 'food', 'nightlife'],
        'travelStyle': 'explorer',
        'groupSize': 2
    }
}

response = requests.post(
    'http://localhost:8088/recommendations',
    json=recommendation_data,
    headers=headers
)

if response.status_code == 200:
    recommendations = response.json()
    for rec in recommendations['data']:
        print(f"Recommendation: {rec['title']} - {rec['confidence']}% match")
        print(f"Description: {rec['description']}")
        print(f"Estimated cost: ${rec['estimatedCost']}")
        print("---")

# Get travel insights
insights_data = {
    'userId': 'current',
    'timeframe': '12months',
    'analysisType': 'spending-patterns'
}

response = requests.post(
    'http://localhost:8088/insights/travel-patterns',
    json=insights_data,
    headers=headers
)

insights = response.json()
print(f"Total trips: {insights['totalTrips']}")
print(f"Average spending: ${insights['averageSpending']}")
print(f"Favorite destinations: {', '.join(insights['favoriteDestinations'])}")
```

</TabItem>
</Tabs>

## Trip Planning

### Comprehensive Trip Management

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Create a new trip
const newTrip = await HopNGoSDK.tripPlanning.post('/trips', {
  title: 'European Adventure 2024',
  description: 'A 2-week journey through Europe',
  destinations: [
    {
      city: 'Paris',
      country: 'France',
      arrivalDate: '2024-06-01',
      departureDate: '2024-06-05'
    },
    {
      city: 'Rome',
      country: 'Italy',
      arrivalDate: '2024-06-05',
      departureDate: '2024-06-10'
    },
    {
      city: 'Barcelona',
      country: 'Spain',
      arrivalDate: '2024-06-10',
      departureDate: '2024-06-15'
    }
  ],
  budget: {
    total: 5000,
    currency: 'USD',
    breakdown: {
      accommodation: 2000,
      transportation: 1500,
      food: 1000,
      activities: 500
    }
  },
  travelers: [
    {
      name: 'John Doe',
      age: 30,
      preferences: ['culture', 'food']
    },
    {
      name: 'Jane Smith',
      age: 28,
      preferences: ['art', 'shopping']
    }
  ]
});

// Generate detailed itinerary
const itinerary = await HopNGoSDK.tripPlanning.post(`/trips/${tripId}/itinerary`, {
  preferences: {
    paceOfTravel: 'moderate',
    interests: ['museums', 'local-cuisine', 'architecture'],
    budgetDistribution: 'balanced'
  },
  constraints: {
    maxWalkingDistance: 2000, // meters
    avoidCrowds: true,
    accessibilityNeeds: []
  }
});

// Add custom activity to itinerary
const customActivity = await HopNGoSDK.tripPlanning.post(`/trips/${tripId}/activities`, {
  day: 3,
  time: '14:00',
  activity: {
    name: 'Louvre Museum Visit',
    type: 'cultural',
    duration: 180, // minutes
    location: {
      name: 'Louvre Museum',
      address: 'Rue de Rivoli, 75001 Paris, France',
      coordinates: {
        lat: 48.8606,
        lng: 2.3376
      }
    },
    cost: {
      amount: 17,
      currency: 'EUR',
      per: 'person'
    },
    bookingInfo: {
      required: true,
      url: 'https://www.louvre.fr/en/visit',
      notes: 'Book in advance to skip the line'
    }
  }
});

// Get trip expenses
const expenses = await HopNGoSDK.tripPlanning.get(`/trips/${tripId}/expenses`);

// Share trip with others
const shareResponse = await HopNGoSDK.tripPlanning.post(`/trips/${tripId}/share`, {
  recipients: ['friend@example.com'],
  permissions: ['view', 'comment'],
  message: 'Check out our Europe trip plan!'
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Create a new trip
trip_data = {
    'title': 'European Adventure 2024',
    'description': 'A 2-week journey through Europe',
    'destinations': [
        {
            'city': 'Paris',
            'country': 'France',
            'arrivalDate': '2024-06-01',
            'departureDate': '2024-06-05'
        },
        {
            'city': 'Rome',
            'country': 'Italy',
            'arrivalDate': '2024-06-05',
            'departureDate': '2024-06-10'
        }
    ],
    'budget': {
        'total': 5000,
        'currency': 'USD'
    },
    'travelers': [
        {
            'name': 'John Doe',
            'age': 30,
            'preferences': ['culture', 'food']
        }
    ]
}

response = requests.post(
    'http://localhost:8089/trips',
    json=trip_data,
    headers=headers
)

if response.status_code == 201:
    trip = response.json()
    trip_id = trip['id']
    print(f"Trip created: {trip['title']} (ID: {trip_id})")

# Generate itinerary
itinerary_data = {
    'preferences': {
        'paceOfTravel': 'moderate',
        'interests': ['museums', 'local-cuisine'],
        'budgetDistribution': 'balanced'
    }
}

response = requests.post(
    f'http://localhost:8089/trips/{trip_id}/itinerary',
    json=itinerary_data,
    headers=headers
)

itinerary = response.json()
for day in itinerary['days']:
    print(f"Day {day['dayNumber']}: {day['location']}")
    for activity in day['activities']:
        print(f"  {activity['time']}: {activity['name']}")
```

</TabItem>
</Tabs>

## Analytics and Reporting

### Business Intelligence

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Get user engagement metrics
const userMetrics = await HopNGoSDK.analytics.get('/metrics/users', {
  params: {
    timeframe: '30d',
    granularity: 'daily',
    metrics: ['active_users', 'new_registrations', 'retention_rate']
  }
});

// Get booking analytics
const bookingAnalytics = await HopNGoSDK.analytics.get('/metrics/bookings', {
  params: {
    timeframe: '90d',
    groupBy: ['destination', 'category'],
    filters: {
      status: 'confirmed',
      minAmount: 100
    }
  }
});

// Generate custom report
const customReport = await HopNGoSDK.analytics.post('/reports/custom', {
  name: 'Q2 Performance Report',
  timeframe: {
    start: '2024-04-01',
    end: '2024-06-30'
  },
  metrics: [
    'total_bookings',
    'revenue',
    'average_booking_value',
    'user_acquisition_cost'
  ],
  dimensions: ['destination', 'user_segment', 'booking_source'],
  filters: {
    userType: 'premium',
    bookingStatus: 'completed'
  },
  format: 'json'
});

// Get real-time dashboard data
const dashboardData = await HopNGoSDK.analytics.get('/dashboard/realtime', {
  params: {
    widgets: [
      'active_users_now',
      'bookings_today',
      'revenue_today',
      'top_destinations'
    ]
  }
});

// Export data
const exportRequest = await HopNGoSDK.analytics.post('/export', {
  type: 'user_bookings',
  format: 'csv',
  timeframe: '1y',
  filters: {
    status: 'completed'
  },
  columns: [
    'booking_id',
    'user_email',
    'destination',
    'booking_date',
    'amount',
    'status'
  ]
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Get user engagement metrics
metrics_params = {
    'timeframe': '30d',
    'granularity': 'daily',
    'metrics': 'active_users,new_registrations,retention_rate'
}

response = requests.get(
    'http://localhost:8091/metrics/users',
    params=metrics_params,
    headers=headers
)

metrics = response.json()
for day_data in metrics['data']:
    date = day_data['date']
    active_users = day_data['active_users']
    print(f"{date}: {active_users} active users")

# Generate custom report
report_data = {
    'name': 'Q2 Performance Report',
    'timeframe': {
        'start': '2024-04-01',
        'end': '2024-06-30'
    },
    'metrics': [
        'total_bookings',
        'revenue',
        'average_booking_value'
    ],
    'dimensions': ['destination', 'user_segment'],
    'format': 'json'
}

response = requests.post(
    'http://localhost:8091/reports/custom',
    json=report_data,
    headers=headers
)

if response.status_code == 201:
    report = response.json()
    print(f"Report generated: {report['id']}")
    print(f"Total bookings: {report['data']['total_bookings']}")
    print(f"Revenue: ${report['data']['revenue']}")
```

</TabItem>
</Tabs>

## Emergency Services

### Safety and Assistance

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Get emergency contacts for current location
const emergencyContacts = await HopNGoSDK.emergency.get('/contacts', {
  params: {
    location: 'current', // or specific coordinates
    services: ['medical', 'police', 'fire', 'embassy']
  }
});

// Create emergency alert
const alert = await HopNGoSDK.emergency.post('/alerts', {
  type: 'medical',
  severity: 'high',
  location: {
    lat: 48.8566,
    lng: 2.3522,
    address: '123 Rue de la Paix, Paris, France'
  },
  message: 'Tourist needs immediate medical assistance',
  contactInfo: {
    name: 'John Doe',
    phone: '+1-555-0123',
    emergencyContact: {
      name: 'Jane Doe',
      phone: '+1-555-0124',
      relationship: 'spouse'
    }
  },
  medicalInfo: {
    conditions: ['diabetes'],
    medications: ['insulin'],
    allergies: ['penicillin']
  }
});

// Get safety information for destination
const safetyInfo = await HopNGoSDK.emergency.get('/safety-info', {
  params: {
    destination: 'Bangkok, Thailand',
    categories: ['health', 'crime', 'natural-disasters', 'transportation']
  }
});

// Register travel plans for safety monitoring
const travelRegistration = await HopNGoSDK.emergency.post('/travel-registration', {
  traveler: {
    name: 'John Doe',
    passport: 'US123456789',
    nationality: 'US',
    emergencyContact: {
      name: 'Jane Doe',
      phone: '+1-555-0124',
      email: 'jane@example.com'
    }
  },
  itinerary: [
    {
      destination: 'Bangkok, Thailand',
      arrivalDate: '2024-08-01',
      departureDate: '2024-08-07',
      accommodation: {
        name: 'Bangkok Hotel',
        address: '123 Sukhumvit Road, Bangkok',
        phone: '+66-2-123-4567'
      }
    }
  ]
});

// Check travel advisories
const advisories = await HopNGoSDK.emergency.get('/travel-advisories', {
  params: {
    destinations: ['Thailand', 'Cambodia', 'Vietnam'],
    severity: 'all'
  }
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
# Get emergency contacts
contacts_params = {
    'location': 'current',
    'services': 'medical,police,fire,embassy'
}

response = requests.get(
    'http://localhost:8092/contacts',
    params=contacts_params,
    headers=headers
)

contacts = response.json()
for contact in contacts['data']:
    print(f"{contact['service']}: {contact['phone']} ({contact['name']})")

# Create emergency alert
alert_data = {
    'type': 'medical',
    'severity': 'high',
    'location': {
        'lat': 48.8566,
        'lng': 2.3522,
        'address': '123 Rue de la Paix, Paris, France'
    },
    'message': 'Tourist needs immediate medical assistance',
    'contactInfo': {
        'name': 'John Doe',
        'phone': '+1-555-0123'
    }
}

response = requests.post(
    'http://localhost:8092/alerts',
    json=alert_data,
    headers=headers
)

if response.status_code == 201:
    alert = response.json()
    print(f"Emergency alert created: {alert['id']}")
    print(f"Response time estimate: {alert['estimatedResponseTime']} minutes")

# Get safety information
safety_params = {
    'destination': 'Bangkok, Thailand',
    'categories': 'health,crime,natural-disasters'
}

response = requests.get(
    'http://localhost:8092/safety-info',
    params=safety_params,
    headers=headers
)

safety_info = response.json()
for category in safety_info['categories']:
    print(f"{category['name']}: {category['riskLevel']}")
    for tip in category['tips']:
        print(f"  - {tip}")
```

</TabItem>
</Tabs>

## Error Handling Patterns

### Comprehensive Error Management

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
import type { ApiError } from './lib/sdk';

// Generic error handler
function handleApiError(error: ApiError, context?: string) {
  console.error(`API Error${context ? ` in ${context}` : ''}:`, {
    status: error.status,
    message: error.message,
    data: error.data
  });
  
  switch (error.status) {
    case 400:
      // Bad Request - show validation errors
      if (error.data?.errors) {
        error.data.errors.forEach((err: any) => {
          console.error(`Validation error: ${err.field} - ${err.message}`);
        });
      }
      break;
      
    case 401:
      // Unauthorized - redirect to login
      localStorage.removeItem('authToken');
      window.location.href = '/login';
      break;
      
    case 403:
      // Forbidden - show access denied message
      alert('Access denied. You do not have permission to perform this action.');
      break;
      
    case 404:
      // Not Found
      console.error('Resource not found');
      break;
      
    case 429:
      // Rate Limited
      const retryAfter = error.data?.retryAfter || 60;
      console.error(`Rate limited. Retry after ${retryAfter} seconds.`);
      break;
      
    case 500:
      // Server Error
      console.error('Server error. Please try again later.');
      break;
      
    default:
      console.error('Unexpected error occurred');
  }
}

// Retry mechanism
async function withRetry<T>(
  operation: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: any;
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;
      
      // Don't retry on client errors (4xx)
      if (error.status >= 400 && error.status < 500) {
        throw error;
      }
      
      if (attempt < maxRetries) {
        console.log(`Attempt ${attempt} failed, retrying in ${delay}ms...`);
        await new Promise(resolve => setTimeout(resolve, delay));
        delay *= 2; // Exponential backoff
      }
    }
  }
  
  throw lastError;
}

// Usage example with error handling
async function createBookingWithErrorHandling(bookingData: any) {
  try {
    const booking = await withRetry(() => 
      HopNGoSDK.booking.post('/bookings', bookingData)
    );
    
    console.log('Booking created successfully:', booking.data);
    return booking.data;
    
  } catch (error) {
    handleApiError(error as ApiError, 'booking creation');
    throw error;
  }
}
```

</TabItem>
<TabItem value="python" label="Python">

```python
import time
import requests
from typing import Optional, Callable, Any

class ApiError(Exception):
    def __init__(self, status_code: int, message: str, data: Optional[dict] = None):
        self.status_code = status_code
        self.message = message
        self.data = data
        super().__init__(f"API Error {status_code}: {message}")

def handle_api_error(error: ApiError, context: Optional[str] = None):
    """Generic error handler for API responses"""
    context_str = f" in {context}" if context else ""
    print(f"API Error{context_str}: {error.status_code} - {error.message}")
    
    if error.status_code == 400:
        # Bad Request - show validation errors
        if error.data and 'errors' in error.data:
            for err in error.data['errors']:
                print(f"Validation error: {err.get('field')} - {err.get('message')}")
    
    elif error.status_code == 401:
        # Unauthorized - clear token
        print("Authentication failed. Please log in again.")
        # Clear stored token
        
    elif error.status_code == 403:
        print("Access denied. Insufficient permissions.")
        
    elif error.status_code == 404:
        print("Resource not found.")
        
    elif error.status_code == 429:
        retry_after = error.data.get('retryAfter', 60) if error.data else 60
        print(f"Rate limited. Retry after {retry_after} seconds.")
        
    elif error.status_code >= 500:
        print("Server error. Please try again later.")

def make_api_request(method: str, url: str, **kwargs) -> dict:
    """Make API request with error handling"""
    try:
        response = requests.request(method, url, **kwargs)
        
        if response.status_code >= 400:
            try:
                error_data = response.json()
            except:
                error_data = None
                
            raise ApiError(
                status_code=response.status_code,
                message=response.reason,
                data=error_data
            )
            
        return response.json()
        
    except requests.RequestException as e:
        raise ApiError(status_code=0, message=str(e))

def with_retry(
    operation: Callable[[], Any],
    max_retries: int = 3,
    delay: float = 1.0
) -> Any:
    """Execute operation with retry logic"""
    last_error = None
    
    for attempt in range(1, max_retries + 1):
        try:
            return operation()
        except ApiError as error:
            last_error = error
            
            # Don't retry on client errors (4xx)
            if 400 <= error.status_code < 500:
                raise error
                
            if attempt < max_retries:
                print(f"Attempt {attempt} failed, retrying in {delay}s...")
                time.sleep(delay)
                delay *= 2  # Exponential backoff
    
    raise last_error

# Usage example
def create_booking_with_error_handling(booking_data: dict, headers: dict):
    """Create booking with comprehensive error handling"""
    try:
        def make_booking():
            return make_api_request(
                'POST',
                'http://localhost:8083/bookings',
                json=booking_data,
                headers=headers
            )
        
        booking = with_retry(make_booking)
        print(f"Booking created successfully: {booking['id']}")
        return booking
        
    except ApiError as error:
        handle_api_error(error, 'booking creation')
        raise
```

</TabItem>
</Tabs>

## Testing API Endpoints

### Unit Testing Examples

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Jest test example
import { HopNGoSDK } from '../lib/sdk';

// Mock the SDK for testing
jest.mock('../lib/sdk');
const mockSDK = HopNGoSDK as jest.Mocked<typeof HopNGoSDK>;

describe('Booking Service', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  
  test('should create booking successfully', async () => {
    const mockBooking = {
      id: 'booking-123',
      vendorId: 'vendor-456',
      status: 'confirmed',
      confirmationNumber: 'CONF123'
    };
    
    mockSDK.booking.post.mockResolvedValue({
      data: mockBooking,
      status: 201,
      statusText: 'Created',
      headers: {}
    });
    
    const bookingData = {
      vendorId: 'vendor-456',
      checkIn: '2024-06-01',
      checkOut: '2024-06-05',
      guests: { adults: 2, children: 0 }
    };
    
    const result = await HopNGoSDK.booking.post('/bookings', bookingData);
    
    expect(result.data).toEqual(mockBooking);
    expect(mockSDK.booking.post).toHaveBeenCalledWith('/bookings', bookingData);
  });
  
  test('should handle booking creation error', async () => {
    const mockError = {
      status: 400,
      message: 'Invalid booking data',
      data: {
        errors: [{
          field: 'checkIn',
          message: 'Check-in date must be in the future'
        }]
      }
    };
    
    mockSDK.booking.post.mockRejectedValue(mockError);
    
    const bookingData = {
      vendorId: 'vendor-456',
      checkIn: '2024-01-01', // Past date
      checkOut: '2024-01-05',
      guests: { adults: 2, children: 0 }
    };
    
    await expect(
      HopNGoSDK.booking.post('/bookings', bookingData)
    ).rejects.toEqual(mockError);
  });
});
```

</TabItem>
<TabItem value="python" label="Python">

```python
import unittest
from unittest.mock import patch, Mock
import requests

class TestBookingAPI(unittest.TestCase):
    def setUp(self):
        self.headers = {
            'Authorization': 'Bearer test-token',
            'Content-Type': 'application/json'
        }
        self.base_url = 'http://localhost:8083'
    
    @patch('requests.post')
    def test_create_booking_success(self, mock_post):
        # Mock successful response
        mock_response = Mock()
        mock_response.status_code = 201
        mock_response.json.return_value = {
            'id': 'booking-123',
            'vendorId': 'vendor-456',
            'status': 'confirmed',
            'confirmationNumber': 'CONF123'
        }
        mock_post.return_value = mock_response
        
        booking_data = {
            'vendorId': 'vendor-456',
            'checkIn': '2024-06-01',
            'checkOut': '2024-06-05',
            'guests': {'adults': 2, 'children': 0}
        }
        
        response = requests.post(
            f'{self.base_url}/bookings',
            json=booking_data,
            headers=self.headers
        )
        
        self.assertEqual(response.status_code, 201)
        booking = response.json()
        self.assertEqual(booking['id'], 'booking-123')
        self.assertEqual(booking['status'], 'confirmed')
        
        mock_post.assert_called_once_with(
            f'{self.base_url}/bookings',
            json=booking_data,
            headers=self.headers
        )
    
    @patch('requests.post')
    def test_create_booking_validation_error(self, mock_post):
        # Mock validation error response
        mock_response = Mock()
        mock_response.status_code = 400
        mock_response.json.return_value = {
            'message': 'Validation failed',
            'errors': [{
                'field': 'checkIn',
                'message': 'Check-in date must be in the future'
            }]
        }
        mock_post.return_value = mock_response
        
        booking_data = {
            'vendorId': 'vendor-456',
            'checkIn': '2024-01-01',  # Past date
            'checkOut': '2024-01-05',
            'guests': {'adults': 2, 'children': 0}
        }
        
        response = requests.post(
            f'{self.base_url}/bookings',
            json=booking_data,
            headers=self.headers
        )
        
        self.assertEqual(response.status_code, 400)
        error_data = response.json()
        self.assertEqual(error_data['message'], 'Validation failed')
        self.assertEqual(len(error_data['errors']), 1)
        self.assertEqual(error_data['errors'][0]['field'], 'checkIn')

if __name__ == '__main__':
    unittest.main()
```

</TabItem>
</Tabs>

## Performance Optimization

### Caching and Optimization Strategies

<Tabs>
<TabItem value="typescript" label="TypeScript">

```typescript
// Simple in-memory cache
class ApiCache {
  private cache = new Map<string, { data: any; timestamp: number; ttl: number }>();
  
  set(key: string, data: any, ttlSeconds: number = 300) {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl: ttlSeconds * 1000
    });
  }
  
  get(key: string): any | null {
    const entry = this.cache.get(key);
    if (!entry) return null;
    
    if (Date.now() - entry.timestamp > entry.ttl) {
      this.cache.delete(key);
      return null;
    }
    
    return entry.data;
  }
  
  clear() {
    this.cache.clear();
  }
}

const apiCache = new ApiCache();

// Cached API wrapper
class CachedApiClient {
  async getCached<T>(key: string, fetcher: () => Promise<T>, ttlSeconds: number = 300): Promise<T> {
    // Try cache first
    const cached = apiCache.get(key);
    if (cached) {
      console.log(`Cache hit for ${key}`);
      return cached;
    }
    
    // Fetch from API
    console.log(`Cache miss for ${key}, fetching...`);
    const data = await fetcher();
    
    // Store in cache
    apiCache.set(key, data, ttlSeconds);
    return data;
  }
}

const cachedClient = new CachedApiClient();

// Usage examples
async function getVendorsWithCache(location: string, category: string) {
  const cacheKey = `vendors:${location}:${category}`;
  
  return cachedClient.getCached(
    cacheKey,
    async () => {
      const response = await HopNGoSDK.booking.get('/vendors', {
        params: { location, category }
      });
      return response.data;
    },
    600 // Cache for 10 minutes
  );
}

// Batch requests
async function batchApiRequests<T>(requests: Array<() => Promise<T>>): Promise<T[]> {
  // Execute requests in parallel with concurrency limit
  const concurrencyLimit = 5;
  const results: T[] = [];
  
  for (let i = 0; i < requests.length; i += concurrencyLimit) {
    const batch = requests.slice(i, i + concurrencyLimit);
    const batchResults = await Promise.all(batch.map(req => req()));
    results.push(...batchResults);
  }
  
  return results;
}

// Request deduplication
class RequestDeduplicator {
  private pendingRequests = new Map<string, Promise<any>>();
  
  async dedupe<T>(key: string, request: () => Promise<T>): Promise<T> {
    if (this.pendingRequests.has(key)) {
      console.log(`Deduplicating request: ${key}`);
      return this.pendingRequests.get(key)!;
    }
    
    const promise = request().finally(() => {
      this.pendingRequests.delete(key);
    });
    
    this.pendingRequests.set(key, promise);
    return promise;
  }
}

const deduplicator = new RequestDeduplicator();

// Usage
async function getUserProfileOptimized(userId: string) {
  return deduplicator.dedupe(
    `user:${userId}`,
    () => HopNGoSDK.auth.get(`/users/${userId}`)
  );
}
```

</TabItem>
</Tabs>

## Integration Examples

### React Integration

```typescript
// Custom hooks for API integration
import { useState, useEffect, useCallback } from 'react';
import { HopNGoSDK } from '../lib/sdk';

// Generic API hook
function useApi<T>(apiCall: () => Promise<T>, dependencies: any[] = []) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiCall();
      setData(result);
    } catch (err: any) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  }, dependencies);
  
  useEffect(() => {
    fetchData();
  }, [fetchData]);
  
  return { data, loading, error, refetch: fetchData };
}

// Specific hooks
export function useUserProfile() {
  return useApi(async () => {
    const response = await HopNGoSDK.auth.get('/users/profile');
    return response.data;
  });
}

export function useVendors(location: string, category: string) {
  return useApi(async () => {
    const response = await HopNGoSDK.booking.get('/vendors', {
      params: { location, category }
    });
    return response.data;
  }, [location, category]);
}

// Component usage
function UserProfile() {
  const { data: profile, loading, error } = useUserProfile();
  
  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!profile) return <div>No profile found</div>;
  
  return (
    <div>
      <h1>{profile.name}</h1>
      <p>{profile.email}</p>
      <p>{profile.bio}</p>
    </div>
  );
}
```

This comprehensive code samples documentation provides developers with practical examples for integrating with all HopNGo APIs across multiple programming languages and frameworks.
const axios = require('axios');
const { expect } = require('chai');

// Configuration
const config = {
  baseUrl: process.env.BASE_URL || 'http://localhost:8080',
  timeout: 30000,
  retries: 3
};

// Test user credentials
const testUser = {
  email: 'smoketest@hopngo.com',
  password: 'SmokeTest123!',
  firstName: 'Smoke',
  lastName: 'Test'
};

// HTTP client with retry logic
const httpClient = axios.create({
  baseURL: config.baseUrl,
  timeout: config.timeout,
  headers: {
    'Content-Type': 'application/json',
    'User-Agent': 'HopNGo-SmokeTest/1.0'
  }
});

// Retry wrapper for HTTP requests
async function withRetry(fn, retries = config.retries) {
  for (let i = 0; i < retries; i++) {
    try {
      return await fn();
    } catch (error) {
      console.log(`Attempt ${i + 1} failed:`, error.message);
      if (i === retries - 1) throw error;
      await sleep(2000 * (i + 1)); // Exponential backoff
    }
  }
}

// Utility functions
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function logTest(testName) {
  console.log(`\n🧪 Running: ${testName}`);
}

function logSuccess(message) {
  console.log(`✅ ${message}`);
}

function logError(message) {
  console.log(`❌ ${message}`);
}

// Test suite
class SmokeTests {
  constructor() {
    this.authToken = null;
    this.userId = null;
  }

  async runAll() {
    console.log('🚀 Starting HopNGo Smoke Tests');
    console.log(`🔗 Base URL: ${config.baseUrl}`);
    
    try {
      await this.testHealthChecks();
      await this.testUserRegistration();
      await this.testUserLogin();
      await this.testUserFeed();
      await this.testBookingSearch();
      await this.testTripPlanning();
      await this.testChatSystem();
      
      console.log('\n🎉 All smoke tests passed!');
      return true;
    } catch (error) {
      logError(`Smoke tests failed: ${error.message}`);
      console.error(error);
      return false;
    }
  }

  async testHealthChecks() {
    logTest('Health Checks');
    
    const services = [
      '/actuator/health',
      '/api/v1/auth/health',
      '/api/v1/social/health',
      '/api/v1/booking/health',
      '/api/v1/trips/health'
    ];

    for (const endpoint of services) {
      await withRetry(async () => {
        const response = await httpClient.get(endpoint);
        expect(response.status).to.equal(200);
        logSuccess(`${endpoint} is healthy`);
      });
    }
  }

  async testUserRegistration() {
    logTest('User Registration Flow');
    
    // Clean up any existing test user first
    try {
      await httpClient.delete(`/api/v1/auth/users/${testUser.email}`);
    } catch (error) {
      // Ignore if user doesn't exist
    }

    await withRetry(async () => {
      const response = await httpClient.post('/api/v1/auth/register', {
        email: testUser.email,
        password: testUser.password,
        firstName: testUser.firstName,
        lastName: testUser.lastName,
        acceptTerms: true
      });
      
      expect(response.status).to.be.oneOf([200, 201]);
      expect(response.data).to.have.property('userId');
      
      this.userId = response.data.userId;
      logSuccess('User registration successful');
    });
  }

  async testUserLogin() {
    logTest('User Login Flow');
    
    await withRetry(async () => {
      const response = await httpClient.post('/api/v1/auth/login', {
        email: testUser.email,
        password: testUser.password
      });
      
      expect(response.status).to.equal(200);
      expect(response.data).to.have.property('accessToken');
      expect(response.data).to.have.property('user');
      
      this.authToken = response.data.accessToken;
      
      // Set auth header for subsequent requests
      httpClient.defaults.headers.common['Authorization'] = `Bearer ${this.authToken}`;
      
      logSuccess('User login successful');
    });
  }

  async testUserFeed() {
    logTest('User Feed Flow');
    
    if (!this.authToken) {
      throw new Error('Authentication required for feed test');
    }

    await withRetry(async () => {
      // Test getting user profile
      const profileResponse = await httpClient.get('/api/v1/auth/profile');
      expect(profileResponse.status).to.equal(200);
      expect(profileResponse.data).to.have.property('email', testUser.email);
      logSuccess('User profile retrieved');
      
      // Test getting social feed
      const feedResponse = await httpClient.get('/api/v1/social/feed', {
        params: { page: 0, size: 10 }
      });
      expect(feedResponse.status).to.equal(200);
      expect(feedResponse.data).to.have.property('content');
      expect(feedResponse.data.content).to.be.an('array');
      logSuccess('Social feed retrieved');
      
      // Test creating a post
      const postResponse = await httpClient.post('/api/v1/social/posts', {
        content: 'Smoke test post from automated testing',
        type: 'TEXT',
        visibility: 'PUBLIC'
      });
      expect(postResponse.status).to.be.oneOf([200, 201]);
      logSuccess('Social post created');
    });
  }

  async testBookingSearch() {
    logTest('Booking Search Flow');
    
    if (!this.authToken) {
      throw new Error('Authentication required for booking search test');
    }

    await withRetry(async () => {
      // Test trip search
      const searchResponse = await httpClient.get('/api/v1/trips/search', {
        params: {
          origin: 'New York',
          destination: 'Boston',
          departureDate: '2024-12-31',
          passengers: 2
        }
      });
      expect(searchResponse.status).to.equal(200);
      expect(searchResponse.data).to.have.property('trips');
      expect(searchResponse.data.trips).to.be.an('array');
      logSuccess('Trip search completed');
      
      // Test getting available routes
      const routesResponse = await httpClient.get('/api/v1/trips/routes');
      expect(routesResponse.status).to.equal(200);
      expect(routesResponse.data).to.be.an('array');
      logSuccess('Available routes retrieved');
      
      // Test booking availability (if trips exist)
      if (searchResponse.data.trips.length > 0) {
        const tripId = searchResponse.data.trips[0].id;
        const availabilityResponse = await httpClient.get(`/api/v1/booking/availability/${tripId}`);
        expect(availabilityResponse.status).to.equal(200);
        logSuccess('Booking availability checked');
      }
    });
  }

  async testTripPlanning() {
    logTest('Trip Planning Flow');
    
    if (!this.authToken) {
      throw new Error('Authentication required for trip planning test');
    }

    await withRetry(async () => {
      // Test getting trip recommendations
      const recommendationsResponse = await httpClient.get('/api/v1/trips/recommendations', {
        params: {
          location: 'New York',
          interests: 'sightseeing,food'
        }
      });
      expect(recommendationsResponse.status).to.equal(200);
      logSuccess('Trip recommendations retrieved');
      
      // Test AI-powered trip planning
      const aiPlanResponse = await httpClient.post('/api/v1/ai/plan-trip', {
        destination: 'Boston',
        duration: 3,
        budget: 1000,
        interests: ['history', 'food', 'culture']
      });
      expect(aiPlanResponse.status).to.be.oneOf([200, 201]);
      logSuccess('AI trip planning completed');
    });
  }

  async testChatSystem() {
    logTest('Chat System Flow');
    
    if (!this.authToken) {
      throw new Error('Authentication required for chat test');
    }

    await withRetry(async () => {
      // Test getting chat conversations
      const conversationsResponse = await httpClient.get('/api/v1/chat/conversations');
      expect(conversationsResponse.status).to.equal(200);
      expect(conversationsResponse.data).to.be.an('array');
      logSuccess('Chat conversations retrieved');
      
      // Test chat health
      const chatHealthResponse = await httpClient.get('/api/v1/chat/health');
      expect(chatHealthResponse.status).to.equal(200);
      logSuccess('Chat system is healthy');
    });
  }
}

// Main execution
if (require.main === module) {
  const smokeTests = new SmokeTests();
  
  smokeTests.runAll()
    .then(success => {
      process.exit(success ? 0 : 1);
    })
    .catch(error => {
      console.error('Smoke tests crashed:', error);
      process.exit(1);
    });
}

module.exports = SmokeTests;
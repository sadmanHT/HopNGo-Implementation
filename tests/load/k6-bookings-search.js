import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const searchLatency = new Trend('search_latency');
const dbQueryTime = new Trend('db_query_time');

// Test configuration
export const options = {
  scenarios: {
    // Sustained load test - 500 RPS for 10 minutes
    sustained_load: {
      executor: 'constant-arrival-rate',
      rate: 500, // 500 requests per second
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
    // Ramp test to find capacity limits
    ramp_test: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 500,
      stages: [
        { duration: '2m', target: 100 }, // Warm up
        { duration: '5m', target: 300 }, // Ramp up
        { duration: '5m', target: 500 }, // Target load
        { duration: '5m', target: 800 }, // Stress test
        { duration: '3m', target: 1000 }, // Peak load
        { duration: '5m', target: 500 }, // Scale down
        { duration: '2m', target: 0 }, // Cool down
      ],
    },
  },
  thresholds: {
    // Error rate must be less than 1%
    'errors': ['rate<0.01'],
    // 95th percentile response time must be less than 400ms
    'http_req_duration': ['p(95)<400'],
    // Average response time should be under 200ms
    'http_req_duration': ['avg<200'],
    // 99th percentile should be under 800ms
    'http_req_duration': ['p(99)<800'],
    // Search latency specific threshold
    'search_latency': ['p(95)<400'],
    // Database query time threshold
    'db_query_time': ['p(95)<100'],
  },
};

// Test data generators
const locations = [
  'New York', 'Los Angeles', 'Chicago', 'Houston', 'Phoenix',
  'Philadelphia', 'San Antonio', 'San Diego', 'Dallas', 'San Jose',
  'Austin', 'Jacksonville', 'Fort Worth', 'Columbus', 'Charlotte'
];

const categories = [
  'adventure', 'cultural', 'food', 'nightlife', 'outdoor',
  'shopping', 'sightseeing', 'sports', 'wellness', 'family'
];

const priceRanges = ['budget', 'mid-range', 'luxury'];
const durations = ['half-day', 'full-day', 'multi-day'];

function generateSearchParams() {
  const params = {
    location: randomItem(locations),
    category: randomItem(categories),
    priceRange: randomItem(priceRanges),
    duration: randomItem(durations),
    startDate: new Date(Date.now() + randomIntBetween(1, 30) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    guests: randomIntBetween(1, 8),
    page: randomIntBetween(1, 5),
    limit: randomIntBetween(10, 50),
    sortBy: randomItem(['price', 'rating', 'popularity', 'distance']),
    sortOrder: randomItem(['asc', 'desc'])
  };
  
  // Sometimes include additional filters
  if (Math.random() > 0.7) {
    params.minRating = randomIntBetween(3, 5);
  }
  
  if (Math.random() > 0.8) {
    params.maxPrice = randomIntBetween(50, 500);
  }
  
  if (Math.random() > 0.9) {
    params.amenities = randomItem(['wifi', 'parking', 'breakfast', 'pool', 'gym']).split(',');
  }
  
  return params;
}

function buildQueryString(params) {
  return Object.entries(params)
    .filter(([key, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
}

export function setup() {
  // Warm up the system
  console.log('Warming up the system...');
  const warmupParams = generateSearchParams();
  const warmupUrl = `${__ENV.BASE_URL || 'http://localhost:8080'}/api/bookings/search?${buildQueryString(warmupParams)}`;
  
  const warmupResponse = http.get(warmupUrl);
  console.log(`Warmup response status: ${warmupResponse.status}`);
  
  return {
    baseUrl: __ENV.BASE_URL || 'http://localhost:8080',
    authToken: __ENV.AUTH_TOKEN || null
  };
}

export default function(data) {
  const searchParams = generateSearchParams();
  const url = `${data.baseUrl}/api/bookings/search?${buildQueryString(searchParams)}`;
  
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'User-Agent': 'k6-load-test/1.0'
  };
  
  if (data.authToken) {
    headers['Authorization'] = `Bearer ${data.authToken}`;
  }
  
  const startTime = Date.now();
  const response = http.get(url, { headers });
  const endTime = Date.now();
  
  // Record custom metrics
  searchLatency.add(endTime - startTime);
  
  // Extract database query time from response headers if available
  const dbTime = response.headers['X-DB-Query-Time'];
  if (dbTime) {
    dbQueryTime.add(parseFloat(dbTime));
  }
  
  // Comprehensive response validation
  const isSuccess = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 400ms': (r) => r.timings.duration < 400,
    'response time < 200ms (avg target)': (r) => r.timings.duration < 200,
    'has search results': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && Array.isArray(body.data.results);
      } catch (e) {
        return false;
      }
    },
    'has pagination info': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.pagination && 
               typeof body.data.pagination.total === 'number';
      } catch (e) {
        return false;
      }
    },
    'results have required fields': (r) => {
      try {
        const body = JSON.parse(r.body);
        if (!body.data || !body.data.results || body.data.results.length === 0) {
          return true; // Empty results are valid
        }
        
        const firstResult = body.data.results[0];
        return firstResult.id && firstResult.title && 
               firstResult.price !== undefined && firstResult.location;
      } catch (e) {
        return false;
      }
    },
    'response size reasonable': (r) => r.body.length < 1024 * 1024, // Less than 1MB
    'no server errors': (r) => r.status < 500,
  });
  
  // Record error rate
  errorRate.add(!isSuccess);
  
  // Log errors for debugging
  if (!isSuccess) {
    console.error(`Request failed: ${response.status} - ${response.body.substring(0, 200)}`);
  }
  
  // Simulate user think time
  sleep(randomIntBetween(1, 3));
}

export function teardown(data) {
  console.log('Load test completed');
  console.log(`Base URL: ${data.baseUrl}`);
}

// Handle different test scenarios
export function handleSummary(data) {
  const summary = {
    testType: 'Bookings Search Load Test',
    timestamp: new Date().toISOString(),
    duration: data.state.testRunDurationMs,
    scenarios: Object.keys(options.scenarios),
    metrics: {
      http_reqs: data.metrics.http_reqs,
      http_req_duration: data.metrics.http_req_duration,
      http_req_failed: data.metrics.http_req_failed,
      errors: data.metrics.errors,
      search_latency: data.metrics.search_latency,
      db_query_time: data.metrics.db_query_time,
    },
    thresholds: data.thresholds,
  };
  
  return {
    'summary.json': JSON.stringify(summary, null, 2),
    stdout: `
=== Bookings Search Load Test Results ===
Requests: ${data.metrics.http_reqs.count}
Failures: ${data.metrics.http_req_failed.count} (${(data.metrics.http_req_failed.rate * 100).toFixed(2)}%)
Avg Response Time: ${data.metrics.http_req_duration.avg.toFixed(2)}ms
P95 Response Time: ${data.metrics['http_req_duration{p(95)}'].toFixed(2)}ms
P99 Response Time: ${data.metrics['http_req_duration{p(99)}'].toFixed(2)}ms
==========================================
`
  };
}
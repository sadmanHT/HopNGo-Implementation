import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween, randomItem, randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const feedLatency = new Trend('feed_latency');
const cacheHitRate = new Rate('cache_hits');
const dbQueryTime = new Trend('db_query_time');
const feedInteractions = new Counter('feed_interactions');
const contentLoadTime = new Trend('content_load_time');

// Test configuration
export const options = {
  scenarios: {
    // Social feed browsing - sustained load
    feed_browsing: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 80,
      maxVUs: 250,
    },
    // Peak social activity (evening hours simulation)
    peak_social: {
      executor: 'ramping-arrival-rate',
      startRate: 300,
      timeUnit: '1s',
      preAllocatedVUs: 60,
      maxVUs: 500,
      stages: [
        { duration: '1m', target: 300 }, // Evening starts
        { duration: '3m', target: 600 }, // Peak engagement
        { duration: '5m', target: 900 }, // Prime time
        { duration: '4m', target: 1200 }, // Peak social hour
        { duration: '3m', target: 800 }, // Winding down
        { duration: '2m', target: 400 }, // Late evening
        { duration: '2m', target: 0 }, // Cool down
      ],
    },
  },
  thresholds: {
    'errors': ['rate<0.01'], // Less than 1% error rate
    'http_req_duration': ['p(95)<400'], // P95 < 400ms
    'http_req_duration': ['avg<150'], // Average < 150ms (faster for social)
    'feed_latency': ['p(95)<400'],
    'content_load_time': ['p(95)<200'],
    'cache_hits': ['rate>0.8'], // 80%+ cache hit rate expected
    'db_query_time': ['p(95)<50'], // Fast DB queries for cached content
  },
};

// Test data
const feedTypes = ['following', 'discover', 'trending', 'local', 'category'];
const categories = ['adventure', 'food', 'culture', 'nightlife', 'outdoor', 'wellness'];
const sortOptions = ['recent', 'popular', 'trending', 'nearby'];
const contentTypes = ['post', 'review', 'photo', 'video', 'story', 'tip'];

const userProfiles = [
  { id: 'user_001', interests: ['adventure', 'outdoor'], location: 'New York' },
  { id: 'user_002', interests: ['food', 'culture'], location: 'Los Angeles' },
  { id: 'user_003', interests: ['nightlife', 'culture'], location: 'Chicago' },
  { id: 'user_004', interests: ['wellness', 'outdoor'], location: 'San Francisco' },
  { id: 'user_005', interests: ['adventure', 'food'], location: 'Austin' },
  { id: 'user_006', interests: ['culture', 'wellness'], location: 'Seattle' },
  { id: 'user_007', interests: ['outdoor', 'adventure'], location: 'Denver' },
  { id: 'user_008', interests: ['food', 'nightlife'], location: 'Miami' },
];

function generateFeedRequest() {
  const user = randomItem(userProfiles);
  const feedType = randomItem(feedTypes);
  
  const params = {
    feedType: feedType,
    page: randomIntBetween(1, 10),
    limit: randomIntBetween(10, 30),
    sort: randomItem(sortOptions),
  };
  
  // Add type-specific parameters
  switch (feedType) {
    case 'category':
      params.category = randomItem(user.interests);
      break;
    case 'local':
      params.location = user.location;
      params.radius = randomIntBetween(5, 50); // km
      break;
    case 'trending':
      params.timeframe = randomItem(['1h', '6h', '24h', '7d']);
      break;
    case 'discover':
      params.excludeFollowing = true;
      params.diversify = true;
      break;
  }
  
  // Sometimes include content type filter
  if (Math.random() > 0.7) {
    params.contentType = randomItem(contentTypes);
  }
  
  // Sometimes include date range
  if (Math.random() > 0.8) {
    const daysBack = randomIntBetween(1, 30);
    params.since = new Date(Date.now() - daysBack * 24 * 60 * 60 * 1000).toISOString();
  }
  
  return { user, params };
}

function buildQueryString(params) {
  return Object.entries(params)
    .filter(([key, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
}

function simulateInteraction(baseUrl, headers, postId) {
  const interactions = ['like', 'comment', 'share', 'save'];
  const interaction = randomItem(interactions);
  
  const interactionUrl = `${baseUrl}/api/social/posts/${postId}/${interaction}`;
  
  let response;
  switch (interaction) {
    case 'like':
      response = http.post(interactionUrl, null, { headers });
      break;
    case 'comment':
      const comment = {
        text: randomItem([
          'Amazing experience!',
          'Thanks for sharing!',
          'Looks incredible!',
          'Added to my wishlist!',
          'Great photos!'
        ])
      };
      response = http.post(interactionUrl, JSON.stringify(comment), { headers });
      break;
    case 'share':
      response = http.post(interactionUrl, JSON.stringify({ platform: 'internal' }), { headers });
      break;
    case 'save':
      response = http.post(interactionUrl, null, { headers });
      break;
  }
  
  if (response && response.status < 400) {
    feedInteractions.add(1);
  }
  
  return response;
}

export function setup() {
  console.log('Setting up social feed load test...');
  
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  
  // Warm up the cache with some feed requests
  console.log('Warming up feed cache...');
  const warmupUser = randomItem(userProfiles);
  const warmupUrl = `${baseUrl}/api/social/feed?feedType=following&limit=20`;
  
  const warmupHeaders = {
    'Content-Type': 'application/json',
    'X-User-ID': warmupUser.id,
  };
  
  const warmupResponse = http.get(warmupUrl, { headers: warmupHeaders });
  console.log(`Warmup response status: ${warmupResponse.status}`);
  
  return {
    baseUrl: baseUrl,
    authToken: __ENV.AUTH_TOKEN || null,
    enableInteractions: __ENV.ENABLE_INTERACTIONS !== 'false',
  };
}

export default function(data) {
  const { user, params } = generateFeedRequest();
  const url = `${data.baseUrl}/api/social/feed?${buildQueryString(params)}`;
  
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'User-Agent': 'k6-social-test/1.0',
    'X-User-ID': user.id,
    'X-User-Location': user.location,
    'X-User-Interests': user.interests.join(','),
  };
  
  if (data.authToken) {
    headers['Authorization'] = `Bearer ${data.authToken}`;
  }
  
  const startTime = Date.now();
  const response = http.get(url, { headers });
  const endTime = Date.now();
  
  // Record custom metrics
  feedLatency.add(endTime - startTime);
  
  // Check for cache hit indicators
  const cacheStatus = response.headers['X-Cache-Status'];
  if (cacheStatus) {
    cacheHitRate.add(cacheStatus === 'HIT');
  }
  
  // Extract processing times
  const dbTime = response.headers['X-DB-Query-Time'];
  if (dbTime) {
    dbQueryTime.add(parseFloat(dbTime));
  }
  
  const contentTime = response.headers['X-Content-Load-Time'];
  if (contentTime) {
    contentLoadTime.add(parseFloat(contentTime));
  }
  
  // Comprehensive feed validation
  const isSuccess = check(response, {
    'feed status is 200': (r) => r.status === 200,
    'response time < 400ms': (r) => r.timings.duration < 400,
    'response time < 150ms (avg target)': (r) => r.timings.duration < 150,
    'has feed data': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && Array.isArray(body.data.posts);
      } catch (e) {
        return false;
      }
    },
    'has pagination': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.pagination && 
               typeof body.data.pagination.total === 'number';
      } catch (e) {
        return false;
      }
    },
    'posts have required fields': (r) => {
      try {
        const body = JSON.parse(r.body);
        if (!body.data || !body.data.posts || body.data.posts.length === 0) {
          return true; // Empty feed is valid
        }
        
        const firstPost = body.data.posts[0];
        return firstPost.id && firstPost.author && firstPost.content && 
               firstPost.createdAt && firstPost.engagement;
      } catch (e) {
        return false;
      }
    },
    'has engagement metrics': (r) => {
      try {
        const body = JSON.parse(r.body);
        if (!body.data || !body.data.posts || body.data.posts.length === 0) {
          return true;
        }
        
        const firstPost = body.data.posts[0];
        return firstPost.engagement && 
               typeof firstPost.engagement.likes === 'number' &&
               typeof firstPost.engagement.comments === 'number';
      } catch (e) {
        return false;
      }
    },
    'response size reasonable': (r) => r.body.length < 2 * 1024 * 1024, // Less than 2MB
    'no server errors': (r) => r.status < 500,
    'proper content type': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
  });
  
  errorRate.add(!isSuccess);
  
  if (!isSuccess) {
    console.error(`Feed request failed: ${response.status} - ${response.body.substring(0, 200)}`);
  }
  
  // Simulate user interactions with feed content
  if (isSuccess && data.enableInteractions && Math.random() > 0.6) {
    try {
      const body = JSON.parse(response.body);
      if (body.data && body.data.posts && body.data.posts.length > 0) {
        // Interact with 1-3 random posts
        const numInteractions = randomIntBetween(1, Math.min(3, body.data.posts.length));
        
        for (let i = 0; i < numInteractions; i++) {
          const randomPost = randomItem(body.data.posts);
          simulateInteraction(data.baseUrl, headers, randomPost.id);
          sleep(randomIntBetween(1, 2)); // Brief pause between interactions
        }
      }
    } catch (e) {
      // Ignore interaction errors
    }
  }
  
  // Simulate scrolling behavior (loading more content)
  if (isSuccess && Math.random() > 0.7) {
    const nextPage = params.page + 1;
    const nextPageParams = { ...params, page: nextPage };
    const nextPageUrl = `${data.baseUrl}/api/social/feed?${buildQueryString(nextPageParams)}`;
    
    sleep(randomIntBetween(2, 4)); // User scrolling time
    
    const nextPageResponse = http.get(nextPageUrl, { headers });
    check(nextPageResponse, {
      'next page loads successfully': (r) => r.status === 200,
      'next page response time reasonable': (r) => r.timings.duration < 500,
    });
  }
  
  // Simulate user reading time
  sleep(randomIntBetween(3, 8));
}

export function teardown(data) {
  console.log('Social feed load test completed');
  console.log(`Interactions enabled: ${data.enableInteractions}`);
}

export function handleSummary(data) {
  const summary = {
    testType: 'Social Feed Load Test',
    timestamp: new Date().toISOString(),
    duration: data.state.testRunDurationMs,
    scenarios: Object.keys(options.scenarios),
    metrics: {
      http_reqs: data.metrics.http_reqs,
      http_req_duration: data.metrics.http_req_duration,
      http_req_failed: data.metrics.http_req_failed,
      errors: data.metrics.errors,
      feed_latency: data.metrics.feed_latency,
      cache_hits: data.metrics.cache_hits,
      db_query_time: data.metrics.db_query_time,
      content_load_time: data.metrics.content_load_time,
      feed_interactions: data.metrics.feed_interactions,
    },
    thresholds: data.thresholds,
    socialMetrics: {
      cacheHitRate: data.metrics.cache_hits ? 
        (data.metrics.cache_hits.rate * 100).toFixed(2) + '%' : 'N/A',
      interactionRate: data.metrics.feed_interactions ? 
        (data.metrics.feed_interactions.count / data.metrics.http_reqs.count * 100).toFixed(2) + '%' : '0%',
      avgContentLoadTime: data.metrics.content_load_time ? 
        data.metrics.content_load_time.avg.toFixed(2) + 'ms' : 'N/A',
    },
  };
  
  return {
    'social-feed-summary.json': JSON.stringify(summary, null, 2),
    stdout: `
=== Social Feed Load Test Results ===
Total Requests: ${data.metrics.http_reqs.count}
Failed Requests: ${data.metrics.http_req_failed.count} (${(data.metrics.http_req_failed.rate * 100).toFixed(2)}%)
Avg Response Time: ${data.metrics.http_req_duration.avg.toFixed(2)}ms
P95 Response Time: ${data.metrics['http_req_duration{p(95)}'].toFixed(2)}ms
Cache Hit Rate: ${data.metrics.cache_hits ? (data.metrics.cache_hits.rate * 100).toFixed(2) : 'N/A'}%
User Interactions: ${data.metrics.feed_interactions ? data.metrics.feed_interactions.count : 0}
Interaction Rate: ${data.metrics.feed_interactions ? (data.metrics.feed_interactions.count / data.metrics.http_reqs.count * 100).toFixed(2) : 0}%
=====================================
`
  };
}
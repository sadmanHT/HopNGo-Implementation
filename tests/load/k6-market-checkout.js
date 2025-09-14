import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween, randomItem, randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const checkoutLatency = new Trend('checkout_latency');
const paymentProcessingTime = new Trend('payment_processing_time');
const inventoryCheckTime = new Trend('inventory_check_time');
const successfulCheckouts = new Counter('successful_checkouts');
const failedCheckouts = new Counter('failed_checkouts');

// Test configuration
export const options = {
  scenarios: {
    // Sustained checkout load - 500 RPS
    sustained_checkout: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 100,
      maxVUs: 300,
    },
    // Peak shopping periods simulation
    peak_shopping: {
      executor: 'ramping-arrival-rate',
      startRate: 200,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 600,
      stages: [
        { duration: '2m', target: 200 }, // Normal load
        { duration: '3m', target: 500 }, // Peak starts
        { duration: '5m', target: 800 }, // Peak load
        { duration: '3m', target: 1200 }, // Flash sale simulation
        { duration: '2m', target: 500 }, // Peak ends
        { duration: '3m', target: 200 }, // Back to normal
        { duration: '2m', target: 0 }, // Cool down
      ],
    },
  },
  thresholds: {
    'errors': ['rate<0.01'], // Less than 1% error rate
    'http_req_duration': ['p(95)<400'], // P95 < 400ms
    'http_req_duration': ['avg<200'], // Average < 200ms
    'checkout_latency': ['p(95)<400'],
    'payment_processing_time': ['p(95)<300'],
    'inventory_check_time': ['p(95)<100'],
    'successful_checkouts': ['count>0'],
  },
};

// Test data
const products = [
  { id: 'prod_001', name: 'City Walking Tour', price: 29.99, category: 'tour' },
  { id: 'prod_002', name: 'Food Tasting Experience', price: 89.99, category: 'food' },
  { id: 'prod_003', name: 'Adventure Package', price: 149.99, category: 'adventure' },
  { id: 'prod_004', name: 'Cultural Workshop', price: 59.99, category: 'cultural' },
  { id: 'prod_005', name: 'Nightlife Tour', price: 79.99, category: 'nightlife' },
  { id: 'prod_006', name: 'Wellness Retreat', price: 199.99, category: 'wellness' },
  { id: 'prod_007', name: 'Photography Session', price: 119.99, category: 'photography' },
  { id: 'prod_008', name: 'Cooking Class', price: 69.99, category: 'food' },
  { id: 'prod_009', name: 'Hiking Adventure', price: 99.99, category: 'outdoor' },
  { id: 'prod_010', name: 'Art Gallery Tour', price: 39.99, category: 'cultural' },
];

const paymentMethods = [
  { type: 'credit_card', provider: 'visa' },
  { type: 'credit_card', provider: 'mastercard' },
  { type: 'credit_card', provider: 'amex' },
  { type: 'digital_wallet', provider: 'paypal' },
  { type: 'digital_wallet', provider: 'apple_pay' },
  { type: 'digital_wallet', provider: 'google_pay' },
];

const countries = ['US', 'CA', 'GB', 'DE', 'FR', 'AU', 'JP', 'BR', 'IN', 'MX'];
const currencies = ['USD', 'CAD', 'GBP', 'EUR', 'EUR', 'AUD', 'JPY', 'BRL', 'INR', 'MXN'];

function generateCheckoutData() {
  const selectedProducts = [];
  const numProducts = randomIntBetween(1, 4);
  
  for (let i = 0; i < numProducts; i++) {
    const product = randomItem(products);
    const quantity = randomIntBetween(1, 3);
    selectedProducts.push({
      productId: product.id,
      quantity: quantity,
      price: product.price,
      selectedDate: new Date(Date.now() + randomIntBetween(1, 60) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      selectedTime: `${randomIntBetween(9, 18)}:${randomIntBetween(0, 59).toString().padStart(2, '0')}`,
      participants: randomIntBetween(1, 6),
    });
  }
  
  const paymentMethod = randomItem(paymentMethods);
  const countryIndex = randomIntBetween(0, countries.length - 1);
  
  return {
    sessionId: `session_${randomString(16)}`,
    items: selectedProducts,
    customer: {
      email: `test.user.${randomString(8)}@example.com`,
      firstName: randomItem(['John', 'Jane', 'Mike', 'Sarah', 'David', 'Lisa', 'Chris', 'Emma']),
      lastName: randomItem(['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis']),
      phone: `+1${randomIntBetween(1000000000, 9999999999)}`,
    },
    billing: {
      address: `${randomIntBetween(100, 9999)} ${randomItem(['Main', 'Oak', 'Pine', 'Elm', 'Cedar'])} St`,
      city: randomItem(['New York', 'Los Angeles', 'Chicago', 'Houston', 'Phoenix']),
      state: randomItem(['NY', 'CA', 'IL', 'TX', 'AZ']),
      zipCode: randomIntBetween(10000, 99999).toString(),
      country: countries[countryIndex],
    },
    payment: {
      method: paymentMethod.type,
      provider: paymentMethod.provider,
      currency: currencies[countryIndex],
      // Mock payment details (never use real payment info in tests)
      cardNumber: paymentMethod.type === 'credit_card' ? '4111111111111111' : undefined,
      expiryMonth: paymentMethod.type === 'credit_card' ? randomIntBetween(1, 12) : undefined,
      expiryYear: paymentMethod.type === 'credit_card' ? randomIntBetween(2024, 2030) : undefined,
      cvv: paymentMethod.type === 'credit_card' ? randomIntBetween(100, 999).toString() : undefined,
      walletToken: paymentMethod.type === 'digital_wallet' ? `token_${randomString(32)}` : undefined,
    },
    preferences: {
      newsletter: Math.random() > 0.5,
      smsUpdates: Math.random() > 0.7,
      specialOffers: Math.random() > 0.3,
    },
    promoCode: Math.random() > 0.8 ? randomItem(['SAVE10', 'WELCOME20', 'SUMMER15', 'FIRST25']) : undefined,
    giftMessage: Math.random() > 0.9 ? 'Happy Birthday! Enjoy your experience!' : undefined,
  };
}

export function setup() {
  console.log('Setting up checkout load test...');
  
  // Verify the checkout endpoint is accessible
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  const healthCheck = http.get(`${baseUrl}/api/health`);
  
  if (healthCheck.status !== 200) {
    console.error(`Health check failed: ${healthCheck.status}`);
  }
  
  return {
    baseUrl: baseUrl,
    authToken: __ENV.AUTH_TOKEN || null,
    testMode: __ENV.TEST_MODE || 'load', // 'load' or 'stress'
  };
}

export default function(data) {
  const checkoutData = generateCheckoutData();
  const url = `${data.baseUrl}/api/market/checkout`;
  
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'User-Agent': 'k6-checkout-test/1.0',
    'X-Session-ID': checkoutData.sessionId,
  };
  
  if (data.authToken) {
    headers['Authorization'] = `Bearer ${data.authToken}`;
  }
  
  const startTime = Date.now();
  const response = http.post(url, JSON.stringify(checkoutData), { headers });
  const endTime = Date.now();
  
  // Record custom metrics
  checkoutLatency.add(endTime - startTime);
  
  // Extract processing times from response headers
  const paymentTime = response.headers['X-Payment-Processing-Time'];
  if (paymentTime) {
    paymentProcessingTime.add(parseFloat(paymentTime));
  }
  
  const inventoryTime = response.headers['X-Inventory-Check-Time'];
  if (inventoryTime) {
    inventoryCheckTime.add(parseFloat(inventoryTime));
  }
  
  // Comprehensive checkout validation
  const isSuccess = check(response, {
    'checkout status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response time < 400ms': (r) => r.timings.duration < 400,
    'response time < 200ms (avg target)': (r) => r.timings.duration < 200,
    'has order confirmation': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.orderId && body.status;
      } catch (e) {
        return false;
      }
    },
    'has payment confirmation': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.payment && (body.payment.status === 'completed' || body.payment.status === 'pending');
      } catch (e) {
        return false;
      }
    },
    'has total amount': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.total && typeof body.total === 'number' && body.total > 0;
      } catch (e) {
        return false;
      }
    },
    'has confirmation email sent': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.notifications && body.notifications.email === true;
      } catch (e) {
        return false;
      }
    },
    'inventory reserved': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.inventory && body.inventory.reserved === true;
      } catch (e) {
        return false;
      }
    },
    'no server errors': (r) => r.status < 500,
    'response size reasonable': (r) => r.body.length < 512 * 1024, // Less than 512KB
  });
  
  // Record success/failure metrics
  if (isSuccess) {
    successfulCheckouts.add(1);
  } else {
    failedCheckouts.add(1);
    console.error(`Checkout failed: ${response.status} - ${response.body.substring(0, 300)}`);
  }
  
  errorRate.add(!isSuccess);
  
  // Handle specific error scenarios
  if (response.status === 409) {
    console.log('Inventory conflict detected - this is expected under high load');
  } else if (response.status === 429) {
    console.log('Rate limit hit - backing off');
    sleep(randomIntBetween(2, 5));
  } else if (response.status >= 500) {
    console.error(`Server error: ${response.status}`);
  }
  
  // Simulate user behavior after checkout
  if (isSuccess) {
    // User might check order status
    sleep(randomIntBetween(1, 2));
    
    // 30% chance to check order status
    if (Math.random() > 0.7) {
      try {
        const orderResponse = JSON.parse(response.body);
        if (orderResponse.orderId) {
          const statusUrl = `${data.baseUrl}/api/orders/${orderResponse.orderId}/status`;
          const statusCheck = http.get(statusUrl, { headers });
          check(statusCheck, {
            'order status check successful': (r) => r.status === 200,
          });
        }
      } catch (e) {
        // Ignore parsing errors for status check
      }
    }
  }
  
  // Think time between requests
  sleep(randomIntBetween(2, 5));
}

export function teardown(data) {
  console.log('Checkout load test completed');
  console.log(`Test mode: ${data.testMode}`);
  console.log(`Base URL: ${data.baseUrl}`);
}

export function handleSummary(data) {
  const summary = {
    testType: 'Market Checkout Load Test',
    timestamp: new Date().toISOString(),
    duration: data.state.testRunDurationMs,
    scenarios: Object.keys(options.scenarios),
    metrics: {
      http_reqs: data.metrics.http_reqs,
      http_req_duration: data.metrics.http_req_duration,
      http_req_failed: data.metrics.http_req_failed,
      errors: data.metrics.errors,
      checkout_latency: data.metrics.checkout_latency,
      payment_processing_time: data.metrics.payment_processing_time,
      inventory_check_time: data.metrics.inventory_check_time,
      successful_checkouts: data.metrics.successful_checkouts,
      failed_checkouts: data.metrics.failed_checkouts,
    },
    thresholds: data.thresholds,
    businessMetrics: {
      conversionRate: data.metrics.successful_checkouts ? 
        (data.metrics.successful_checkouts.count / data.metrics.http_reqs.count * 100).toFixed(2) + '%' : '0%',
      averageOrderValue: 'N/A', // Would need to be calculated from actual order data
    },
  };
  
  return {
    'checkout-summary.json': JSON.stringify(summary, null, 2),
    stdout: `
=== Market Checkout Load Test Results ===
Total Requests: ${data.metrics.http_reqs.count}
Successful Checkouts: ${data.metrics.successful_checkouts ? data.metrics.successful_checkouts.count : 0}
Failed Checkouts: ${data.metrics.failed_checkouts ? data.metrics.failed_checkouts.count : 0}
Error Rate: ${(data.metrics.http_req_failed.rate * 100).toFixed(2)}%
Avg Response Time: ${data.metrics.http_req_duration.avg.toFixed(2)}ms
P95 Response Time: ${data.metrics['http_req_duration{p(95)}'].toFixed(2)}ms
Conversion Rate: ${data.metrics.successful_checkouts ? (data.metrics.successful_checkouts.count / data.metrics.http_reqs.count * 100).toFixed(2) : 0}%
==========================================
`
  };
}
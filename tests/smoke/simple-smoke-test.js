const axios = require('axios');

// Configuration
const config = {
  baseUrl: process.env.BASE_URL || 'http://localhost:8081',
  timeout: 10000
};

// HTTP client
const httpClient = axios.create({
  baseURL: config.baseUrl,
  timeout: config.timeout,
  headers: {
    'Content-Type': 'application/json',
    'User-Agent': 'HopNGo-SmokeTest/1.0'
  }
});

// Simple smoke tests for public endpoints
async function runSmokeTests() {
  console.log('🚀 Starting HopNGo Simple Smoke Tests');
  console.log(`🔗 Base URL: ${config.baseUrl}`);
  
  const tests = [
    {
      name: 'Gateway Health Check',
      endpoint: '/actuator/health',
      expectedStatus: 200
    }
  ];
  
  let passed = 0;
  let failed = 0;
  
  for (const test of tests) {
    try {
      console.log(`\n🧪 Testing: ${test.name}`);
      const response = await httpClient.get(test.endpoint);
      
      if (response.status === test.expectedStatus) {
        console.log(`✅ ${test.name} - Status: ${response.status}`);
        if (response.data) {
          console.log(`   Response: ${JSON.stringify(response.data, null, 2)}`);
        }
        passed++;
      } else {
        console.log(`❌ ${test.name} - Expected: ${test.expectedStatus}, Got: ${response.status}`);
        failed++;
      }
    } catch (error) {
      console.log(`❌ ${test.name} - Error: ${error.message}`);
      if (error.response) {
        console.log(`   Status: ${error.response.status}`);
        console.log(`   Data: ${JSON.stringify(error.response.data, null, 2)}`);
      }
      failed++;
    }
  }
  
  console.log(`\n📊 Test Results:`);
  console.log(`   ✅ Passed: ${passed}`);
  console.log(`   ❌ Failed: ${failed}`);
  
  if (failed === 0) {
    console.log('\n🎉 All smoke tests passed!');
    process.exit(0);
  } else {
    console.log('\n💥 Some tests failed!');
    process.exit(1);
  }
}

// Run the tests
runSmokeTests().catch(error => {
  console.error('💥 Smoke tests crashed:', error);
  process.exit(1);
});
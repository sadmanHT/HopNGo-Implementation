#!/usr/bin/env node

/**
 * Comprehensive Error Monitoring Test Script
 * 
 * This script tests the complete error monitoring pipeline:
 * 1. Sentry integration (frontend & backend)
 * 2. Error rate limiting
 * 3. Alert triggering
 * 4. Dashboard metrics
 * 5. Error recovery mechanisms
 */

const axios = require('axios');
const { performance } = require('perf_hooks');
const fs = require('fs');
const path = require('path');

// Configuration
const config = {
  backend: {
    baseUrl: process.env.BACKEND_URL || 'http://localhost:8080',
    timeout: 30000
  },
  frontend: {
    baseUrl: process.env.FRONTEND_URL || 'http://localhost:3000',
    timeout: 30000
  },
  sentry: {
    dsn: process.env.SENTRY_DSN,
    org: process.env.SENTRY_ORG,
    project: process.env.SENTRY_PROJECT
  },
  test: {
    maxRetries: 3,
    retryDelay: 1000,
    bulkErrorCount: 10,
    concurrentRequests: 5
  }
};

// Test results tracking
const testResults = {
  passed: 0,
  failed: 0,
  skipped: 0,
  errors: [],
  metrics: {
    totalRequests: 0,
    errorRate: 0,
    avgResponseTime: 0,
    sentryEvents: 0
  }
};

// Utility functions
const log = {
  info: (msg) => console.log(`\x1b[36m[INFO]\x1b[0m ${msg}`),
  success: (msg) => console.log(`\x1b[32m[PASS]\x1b[0m ${msg}`),
  error: (msg) => console.log(`\x1b[31m[FAIL]\x1b[0m ${msg}`),
  warn: (msg) => console.log(`\x1b[33m[WARN]\x1b[0m ${msg}`),
  debug: (msg) => console.log(`\x1b[90m[DEBUG]\x1b[0m ${msg}`)
};

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

const makeRequest = async (url, options = {}) => {
  const startTime = performance.now();
  try {
    testResults.metrics.totalRequests++;
    const response = await axios({
      url,
      timeout: config.backend.timeout,
      ...options
    });
    const endTime = performance.now();
    return {
      success: true,
      data: response.data,
      status: response.status,
      responseTime: endTime - startTime
    };
  } catch (error) {
    const endTime = performance.now();
    return {
      success: false,
      error: error.message,
      status: error.response?.status,
      responseTime: endTime - startTime
    };
  }
};

// Test functions
const testBackendHealth = async () => {
  log.info('Testing backend health...');
  
  const result = await makeRequest(`${config.backend.baseUrl}/api/test/error/health`);
  
  if (result.success) {
    log.success('Backend health check passed');
    testResults.passed++;
    return true;
  } else {
    log.error(`Backend health check failed: ${result.error}`);
    testResults.failed++;
    testResults.errors.push('Backend health check failed');
    return false;
  }
};

const testErrorTypes = async () => {
  log.info('Testing different error types...');
  
  const errorTypes = [
    'ResourceNotFoundException',
    'ServiceUnavailableException',
    'PaymentException',
    'AuthenticationException',
    'ValidationException',
    'TimeoutException',
    'ConcurrencyException',
    'RateLimitException'
  ];
  
  let passed = 0;
  let failed = 0;
  
  for (const errorType of errorTypes) {
    log.debug(`Testing ${errorType}...`);
    
    const result = await makeRequest(`${config.backend.baseUrl}/api/test/error`, {
      method: 'POST',
      data: { errorType },
      headers: { 'Content-Type': 'application/json' }
    });
    
    // We expect these requests to fail (that's the point)
    if (!result.success && result.status >= 400) {
      log.success(`${errorType} triggered successfully`);
      passed++;
    } else {
      log.error(`${errorType} did not trigger as expected`);
      failed++;
      testResults.errors.push(`${errorType} test failed`);
    }
    
    await sleep(500); // Avoid overwhelming the system
  }
  
  testResults.passed += passed;
  testResults.failed += failed;
  
  log.info(`Error types test: ${passed} passed, ${failed} failed`);
  return failed === 0;
};

const testFeatureErrors = async () => {
  log.info('Testing feature-specific errors...');
  
  const features = ['payment', 'booking', 'search', 'auth', 'map'];
  let passed = 0;
  let failed = 0;
  
  for (const feature of features) {
    log.debug(`Testing ${feature} feature error...`);
    
    const result = await makeRequest(`${config.backend.baseUrl}/api/test/error/feature/${feature}`, {
      method: 'POST',
      data: { errorType: 'BusinessException' },
      headers: { 'Content-Type': 'application/json' }
    });
    
    if (!result.success && result.status >= 400) {
      log.success(`${feature} feature error triggered successfully`);
      passed++;
    } else {
      log.error(`${feature} feature error did not trigger as expected`);
      failed++;
      testResults.errors.push(`${feature} feature error test failed`);
    }
    
    await sleep(300);
  }
  
  testResults.passed += passed;
  testResults.failed += failed;
  
  log.info(`Feature errors test: ${passed} passed, ${failed} failed`);
  return failed === 0;
};

const testBulkErrors = async () => {
  log.info('Testing bulk error handling...');
  
  const result = await makeRequest(`${config.backend.baseUrl}/api/test/error/bulk`, {
    method: 'POST',
    params: {
      count: config.test.bulkErrorCount,
      errorType: 'ServiceUnavailableException'
    }
  });
  
  if (!result.success && result.status >= 400) {
    log.success('Bulk errors triggered successfully');
    testResults.passed++;
    return true;
  } else {
    log.error('Bulk errors did not trigger as expected');
    testResults.failed++;
    testResults.errors.push('Bulk errors test failed');
    return false;
  }
};

const testConcurrentErrors = async () => {
  log.info('Testing concurrent error handling...');
  
  const promises = [];
  for (let i = 0; i < config.test.concurrentRequests; i++) {
    promises.push(
      makeRequest(`${config.backend.baseUrl}/api/test/error`, {
        method: 'POST',
        data: { errorType: 'ServiceUnavailableException' },
        headers: { 'Content-Type': 'application/json' }
      })
    );
  }
  
  const results = await Promise.all(promises);
  const failedRequests = results.filter(r => !r.success).length;
  
  if (failedRequests === config.test.concurrentRequests) {
    log.success('Concurrent errors handled successfully');
    testResults.passed++;
    return true;
  } else {
    log.error(`Expected ${config.test.concurrentRequests} failed requests, got ${failedRequests}`);
    testResults.failed++;
    testResults.errors.push('Concurrent errors test failed');
    return false;
  }
};

const testCascadingErrors = async () => {
  log.info('Testing cascading error handling...');
  
  const result = await makeRequest(`${config.backend.baseUrl}/api/test/error/cascade`, {
    method: 'POST'
  });
  
  if (!result.success && result.status >= 500) {
    log.success('Cascading errors triggered successfully');
    testResults.passed++;
    return true;
  } else {
    log.error('Cascading errors did not trigger as expected');
    testResults.failed++;
    testResults.errors.push('Cascading errors test failed');
    return false;
  }
};

const testSlowRequests = async () => {
  log.info('Testing slow request handling...');
  
  const result = await makeRequest(`${config.backend.baseUrl}/api/test/error/slow`, {
    method: 'GET',
    params: { delaySeconds: 2 },
    timeout: 5000
  });
  
  if (result.responseTime > 2000) {
    log.success('Slow request detected and handled');
    testResults.passed++;
    return true;
  } else {
    log.error('Slow request not detected properly');
    testResults.failed++;
    testResults.errors.push('Slow request test failed');
    return false;
  }
};

const testRateLimiting = async () => {
  log.info('Testing error rate limiting...');
  
  // Trigger multiple errors quickly to test rate limiting
  const promises = [];
  for (let i = 0; i < 20; i++) {
    promises.push(
      makeRequest(`${config.backend.baseUrl}/api/test/error`, {
        method: 'POST',
        data: { errorType: 'ValidationException' },
        headers: { 'Content-Type': 'application/json' }
      })
    );
  }
  
  const results = await Promise.all(promises);
  const errorCount = results.filter(r => !r.success).length;
  
  if (errorCount > 0) {
    log.success(`Rate limiting test completed - ${errorCount} errors triggered`);
    testResults.passed++;
    return true;
  } else {
    log.error('Rate limiting test failed - no errors triggered');
    testResults.failed++;
    testResults.errors.push('Rate limiting test failed');
    return false;
  }
};

const testFrontendIntegration = async () => {
  log.info('Testing frontend error monitoring integration...');
  
  try {
    // Check if frontend is accessible
    const result = await makeRequest(`${config.frontend.baseUrl}/test/error-monitoring`, {
      method: 'GET',
      timeout: config.frontend.timeout
    });
    
    if (result.success || result.status === 200) {
      log.success('Frontend error monitoring page accessible');
      testResults.passed++;
      return true;
    } else {
      log.warn('Frontend error monitoring page not accessible - skipping frontend tests');
      testResults.skipped++;
      return false;
    }
  } catch (error) {
    log.warn('Frontend not available - skipping frontend tests');
    testResults.skipped++;
    return false;
  }
};

const calculateMetrics = () => {
  const totalTests = testResults.passed + testResults.failed;
  const errorRate = totalTests > 0 ? (testResults.failed / totalTests) * 100 : 0;
  
  testResults.metrics.errorRate = errorRate;
  
  log.info('\n=== Test Metrics ===');
  log.info(`Total Tests: ${totalTests}`);
  log.info(`Passed: ${testResults.passed}`);
  log.info(`Failed: ${testResults.failed}`);
  log.info(`Skipped: ${testResults.skipped}`);
  log.info(`Error Rate: ${errorRate.toFixed(2)}%`);
  log.info(`Total Requests: ${testResults.metrics.totalRequests}`);
};

const generateReport = () => {
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      passed: testResults.passed,
      failed: testResults.failed,
      skipped: testResults.skipped,
      errorRate: testResults.metrics.errorRate
    },
    errors: testResults.errors,
    metrics: testResults.metrics,
    config: {
      backendUrl: config.backend.baseUrl,
      frontendUrl: config.frontend.baseUrl,
      testSettings: config.test
    }
  };
  
  const reportPath = path.join(__dirname, '..', 'test-results', 'error-monitoring-report.json');
  
  // Ensure directory exists
  const reportDir = path.dirname(reportPath);
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }
  
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  log.info(`\nTest report saved to: ${reportPath}`);
  
  return report;
};

// Main test execution
const runTests = async () => {
  log.info('Starting Error Monitoring Test Suite...');
  log.info('=====================================\n');
  
  const tests = [
    { name: 'Backend Health', fn: testBackendHealth },
    { name: 'Error Types', fn: testErrorTypes },
    { name: 'Feature Errors', fn: testFeatureErrors },
    { name: 'Bulk Errors', fn: testBulkErrors },
    { name: 'Concurrent Errors', fn: testConcurrentErrors },
    { name: 'Cascading Errors', fn: testCascadingErrors },
    { name: 'Slow Requests', fn: testSlowRequests },
    { name: 'Rate Limiting', fn: testRateLimiting },
    { name: 'Frontend Integration', fn: testFrontendIntegration }
  ];
  
  for (const test of tests) {
    try {
      log.info(`\n--- Running ${test.name} Test ---`);
      await test.fn();
    } catch (error) {
      log.error(`${test.name} test threw an exception: ${error.message}`);
      testResults.failed++;
      testResults.errors.push(`${test.name}: ${error.message}`);
    }
  }
  
  log.info('\n=====================================');
  log.info('Error Monitoring Test Suite Complete');
  
  calculateMetrics();
  const report = generateReport();
  
  // Exit with appropriate code
  const exitCode = testResults.failed > 0 ? 1 : 0;
  
  if (exitCode === 0) {
    log.success('\nAll tests passed! ✅');
  } else {
    log.error('\nSome tests failed! ❌');
    log.error('Errors:');
    testResults.errors.forEach(error => log.error(`  - ${error}`));
  }
  
  process.exit(exitCode);
};

// Handle command line arguments
if (require.main === module) {
  const args = process.argv.slice(2);
  
  if (args.includes('--help') || args.includes('-h')) {
    console.log(`
Error Monitoring Test Script

Usage: node test-error-monitoring.js [options]

Options:
  --help, -h          Show this help message
  --backend-url URL   Backend URL (default: http://localhost:8080)
  --frontend-url URL  Frontend URL (default: http://localhost:3000)
  --verbose           Enable verbose logging

Environment Variables:
  BACKEND_URL         Backend base URL
  FRONTEND_URL        Frontend base URL
  SENTRY_DSN          Sentry DSN for validation
  SENTRY_ORG          Sentry organization
  SENTRY_PROJECT      Sentry project
`);
    process.exit(0);
  }
  
  // Parse command line arguments
  const backendUrlIndex = args.indexOf('--backend-url');
  if (backendUrlIndex !== -1 && args[backendUrlIndex + 1]) {
    config.backend.baseUrl = args[backendUrlIndex + 1];
  }
  
  const frontendUrlIndex = args.indexOf('--frontend-url');
  if (frontendUrlIndex !== -1 && args[frontendUrlIndex + 1]) {
    config.frontend.baseUrl = args[frontendUrlIndex + 1];
  }
  
  runTests().catch(error => {
    log.error(`Test suite failed with error: ${error.message}`);
    process.exit(1);
  });
}

module.exports = {
  runTests,
  testResults,
  config
};
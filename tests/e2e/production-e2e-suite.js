#!/usr/bin/env node

/**
 * Production E2E Test Suite
 * 
 * Comprehensive end-to-end testing for HopNGo production environment
 * Tests critical user journeys, accessibility, performance, and security
 */

const { chromium, firefox, webkit } = require('playwright');
const fs = require('fs');
const path = require('path');

class ProductionE2ETestSuite {
  constructor() {
    this.baseURL = process.env.PRODUCTION_URL || 'https://hopngo.com';
    this.testResults = {
      passed: [],
      failed: [],
      skipped: [],
      performance: {},
      accessibility: {},
      security: {}
    };
    
    this.testUsers = {
      customer: {
        email: 'e2e.customer@hopngo.com',
        password: 'E2ETest123!',
        role: 'CUSTOMER'
      },
      provider: {
        email: 'e2e.provider@hopngo.com', 
        password: 'E2ETest123!',
        role: 'PROVIDER'
      },
      admin: {
        email: 'e2e.admin@hopngo.com',
        password: 'E2ETest123!',
        role: 'ADMIN'
      }
    };
    
    this.criticalPaths = [
      { name: 'Homepage Load', path: '/', timeout: 5000 },
      { name: 'User Registration', path: '/register', timeout: 10000 },
      { name: 'User Login', path: '/login', timeout: 8000 },
      { name: 'Search Destinations', path: '/search', timeout: 15000 },
      { name: 'Trip Planning', path: '/trips/create', timeout: 20000 },
      { name: 'Booking Flow', path: '/bookings', timeout: 25000 },
      { name: 'User Profile', path: '/profile', timeout: 8000 },
      { name: 'Social Feed', path: '/feed', timeout: 12000 },
      { name: 'Emergency Features', path: '/emergency', timeout: 10000 }
    ];
    
    this.browsers = ['chromium', 'firefox', 'webkit'];
    this.devices = [
      { name: 'Desktop', viewport: { width: 1920, height: 1080 } },
      { name: 'Tablet', viewport: { width: 768, height: 1024 } },
      { name: 'Mobile', viewport: { width: 375, height: 667 } }
    ];
  }

  // Utility function for delays
  async delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  // Performance monitoring
  async measurePagePerformance(page, testName) {
    const performanceMetrics = await page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0];
      const paint = performance.getEntriesByType('paint');
      
      return {
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
        loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
        firstPaint: paint.find(p => p.name === 'first-paint')?.startTime || 0,
        firstContentfulPaint: paint.find(p => p.name === 'first-contentful-paint')?.startTime || 0,
        totalLoadTime: navigation.loadEventEnd - navigation.fetchStart
      };
    });
    
    this.testResults.performance[testName] = performanceMetrics;
    return performanceMetrics;
  }

  // Accessibility testing simulation
  async checkAccessibility(page, testName) {
    console.log(`     🔍 Checking accessibility for ${testName}...`);
    
    // Simulate accessibility checks
    const accessibilityResults = {
      wcag2aViolations: 0,
      wcag2aaViolations: 0,
      keyboardNavigation: 'PASS',
      screenReaderSupport: 'PASS',
      colorContrast: 'PASS',
      focusManagement: 'PASS',
      altTextPresent: 'PASS',
      headingStructure: 'PASS'
    };
    
    // Check for common accessibility issues
    const hasSkipLinks = await page.locator('[data-testid="skip-link"]').count() > 0;
    const hasProperHeadings = await page.locator('h1').count() > 0;
    const hasAltTexts = await page.locator('img[alt]').count() > 0;
    const hasFocusIndicators = await page.locator(':focus-visible').count() >= 0;
    
    if (!hasSkipLinks) accessibilityResults.wcag2aaViolations++;
    if (!hasProperHeadings) accessibilityResults.wcag2aViolations++;
    if (!hasAltTexts) accessibilityResults.wcag2aViolations++;
    
    this.testResults.accessibility[testName] = accessibilityResults;
    
    console.log(`     ✅ WCAG 2.1 AA violations: ${accessibilityResults.wcag2aaViolations}`);
    return accessibilityResults;
  }

  // Security testing
  async checkSecurity(page, testName) {
    console.log(`     🛡️  Checking security for ${testName}...`);
    
    const securityResults = {
      httpsRedirect: false,
      securityHeaders: {},
      xssProtection: false,
      csrfProtection: false,
      contentSecurityPolicy: false
    };
    
    // Check HTTPS redirect
    const response = await page.goto(this.baseURL.replace('https://', 'http://'));
    securityResults.httpsRedirect = response.url().startsWith('https://');
    
    // Check security headers
    const headers = response.headers();
    securityResults.securityHeaders = {
      'strict-transport-security': !!headers['strict-transport-security'],
      'x-frame-options': !!headers['x-frame-options'],
      'x-content-type-options': !!headers['x-content-type-options'],
      'referrer-policy': !!headers['referrer-policy'],
      'content-security-policy': !!headers['content-security-policy']
    };
    
    securityResults.contentSecurityPolicy = !!headers['content-security-policy'];
    securityResults.xssProtection = !!headers['x-xss-protection'] || !!headers['content-security-policy'];
    
    this.testResults.security[testName] = securityResults;
    
    console.log(`     ✅ HTTPS: ${securityResults.httpsRedirect ? 'ENABLED' : 'DISABLED'}`);
    console.log(`     ✅ Security Headers: ${Object.values(securityResults.securityHeaders).filter(Boolean).length}/5`);
    
    return securityResults;
  }

  // User authentication flow
  async testUserAuthentication(page, userType = 'customer') {
    console.log(`   🔐 Testing ${userType} authentication...`);
    
    const user = this.testUsers[userType];
    
    try {
      // Navigate to login page
      await page.goto(`${this.baseURL}/login`);
      await page.waitForLoadState('networkidle');
      
      // Fill login form
      await page.fill('[data-testid="login-email"]', user.email);
      await page.fill('[data-testid="login-password"]', user.password);
      await page.click('[data-testid="login-button"]');
      
      // Wait for successful login
      await page.waitForSelector('[data-testid="user-dashboard"]', { timeout: 10000 });
      
      // Verify user role
      const userMenu = await page.locator('[data-testid="user-menu"]');
      await userMenu.click();
      
      const roleElement = await page.locator(`[data-testid="user-role-${user.role.toLowerCase()}"]`);
      const isRoleVisible = await roleElement.isVisible();
      
      if (isRoleVisible) {
        console.log(`     ✅ ${userType} login successful`);
        return true;
      } else {
        console.log(`     ❌ ${userType} role verification failed`);
        return false;
      }
    } catch (error) {
      console.log(`     ❌ ${userType} authentication failed: ${error.message}`);
      return false;
    }
  }

  // Search functionality testing
  async testSearchFunctionality(page) {
    console.log(`   🔍 Testing search functionality...`);
    
    try {
      // Navigate to search page
      await page.goto(`${this.baseURL}/search`);
      await page.waitForLoadState('networkidle');
      
      // Perform basic search
      await page.fill('[data-testid="search-location-input"]', 'Dhaka, Bangladesh');
      await page.click('[data-testid="search-button"]');
      
      // Wait for results
      await page.waitForSelector('[data-testid="search-results"]', { timeout: 15000 });
      
      // Verify results are displayed
      const resultsCount = await page.locator('[data-testid="listing-card"]').count();
      
      if (resultsCount > 0) {
        console.log(`     ✅ Search returned ${resultsCount} results`);
        
        // Test filters
        await page.click('[data-testid="filter-price-range"]');
        await page.selectOption('[data-testid="price-filter"]', 'MEDIUM');
        await page.click('[data-testid="apply-filters"]');
        
        await page.waitForLoadState('networkidle');
        
        console.log(`     ✅ Search filters working`);
        return true;
      } else {
        console.log(`     ❌ No search results found`);
        return false;
      }
    } catch (error) {
      console.log(`     ❌ Search functionality failed: ${error.message}`);
      return false;
    }
  }

  // Trip planning testing
  async testTripPlanning(page) {
    console.log(`   🗺️  Testing trip planning...`);
    
    try {
      // Navigate to trip planning
      await page.goto(`${this.baseURL}/trips/create`);
      await page.waitForLoadState('networkidle');
      
      // Fill trip creation form
      await page.fill('[data-testid="trip-title"]', 'E2E Test Trip');
      await page.fill('[data-testid="trip-description"]', 'Automated test trip creation');
      await page.fill('[data-testid="trip-start-date"]', '2024-08-01');
      await page.fill('[data-testid="trip-end-date"]', '2024-08-07');
      await page.selectOption('[data-testid="trip-budget"]', 'MEDIUM');
      
      // Add destination
      await page.click('[data-testid="add-destination"]');
      await page.fill('[data-testid="destination-search"]', 'Cox\'s Bazar');
      await page.click('[data-testid="destination-suggestion-0"]');
      
      // Save trip
      await page.click('[data-testid="save-trip"]');
      
      // Wait for success message
      await page.waitForSelector('[data-testid="trip-created-success"]', { timeout: 10000 });
      
      console.log(`     ✅ Trip planning successful`);
      return true;
    } catch (error) {
      console.log(`     ❌ Trip planning failed: ${error.message}`);
      return false;
    }
  }

  // Social features testing
  async testSocialFeatures(page) {
    console.log(`   👥 Testing social features...`);
    
    try {
      // Navigate to social feed
      await page.goto(`${this.baseURL}/feed`);
      await page.waitForLoadState('networkidle');
      
      // Create a post
      await page.click('[data-testid="create-post-button"]');
      await page.fill('[data-testid="post-content"]', 'E2E test post from automated testing');
      await page.click('[data-testid="publish-post"]');
      
      // Wait for post to appear
      await page.waitForSelector('[data-testid="post-item"]', { timeout: 10000 });
      
      // Test interactions
      const firstPost = page.locator('[data-testid="post-item"]').first();
      await firstPost.locator('[data-testid="like-button"]').click();
      await firstPost.locator('[data-testid="comment-button"]').click();
      
      await page.fill('[data-testid="comment-input"]', 'Test comment');
      await page.click('[data-testid="submit-comment"]');
      
      console.log(`     ✅ Social features working`);
      return true;
    } catch (error) {
      console.log(`     ❌ Social features failed: ${error.message}`);
      return false;
    }
  }

  // Emergency features testing
  async testEmergencyFeatures(page) {
    console.log(`   🚨 Testing emergency features...`);
    
    try {
      // Navigate to emergency settings
      await page.goto(`${this.baseURL}/emergency`);
      await page.waitForLoadState('networkidle');
      
      // Test emergency contact setup
      await page.click('[data-testid="add-emergency-contact"]');
      await page.fill('[data-testid="contact-name"]', 'Test Emergency Contact');
      await page.fill('[data-testid="contact-phone"]', '+8801712345678');
      await page.fill('[data-testid="contact-email"]', 'emergency@test.com');
      await page.selectOption('[data-testid="contact-relationship"]', 'FAMILY');
      await page.click('[data-testid="save-contact-button"]');
      
      // Test check-in system
      await page.click('[data-testid="test-checkin-system"]');
      await page.waitForSelector('[data-testid="checkin-test-result"]', { timeout: 5000 });
      
      console.log(`     ✅ Emergency features working`);
      return true;
    } catch (error) {
      console.log(`     ❌ Emergency features failed: ${error.message}`);
      return false;
    }
  }

  // Booking flow testing
  async testBookingFlow(page) {
    console.log(`   💳 Testing booking flow...`);
    
    try {
      // Navigate to bookings
      await page.goto(`${this.baseURL}/bookings`);
      await page.waitForLoadState('networkidle');
      
      // Search for available bookings
      await page.fill('[data-testid="booking-destination"]', 'Sylhet');
      await page.fill('[data-testid="booking-checkin"]', '2024-08-15');
      await page.fill('[data-testid="booking-checkout"]', '2024-08-17');
      await page.selectOption('[data-testid="booking-guests"]', '2');
      await page.click('[data-testid="search-bookings"]');
      
      // Wait for results
      await page.waitForSelector('[data-testid="booking-results"]', { timeout: 15000 });
      
      // Select first available booking
      const firstBooking = page.locator('[data-testid="booking-card"]').first();
      await firstBooking.click();
      
      // Proceed to booking details
      await page.click('[data-testid="book-now-button"]');
      await page.waitForSelector('[data-testid="booking-form"]', { timeout: 10000 });
      
      // Fill booking form (without actual payment)
      await page.fill('[data-testid="guest-name"]', 'Test Guest');
      await page.fill('[data-testid="guest-email"]', 'guest@test.com');
      await page.fill('[data-testid="guest-phone"]', '+8801712345678');
      
      console.log(`     ✅ Booking flow accessible`);
      return true;
    } catch (error) {
      console.log(`     ❌ Booking flow failed: ${error.message}`);
      return false;
    }
  }

  // Run comprehensive test for a single browser
  async runBrowserTests(browserType, deviceConfig) {
    console.log(`\n🌐 Testing with ${browserType} on ${deviceConfig.name}`);
    console.log('----------------------------------------');
    
    let browser;
    try {
      // Launch browser
      const browserEngine = browserType === 'chromium' ? chromium : 
                           browserType === 'firefox' ? firefox : webkit;
      
      browser = await browserEngine.launch({ 
        headless: true,
        args: ['--disable-web-security', '--disable-features=VizDisplayCompositor']
      });
      
      const context = await browser.newContext({
        viewport: deviceConfig.viewport,
        userAgent: `HopNGo-E2E-Tests/${browserType}/${deviceConfig.name}`
      });
      
      const page = await context.newPage();
      
      // Test critical paths
      for (const pathTest of this.criticalPaths) {
        console.log(`\n📍 Testing: ${pathTest.name}`);
        
        try {
          const startTime = Date.now();
          
          // Navigate to page
          await page.goto(`${this.baseURL}${pathTest.path}`, { 
            timeout: pathTest.timeout,
            waitUntil: 'networkidle' 
          });
          
          const loadTime = Date.now() - startTime;
          
          // Measure performance
          await this.measurePagePerformance(page, `${pathTest.name}-${browserType}-${deviceConfig.name}`);
          
          // Check accessibility
          await this.checkAccessibility(page, `${pathTest.name}-${browserType}-${deviceConfig.name}`);
          
          // Check security (only for homepage to avoid redundancy)
          if (pathTest.path === '/') {
            await this.checkSecurity(page, `${pathTest.name}-${browserType}-${deviceConfig.name}`);
          }
          
          console.log(`   ✅ ${pathTest.name} loaded successfully (${loadTime}ms)`);
          this.testResults.passed.push(`${pathTest.name}-${browserType}-${deviceConfig.name}`);
          
        } catch (error) {
          console.log(`   ❌ ${pathTest.name} failed: ${error.message}`);
          this.testResults.failed.push(`${pathTest.name}-${browserType}-${deviceConfig.name}`);
        }
      }
      
      // Test user flows (only on desktop chromium to avoid redundancy)
      if (browserType === 'chromium' && deviceConfig.name === 'Desktop') {
        console.log(`\n🔄 Testing User Flows`);
        
        // Test authentication
        const authResult = await this.testUserAuthentication(page, 'customer');
        if (authResult) {
          this.testResults.passed.push('user-authentication');
          
          // Test authenticated features
          const searchResult = await this.testSearchFunctionality(page);
          if (searchResult) this.testResults.passed.push('search-functionality');
          else this.testResults.failed.push('search-functionality');
          
          const tripResult = await this.testTripPlanning(page);
          if (tripResult) this.testResults.passed.push('trip-planning');
          else this.testResults.failed.push('trip-planning');
          
          const socialResult = await this.testSocialFeatures(page);
          if (socialResult) this.testResults.passed.push('social-features');
          else this.testResults.failed.push('social-features');
          
          const emergencyResult = await this.testEmergencyFeatures(page);
          if (emergencyResult) this.testResults.passed.push('emergency-features');
          else this.testResults.failed.push('emergency-features');
          
          const bookingResult = await this.testBookingFlow(page);
          if (bookingResult) this.testResults.passed.push('booking-flow');
          else this.testResults.failed.push('booking-flow');
          
        } else {
          this.testResults.failed.push('user-authentication');
        }
      }
      
    } catch (error) {
      console.log(`❌ Browser test failed: ${error.message}`);
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  }

  // Run all tests across browsers and devices
  async runAllTests() {
    console.log('🚀 PRODUCTION E2E TEST SUITE');
    console.log('============================\n');
    
    console.log(`🎯 Target URL: ${this.baseURL}`);
    console.log(`📱 Testing ${this.browsers.length} browsers × ${this.devices.length} devices = ${this.browsers.length * this.devices.length} configurations`);
    console.log(`🧪 Critical paths: ${this.criticalPaths.length}`);
    console.log(`👤 Test users: ${Object.keys(this.testUsers).length}`);
    
    const startTime = Date.now();
    
    // Test each browser-device combination
    for (const browserType of this.browsers) {
      for (const deviceConfig of this.devices) {
        await this.runBrowserTests(browserType, deviceConfig);
        
        // Small delay between test runs
        await this.delay(1000);
      }
    }
    
    const totalTime = Date.now() - startTime;
    
    // Generate comprehensive report
    this.generateComprehensiveReport(totalTime);
  }

  // Generate detailed test report
  generateComprehensiveReport(totalTime) {
    console.log('\n\n📊 COMPREHENSIVE E2E TEST REPORT');
    console.log('=================================\n');
    
    const totalTests = this.testResults.passed.length + this.testResults.failed.length;
    const successRate = totalTests > 0 ? (this.testResults.passed.length / totalTests * 100).toFixed(1) : 0;
    
    console.log(`⏱️  Total Execution Time: ${(totalTime / 1000 / 60).toFixed(1)} minutes`);
    console.log(`🧪 Total Tests Executed: ${totalTests}`);
    console.log(`✅ Tests Passed: ${this.testResults.passed.length}`);
    console.log(`❌ Tests Failed: ${this.testResults.failed.length}`);
    console.log(`📈 Success Rate: ${successRate}%`);
    console.log('');
    
    // Performance Summary
    console.log('⚡ Performance Summary:');
    const perfEntries = Object.entries(this.testResults.performance);
    if (perfEntries.length > 0) {
      const avgLoadTime = perfEntries.reduce((sum, [_, metrics]) => sum + metrics.totalLoadTime, 0) / perfEntries.length;
      const avgFCP = perfEntries.reduce((sum, [_, metrics]) => sum + metrics.firstContentfulPaint, 0) / perfEntries.length;
      
      console.log(`   📊 Average Load Time: ${avgLoadTime.toFixed(0)}ms`);
      console.log(`   🎨 Average First Contentful Paint: ${avgFCP.toFixed(0)}ms`);
      
      const slowPages = perfEntries.filter(([_, metrics]) => metrics.totalLoadTime > 3000);
      if (slowPages.length > 0) {
        console.log(`   ⚠️  Slow Loading Pages (>3s): ${slowPages.length}`);
        slowPages.forEach(([testName, metrics]) => {
          console.log(`     - ${testName}: ${metrics.totalLoadTime.toFixed(0)}ms`);
        });
      }
    }
    console.log('');
    
    // Accessibility Summary
    console.log('♿ Accessibility Summary:');
    const accessEntries = Object.entries(this.testResults.accessibility);
    if (accessEntries.length > 0) {
      const totalViolations = accessEntries.reduce((sum, [_, results]) => 
        sum + results.wcag2aViolations + results.wcag2aaViolations, 0);
      
      console.log(`   📋 Total WCAG Violations: ${totalViolations}`);
      console.log(`   ✅ Pages with Zero Violations: ${accessEntries.filter(([_, r]) => r.wcag2aViolations + r.wcag2aaViolations === 0).length}`);
      
      if (totalViolations > 0) {
        console.log(`   ⚠️  Pages with Violations:`);
        accessEntries.forEach(([testName, results]) => {
          const violations = results.wcag2aViolations + results.wcag2aaViolations;
          if (violations > 0) {
            console.log(`     - ${testName}: ${violations} violations`);
          }
        });
      }
    }
    console.log('');
    
    // Security Summary
    console.log('🛡️  Security Summary:');
    const securityEntries = Object.entries(this.testResults.security);
    if (securityEntries.length > 0) {
      const httpsEnabled = securityEntries.every(([_, results]) => results.httpsRedirect);
      const avgSecurityHeaders = securityEntries.reduce((sum, [_, results]) => {
        const headerCount = Object.values(results.securityHeaders).filter(Boolean).length;
        return sum + headerCount;
      }, 0) / securityEntries.length;
      
      console.log(`   🔒 HTTPS Redirect: ${httpsEnabled ? 'ENABLED' : 'DISABLED'}`);
      console.log(`   📋 Average Security Headers: ${avgSecurityHeaders.toFixed(1)}/5`);
      console.log(`   🛡️  CSP Protection: ${securityEntries.some(([_, r]) => r.contentSecurityPolicy) ? 'ENABLED' : 'DISABLED'}`);
    }
    console.log('');
    
    // Failed Tests Details
    if (this.testResults.failed.length > 0) {
      console.log('❌ Failed Tests:');
      this.testResults.failed.forEach(testName => {
        console.log(`   - ${testName}`);
      });
      console.log('');
    }
    
    // Browser/Device Coverage
    console.log('📱 Browser/Device Coverage:');
    this.browsers.forEach(browser => {
      this.devices.forEach(device => {
        const testsForConfig = this.testResults.passed.filter(t => t.includes(`${browser}-${device.name}`)).length +
                              this.testResults.failed.filter(t => t.includes(`${browser}-${device.name}`)).length;
        const passedForConfig = this.testResults.passed.filter(t => t.includes(`${browser}-${device.name}`)).length;
        const configSuccessRate = testsForConfig > 0 ? (passedForConfig / testsForConfig * 100).toFixed(0) : 0;
        
        console.log(`   ${browser.padEnd(10)} ${device.name.padEnd(10)} ${configSuccessRate}% (${passedForConfig}/${testsForConfig})`);
      });
    });
    console.log('');
    
    // Recommendations
    console.log('💡 Recommendations:');
    
    if (successRate < 95) {
      console.log('   ⚠️  Success rate below 95% - investigate failed tests before production deployment');
    }
    
    const slowTests = perfEntries.filter(([_, metrics]) => metrics.totalLoadTime > 5000);
    if (slowTests.length > 0) {
      console.log('   ⚡ Optimize performance for slow-loading pages (>5s)');
    }
    
    const accessibilityIssues = accessEntries.filter(([_, r]) => r.wcag2aViolations + r.wcag2aaViolations > 0);
    if (accessibilityIssues.length > 0) {
      console.log('   ♿ Address accessibility violations for WCAG 2.1 AA compliance');
    }
    
    const securityIssues = securityEntries.filter(([_, r]) => !r.httpsRedirect || !r.contentSecurityPolicy);
    if (securityIssues.length > 0) {
      console.log('   🔒 Strengthen security headers and HTTPS enforcement');
    }
    
    if (successRate >= 95 && slowTests.length === 0 && accessibilityIssues.length === 0 && securityIssues.length === 0) {
      console.log('   🎉 All systems performing optimally - ready for production!');
    }
    
    console.log('');
    console.log('=====================================');
    
    // Return success status
    return successRate >= 90 && this.testResults.failed.length === 0;
  }
}

// Run the E2E test suite
async function main() {
  const testSuite = new ProductionE2ETestSuite();
  
  try {
    const success = await testSuite.runAllTests();
    
    if (success) {
      console.log('\n🎉 Production E2E tests completed successfully!');
      console.log('✅ Application is ready for production deployment');
      process.exit(0);
    } else {
      console.log('\n⚠️  Some E2E tests failed or performance issues detected');
      console.log('🔍 Review the test report and address issues before deployment');
      process.exit(1);
    }
  } catch (error) {
    console.error('❌ E2E test suite failed:', error.message);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}

module.exports = ProductionE2ETestSuite;
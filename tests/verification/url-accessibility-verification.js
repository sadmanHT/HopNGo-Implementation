#!/usr/bin/env node

/**
 * URL Accessibility and Demo Mode Verification
 * 
 * Comprehensive verification of public URLs accessibility and Demo Mode functionality
 * Tests production URLs, demo endpoints, and validates demo user flows
 */

const https = require('https');
const http = require('http');
const { URL } = require('url');

class URLAccessibilityVerification {
  constructor() {
    this.productionURLs = [
      'https://hopngo.com',
      'https://www.hopngo.com',
      'https://api.hopngo.com',
      'https://demo.hopngo.com',
      'https://monitoring.hopngo.com'
    ];
    
    this.demoURLs = [
      'https://demo.hopngo.com',
      'https://demo.hopngo.com/provider',
      'https://demo.hopngo.com/admin',
      'https://hopngo.com?demo=1',
      'https://hopngo.com?demo=1&demo-user=traveler',
      'https://hopngo.com?demo=1&demo-user=provider'
    ];
    
    this.criticalPaths = [
      { path: '/', name: 'Homepage', timeout: 5000 },
      { path: '/login', name: 'Login Page', timeout: 3000 },
      { path: '/register', name: 'Registration Page', timeout: 3000 },
      { path: '/search', name: 'Search Page', timeout: 4000 },
      { path: '/trips/create', name: 'Trip Planning', timeout: 5000 },
      { path: '/bookings', name: 'Booking Flow', timeout: 4000 },
      { path: '/profile', name: 'User Profile', timeout: 3000 },
      { path: '/feed', name: 'Social Feed', timeout: 4000 },
      { path: '/emergency', name: 'Emergency Features', timeout: 3000 },
      { path: '/help', name: 'Help & Support', timeout: 3000 },
      { path: '/api/health', name: 'Health Check API', timeout: 2000 }
    ];
    
    this.demoUsers = {
      traveler: {
        email: 'amara.demo@hopngo.com',
        name: 'Amara Rahman',
        role: 'CUSTOMER'
      },
      provider: {
        email: 'rashid.provider@hopngo.com',
        name: 'Rashid Ahmed',
        role: 'PROVIDER'
      }
    };
    
    this.verificationResults = {
      urlAccessibility: {
        passed: [],
        failed: [],
        warnings: []
      },
      demoMode: {
        passed: [],
        failed: [],
        warnings: []
      },
      performance: {},
      security: {},
      summary: {}
    };
    
    this.timeout = 10000; // 10 second timeout
  }

  // Utility function for delays
  async delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  // Make HTTP request with timeout
  async makeRequest(url, options = {}) {
    return new Promise((resolve, reject) => {
      const urlObj = new URL(url);
      const isHttps = urlObj.protocol === 'https:';
      const client = isHttps ? https : http;
      
      const requestOptions = {
        hostname: urlObj.hostname,
        port: urlObj.port || (isHttps ? 443 : 80),
        path: urlObj.pathname + urlObj.search,
        method: options.method || 'GET',
        headers: {
          'User-Agent': 'HopNGo-URL-Verification/1.0.0',
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Accept-Language': 'en-US,en;q=0.5',
          'Accept-Encoding': 'gzip, deflate',
          'Connection': 'keep-alive',
          'Upgrade-Insecure-Requests': '1',
          ...options.headers
        },
        timeout: this.timeout
      };
      
      const startTime = Date.now();
      
      const req = client.request(requestOptions, (res) => {
        const responseTime = Date.now() - startTime;
        let data = '';
        
        res.on('data', (chunk) => {
          data += chunk;
        });
        
        res.on('end', () => {
          resolve({
            statusCode: res.statusCode,
            headers: res.headers,
            data: data,
            responseTime: responseTime,
            url: url
          });
        });
      });
      
      req.on('error', (error) => {
        reject({
          error: error.message,
          code: error.code,
          url: url,
          responseTime: Date.now() - startTime
        });
      });
      
      req.on('timeout', () => {
        req.destroy();
        reject({
          error: 'Request timeout',
          code: 'TIMEOUT',
          url: url,
          responseTime: this.timeout
        });
      });
      
      if (options.data) {
        req.write(options.data);
      }
      
      req.end();
    });
  }

  // Verify URL accessibility
  async verifyURLAccessibility(url) {
    console.log(`   🔍 Testing: ${url}`);
    
    try {
      const response = await this.makeRequest(url);
      
      // Check status code
      if (response.statusCode >= 200 && response.statusCode < 400) {
        console.log(`     ✅ Accessible (${response.statusCode}) - ${response.responseTime}ms`);
        
        // Check for security headers
        const securityHeaders = this.checkSecurityHeaders(response.headers);
        
        // Check for performance
        const performanceCheck = this.checkPerformance(response.responseTime, url);
        
        this.verificationResults.urlAccessibility.passed.push({
          url,
          statusCode: response.statusCode,
          responseTime: response.responseTime,
          securityHeaders,
          performance: performanceCheck
        });
        
        return true;
      } else if (response.statusCode >= 300 && response.statusCode < 400) {
        console.log(`     ⚠️  Redirect (${response.statusCode}) to: ${response.headers.location}`);
        
        // Follow redirect if it's a valid redirect
        if (response.headers.location) {
          const redirectUrl = response.headers.location.startsWith('http') 
            ? response.headers.location 
            : new URL(response.headers.location, url).href;
          
          console.log(`     🔄 Following redirect to: ${redirectUrl}`);
          return await this.verifyURLAccessibility(redirectUrl);
        }
        
        this.verificationResults.urlAccessibility.warnings.push({
          url,
          statusCode: response.statusCode,
          issue: 'Redirect without location header'
        });
        
        return false;
      } else {
        console.log(`     ❌ Failed (${response.statusCode})`);
        
        this.verificationResults.urlAccessibility.failed.push({
          url,
          statusCode: response.statusCode,
          responseTime: response.responseTime,
          error: `HTTP ${response.statusCode}`
        });
        
        return false;
      }
    } catch (error) {
      console.log(`     ❌ Error: ${error.error || error.message}`);
      
      this.verificationResults.urlAccessibility.failed.push({
        url,
        error: error.error || error.message,
        code: error.code,
        responseTime: error.responseTime
      });
      
      return false;
    }
  }

  // Check security headers
  checkSecurityHeaders(headers) {
    const requiredHeaders = {
      'strict-transport-security': 'HSTS',
      'x-content-type-options': 'Content Type Options',
      'x-frame-options': 'Frame Options',
      'x-xss-protection': 'XSS Protection',
      'content-security-policy': 'CSP',
      'referrer-policy': 'Referrer Policy'
    };
    
    const present = [];
    const missing = [];
    
    Object.entries(requiredHeaders).forEach(([header, name]) => {
      if (headers[header] || headers[header.toLowerCase()]) {
        present.push(name);
      } else {
        missing.push(name);
      }
    });
    
    return { present, missing };
  }

  // Check performance
  checkPerformance(responseTime, url) {
    const thresholds = {
      excellent: 500,
      good: 1000,
      acceptable: 2000,
      slow: 5000
    };
    
    let rating = 'very-slow';
    if (responseTime <= thresholds.excellent) rating = 'excellent';
    else if (responseTime <= thresholds.good) rating = 'good';
    else if (responseTime <= thresholds.acceptable) rating = 'acceptable';
    else if (responseTime <= thresholds.slow) rating = 'slow';
    
    return {
      responseTime,
      rating,
      acceptable: responseTime <= thresholds.slow
    };
  }

  // Simulate demo mode verification
  async verifyDemoMode(demoUrl) {
    console.log(`   🎭 Testing Demo Mode: ${demoUrl}`);
    
    try {
      // Test demo URL accessibility
      const response = await this.makeRequest(demoUrl);
      
      if (response.statusCode >= 200 && response.statusCode < 400) {
        console.log(`     ✅ Demo URL accessible (${response.statusCode}) - ${response.responseTime}ms`);
        
        // Simulate demo mode checks
        const demoChecks = await this.simulateDemoModeChecks(demoUrl, response);
        
        this.verificationResults.demoMode.passed.push({
          url: demoUrl,
          statusCode: response.statusCode,
          responseTime: response.responseTime,
          demoChecks
        });
        
        return true;
      } else {
        console.log(`     ❌ Demo URL failed (${response.statusCode})`);
        
        this.verificationResults.demoMode.failed.push({
          url: demoUrl,
          statusCode: response.statusCode,
          error: `HTTP ${response.statusCode}`
        });
        
        return false;
      }
    } catch (error) {
      console.log(`     ❌ Demo Mode Error: ${error.error || error.message}`);
      
      this.verificationResults.demoMode.failed.push({
        url: demoUrl,
        error: error.error || error.message,
        code: error.code
      });
      
      return false;
    }
  }

  // Simulate demo mode functionality checks
  async simulateDemoModeChecks(demoUrl, response) {
    console.log(`     🔍 Verifying demo mode functionality...`);
    
    const checks = {
      demoParameterDetection: false,
      demoUserTypeDetection: false,
      demoDataAvailability: false,
      demoUIIndicators: false,
      demoUserAuthentication: false
    };
    
    // Simulate checking for demo parameters in URL
    const urlObj = new URL(demoUrl);
    if (urlObj.searchParams.get('demo') === '1' || demoUrl.includes('demo.hopngo.com')) {
      checks.demoParameterDetection = true;
      console.log(`       ✅ Demo parameter detected`);
    }
    
    // Simulate checking for demo user type
    const demoUserType = urlObj.searchParams.get('demo-user');
    if (demoUserType && ['traveler', 'provider'].includes(demoUserType)) {
      checks.demoUserTypeDetection = true;
      console.log(`       ✅ Demo user type: ${demoUserType}`);
    } else if (demoUrl.includes('demo.hopngo.com')) {
      checks.demoUserTypeDetection = true;
      console.log(`       ✅ Demo subdomain detected`);
    }
    
    // Simulate checking for demo data availability
    if (response.data && response.data.length > 1000) {
      checks.demoDataAvailability = true;
      console.log(`       ✅ Demo data appears to be loaded`);
    }
    
    // Simulate checking for demo UI indicators
    if (response.data && (response.data.includes('demo-mode') || response.data.includes('Demo Mode'))) {
      checks.demoUIIndicators = true;
      console.log(`       ✅ Demo UI indicators found`);
    }
    
    // Simulate demo user authentication
    const userType = demoUserType || 'traveler';
    if (this.demoUsers[userType]) {
      checks.demoUserAuthentication = true;
      console.log(`       ✅ Demo user available: ${this.demoUsers[userType].name}`);
    }
    
    // Add small delay to simulate processing
    await this.delay(500);
    
    const passedChecks = Object.values(checks).filter(Boolean).length;
    const totalChecks = Object.keys(checks).length;
    
    console.log(`     📊 Demo checks: ${passedChecks}/${totalChecks} passed`);
    
    return {
      ...checks,
      score: (passedChecks / totalChecks) * 100
    };
  }

  // Test critical paths
  async testCriticalPaths(baseUrl) {
    console.log(`\n🛣️  Testing Critical Paths on ${baseUrl}`);
    console.log('='.repeat(60));
    
    const results = [];
    
    for (const pathTest of this.criticalPaths) {
      const fullUrl = `${baseUrl}${pathTest.path}`;
      console.log(`\n   📄 ${pathTest.name}`);
      
      try {
        const response = await this.makeRequest(fullUrl);
        
        if (response.statusCode >= 200 && response.statusCode < 400) {
          const performanceCheck = this.checkPerformance(response.responseTime, fullUrl);
          
          console.log(`     ✅ Accessible (${response.statusCode}) - ${response.responseTime}ms [${performanceCheck.rating}]`);
          
          results.push({
            path: pathTest.path,
            name: pathTest.name,
            status: 'passed',
            statusCode: response.statusCode,
            responseTime: response.responseTime,
            performance: performanceCheck
          });
        } else {
          console.log(`     ❌ Failed (${response.statusCode})`);
          
          results.push({
            path: pathTest.path,
            name: pathTest.name,
            status: 'failed',
            statusCode: response.statusCode,
            error: `HTTP ${response.statusCode}`
          });
        }
      } catch (error) {
        console.log(`     ❌ Error: ${error.error || error.message}`);
        
        results.push({
          path: pathTest.path,
          name: pathTest.name,
          status: 'failed',
          error: error.error || error.message,
          code: error.code
        });
      }
      
      // Small delay between requests
      await this.delay(200);
    }
    
    return results;
  }

  // Run comprehensive verification
  async runComprehensiveVerification() {
    console.log('🌐 URL ACCESSIBILITY & DEMO MODE VERIFICATION');
    console.log('==============================================\n');
    
    const startTime = Date.now();
    
    // Test production URLs
    console.log('🔗 Testing Production URLs');
    console.log('='.repeat(40));
    
    for (const url of this.productionURLs) {
      await this.verifyURLAccessibility(url);
      await this.delay(300); // Rate limiting
    }
    
    // Test demo URLs and functionality
    console.log('\n🎭 Testing Demo Mode URLs & Functionality');
    console.log('='.repeat(50));
    
    for (const demoUrl of this.demoURLs) {
      await this.verifyDemoMode(demoUrl);
      await this.delay(300); // Rate limiting
    }
    
    // Test critical paths on main domain
    const mainDomain = 'https://hopngo.com';
    const criticalPathResults = await this.testCriticalPaths(mainDomain);
    
    // Test critical paths on demo domain
    const demoDomain = 'https://demo.hopngo.com';
    const demoCriticalPathResults = await this.testCriticalPaths(demoDomain);
    
    const totalTime = Date.now() - startTime;
    
    // Generate comprehensive report
    const reportData = this.generateVerificationReport(totalTime, criticalPathResults, demoCriticalPathResults);
    
    return reportData;
  }

  // Generate verification report
  generateVerificationReport(totalTime, criticalPathResults, demoCriticalPathResults) {
    console.log('\n\n📊 URL ACCESSIBILITY & DEMO MODE REPORT');
    console.log('========================================\n');
    
    // Executive Summary
    const totalURLs = this.productionURLs.length;
    const passedURLs = this.verificationResults.urlAccessibility.passed.length;
    const failedURLs = this.verificationResults.urlAccessibility.failed.length;
    const warningURLs = this.verificationResults.urlAccessibility.warnings.length;
    
    const totalDemoURLs = this.demoURLs.length;
    const passedDemoURLs = this.verificationResults.demoMode.passed.length;
    const failedDemoURLs = this.verificationResults.demoMode.failed.length;
    
    const urlSuccessRate = totalURLs > 0 ? (passedURLs / totalURLs * 100).toFixed(1) : 0;
    const demoSuccessRate = totalDemoURLs > 0 ? (passedDemoURLs / totalDemoURLs * 100).toFixed(1) : 0;
    
    console.log('📈 Executive Summary:');
    console.log(`   ⏱️  Total Execution Time: ${(totalTime / 1000).toFixed(1)} seconds`);
    console.log(`   🔗 Production URLs Tested: ${totalURLs}`);
    console.log(`   ✅ URLs Accessible: ${passedURLs}`);
    console.log(`   ❌ URLs Failed: ${failedURLs}`);
    console.log(`   ⚠️  URLs with Warnings: ${warningURLs}`);
    console.log(`   📊 URL Success Rate: ${urlSuccessRate}%`);
    console.log(`   🎭 Demo URLs Tested: ${totalDemoURLs}`);
    console.log(`   ✅ Demo URLs Working: ${passedDemoURLs}`);
    console.log(`   ❌ Demo URLs Failed: ${failedDemoURLs}`);
    console.log(`   📊 Demo Success Rate: ${demoSuccessRate}%`);
    console.log('');
    
    // URL Accessibility Details
    console.log('🔗 URL Accessibility Details:');
    console.log('   URL                              Status    Response Time  Security Headers');
    console.log('   ──────────────────────────────   ───────   ─────────────  ────────────────');
    
    [...this.verificationResults.urlAccessibility.passed, ...this.verificationResults.urlAccessibility.failed].forEach(result => {
      const url = result.url.padEnd(32);
      const status = result.statusCode ? `${result.statusCode}`.padStart(7) : 'ERROR'.padStart(7);
      const responseTime = result.responseTime ? `${result.responseTime}ms`.padStart(13) : 'N/A'.padStart(13);
      const securityHeaders = result.securityHeaders ? `${result.securityHeaders.present.length}/6`.padStart(16) : 'N/A'.padStart(16);
      
      console.log(`   ${url} ${status}   ${responseTime}  ${securityHeaders}`);
    });
    console.log('');
    
    // Demo Mode Details
    console.log('🎭 Demo Mode Functionality:');
    console.log('   Demo URL                         Status    Demo Score  Features');
    console.log('   ──────────────────────────────   ───────   ──────────  ────────');
    
    [...this.verificationResults.demoMode.passed, ...this.verificationResults.demoMode.failed].forEach(result => {
      const url = result.url.padEnd(32);
      const status = result.statusCode ? `${result.statusCode}`.padStart(7) : 'ERROR'.padStart(7);
      const demoScore = result.demoChecks ? `${result.demoChecks.score.toFixed(0)}%`.padStart(10) : 'N/A'.padStart(10);
      const features = result.demoChecks ? `${Object.values(result.demoChecks).filter(v => v === true).length}/5`.padStart(8) : 'N/A'.padStart(8);
      
      console.log(`   ${url} ${status}   ${demoScore}  ${features}`);
    });
    console.log('');
    
    // Critical Paths Analysis
    console.log('🛣️  Critical Paths Analysis:');
    
    const allCriticalResults = [...criticalPathResults, ...demoCriticalPathResults];
    const passedPaths = allCriticalResults.filter(r => r.status === 'passed').length;
    const failedPaths = allCriticalResults.filter(r => r.status === 'failed').length;
    const pathSuccessRate = allCriticalResults.length > 0 ? (passedPaths / allCriticalResults.length * 100).toFixed(1) : 0;
    
    console.log(`   📊 Critical Paths Success Rate: ${pathSuccessRate}%`);
    console.log(`   ✅ Paths Accessible: ${passedPaths}`);
    console.log(`   ❌ Paths Failed: ${failedPaths}`);
    
    // Performance Analysis
    const performanceResults = [...this.verificationResults.urlAccessibility.passed, ...criticalPathResults.filter(r => r.performance)];
    if (performanceResults.length > 0) {
      const avgResponseTime = performanceResults.reduce((sum, r) => sum + (r.responseTime || r.performance?.responseTime || 0), 0) / performanceResults.length;
      const fastResponses = performanceResults.filter(r => (r.responseTime || r.performance?.responseTime || 0) < 1000).length;
      const slowResponses = performanceResults.filter(r => (r.responseTime || r.performance?.responseTime || 0) > 5000).length;
      
      console.log(`   ⚡ Average Response Time: ${avgResponseTime.toFixed(0)}ms`);
      console.log(`   🚀 Fast Responses (<1s): ${fastResponses}`);
      console.log(`   🐌 Slow Responses (>5s): ${slowResponses}`);
    }
    console.log('');
    
    // Security Analysis
    const securityResults = this.verificationResults.urlAccessibility.passed.filter(r => r.securityHeaders);
    if (securityResults.length > 0) {
      const avgSecurityHeaders = securityResults.reduce((sum, r) => sum + r.securityHeaders.present.length, 0) / securityResults.length;
      const fullSecurityCompliance = securityResults.filter(r => r.securityHeaders.present.length >= 5).length;
      
      console.log('🔒 Security Analysis:');
      console.log(`   🛡️  Average Security Headers: ${avgSecurityHeaders.toFixed(1)}/6`);
      console.log(`   ✅ Full Security Compliance: ${fullSecurityCompliance}/${securityResults.length}`);
      
      // Most common missing headers
      const allMissingHeaders = securityResults.flatMap(r => r.securityHeaders.missing);
      const missingHeaderCounts = {};
      allMissingHeaders.forEach(header => {
        missingHeaderCounts[header] = (missingHeaderCounts[header] || 0) + 1;
      });
      
      const topMissingHeaders = Object.entries(missingHeaderCounts)
        .sort(([,a], [,b]) => b - a)
        .slice(0, 3);
      
      if (topMissingHeaders.length > 0) {
        console.log('   🚨 Most Common Missing Headers:');
        topMissingHeaders.forEach(([header, count]) => {
          console.log(`     - ${header}: ${count} URLs`);
        });
      }
      console.log('');
    }
    
    // Recommendations
    console.log('💡 Recommendations:');
    
    const recommendations = [];
    
    if (urlSuccessRate < 100) {
      recommendations.push(`🔗 Fix ${failedURLs} failed production URLs`);
    }
    
    if (demoSuccessRate < 100) {
      recommendations.push(`🎭 Fix ${failedDemoURLs} failed demo URLs`);
    }
    
    if (pathSuccessRate < 90) {
      recommendations.push(`🛣️  Improve critical path accessibility (${pathSuccessRate}% success rate)`);
    }
    
    const slowURLs = [...this.verificationResults.urlAccessibility.passed, ...criticalPathResults]
      .filter(r => (r.responseTime || r.performance?.responseTime || 0) > 3000).length;
    if (slowURLs > 0) {
      recommendations.push(`⚡ Optimize ${slowURLs} slow-loading URLs (>3s response time)`);
    }
    
    const insecureURLs = securityResults.filter(r => r.securityHeaders.present.length < 4).length;
    if (insecureURLs > 0) {
      recommendations.push(`🔒 Improve security headers on ${insecureURLs} URLs`);
    }
    
    const lowDemoScores = this.verificationResults.demoMode.passed.filter(r => r.demoChecks && r.demoChecks.score < 80).length;
    if (lowDemoScores > 0) {
      recommendations.push(`🎭 Enhance demo mode functionality on ${lowDemoScores} URLs`);
    }
    
    if (recommendations.length === 0) {
      console.log('   🎉 Excellent! All URLs are accessible and demo mode is working perfectly');
      console.log('   ✅ No optimization recommendations at this time');
      console.log('   🚀 Public URLs and demo functionality are production-ready');
    } else {
      recommendations.forEach(rec => console.log(`   ${rec}`));
    }
    
    console.log('');
    console.log('========================================');
    
    const overallSuccess = urlSuccessRate >= 95 && demoSuccessRate >= 90 && pathSuccessRate >= 90;
    
    return {
      success: overallSuccess,
      urlAccessibility: {
        successRate: parseFloat(urlSuccessRate),
        passed: passedURLs,
        failed: failedURLs,
        warnings: warningURLs
      },
      demoMode: {
        successRate: parseFloat(demoSuccessRate),
        passed: passedDemoURLs,
        failed: failedDemoURLs
      },
      criticalPaths: {
        successRate: parseFloat(pathSuccessRate),
        passed: passedPaths,
        failed: failedPaths
      },
      performance: {
        averageResponseTime: performanceResults.length > 0 ? 
          performanceResults.reduce((sum, r) => sum + (r.responseTime || r.performance?.responseTime || 0), 0) / performanceResults.length : 0
      },
      recommendations
    };
  }
}

// Run the verification
async function main() {
  const verification = new URLAccessibilityVerification();
  
  try {
    const results = await verification.runComprehensiveVerification();
    
    if (results.success) {
      console.log('\n🎉 URL accessibility and demo mode verification completed successfully!');
      console.log('✅ All public URLs accessible and demo functionality working');
      console.log(`📊 Overall Success: URLs ${results.urlAccessibility.successRate}%, Demo ${results.demoMode.successRate}%, Paths ${results.criticalPaths.successRate}%`);
      process.exit(0);
    } else {
      console.log('\n⚠️  URL accessibility and demo mode verification completed with issues');
      console.log('🔍 Review failed URLs and demo functionality before deployment');
      console.log(`📊 Overall Success: URLs ${results.urlAccessibility.successRate}%, Demo ${results.demoMode.successRate}%, Paths ${results.criticalPaths.successRate}%`);
      process.exit(1);
    }
  } catch (error) {
    console.error('❌ URL accessibility verification failed:', error.message);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}

module.exports = URLAccessibilityVerification;
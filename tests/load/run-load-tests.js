#!/usr/bin/env node

const { execSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

// Test configuration
const config = {
  baseUrl: process.env.BASE_URL || 'http://localhost:8080',
  wsUrl: process.env.WS_URL || 'ws://localhost:8080/ws/chat',
  authToken: process.env.AUTH_TOKEN || null,
  outputDir: './results',
  parallel: process.env.PARALLEL_TESTS === 'true',
  
  tests: [
    {
      name: 'bookings-search',
      file: 'k6-bookings-search.js',
      description: 'Bookings search endpoint load test',
      priority: 'high',
      targets: {
        rps: 500,
        p95: 400, // ms
        errorRate: 0.01 // 1%
      }
    },
    {
      name: 'market-checkout',
      file: 'k6-market-checkout.js',
      description: 'Market checkout flow load test',
      priority: 'high',
      targets: {
        rps: 300,
        p95: 500, // ms (checkout can be slower)
        errorRate: 0.005 // 0.5%
      }
    },
    {
      name: 'social-feed',
      file: 'k6-social-feed.js',
      description: 'Social feed endpoint load test',
      priority: 'medium',
      targets: {
        rps: 500,
        p95: 400, // ms
        errorRate: 0.01 // 1%
      }
    },
    {
      name: 'websocket-chat',
      file: 'k6-websocket-chat.js',
      description: 'WebSocket chat functionality load test',
      priority: 'medium',
      targets: {
        concurrentUsers: 500,
        messageLatency: 100, // ms
        connectionTime: 2000, // ms
        errorRate: 0.01 // 1%
      }
    }
  ]
};

// Utility functions
function ensureDirectory(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function formatDuration(ms) {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
}

function checkK6Installation() {
  try {
    execSync('k6 version', { stdio: 'pipe' });
    console.log('✓ k6 is installed and available');
    return true;
  } catch (error) {
    console.error('✗ k6 is not installed or not in PATH');
    console.error('Please install k6: https://k6.io/docs/getting-started/installation/');
    return false;
  }
}

function validateTestFiles() {
  const missingFiles = [];
  
  config.tests.forEach(test => {
    const filePath = path.join(__dirname, test.file);
    if (!fs.existsSync(filePath)) {
      missingFiles.push(test.file);
    }
  });
  
  if (missingFiles.length > 0) {
    console.error('✗ Missing test files:');
    missingFiles.forEach(file => console.error(`  - ${file}`));
    return false;
  }
  
  console.log('✓ All test files are present');
  return true;
}

function runSingleTest(test, options = {}) {
  return new Promise((resolve, reject) => {
    const testFile = path.join(__dirname, test.file);
    const outputFile = path.join(config.outputDir, `${test.name}-results.json`);
    const summaryFile = path.join(config.outputDir, `${test.name}-summary.json`);
    
    const env = {
      ...process.env,
      BASE_URL: config.baseUrl,
      WS_URL: config.wsUrl,
      AUTH_TOKEN: config.authToken,
      ...options.env
    };
    
    const args = [
      'run',
      '--out', `json=${outputFile}`,
      '--summary-export', summaryFile,
      testFile
    ];
    
    if (options.quiet) {
      args.push('--quiet');
    }
    
    console.log(`\n🚀 Running ${test.name} load test...`);
    console.log(`   Description: ${test.description}`);
    console.log(`   Priority: ${test.priority}`);
    console.log(`   Targets: ${JSON.stringify(test.targets)}`);
    
    const startTime = Date.now();
    
    const k6Process = spawn('k6', args, {
      env,
      stdio: options.quiet ? 'pipe' : 'inherit'
    });
    
    let stdout = '';
    let stderr = '';
    
    if (options.quiet) {
      k6Process.stdout.on('data', (data) => {
        stdout += data.toString();
      });
      
      k6Process.stderr.on('data', (data) => {
        stderr += data.toString();
      });
    }
    
    k6Process.on('close', (code) => {
      const duration = Date.now() - startTime;
      
      if (code === 0) {
        console.log(`✓ ${test.name} completed successfully in ${formatDuration(duration)}`);
        
        // Parse and validate results
        try {
          if (fs.existsSync(summaryFile)) {
            const summary = JSON.parse(fs.readFileSync(summaryFile, 'utf8'));
            validateTestResults(test, summary);
          }
        } catch (error) {
          console.warn(`⚠ Could not parse results for ${test.name}: ${error.message}`);
        }
        
        resolve({
          test: test.name,
          success: true,
          duration,
          code,
          stdout: options.quiet ? stdout : null,
          stderr: options.quiet ? stderr : null
        });
      } else {
        console.error(`✗ ${test.name} failed with exit code ${code}`);
        if (options.quiet && stderr) {
          console.error('Error output:', stderr);
        }
        
        resolve({
          test: test.name,
          success: false,
          duration,
          code,
          stdout: options.quiet ? stdout : null,
          stderr: options.quiet ? stderr : null
        });
      }
    });
    
    k6Process.on('error', (error) => {
      console.error(`✗ Failed to start ${test.name}: ${error.message}`);
      reject(error);
    });
  });
}

function validateTestResults(test, summary) {
  const metrics = summary.metrics || {};
  const thresholds = summary.thresholds || {};
  
  console.log(`\n📊 Results for ${test.name}:`);
  
  // Check HTTP metrics
  if (metrics.http_reqs) {
    const totalRequests = metrics.http_reqs.count || 0;
    const failedRequests = metrics.http_req_failed ? metrics.http_req_failed.count || 0 : 0;
    const errorRate = totalRequests > 0 ? failedRequests / totalRequests : 0;
    
    console.log(`   Total Requests: ${totalRequests}`);
    console.log(`   Failed Requests: ${failedRequests} (${(errorRate * 100).toFixed(2)}%)`);
    
    if (test.targets.errorRate && errorRate > test.targets.errorRate) {
      console.warn(`   ⚠ Error rate ${(errorRate * 100).toFixed(2)}% exceeds target ${(test.targets.errorRate * 100).toFixed(2)}%`);
    } else {
      console.log(`   ✓ Error rate within target`);
    }
  }
  
  // Check response time metrics
  if (metrics.http_req_duration) {
    const avgDuration = metrics.http_req_duration.avg || 0;
    const p95Duration = metrics['http_req_duration{p(95)}'] || 0;
    
    console.log(`   Avg Response Time: ${avgDuration.toFixed(2)}ms`);
    console.log(`   P95 Response Time: ${p95Duration.toFixed(2)}ms`);
    
    if (test.targets.p95 && p95Duration > test.targets.p95) {
      console.warn(`   ⚠ P95 response time ${p95Duration.toFixed(2)}ms exceeds target ${test.targets.p95}ms`);
    } else {
      console.log(`   ✓ Response time within target`);
    }
  }
  
  // Check WebSocket specific metrics
  if (metrics.ws_sessions) {
    const sessions = metrics.ws_sessions.count || 0;
    console.log(`   WebSocket Sessions: ${sessions}`);
    
    if (metrics.connection_time) {
      const avgConnectionTime = metrics.connection_time.avg || 0;
      console.log(`   Avg Connection Time: ${avgConnectionTime.toFixed(2)}ms`);
      
      if (test.targets.connectionTime && avgConnectionTime > test.targets.connectionTime) {
        console.warn(`   ⚠ Connection time ${avgConnectionTime.toFixed(2)}ms exceeds target ${test.targets.connectionTime}ms`);
      } else {
        console.log(`   ✓ Connection time within target`);
      }
    }
    
    if (metrics.message_latency) {
      const avgMessageLatency = metrics.message_latency.avg || 0;
      console.log(`   Avg Message Latency: ${avgMessageLatency.toFixed(2)}ms`);
      
      if (test.targets.messageLatency && avgMessageLatency > test.targets.messageLatency) {
        console.warn(`   ⚠ Message latency ${avgMessageLatency.toFixed(2)}ms exceeds target ${test.targets.messageLatency}ms`);
      } else {
        console.log(`   ✓ Message latency within target`);
      }
    }
  }
  
  // Check threshold violations
  const failedThresholds = Object.entries(thresholds)
    .filter(([name, threshold]) => !threshold.ok)
    .map(([name]) => name);
  
  if (failedThresholds.length > 0) {
    console.warn(`   ⚠ Failed thresholds: ${failedThresholds.join(', ')}`);
  } else {
    console.log(`   ✓ All thresholds passed`);
  }
}

async function runAllTests(options = {}) {
  const startTime = Date.now();
  const results = [];
  
  if (config.parallel && !options.sequential) {
    console.log('\n🔄 Running tests in parallel...');
    
    const promises = config.tests.map(test => 
      runSingleTest(test, { quiet: true, ...options })
    );
    
    const parallelResults = await Promise.allSettled(promises);
    
    parallelResults.forEach((result, index) => {
      if (result.status === 'fulfilled') {
        results.push(result.value);
      } else {
        results.push({
          test: config.tests[index].name,
          success: false,
          error: result.reason.message
        });
      }
    });
  } else {
    console.log('\n🔄 Running tests sequentially...');
    
    for (const test of config.tests) {
      try {
        const result = await runSingleTest(test, options);
        results.push(result);
        
        // Brief pause between tests
        if (config.tests.indexOf(test) < config.tests.length - 1) {
          console.log('\n⏸ Pausing 30 seconds between tests...');
          await new Promise(resolve => setTimeout(resolve, 30000));
        }
      } catch (error) {
        results.push({
          test: test.name,
          success: false,
          error: error.message
        });
      }
    }
  }
  
  const totalDuration = Date.now() - startTime;
  
  // Generate final report
  generateFinalReport(results, totalDuration);
  
  return results;
}

function generateFinalReport(results, totalDuration) {
  const reportFile = path.join(config.outputDir, 'load-test-report.json');
  const summaryFile = path.join(config.outputDir, 'load-test-summary.txt');
  
  const successful = results.filter(r => r.success).length;
  const failed = results.length - successful;
  
  const report = {
    timestamp: new Date().toISOString(),
    totalDuration: totalDuration,
    totalTests: results.length,
    successful: successful,
    failed: failed,
    successRate: (successful / results.length * 100).toFixed(2) + '%',
    configuration: {
      baseUrl: config.baseUrl,
      wsUrl: config.wsUrl,
      parallel: config.parallel
    },
    results: results
  };
  
  // Write JSON report
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2));
  
  // Write text summary
  const summary = `
=== HopNGo Load Test Summary ===
Timestamp: ${report.timestamp}
Total Duration: ${formatDuration(totalDuration)}
Total Tests: ${results.length}
Successful: ${successful}
Failed: ${failed}
Success Rate: ${report.successRate}

Test Results:
${results.map(r => `  ${r.success ? '✓' : '✗'} ${r.test} ${r.duration ? `(${formatDuration(r.duration)})` : ''}`).join('\n')}

Configuration:
  Base URL: ${config.baseUrl}
  WebSocket URL: ${config.wsUrl}
  Parallel Execution: ${config.parallel}

Output Directory: ${config.outputDir}
===============================
`;
  
  fs.writeFileSync(summaryFile, summary);
  
  // Console output
  console.log('\n' + '='.repeat(50));
  console.log('🎯 LOAD TEST SUMMARY');
  console.log('='.repeat(50));
  console.log(`Total Duration: ${formatDuration(totalDuration)}`);
  console.log(`Tests: ${successful}/${results.length} passed (${report.successRate})`);
  
  if (failed > 0) {
    console.log('\n❌ Failed Tests:');
    results.filter(r => !r.success).forEach(r => {
      console.log(`  - ${r.test}: ${r.error || `Exit code ${r.code}`}`);
    });
  }
  
  console.log(`\n📁 Results saved to: ${config.outputDir}`);
  console.log(`📊 Full report: ${reportFile}`);
  console.log(`📝 Summary: ${summaryFile}`);
  console.log('='.repeat(50));
}

// CLI handling
function printUsage() {
  console.log(`
Usage: node run-load-tests.js [options] [test-names...]

Options:
  --help, -h          Show this help message
  --list, -l          List available tests
  --sequential, -s    Run tests sequentially (default: parallel)
  --base-url <url>    Override base URL (default: ${config.baseUrl})
  --ws-url <url>      Override WebSocket URL (default: ${config.wsUrl})
  --auth-token <token> Set authentication token
  --output-dir <dir>  Set output directory (default: ${config.outputDir})
  --quiet, -q         Suppress test output

Examples:
  node run-load-tests.js                    # Run all tests
  node run-load-tests.js bookings-search    # Run specific test
  node run-load-tests.js --sequential       # Run tests one by one
  node run-load-tests.js --base-url http://staging.hopngo.com
`);
}

function listTests() {
  console.log('\nAvailable Load Tests:');
  config.tests.forEach(test => {
    console.log(`  ${test.name.padEnd(20)} - ${test.description} (${test.priority} priority)`);
  });
  console.log('');
}

async function main() {
  const args = process.argv.slice(2);
  const options = {};
  const testNames = [];
  
  // Parse arguments
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    
    switch (arg) {
      case '--help':
      case '-h':
        printUsage();
        return;
      
      case '--list':
      case '-l':
        listTests();
        return;
      
      case '--sequential':
      case '-s':
        options.sequential = true;
        break;
      
      case '--quiet':
      case '-q':
        options.quiet = true;
        break;
      
      case '--base-url':
        config.baseUrl = args[++i];
        break;
      
      case '--ws-url':
        config.wsUrl = args[++i];
        break;
      
      case '--auth-token':
        config.authToken = args[++i];
        break;
      
      case '--output-dir':
        config.outputDir = args[++i];
        break;
      
      default:
        if (!arg.startsWith('--')) {
          testNames.push(arg);
        }
        break;
    }
  }
  
  // Filter tests if specific names provided
  if (testNames.length > 0) {
    config.tests = config.tests.filter(test => testNames.includes(test.name));
    
    if (config.tests.length === 0) {
      console.error('❌ No matching tests found');
      listTests();
      process.exit(1);
    }
  }
  
  // Pre-flight checks
  console.log('🔍 Pre-flight checks...');
  
  if (!checkK6Installation()) {
    process.exit(1);
  }
  
  if (!validateTestFiles()) {
    process.exit(1);
  }
  
  // Ensure output directory exists
  ensureDirectory(config.outputDir);
  
  console.log(`✓ Output directory: ${config.outputDir}`);
  console.log(`✓ Base URL: ${config.baseUrl}`);
  console.log(`✓ WebSocket URL: ${config.wsUrl}`);
  
  // Run tests
  try {
    const results = await runAllTests(options);
    
    // Exit with error code if any tests failed
    const hasFailures = results.some(r => !r.success);
    process.exit(hasFailures ? 1 : 0);
    
  } catch (error) {
    console.error('❌ Load test execution failed:', error.message);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  main().catch(error => {
    console.error('❌ Unexpected error:', error);
    process.exit(1);
  });
}

module.exports = {
  config,
  runSingleTest,
  runAllTests,
  generateFinalReport
};
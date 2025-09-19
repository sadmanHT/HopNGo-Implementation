import { chromium, FullConfig } from '@playwright/test';

async function globalTeardown(config: FullConfig) {
  console.log('🧹 Starting global test teardown...');
  
  // Create a browser instance for teardown
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Navigate to the application
    await page.goto(config.projects[0].use?.baseURL || 'http://localhost:3000');
    
    // Clean up test data
    await cleanupTestData(page);
    
    // Clean up test users (optional - might want to keep for next run)
    // await cleanupTestUsers(page);
    
    // Clear browser storage
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    
    console.log('✅ Global test teardown completed successfully');
  } catch (error) {
    console.error('❌ Global test teardown failed:', error);
    // Don't throw error in teardown to avoid masking test failures
  } finally {
    await context.close();
    await browser.close();
  }
}

async function cleanupTestData(page: any) {
  try {
    // Clean up test trips
    await page.goto('/api/test/cleanup-trips');
    await page.click('[data-testid="cleanup-test-trips"]');
    console.log('✅ Cleaned up test trips');
    
    // Clean up test posts
    await page.goto('/api/test/cleanup-posts');
    await page.click('[data-testid="cleanup-test-posts"]');
    console.log('✅ Cleaned up test posts');
    
    // Clean up test bookings
    await page.goto('/api/test/cleanup-bookings');
    await page.click('[data-testid="cleanup-test-bookings"]');
    console.log('✅ Cleaned up test bookings');
    
    // Clean up test messages
    await page.goto('/api/test/cleanup-messages');
    await page.click('[data-testid="cleanup-test-messages"]');
    console.log('✅ Cleaned up test messages');
    
  } catch (error) {
    console.warn('⚠️ Could not clean up all test data:', error);
  }
}

async function cleanupTestUsers(page: any) {
  const testUserEmails = [
    'user@example.com',
    'provider@example.com',
    'admin@example.com',
    'friend@example.com'
  ];
  
  for (const email of testUserEmails) {
    try {
      await page.goto('/api/test/delete-user');
      await page.fill('[data-testid="delete-email"]', email);
      await page.click('[data-testid="delete-user-button"]');
      console.log(`✅ Deleted test user: ${email}`);
    } catch (error) {
      console.warn(`⚠️ Could not delete test user ${email}:`, error);
    }
  }
}

export default globalTeardown;
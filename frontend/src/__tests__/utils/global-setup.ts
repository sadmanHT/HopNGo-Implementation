import { chromium, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig) {
  console.log('🚀 Starting global test setup...');
  
  // Create a browser instance for setup
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Navigate to the application
    await page.goto(config.projects[0].use?.baseURL || 'http://localhost:3000');
    
    // Wait for the application to be ready
    await page.waitForSelector('[data-testid="app-ready"]', { timeout: 30000 });
    
    // Create test users if needed
    await setupTestUsers(page);
    
    // Setup test data
    await setupTestData(page);
    
    console.log('✅ Global test setup completed successfully');
  } catch (error) {
    console.error('❌ Global test setup failed:', error);
    throw error;
  } finally {
    await context.close();
    await browser.close();
  }
}

async function setupTestUsers(page: any) {
  // Create test users for different scenarios
  const testUsers = [
    {
      email: 'user@example.com',
      password: 'password123',
      role: 'USER',
      name: 'Test User'
    },
    {
      email: 'provider@example.com',
      password: 'password123',
      role: 'PROVIDER',
      name: 'Test Provider'
    },
    {
      email: 'admin@example.com',
      password: 'password123',
      role: 'ADMIN',
      name: 'Test Admin'
    },
    {
      email: 'friend@example.com',
      password: 'password123',
      role: 'USER',
      name: 'Test Friend'
    }
  ];
  
  for (const user of testUsers) {
    try {
      // Check if user already exists
      await page.goto('/api/test/check-user');
      await page.fill('[data-testid="check-email"]', user.email);
      const userExists = await page.locator('[data-testid="user-exists"]').isVisible();
      
      if (!userExists) {
        // Create the user
        await page.goto('/api/test/create-user');
        await page.fill('[data-testid="user-email"]', user.email);
        await page.fill('[data-testid="user-password"]', user.password);
        await page.fill('[data-testid="user-name"]', user.name);
        await page.selectOption('[data-testid="user-role"]', user.role);
        await page.click('[data-testid="create-user-button"]');
        
        console.log(`✅ Created test user: ${user.email}`);
      } else {
        console.log(`ℹ️ Test user already exists: ${user.email}`);
      }
    } catch (error) {
      console.warn(`⚠️ Could not create test user ${user.email}:`, error);
    }
  }
}

async function setupTestData(page: any) {
  // Setup test destinations
  const testDestinations = [
    {
      name: 'Sundarbans National Park',
      category: 'NATURE',
      region: 'KHULNA',
      description: 'World\'s largest mangrove forest'
    },
    {
      name: 'Cox\'s Bazar',
      category: 'BEACH',
      region: 'CHITTAGONG',
      description: 'World\'s longest natural sea beach'
    },
    {
      name: 'Ahsan Manzil',
      category: 'HISTORICAL',
      region: 'DHAKA',
      description: 'Pink Palace of Dhaka'
    }
  ];
  
  for (const destination of testDestinations) {
    try {
      await page.goto('/api/test/create-destination');
      await page.fill('[data-testid="destination-name"]', destination.name);
      await page.selectOption('[data-testid="destination-category"]', destination.category);
      await page.selectOption('[data-testid="destination-region"]', destination.region);
      await page.fill('[data-testid="destination-description"]', destination.description);
      await page.click('[data-testid="create-destination-button"]');
      
      console.log(`✅ Created test destination: ${destination.name}`);
    } catch (error) {
      console.warn(`⚠️ Could not create test destination ${destination.name}:`, error);
    }
  }
  
  // Setup test trips
  try {
    await page.goto('/api/test/create-trip');
    await page.fill('[data-testid="trip-title"]', 'Amazing Bangladesh Adventure');
    await page.fill('[data-testid="trip-description"]', 'A comprehensive tour of Bangladesh\'s best destinations');
    await page.fill('[data-testid="trip-start-date"]', '2024-07-01');
    await page.fill('[data-testid="trip-end-date"]', '2024-07-07');
    await page.click('[data-testid="create-trip-button"]');
    
    console.log('✅ Created test trip: Amazing Bangladesh Adventure');
  } catch (error) {
    console.warn('⚠️ Could not create test trip:', error);
  }
}

export default globalSetup;
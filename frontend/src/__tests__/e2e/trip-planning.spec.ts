import { test, expect } from '@playwright/test';

test.describe('Trip Planning Features', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'customer@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();
  });

  test('should create a new trip itinerary', async ({ page }) => {
    // Navigate to trip planning
    await page.click('[data-testid="nav-trips"]');
    await expect(page.locator('[data-testid="trips-page"]')).toBeVisible();

    // Create new trip
    await page.click('[data-testid="create-trip-button"]');
    await expect(page.locator('[data-testid="trip-creation-form"]')).toBeVisible();

    // Fill trip details
    await page.fill('[data-testid="trip-title"]', 'Amazing Bangladesh Adventure');
    await page.fill('[data-testid="trip-description"]', 'Exploring the beautiful landscapes and culture of Bangladesh');
    await page.fill('[data-testid="trip-start-date"]', '2024-07-01');
    await page.fill('[data-testid="trip-end-date"]', '2024-07-10');
    await page.selectOption('[data-testid="trip-budget"]', 'MEDIUM');
    await page.selectOption('[data-testid="trip-type"]', 'ADVENTURE');

    // Add destinations
    await page.click('[data-testid="add-destination-button"]');
    await page.fill('[data-testid="destination-search"]', 'Dhaka');
    await page.click('[data-testid="destination-suggestion"]:first-child');
    
    await page.click('[data-testid="add-destination-button"]');
    await page.fill('[data-testid="destination-search"]', 'Cox\'s Bazar');
    await page.click('[data-testid="destination-suggestion"]:first-child');

    await page.click('[data-testid="add-destination-button"]');
    await page.fill('[data-testid="destination-search"]', 'Sylhet');
    await page.click('[data-testid="destination-suggestion"]:first-child');

    // Save trip
    await page.click('[data-testid="save-trip-button"]');

    // Verify trip creation
    await expect(page.locator('text=Trip created successfully')).toBeVisible();
    await expect(page.locator('[data-testid="trip-card"]')).toBeVisible();
    await expect(page.locator('text=Amazing Bangladesh Adventure')).toBeVisible();
  });

  test('should add activities to trip itinerary', async ({ page }) => {
    // Navigate to existing trip
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    await expect(page.locator('[data-testid="trip-details"]')).toBeVisible();

    // Add activity for Day 1
    await page.click('[data-testid="day-1-add-activity"]');
    await page.fill('[data-testid="activity-title"]', 'Visit National Museum');
    await page.fill('[data-testid="activity-description"]', 'Explore Bangladesh\'s rich history and culture');
    await page.fill('[data-testid="activity-time"]', '10:00');
    await page.fill('[data-testid="activity-duration"]', '2');
    await page.selectOption('[data-testid="activity-category"]', 'CULTURAL');
    await page.click('[data-testid="save-activity-button"]');

    // Add activity for Day 2
    await page.click('[data-testid="day-2-add-activity"]');
    await page.fill('[data-testid="activity-title"]', 'Beach Walk at Cox\'s Bazar');
    await page.fill('[data-testid="activity-description"]', 'Enjoy the world\'s longest natural sea beach');
    await page.fill('[data-testid="activity-time"]', '06:00');
    await page.fill('[data-testid="activity-duration"]', '3');
    await page.selectOption('[data-testid="activity-category"]', 'OUTDOOR');
    await page.click('[data-testid="save-activity-button"]');

    // Verify activities are added
    await expect(page.locator('text=Visit National Museum')).toBeVisible();
    await expect(page.locator('text=Beach Walk at Cox\'s Bazar')).toBeVisible();
    await expect(page.locator('[data-testid="activity-item"]')).toHaveCount(2);
  });

  test('should search and filter destinations', async ({ page }) => {
    await page.click('[data-testid="nav-destinations"]');
    await expect(page.locator('[data-testid="destinations-page"]')).toBeVisible();

    // Test search functionality
    await page.fill('[data-testid="destination-search-input"]', 'Sundarbans');
    await page.click('[data-testid="search-button"]');
    
    await expect(page.locator('[data-testid="destination-card"]')).toBeVisible();
    await expect(page.locator('text=Sundarbans')).toBeVisible();

    // Test category filter
    await page.selectOption('[data-testid="category-filter"]', 'NATURE');
    await expect(page.locator('[data-testid="destination-card"]')).toBeVisible();

    // Test region filter
    await page.selectOption('[data-testid="region-filter"]', 'KHULNA');
    await expect(page.locator('[data-testid="destination-card"]')).toBeVisible();

    // Clear filters
    await page.click('[data-testid="clear-filters-button"]');
    await expect(page.locator('[data-testid="destination-card"]')).toHaveCount.toBeGreaterThan(1);
  });

  test('should view destination details and add to trip', async ({ page }) => {
    await page.click('[data-testid="nav-destinations"]');
    
    // Click on first destination
    await page.click('[data-testid="destination-card"]:first-child');
    await expect(page.locator('[data-testid="destination-details"]')).toBeVisible();

    // Verify destination information
    await expect(page.locator('[data-testid="destination-title"]')).toBeVisible();
    await expect(page.locator('[data-testid="destination-description"]')).toBeVisible();
    await expect(page.locator('[data-testid="destination-images"]')).toBeVisible();
    await expect(page.locator('[data-testid="destination-activities"]')).toBeVisible();

    // Add to existing trip
    await page.click('[data-testid="add-to-trip-button"]');
    await page.selectOption('[data-testid="select-trip"]', 'Amazing Bangladesh Adventure');
    await page.click('[data-testid="confirm-add-to-trip"]');

    // Verify success message
    await expect(page.locator('text=Destination added to trip')).toBeVisible();
  });

  test('should generate trip recommendations', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="get-recommendations-button"]');

    // Fill recommendation preferences
    await page.selectOption('[data-testid="travel-style"]', 'ADVENTURE');
    await page.selectOption('[data-testid="budget-range"]', 'MEDIUM');
    await page.fill('[data-testid="trip-duration"]', '7');
    await page.check('[data-testid="interest-nature"]');
    await page.check('[data-testid="interest-culture"]');
    await page.check('[data-testid="interest-food"]');

    await page.click('[data-testid="generate-recommendations"]');

    // Verify recommendations are generated
    await expect(page.locator('[data-testid="recommendations-list"]')).toBeVisible();
    await expect(page.locator('[data-testid="recommendation-card"]')).toHaveCount.toBeGreaterThan(0);

    // Accept a recommendation
    await page.click('[data-testid="accept-recommendation"]:first-child');
    await expect(page.locator('text=Recommendation added to your trip')).toBeVisible();
  });

  test('should manage trip budget and expenses', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    await page.click('[data-testid="budget-tab"]');

    // Set overall budget
    await page.fill('[data-testid="total-budget"]', '50000');
    await page.click('[data-testid="save-budget-button"]');

    // Add expense categories
    await page.click('[data-testid="add-expense-category"]');
    await page.fill('[data-testid="category-name"]', 'Accommodation');
    await page.fill('[data-testid="category-budget"]', '20000');
    await page.click('[data-testid="save-category-button"]');

    await page.click('[data-testid="add-expense-category"]');
    await page.fill('[data-testid="category-name"]', 'Transportation');
    await page.fill('[data-testid="category-budget"]', '15000');
    await page.click('[data-testid="save-category-button"]');

    // Add actual expenses
    await page.click('[data-testid="add-expense"]');
    await page.fill('[data-testid="expense-title"]', 'Hotel booking');
    await page.fill('[data-testid="expense-amount"]', '8000');
    await page.selectOption('[data-testid="expense-category"]', 'Accommodation');
    await page.fill('[data-testid="expense-date"]', '2024-07-01');
    await page.click('[data-testid="save-expense-button"]');

    // Verify budget tracking
    await expect(page.locator('[data-testid="budget-progress"]')).toBeVisible();
    await expect(page.locator('text=8000')).toBeVisible();
    await expect(page.locator('[data-testid="remaining-budget"]')).toContainText('42000');
  });

  test('should share trip with other users', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    
    // Share trip
    await page.click('[data-testid="share-trip-button"]');
    await expect(page.locator('[data-testid="share-modal"]')).toBeVisible();

    // Add collaborators
    await page.fill('[data-testid="collaborator-email"]', 'friend@example.com');
    await page.selectOption('[data-testid="permission-level"]', 'EDIT');
    await page.click('[data-testid="add-collaborator-button"]');

    // Generate shareable link
    await page.click('[data-testid="generate-link-button"]');
    await expect(page.locator('[data-testid="shareable-link"]')).toBeVisible();

    // Copy link
    await page.click('[data-testid="copy-link-button"]');
    await expect(page.locator('text=Link copied to clipboard')).toBeVisible();

    await page.click('[data-testid="close-share-modal"]');
  });

  test('should export trip itinerary', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    
    // Export options
    await page.click('[data-testid="export-trip-button"]');
    await expect(page.locator('[data-testid="export-modal"]')).toBeVisible();

    // Export as PDF
    const downloadPromise = page.waitForEvent('download');
    await page.click('[data-testid="export-pdf-button"]');
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toContain('.pdf');

    // Export as calendar
    await page.click('[data-testid="export-calendar-button"]');
    await expect(page.locator('text=Calendar events created')).toBeVisible();

    await page.click('[data-testid="close-export-modal"]');
  });

  test('should handle trip collaboration', async ({ page, context }) => {
    // Create second browser context for collaborator
    const collaboratorPage = await context.newPage();
    
    // Collaborator login
    await collaboratorPage.goto('/');
    await collaboratorPage.click('text=Sign In');
    await collaboratorPage.fill('[data-testid="login-email"]', 'friend@example.com');
    await collaboratorPage.fill('[data-testid="login-password"]', 'password123');
    await collaboratorPage.click('[data-testid="login-button"]');

    // Original user shares trip
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    await page.click('[data-testid="share-trip-button"]');
    await page.fill('[data-testid="collaborator-email"]', 'friend@example.com');
    await page.selectOption('[data-testid="permission-level"]', 'EDIT');
    await page.click('[data-testid="add-collaborator-button"]');

    // Collaborator should see shared trip
    await collaboratorPage.click('[data-testid="nav-trips"]');
    await expect(collaboratorPage.locator('[data-testid="shared-trips"]')).toBeVisible();
    await expect(collaboratorPage.locator('text=Amazing Bangladesh Adventure')).toBeVisible();

    // Collaborator makes changes
    await collaboratorPage.click('[data-testid="trip-card"]:first-child');
    await collaboratorPage.click('[data-testid="day-1-add-activity"]');
    await collaboratorPage.fill('[data-testid="activity-title"]', 'Collaborative Activity');
    await collaboratorPage.fill('[data-testid="activity-time"]', '14:00');
    await collaboratorPage.click('[data-testid="save-activity-button"]');

    // Original user should see the changes
    await page.reload();
    await expect(page.locator('text=Collaborative Activity')).toBeVisible();
  });

  test('should handle offline trip access', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    
    // Download for offline access
    await page.click('[data-testid="offline-download-button"]');
    await expect(page.locator('text=Trip downloaded for offline access')).toBeVisible();

    // Simulate offline mode
    await page.context().setOffline(true);
    
    // Verify trip is still accessible
    await page.reload();
    await expect(page.locator('[data-testid="trip-details"]')).toBeVisible();
    await expect(page.locator('[data-testid="offline-indicator"]')).toBeVisible();

    // Re-enable online mode
    await page.context().setOffline(false);
  });
});
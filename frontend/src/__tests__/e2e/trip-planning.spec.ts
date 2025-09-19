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

  test('should perform visual search for destinations', async ({ page }) => {
    await page.click('[data-testid="nav-destinations"]');
    await expect(page.locator('[data-testid="destinations-page"]')).toBeVisible();

    // Access visual search feature
    await page.click('[data-testid="visual-search-button"]');
    await expect(page.locator('[data-testid="visual-search-modal"]')).toBeVisible();

    // Upload image for visual search
    const fileInput = page.locator('[data-testid="image-upload-input"]');
    await fileInput.setInputFiles('tests/fixtures/sample-travel-photo.jpg');
    
    // Wait for image processing
    await expect(page.locator('[data-testid="image-preview"]')).toBeVisible();
    await page.click('[data-testid="analyze-image-button"]');
    
    // Wait for AI analysis
    await expect(page.locator('[data-testid="analysis-loading"]')).toBeVisible();
    await expect(page.locator('[data-testid="analysis-results"]')).toBeVisible({ timeout: 10000 });
    
    // Verify detected features
    await expect(page.locator('[data-testid="detected-features"]')).toBeVisible();
    await expect(page.locator('[data-testid="similar-destinations"]')).toBeVisible();
    
    // Select a similar destination
    await page.click('[data-testid="similar-destination-card"]:first-child');
    await expect(page.locator('[data-testid="destination-details"]')).toBeVisible();
  });

  test('should use camera for real-time visual search', async ({ page }) => {
    await page.click('[data-testid="nav-destinations"]');
    await page.click('[data-testid="visual-search-button"]');
    
    // Grant camera permissions (mocked)
    await page.evaluate(() => {
      navigator.mediaDevices.getUserMedia = () => Promise.resolve({
        getTracks: () => [{ stop: () => {} }]
      });
    });
    
    // Use camera for visual search
    await page.click('[data-testid="use-camera-button"]');
    await expect(page.locator('[data-testid="camera-preview"]')).toBeVisible();
    
    // Capture photo
    await page.click('[data-testid="capture-photo-button"]');
    await expect(page.locator('[data-testid="captured-image"]')).toBeVisible();
    
    // Analyze captured image
    await page.click('[data-testid="analyze-captured-image"]');
    await expect(page.locator('[data-testid="analysis-results"]')).toBeVisible({ timeout: 10000 });
    
    // Verify real-time suggestions
    await expect(page.locator('[data-testid="instant-suggestions"]')).toBeVisible();
  });

  test('should generate AI-powered trip recommendations', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="ai-trip-planner-button"]');
    
    await expect(page.locator('[data-testid="ai-planner-modal"]')).toBeVisible();
    
    // Provide detailed preferences
    await page.fill('[data-testid="trip-description"]', 'I want a cultural and adventure trip to Bangladesh with historical sites and nature');
    await page.selectOption('[data-testid="travel-season"]', 'WINTER');
    await page.selectOption('[data-testid="group-size"]', '2-4');
    await page.selectOption('[data-testid="accommodation-type"]', 'MID_RANGE');
    
    // Set interests with sliders
    await page.locator('[data-testid="culture-interest-slider"]').fill('90');
    await page.locator('[data-testid="adventure-interest-slider"]').fill('70');
    await page.locator('[data-testid="nature-interest-slider"]').fill('80');
    await page.locator('[data-testid="food-interest-slider"]').fill('60');
    
    // Generate AI recommendations
    await page.click('[data-testid="generate-ai-trip"]');
    
    // Wait for AI processing
    await expect(page.locator('[data-testid="ai-processing"]')).toBeVisible();
    await expect(page.locator('[data-testid="ai-recommendations"]')).toBeVisible({ timeout: 15000 });
    
    // Verify comprehensive recommendations
    await expect(page.locator('[data-testid="recommended-itinerary"]')).toBeVisible();
    await expect(page.locator('[data-testid="recommended-accommodations"]')).toBeVisible();
    await expect(page.locator('[data-testid="recommended-activities"]')).toBeVisible();
    await expect(page.locator('[data-testid="budget-estimation"]')).toBeVisible();
    
    // Accept AI-generated trip
    await page.click('[data-testid="accept-ai-trip"]');
    await page.fill('[data-testid="trip-name"]', 'AI-Powered Bangladesh Adventure');
    await page.click('[data-testid="create-ai-trip"]');
    
    await expect(page.locator('text=AI-Powered Bangladesh Adventure')).toBeVisible();
  });

  test('should optimize existing trip itinerary', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    
    // Access itinerary optimization
    await page.click('[data-testid="optimize-itinerary-button"]');
    await expect(page.locator('[data-testid="optimization-modal"]')).toBeVisible();
    
    // Set optimization preferences
    await page.check('[data-testid="minimize-travel-time"]');
    await page.check('[data-testid="maximize-experiences"]');
    await page.check('[data-testid="budget-conscious"]');
    
    // Run optimization
    await page.click('[data-testid="run-optimization"]');
    
    // Wait for optimization results
    await expect(page.locator('[data-testid="optimization-results"]')).toBeVisible({ timeout: 10000 });
    
    // Review suggested changes
    await expect(page.locator('[data-testid="suggested-changes"]')).toBeVisible();
    await expect(page.locator('[data-testid="time-savings"]')).toBeVisible();
    await expect(page.locator('[data-testid="cost-impact"]')).toBeVisible();
    
    // Apply optimizations
    await page.click('[data-testid="apply-optimizations"]');
    await expect(page.locator('text=Itinerary optimized successfully')).toBeVisible();
    
    // Verify optimized itinerary
    await expect(page.locator('[data-testid="optimized-badge"]')).toBeVisible();
  });

  test('should discover destinations through image similarity', async ({ page }) => {
    await page.click('[data-testid="nav-destinations"]');
    
    // Browse destinations and find similar ones
    await page.click('[data-testid="destination-card"]:first-child');
    await expect(page.locator('[data-testid="destination-details"]')).toBeVisible();
    
    // Use "Find Similar" feature
    await page.click('[data-testid="find-similar-button"]');
    await expect(page.locator('[data-testid="similar-destinations-modal"]')).toBeVisible();
    
    // Verify similar destinations based on visual features
    await expect(page.locator('[data-testid="similar-destination-card"]')).toHaveCount.toBeGreaterThan(2);
    await expect(page.locator('[data-testid="similarity-score"]')).toBeVisible();
    
    // Filter similar destinations
    await page.selectOption('[data-testid="similarity-filter"]', 'LANDSCAPE');
    await expect(page.locator('[data-testid="filtered-similar-destinations"]')).toBeVisible();
    
    // Add similar destination to trip
    await page.click('[data-testid="add-similar-to-trip"]:first-child');
    await page.selectOption('[data-testid="select-trip"]', 'Amazing Bangladesh Adventure');
    await page.click('[data-testid="confirm-add-similar"]');
    
    await expect(page.locator('text=Similar destination added to trip')).toBeVisible();
  });

  test('should handle smart trip suggestions based on user behavior', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    
    // Verify smart suggestions appear
    await expect(page.locator('[data-testid="smart-suggestions"]')).toBeVisible();
    await expect(page.locator('[data-testid="suggestion-card"]')).toHaveCount.toBeGreaterThan(0);
    
    // View suggestion details
    await page.click('[data-testid="suggestion-card"]:first-child');
    await expect(page.locator('[data-testid="suggestion-details"]')).toBeVisible();
    await expect(page.locator('[data-testid="suggestion-reasoning"]')).toBeVisible();
    
    // Accept smart suggestion
    await page.click('[data-testid="accept-suggestion"]');
    await page.fill('[data-testid="suggested-trip-name"]', 'Smart Suggested Adventure');
    await page.click('[data-testid="create-suggested-trip"]');
    
    await expect(page.locator('text=Smart Suggested Adventure')).toBeVisible();
    
    // Dismiss suggestion
    await page.click('[data-testid="suggestion-card"]:nth-child(2)');
    await page.click('[data-testid="dismiss-suggestion"]');
    await page.selectOption('[data-testid="dismiss-reason"]', 'NOT_INTERESTED');
    await page.click('[data-testid="confirm-dismiss"]');
    
    await expect(page.locator('[data-testid="suggestion-dismissed"]')).toBeVisible();
  });

  test('should provide weather-aware trip planning', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="create-trip-button"]');
    
    // Fill basic trip information
    await page.fill('[data-testid="trip-title"]', 'Weather-Optimized Trip');
    await page.fill('[data-testid="trip-start-date"]', '2024-12-01');
    await page.fill('[data-testid="trip-end-date"]', '2024-12-07');
    
    // Enable weather optimization
    await page.check('[data-testid="weather-optimization"]');
    await page.click('[data-testid="analyze-weather"]');
    
    // Wait for weather analysis
    await expect(page.locator('[data-testid="weather-analysis"]')).toBeVisible({ timeout: 8000 });
    await expect(page.locator('[data-testid="weather-recommendations"]')).toBeVisible();
    
    // View weather-based suggestions
    await expect(page.locator('[data-testid="weather-suitable-activities"]')).toBeVisible();
    await expect(page.locator('[data-testid="weather-warnings"]')).toBeVisible();
    
    // Apply weather recommendations
    await page.click('[data-testid="apply-weather-recommendations"]');
    await page.click('[data-testid="create-weather-optimized-trip"]');
    
    await expect(page.locator('text=Weather-Optimized Trip')).toBeVisible();
    await expect(page.locator('[data-testid="weather-optimized-badge"]')).toBeVisible();
  });
});
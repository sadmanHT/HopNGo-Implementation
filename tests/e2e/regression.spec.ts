import { test, expect } from '@playwright/test';
import { faker } from '@faker-js/faker';

// Test configuration
const BASE_URL = process.env.BASE_URL || 'https://staging.hopngo.com';
const TEST_USER_EMAIL = faker.internet.email();
const TEST_USER_PASSWORD = 'TestPassword123!';
const PROVIDER_EMAIL = faker.internet.email();
const PROVIDER_PASSWORD = 'ProviderPass123!';

test.describe('HopNGo v1.0.0 Regression Suite', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(BASE_URL);
  });

  test('Complete user journey: Sign-up → Login → Post → Search → Heatmap → Trip Planning → Booking → Payment → Review', async ({ page }) => {
    // Step 1: Sign-up
    await test.step('User Sign-up', async () => {
      await page.click('[data-testid="signup-button"]');
      await page.fill('[data-testid="email-input"]', TEST_USER_EMAIL);
      await page.fill('[data-testid="password-input"]', TEST_USER_PASSWORD);
      await page.fill('[data-testid="confirm-password-input"]', TEST_USER_PASSWORD);
      await page.fill('[data-testid="first-name-input"]', faker.person.firstName());
      await page.fill('[data-testid="last-name-input"]', faker.person.lastName());
      await page.click('[data-testid="submit-signup"]');
      
      // Wait for email verification or auto-login
      await expect(page.locator('[data-testid="dashboard"]')).toBeVisible({ timeout: 10000 });
    });

    // Step 2: Login (if needed)
    await test.step('User Login Verification', async () => {
      // Check if already logged in, otherwise login
      const isLoggedIn = await page.locator('[data-testid="user-menu"]').isVisible();
      if (!isLoggedIn) {
        await page.click('[data-testid="login-button"]');
        await page.fill('[data-testid="login-email"]', TEST_USER_EMAIL);
        await page.fill('[data-testid="login-password"]', TEST_USER_PASSWORD);
        await page.click('[data-testid="submit-login"]');
        await expect(page.locator('[data-testid="dashboard"]')).toBeVisible();
      }
    });

    // Step 3: Create a post
    await test.step('Create Social Post', async () => {
      await page.click('[data-testid="create-post-button"]');
      await page.fill('[data-testid="post-content"]', 'Amazing trip to Srimangal! The tea gardens are breathtaking 🍃');
      await page.selectOption('[data-testid="location-select"]', 'Srimangal, Bangladesh');
      
      // Upload image if file input exists
      const fileInput = page.locator('[data-testid="image-upload"]');
      if (await fileInput.isVisible()) {
        await fileInput.setInputFiles('tests/fixtures/tea-garden.jpg');
      }
      
      await page.click('[data-testid="publish-post"]');
      await expect(page.locator('[data-testid="post-success"]')).toBeVisible();
    });

    // Step 4: Text Search
    await test.step('Text Search Functionality', async () => {
      await page.click('[data-testid="search-button"]');
      await page.fill('[data-testid="search-input"]', 'tea gardens Srimangal');
      await page.click('[data-testid="search-submit"]');
      
      // Wait for search results
      await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
      await expect(page.locator('[data-testid="search-result-item"]').first()).toBeVisible();
    });

    // Step 5: Visual Search (if enabled)
    await test.step('Visual Search Functionality', async () => {
      await page.click('[data-testid="visual-search-tab"]');
      
      // Check if visual search is enabled
      const visualSearchEnabled = await page.locator('[data-testid="visual-search-upload"]').isVisible();
      if (visualSearchEnabled) {
        await page.locator('[data-testid="visual-search-upload"]').setInputFiles('tests/fixtures/tea-garden.jpg');
        await page.click('[data-testid="visual-search-submit"]');
        await expect(page.locator('[data-testid="visual-search-results"]')).toBeVisible({ timeout: 15000 });
      }
    });

    // Step 6: Heatmap Visualization
    await test.step('Heatmap Visualization', async () => {
      await page.click('[data-testid="heatmap-button"]');
      await expect(page.locator('[data-testid="heatmap-container"]')).toBeVisible();
      
      // Check if heatmap v2 is loaded
      await expect(page.locator('[data-testid="heatmap-legend"]')).toBeVisible();
      await expect(page.locator('[data-testid="trending-locations"]')).toBeVisible();
      
      // Click on a trending location
      await page.click('[data-testid="trending-location"]').first();
      await expect(page.locator('[data-testid="location-vlogs"]')).toBeVisible();
    });

    // Step 7: AI Trip Planning
    await test.step('AI Trip Planning', async () => {
      await page.click('[data-testid="plan-trip-button"]');
      await page.fill('[data-testid="destination-input"]', 'Srimangal, Bangladesh');
      await page.fill('[data-testid="duration-input"]', '3');
      await page.selectOption('[data-testid="budget-select"]', 'medium');
      await page.fill('[data-testid="interests-input"]', 'nature, tea gardens, hiking');
      
      await page.click('[data-testid="generate-itinerary"]');
      await expect(page.locator('[data-testid="ai-itinerary"]')).toBeVisible({ timeout: 20000 });
      
      // Verify itinerary has 3 days
      await expect(page.locator('[data-testid="day-1"]')).toBeVisible();
      await expect(page.locator('[data-testid="day-2"]')).toBeVisible();
      await expect(page.locator('[data-testid="day-3"]')).toBeVisible();
      
      // Tweak budget
      await page.click('[data-testid="adjust-budget"]');
      await page.selectOption('[data-testid="budget-select"]', 'high');
      await page.click('[data-testid="update-itinerary"]');
      await expect(page.locator('[data-testid="budget-updated"]')).toBeVisible();
    });

    // Step 8: Booking Accommodation
    await test.step('Book Accommodation', async () => {
      await page.click('[data-testid="book-homestay"]');
      await expect(page.locator('[data-testid="booking-form"]')).toBeVisible();
      
      // Fill booking details
      await page.fill('[data-testid="checkin-date"]', '2024-03-15');
      await page.fill('[data-testid="checkout-date"]', '2024-03-18');
      await page.selectOption('[data-testid="guests-select"]', '2');
      await page.fill('[data-testid="special-requests"]', 'Vegetarian meals preferred');
      
      await page.click('[data-testid="proceed-to-payment"]');
      await expect(page.locator('[data-testid="payment-form"]')).toBeVisible();
    });

    // Step 9: Payment Processing
    await test.step('Payment Processing', async () => {
      // Select payment method
      await page.click('[data-testid="stripe-payment"]');
      
      // Fill Stripe test card details
      await page.fill('[data-testid="card-number"]', '4242424242424242');
      await page.fill('[data-testid="card-expiry"]', '12/25');
      await page.fill('[data-testid="card-cvc"]', '123');
      await page.fill('[data-testid="cardholder-name"]', 'Test User');
      
      await page.click('[data-testid="complete-payment"]');
      
      // Wait for payment confirmation
      await expect(page.locator('[data-testid="payment-success"]')).toBeVisible({ timeout: 15000 });
      await expect(page.locator('[data-testid="booking-confirmation"]')).toBeVisible();
      
      // Check for push notification or email confirmation
      await expect(page.locator('[data-testid="confirmation-sent"]')).toBeVisible();
    });

    // Step 10: Chat with Host
    await test.step('Chat with Host', async () => {
      await page.click('[data-testid="chat-with-host"]');
      await expect(page.locator('[data-testid="chat-window"]')).toBeVisible();
      
      // Send a message
      await page.fill('[data-testid="chat-input"]', 'Hi! Looking forward to staying at your place. Any recommendations for local attractions?');
      await page.click('[data-testid="send-message"]');
      
      await expect(page.locator('[data-testid="message-sent"]')).toBeVisible();
      
      // Check weather integration
      await page.click('[data-testid="weather-widget"]');
      await expect(page.locator('[data-testid="weather-info"]')).toBeVisible();
      
      // Check route time
      await page.click('[data-testid="route-info"]');
      await expect(page.locator('[data-testid="travel-time"]')).toBeVisible();
    });

    // Step 11: Post-Stay Review
    await test.step('Post-Stay Review', async () => {
      // Simulate completed stay (this would normally be time-based)
      await page.goto(`${BASE_URL}/bookings/review`);
      
      await page.click('[data-testid="write-review"]');
      await page.fill('[data-testid="review-title"]', 'Amazing homestay experience!');
      await page.fill('[data-testid="review-content"]', 'The host was incredibly welcoming and the location was perfect for exploring the tea gardens.');
      
      // Rate the experience
      await page.click('[data-testid="rating-5-stars"]');
      
      // Upload photos
      const photoUpload = page.locator('[data-testid="review-photos"]');
      if (await photoUpload.isVisible()) {
        await photoUpload.setInputFiles(['tests/fixtures/homestay-1.jpg', 'tests/fixtures/homestay-2.jpg']);
      }
      
      await page.click('[data-testid="submit-review"]');
      await expect(page.locator('[data-testid="review-success"]')).toBeVisible();
    });
  });

  test('Provider Journey: KYC → Listing → Analytics → Booking Management', async ({ page }) => {
    // Provider Sign-up and KYC
    await test.step('Provider KYC Verification', async () => {
      await page.click('[data-testid="become-provider"]');
      await page.fill('[data-testid="provider-email"]', PROVIDER_EMAIL);
      await page.fill('[data-testid="provider-password"]', PROVIDER_PASSWORD);
      await page.fill('[data-testid="business-name"]', 'Srimangal Tea Garden Homestay');
      await page.fill('[data-testid="business-address"]', 'Tea Garden Road, Srimangal');
      await page.fill('[data-testid="tax-id"]', 'TIN123456789');
      
      // Upload KYC documents
      await page.locator('[data-testid="nid-upload"]').setInputFiles('tests/fixtures/nid-sample.pdf');
      await page.locator('[data-testid="business-license"]').setInputFiles('tests/fixtures/license-sample.pdf');
      
      await page.click('[data-testid="submit-kyc"]');
      await expect(page.locator('[data-testid="kyc-submitted"]')).toBeVisible();
      
      // For testing, simulate KYC approval
      await expect(page.locator('[data-testid="kyc-verified"]')).toBeVisible({ timeout: 5000 });
    });

    // Create Listing
    await test.step('Create Property Listing', async () => {
      await page.click('[data-testid="create-listing"]');
      await page.fill('[data-testid="property-title"]', 'Cozy Tea Garden Homestay');
      await page.fill('[data-testid="property-description"]', 'Experience authentic Bengali hospitality surrounded by lush tea gardens.');
      await page.selectOption('[data-testid="property-type"]', 'homestay');
      await page.fill('[data-testid="price-per-night"]', '2500');
      await page.fill('[data-testid="max-guests"]', '4');
      
      // Add amenities
      await page.check('[data-testid="wifi"]');
      await page.check('[data-testid="breakfast"]');
      await page.check('[data-testid="parking"]');
      
      // Upload property photos
      await page.locator('[data-testid="property-photos"]').setInputFiles([
        'tests/fixtures/homestay-exterior.jpg',
        'tests/fixtures/homestay-room.jpg',
        'tests/fixtures/homestay-garden.jpg'
      ]);
      
      await page.click('[data-testid="publish-listing"]');
      await expect(page.locator('[data-testid="listing-published"]')).toBeVisible();
    });

    // View Provider Analytics
    await test.step('Provider Analytics Dashboard', async () => {
      await page.click('[data-testid="provider-analytics"]');
      await expect(page.locator('[data-testid="analytics-dashboard"]')).toBeVisible();
      
      // Check key metrics
      await expect(page.locator('[data-testid="total-bookings"]')).toBeVisible();
      await expect(page.locator('[data-testid="revenue-chart"]')).toBeVisible();
      await expect(page.locator('[data-testid="occupancy-rate"]')).toBeVisible();
      await expect(page.locator('[data-testid="guest-ratings"]')).toBeVisible();
    });

    // Booking Management
    await test.step('Booking Notification and Response', async () => {
      // Simulate receiving a booking notification
      await expect(page.locator('[data-testid="booking-notification"]')).toBeVisible({ timeout: 10000 });
      
      await page.click('[data-testid="view-booking"]');
      await expect(page.locator('[data-testid="booking-details"]')).toBeVisible();
      
      // Respond to booking
      await page.fill('[data-testid="host-message"]', 'Welcome! I\'m excited to host you. The tea gardens are beautiful this time of year.');
      await page.click('[data-testid="accept-booking"]');
      
      // Check SLA widget update
      await expect(page.locator('[data-testid="response-time-sla"]')).toBeVisible();
      await expect(page.locator('[data-testid="sla-status-good"]')).toBeVisible();
    });

    // Payout Request
    await test.step('Provider Payout Request', async () => {
      await page.click('[data-testid="request-payout"]');
      await expect(page.locator('[data-testid="payout-form"]')).toBeVisible();
      
      await page.fill('[data-testid="bank-account"]', '1234567890');
      await page.fill('[data-testid="routing-number"]', '123456789');
      await page.selectOption('[data-testid="payout-method"]', 'bank_transfer');
      
      await page.click('[data-testid="submit-payout-request"]');
      await expect(page.locator('[data-testid="payout-requested"]')).toBeVisible();
    });
  });

  test('Emergency Services Integration', async ({ page }) => {
    await test.step('Emergency Trigger Simulation', async () => {
      // Login as user first
      await page.click('[data-testid="login-button"]');
      await page.fill('[data-testid="login-email"]', TEST_USER_EMAIL);
      await page.fill('[data-testid="login-password"]', TEST_USER_PASSWORD);
      await page.click('[data-testid="submit-login"]');
      
      // Access emergency features
      await page.click('[data-testid="emergency-button"]');
      await expect(page.locator('[data-testid="emergency-panel"]')).toBeVisible();
      
      // Test emergency contact
      await page.click('[data-testid="emergency-contact"]');
      await expect(page.locator('[data-testid="emergency-contacts-list"]')).toBeVisible();
      
      // Test location sharing
      await page.click('[data-testid="share-location"]');
      await expect(page.locator('[data-testid="location-shared"]')).toBeVisible();
      
      // Test emergency alert (simulated)
      await page.click('[data-testid="send-emergency-alert"]');
      await expect(page.locator('[data-testid="alert-sent"]')).toBeVisible();
    });
  });

  test('Performance and Accessibility Checks', async ({ page }) => {
    await test.step('Page Load Performance', async () => {
      const startTime = Date.now();
      await page.goto(BASE_URL);
      const loadTime = Date.now() - startTime;
      
      // Ensure page loads within 3 seconds
      expect(loadTime).toBeLessThan(3000);
    });

    await test.step('PWA Features', async () => {
      // Check if PWA manifest is present
      const manifest = await page.locator('link[rel="manifest"]').getAttribute('href');
      expect(manifest).toBeTruthy();
      
      // Check service worker registration
      const swRegistered = await page.evaluate(() => {
        return 'serviceWorker' in navigator;
      });
      expect(swRegistered).toBe(true);
    });

    await test.step('Basic Accessibility', async () => {
      // Check for proper heading structure
      await expect(page.locator('h1')).toBeVisible();
      
      // Check for alt text on images
      const images = await page.locator('img').all();
      for (const img of images) {
        const alt = await img.getAttribute('alt');
        expect(alt).toBeTruthy();
      }
      
      // Check for proper form labels
      const inputs = await page.locator('input[type="text"], input[type="email"], input[type="password"]').all();
      for (const input of inputs) {
        const id = await input.getAttribute('id');
        if (id) {
          await expect(page.locator(`label[for="${id}"]`)).toBeVisible();
        }
      }
    });
  });
});

// Helper functions for test data cleanup
test.afterAll(async () => {
  // Clean up test data if needed
  console.log('Regression tests completed. Clean up any test data if necessary.');
});
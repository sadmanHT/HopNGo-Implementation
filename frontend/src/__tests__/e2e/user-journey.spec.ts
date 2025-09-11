import { test, expect } from '@playwright/test';

test.describe('User Journey - Social Features', () => {
  test('should register, login, create post, and view feed', async ({ page }) => {
    // Navigate to the application
    await page.goto('/');

    // Register a new user
    await page.click('text=Sign Up');
    await page.fill('[data-testid="firstName"]', 'John');
    await page.fill('[data-testid="lastName"]', 'Doe');
    await page.fill('[data-testid="email"]', `test-${Date.now()}@example.com`);
    await page.fill('[data-testid="password"]', 'Password123!');
    await page.selectOption('[data-testid="role"]', 'CUSTOMER');
    await page.click('[data-testid="register-button"]');

    // Verify registration success
    await expect(page.locator('text=Registration successful')).toBeVisible();

    // Login with the new user
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', `test-${Date.now()}@example.com`);
    await page.fill('[data-testid="login-password"]', 'Password123!');
    await page.click('[data-testid="login-button"]');

    // Verify login success - should be redirected to dashboard
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();

    // Navigate to social feed
    await page.click('[data-testid="nav-social"]');

    // Create a new post
    await page.click('[data-testid="create-post-button"]');
    await page.fill('[data-testid="post-content"]', 'This is my first post on HopNGo! 🎉');
    await page.fill('[data-testid="post-tags"]', 'travel,adventure,hopngo');
    await page.click('[data-testid="submit-post-button"]');

    // Verify post creation success
    await expect(page.locator('text=Post created successfully')).toBeVisible();

    // Verify the post appears in the feed
    await expect(page.locator('text=This is my first post on HopNGo! 🎉')).toBeVisible();
    await expect(page.locator('[data-testid="post-tags"] >> text=travel')).toBeVisible();
    await expect(page.locator('[data-testid="post-tags"] >> text=adventure')).toBeVisible();

    // Interact with the post (like)
    await page.click('[data-testid="like-button"]');
    await expect(page.locator('[data-testid="like-count"] >> text=1')).toBeVisible();

    // Verify user can see their own post in their profile
    await page.click('[data-testid="user-profile"]');
    await expect(page.locator('text=This is my first post on HopNGo! 🎉')).toBeVisible();
  });
});

test.describe('User Journey - Booking Features', () => {
  test('should search listings, create booking, and complete checkout', async ({ page }) => {
    // Navigate to the application and login
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'customer@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');

    // Navigate to booking search
    await page.click('[data-testid="nav-bookings"]');

    // Search for listings
    await page.fill('[data-testid="search-location"]', 'New York');
    await page.fill('[data-testid="search-checkin"]', '2024-06-01');
    await page.fill('[data-testid="search-checkout"]', '2024-06-03');
    await page.fill('[data-testid="search-guests"]', '2');
    await page.selectOption('[data-testid="search-category"]', 'HOTEL');
    await page.click('[data-testid="search-button"]');

    // Verify search results
    await expect(page.locator('[data-testid="listing-card"]')).toBeVisible();

    // Select the first listing
    await page.click('[data-testid="listing-card"]:first-child');

    // Verify listing details page
    await expect(page.locator('[data-testid="listing-title"]')).toBeVisible();
    await expect(page.locator('[data-testid="listing-price"]')).toBeVisible();

    // Create a booking
    await page.click('[data-testid="book-now-button"]');
    await page.fill('[data-testid="special-requests"]', 'Please provide extra towels');
    await page.click('[data-testid="confirm-booking-button"]');

    // Proceed to checkout
    await expect(page.locator('[data-testid="checkout-page"]')).toBeVisible();
    await expect(page.locator('text=Please provide extra towels')).toBeVisible();

    // Fill payment information (mock)
    await page.fill('[data-testid="card-number"]', '4111111111111111');
    await page.fill('[data-testid="card-expiry"]', '12/25');
    await page.fill('[data-testid="card-cvc"]', '123');
    await page.fill('[data-testid="cardholder-name"]', 'John Doe');

    // Complete the booking
    await page.click('[data-testid="complete-payment-button"]');

    // Verify booking confirmation
    await expect(page.locator('[data-testid="booking-confirmation"]')).toBeVisible();
    await expect(page.locator('text=Booking confirmed')).toBeVisible();
    await expect(page.locator('[data-testid="booking-id"]')).toBeVisible();

    // Verify booking appears in user's bookings
    await page.click('[data-testid="my-bookings"]');
    await expect(page.locator('[data-testid="booking-item"]')).toBeVisible();
  });
});

test.describe('User Journey - Chat Features', () => {
  test('should enable chat between users', async ({ page, context }) => {
    // Create two browser contexts for two different users
    const page1 = page;
    const page2 = await context.newPage();

    // User 1 login
    await page1.goto('/');
    await page1.click('text=Sign In');
    await page1.fill('[data-testid="login-email"]', 'user1@example.com');
    await page1.fill('[data-testid="login-password"]', 'password123');
    await page1.click('[data-testid="login-button"]');

    // User 2 login
    await page2.goto('/');
    await page2.click('text=Sign In');
    await page2.fill('[data-testid="login-email"]', 'user2@example.com');
    await page2.fill('[data-testid="login-password"]', 'password123');
    await page2.click('[data-testid="login-button"]');

    // User 1 navigates to chat
    await page1.click('[data-testid="nav-chat"]');
    await page1.click('[data-testid="start-new-chat"]');
    await page1.fill('[data-testid="search-users"]', 'user2@example.com');
    await page1.click('[data-testid="select-user"]:first-child');

    // User 1 sends a message
    await page1.fill('[data-testid="message-input"]', 'Hello! I\'m interested in your listing.');
    await page1.click('[data-testid="send-message-button"]');

    // Verify message appears in User 1's chat
    await expect(page1.locator('text=Hello! I\'m interested in your listing.')).toBeVisible();

    // User 2 navigates to chat and should see the message
    await page2.click('[data-testid="nav-chat"]');
    await expect(page2.locator('[data-testid="chat-notification"]')).toBeVisible();
    await page2.click('[data-testid="chat-conversation"]:first-child');

    // Verify User 2 can see User 1's message
    await expect(page2.locator('text=Hello! I\'m interested in your listing.')).toBeVisible();

    // User 2 replies
    await page2.fill('[data-testid="message-input"]', 'Hi! Thanks for your interest. When are you planning to visit?');
    await page2.click('[data-testid="send-message-button"]');

    // Verify User 1 receives the reply
    await expect(page1.locator('text=Hi! Thanks for your interest. When are you planning to visit?')).toBeVisible();

    // Test real-time messaging
    await page1.fill('[data-testid="message-input"]', 'I\'m planning for next weekend.');
    await page1.click('[data-testid="send-message-button"]');

    // Verify real-time delivery to User 2
    await expect(page2.locator('text=I\'m planning for next weekend.')).toBeVisible();
  });
});
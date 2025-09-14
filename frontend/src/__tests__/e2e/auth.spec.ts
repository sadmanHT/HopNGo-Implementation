import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should register a new user successfully', async ({ page }) => {
    const timestamp = Date.now();
    const testEmail = `test-user-${timestamp}@example.com`;

    // Navigate to registration
    await page.click('text=Sign Up');
    await expect(page.locator('[data-testid="register-form"]')).toBeVisible();

    // Fill registration form
    await page.fill('[data-testid="firstName"]', 'John');
    await page.fill('[data-testid="lastName"]', 'Doe');
    await page.fill('[data-testid="email"]', testEmail);
    await page.fill('[data-testid="password"]', 'SecurePass123!');
    await page.fill('[data-testid="confirmPassword"]', 'SecurePass123!');
    await page.selectOption('[data-testid="role"]', 'CUSTOMER');
    await page.check('[data-testid="terms-checkbox"]');

    // Submit registration
    await page.click('[data-testid="register-button"]');

    // Verify registration success
    await expect(page.locator('text=Registration successful')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="verification-message"]')).toBeVisible();
  });

  test('should validate registration form fields', async ({ page }) => {
    await page.click('text=Sign Up');

    // Test empty form submission
    await page.click('[data-testid="register-button"]');
    await expect(page.locator('text=First name is required')).toBeVisible();
    await expect(page.locator('text=Last name is required')).toBeVisible();
    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();

    // Test invalid email format
    await page.fill('[data-testid="email"]', 'invalid-email');
    await page.click('[data-testid="register-button"]');
    await expect(page.locator('text=Please enter a valid email')).toBeVisible();

    // Test weak password
    await page.fill('[data-testid="email"]', 'test@example.com');
    await page.fill('[data-testid="password"]', '123');
    await page.click('[data-testid="register-button"]');
    await expect(page.locator('text=Password must be at least 8 characters')).toBeVisible();

    // Test password mismatch
    await page.fill('[data-testid="password"]', 'SecurePass123!');
    await page.fill('[data-testid="confirmPassword"]', 'DifferentPass123!');
    await page.click('[data-testid="register-button"]');
    await expect(page.locator('text=Passwords do not match')).toBeVisible();
  });

  test('should login with valid credentials', async ({ page }) => {
    // Navigate to login
    await page.click('text=Sign In');
    await expect(page.locator('[data-testid="login-form"]')).toBeVisible();

    // Fill login form with test credentials
    await page.fill('[data-testid="login-email"]', 'customer@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');

    // Verify successful login
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
    await expect(page.locator('text=Welcome back')).toBeVisible();

    // Verify user is redirected to dashboard
    await expect(page).toHaveURL(/.*\/dashboard/);
  });

  test('should handle invalid login credentials', async ({ page }) => {
    await page.click('text=Sign In');

    // Test with invalid email
    await page.fill('[data-testid="login-email"]', 'nonexistent@example.com');
    await page.fill('[data-testid="login-password"]', 'wrongpassword');
    await page.click('[data-testid="login-button"]');

    // Verify error message
    await expect(page.locator('text=Invalid email or password')).toBeVisible();
    await expect(page.locator('[data-testid="login-error"]')).toBeVisible();

    // Test with empty fields
    await page.fill('[data-testid="login-email"]', '');
    await page.fill('[data-testid="login-password"]', '');
    await page.click('[data-testid="login-button"]');

    await expect(page.locator('text=Email is required')).toBeVisible();
    await expect(page.locator('text=Password is required')).toBeVisible();
  });

  test('should handle forgot password flow', async ({ page }) => {
    await page.click('text=Sign In');
    await page.click('text=Forgot Password?');

    // Verify forgot password form
    await expect(page.locator('[data-testid="forgot-password-form"]')).toBeVisible();

    // Submit valid email
    await page.fill('[data-testid="reset-email"]', 'customer@example.com');
    await page.click('[data-testid="send-reset-button"]');

    // Verify success message
    await expect(page.locator('text=Password reset email sent')).toBeVisible();
    await expect(page.locator('[data-testid="reset-success-message"]')).toBeVisible();
  });

  test('should logout user successfully', async ({ page }) => {
    // Login first
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'customer@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');

    // Wait for dashboard to load
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();

    // Logout
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="logout-button"]');

    // Verify logout
    await expect(page.locator('text=Sign In')).toBeVisible();
    await expect(page.locator('[data-testid="user-menu"]')).not.toBeVisible();
    await expect(page).toHaveURL(/.*\/$/);
  });

  test('should handle session expiration', async ({ page }) => {
    // Login first
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'customer@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');

    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();

    // Simulate session expiration by clearing localStorage
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });

    // Try to access protected route
    await page.goto('/profile');

    // Should be redirected to login
    await expect(page.locator('[data-testid="login-form"]')).toBeVisible();
    await expect(page.locator('text=Session expired. Please login again.')).toBeVisible();
  });

  test('should handle different user roles', async ({ page }) => {
    // Test PROVIDER role login
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'provider@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');

    // Verify provider dashboard
    await expect(page.locator('[data-testid="provider-dashboard"]')).toBeVisible();
    await expect(page.locator('text=Provider Portal')).toBeVisible();
    await expect(page.locator('[data-testid="manage-listings"]')).toBeVisible();

    // Logout and test ADMIN role
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="logout-button"]');

    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'admin@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');

    // Verify admin dashboard
    await expect(page.locator('[data-testid="admin-dashboard"]')).toBeVisible();
    await expect(page.locator('text=Admin Panel')).toBeVisible();
    await expect(page.locator('[data-testid="user-management"]')).toBeVisible();
  });

  test('should handle social login (OAuth)', async ({ page }) => {
    await page.click('text=Sign In');

    // Test Google OAuth (mock)
    await page.click('[data-testid="google-login-button"]');
    
    // In a real test, this would redirect to Google OAuth
    // For now, we'll mock the success response
    await page.evaluate(() => {
      window.postMessage({
        type: 'OAUTH_SUCCESS',
        provider: 'google',
        user: {
          email: 'oauth-user@gmail.com',
          name: 'OAuth User',
          picture: 'https://example.com/avatar.jpg'
        }
      }, '*');
    });

    // Verify OAuth login success
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();
    await expect(page.locator('text=Welcome, OAuth User')).toBeVisible();
  });

  test('should handle account verification', async ({ page }) => {
    // Simulate clicking verification link from email
    const verificationToken = 'mock-verification-token-123';
    await page.goto(`/verify-email?token=${verificationToken}`);

    // Verify account verification page
    await expect(page.locator('[data-testid="verification-page"]')).toBeVisible();
    await expect(page.locator('text=Verifying your account...')).toBeVisible();

    // Wait for verification to complete
    await expect(page.locator('text=Account verified successfully!')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-testid="verification-success"]')).toBeVisible();

    // Should be able to login now
    await page.click('[data-testid="proceed-to-login"]');
    await expect(page.locator('[data-testid="login-form"]')).toBeVisible();
  });
});
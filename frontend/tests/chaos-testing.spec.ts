import { test, expect, Page } from '@playwright/test';

// Chaos Testing Suite - Service Failure Scenarios
test.describe('Chaos Testing - Service Failures', () => {
  let page: Page;

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
    await page.goto('/');
  });

  test('handles authentication service failure gracefully', async () => {
    // Mock auth service failure
    await page.route('**/api/auth/**', route => {
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Service Unavailable' })
      });
    });

    // Attempt login
    await page.click('[data-testid="login-button"]');
    await page.fill('[data-testid="email-input"]', 'test@example.com');
    await page.fill('[data-testid="password-input"]', 'password123');
    await page.click('[data-testid="submit-login"]');

    // Verify graceful error handling
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="error-message"]')).toContainText('Authentication service is temporarily unavailable');
    
    // Verify retry mechanism
    await expect(page.locator('[data-testid="retry-button"]')).toBeVisible();
  });

  test('handles booking service failure with fallback', async () => {
    // Mock booking service failure
    await page.route('**/api/bookings/**', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal Server Error' })
      });
    });

    // Navigate to booking page
    await page.goto('/book/trip-123');
    await page.fill('[data-testid="traveler-count"]', '2');
    await page.click('[data-testid="book-now-button"]');

    // Verify error handling and fallback options
    await expect(page.locator('[data-testid="booking-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();
    await expect(page.locator('[data-testid="save-for-later"]')).toBeVisible();
  });

  test('handles payment gateway timeout', async () => {
    // Mock payment gateway timeout
    await page.route('**/api/payments/**', route => {
      // Simulate timeout by not responding
      setTimeout(() => {
        route.fulfill({
          status: 408,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Request Timeout' })
        });
      }, 30000);
    });

    // Proceed to payment
    await page.goto('/checkout');
    await page.fill('[data-testid="card-number"]', '4111111111111111');
    await page.fill('[data-testid="expiry"]', '12/25');
    await page.fill('[data-testid="cvv"]', '123');
    await page.click('[data-testid="pay-button"]');

    // Verify timeout handling
    await expect(page.locator('[data-testid="payment-timeout"]')).toBeVisible({ timeout: 35000 });
    await expect(page.locator('[data-testid="retry-payment"]')).toBeVisible();
    await expect(page.locator('[data-testid="alternative-payment"]')).toBeVisible();
  });

  test('handles search service degradation', async () => {
    // Mock search service returning partial results
    await page.route('**/api/search/**', route => {
      route.fulfill({
        status: 206, // Partial Content
        contentType: 'application/json',
        body: JSON.stringify({
          results: [],
          partial: true,
          message: 'Search service is experiencing issues'
        })
      });
    });

    // Perform search
    await page.fill('[data-testid="search-input"]', 'Paris');
    await page.click('[data-testid="search-button"]');

    // Verify degraded service handling
    await expect(page.locator('[data-testid="partial-results-warning"]')).toBeVisible();
    await expect(page.locator('[data-testid="try-again-later"]')).toBeVisible();
  });
});

// Network Interruption Handling
test.describe('Chaos Testing - Network Interruptions', () => {
  test('handles network disconnection during form submission', async ({ page }) => {
    await page.goto('/contact');
    
    // Fill form
    await page.fill('[data-testid="name-input"]', 'John Doe');
    await page.fill('[data-testid="email-input"]', 'john@example.com');
    await page.fill('[data-testid="message-input"]', 'Test message');
    
    // Simulate network disconnection
    await page.context().setOffline(true);
    
    // Attempt form submission
    await page.click('[data-testid="submit-button"]');
    
    // Verify offline handling
    await expect(page.locator('[data-testid="offline-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="save-draft"]')).toBeVisible();
    
    // Restore connection and verify auto-retry
    await page.context().setOffline(false);
    await expect(page.locator('[data-testid="connection-restored"]')).toBeVisible();
  });

  test('handles intermittent connectivity', async ({ page }) => {
    await page.goto('/dashboard');
    
    // Simulate intermittent connectivity
    for (let i = 0; i < 5; i++) {
      await page.context().setOffline(true);
      await page.waitForTimeout(2000);
      await page.context().setOffline(false);
      await page.waitForTimeout(1000);
    }
    
    // Verify app remains functional
    await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
    await expect(page.locator('[data-testid="sync-status"]')).toContainText('Synced');
  });

  test('handles slow network conditions', async ({ page }) => {
    // Simulate slow network
    await page.route('**/*', route => {
      setTimeout(() => {
        route.continue();
      }, 5000); // 5 second delay
    });
    
    await page.goto('/trips');
    
    // Verify loading states and timeouts
    await expect(page.locator('[data-testid="loading-spinner"]')).toBeVisible();
    await expect(page.locator('[data-testid="slow-connection-warning"]')).toBeVisible({ timeout: 10000 });
  });
});

// Idempotency Checks
test.describe('Chaos Testing - Idempotency', () => {
  test('booking creation is idempotent', async ({ page }) => {
    let bookingRequests = 0;
    
    // Intercept and count booking requests
    await page.route('**/api/bookings', route => {
      bookingRequests++;
      if (bookingRequests === 1) {
        // First request succeeds
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ id: 'booking-123', status: 'confirmed' })
        });
      } else {
        // Subsequent requests return existing booking
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ id: 'booking-123', status: 'confirmed' })
        });
      }
    });
    
    await page.goto('/book/trip-456');
    
    // Rapidly click book button multiple times
    for (let i = 0; i < 5; i++) {
      await page.click('[data-testid="book-now-button"]');
      await page.waitForTimeout(100);
    }
    
    // Verify only one booking was created
    await expect(page.locator('[data-testid="booking-confirmation"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="booking-id"]')).toContainText('booking-123');
  });

  test('payment processing is idempotent', async ({ page }) => {
    let paymentRequests = 0;
    
    await page.route('**/api/payments', route => {
      paymentRequests++;
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ 
          id: 'payment-789', 
          status: 'completed',
          idempotencyKey: 'key-123'
        })
      });
    });
    
    await page.goto('/checkout');
    await page.fill('[data-testid="card-number"]', '4111111111111111');
    
    // Submit payment multiple times
    for (let i = 0; i < 3; i++) {
      await page.click('[data-testid="pay-button"]');
      await page.waitForTimeout(500);
    }
    
    // Verify single payment processed
    await expect(page.locator('[data-testid="payment-success"]')).toHaveCount(1);
  });

  test('user registration is idempotent', async ({ page }) => {
    await page.route('**/api/auth/register', route => {
      route.fulfill({
        status: 409, // Conflict - user already exists
        contentType: 'application/json',
        body: JSON.stringify({ error: 'User already exists' })
      });
    });
    
    await page.goto('/register');
    await page.fill('[data-testid="email-input"]', 'existing@example.com');
    await page.fill('[data-testid="password-input"]', 'password123');
    await page.click('[data-testid="register-button"]');
    
    // Verify graceful handling of duplicate registration
    await expect(page.locator('[data-testid="user-exists-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-instead"]')).toBeVisible();
  });
});

// Database Connection Failures
test.describe('Chaos Testing - Database Failures', () => {
  test('handles database connection timeout', async ({ page }) => {
    await page.route('**/api/**', route => {
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Database connection timeout' })
      });
    });
    
    await page.goto('/profile');
    
    // Verify fallback to cached data
    await expect(page.locator('[data-testid="cached-data-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="retry-connection"]')).toBeVisible();
  });

  test('handles read replica failure', async ({ page }) => {
    await page.route('**/api/search/**', route => {
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Read replica unavailable' })
      });
    });
    
    await page.goto('/search');
    await page.fill('[data-testid="search-input"]', 'Tokyo');
    await page.click('[data-testid="search-button"]');
    
    // Verify fallback to primary database
    await expect(page.locator('[data-testid="search-degraded"]')).toBeVisible();
  });
});

// Third-party Service Failures
test.describe('Chaos Testing - Third-party Services', () => {
  test('handles maps service failure', async ({ page }) => {
    // Mock maps API failure
    await page.addInitScript(() => {
      window.google = {
        maps: {
          Map: function() {
            throw new Error('Maps service unavailable');
          }
        }
      };
    });
    
    await page.goto('/trip/123');
    
    // Verify fallback to static map or text directions
    await expect(page.locator('[data-testid="map-unavailable"]')).toBeVisible();
    await expect(page.locator('[data-testid="text-directions"]')).toBeVisible();
  });

  test('handles weather service failure', async ({ page }) => {
    await page.route('**/api/weather/**', route => {
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Weather service unavailable' })
      });
    });
    
    await page.goto('/destination/paris');
    
    // Verify graceful degradation
    await expect(page.locator('[data-testid="weather-unavailable"]')).toBeVisible();
    await expect(page.locator('[data-testid="destination-info"]')).toBeVisible();
  });

  test('handles email service failure', async ({ page }) => {
    await page.route('**/api/notifications/email', route => {
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Email service unavailable' })
      });
    });
    
    await page.goto('/contact');
    await page.fill('[data-testid="email-input"]', 'test@example.com');
    await page.fill('[data-testid="message-input"]', 'Test message');
    await page.click('[data-testid="send-button"]');
    
    // Verify fallback notification method
    await expect(page.locator('[data-testid="email-failed-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="alternative-contact"]')).toBeVisible();
  });
});

// Resource Exhaustion
test.describe('Chaos Testing - Resource Exhaustion', () => {
  test('handles memory pressure gracefully', async ({ page }) => {
    // Simulate memory pressure by creating large objects
    await page.addInitScript(() => {
      const largeArray = new Array(1000000).fill('memory-pressure-test');
      window.testData = largeArray;
    });
    
    await page.goto('/dashboard');
    
    // Verify app remains responsive
    await expect(page.locator('[data-testid="dashboard-content"]')).toBeVisible();
    
    // Check for memory warnings
    const memoryWarning = page.locator('[data-testid="memory-warning"]');
    if (await memoryWarning.isVisible()) {
      await expect(memoryWarning).toContainText('High memory usage detected');
    }
  });

  test('handles rate limiting', async ({ page }) => {
    await page.route('**/api/**', route => {
      route.fulfill({
        status: 429,
        contentType: 'application/json',
        headers: {
          'Retry-After': '60'
        },
        body: JSON.stringify({ error: 'Rate limit exceeded' })
      });
    });
    
    await page.goto('/search');
    await page.fill('[data-testid="search-input"]', 'London');
    await page.click('[data-testid="search-button"]');
    
    // Verify rate limiting handling
    await expect(page.locator('[data-testid="rate-limit-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="retry-after"]')).toContainText('60 seconds');
  });
});

// Recovery Testing
test.describe('Chaos Testing - Recovery Scenarios', () => {
  test('recovers from service outage', async ({ page }) => {
    let requestCount = 0;
    
    await page.route('**/api/trips', route => {
      requestCount++;
      if (requestCount <= 3) {
        // First 3 requests fail
        route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Service Unavailable' })
        });
      } else {
        // Service recovers
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ trips: [{ id: 1, name: 'Paris Trip' }] })
        });
      }
    });
    
    await page.goto('/trips');
    
    // Verify initial failure
    await expect(page.locator('[data-testid="service-error"]')).toBeVisible();
    
    // Trigger retry
    await page.click('[data-testid="retry-button"]');
    await page.waitForTimeout(1000);
    await page.click('[data-testid="retry-button"]');
    await page.waitForTimeout(1000);
    await page.click('[data-testid="retry-button"]');
    
    // Verify recovery
    await expect(page.locator('[data-testid="trips-list"]')).toBeVisible();
    await expect(page.locator('[data-testid="trip-item"]')).toContainText('Paris Trip');
  });
});
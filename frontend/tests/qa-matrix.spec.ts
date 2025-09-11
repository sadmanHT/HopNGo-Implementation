import { test, expect, devices } from '@playwright/test';

// QA Testing Matrix - Comprehensive cross-browser and device testing

const TEST_SCENARIOS = {
  CRITICAL_PATHS: [
    { path: '/', name: 'Home Page' },
    { path: '/en/itinerary', name: 'Itinerary Planning' },
    { path: '/en/bookings', name: 'Booking System' },
    { path: '/en/tickets', name: 'Ticket Management' },
  ],
  
  FORM_INTERACTIONS: [
    'search-form',
    'booking-form',
    'contact-form',
    'login-form',
  ],
  
  RESPONSIVE_BREAKPOINTS: [
    { width: 320, height: 568, name: 'Mobile Small' },
    { width: 375, height: 667, name: 'Mobile Medium' },
    { width: 768, height: 1024, name: 'Tablet' },
    { width: 1024, height: 768, name: 'Desktop Small' },
    { width: 1440, height: 900, name: 'Desktop Large' },
  ],
};

// Cross-browser compatibility tests
test.describe('Cross-Browser Compatibility', () => {
  TEST_SCENARIOS.CRITICAL_PATHS.forEach(({ path, name }) => {
    test(`${name} loads correctly across browsers`, async ({ page, browserName }) => {
      await page.goto(path);
      
      // Check page loads without errors
      await expect(page).toHaveTitle(/HopNGo/);
      
      // Check for JavaScript errors
      const errors: string[] = [];
      page.on('pageerror', (error) => {
        errors.push(error.message);
      });
      
      // Wait for page to be fully loaded
      await page.waitForLoadState('networkidle');
      
      // Verify no JavaScript errors
      expect(errors).toHaveLength(0);
      
      // Check essential elements are visible
      await expect(page.locator('header')).toBeVisible();
      await expect(page.locator('main')).toBeVisible();
      
      // Browser-specific checks
      if (browserName === 'webkit') {
        // Safari-specific tests
        await expect(page.locator('[data-testid="safari-compatible"]')).toBeVisible({ timeout: 10000 });
      }
      
      if (browserName === 'firefox') {
        // Firefox-specific tests
        await page.waitForFunction(() => {
          return window.getComputedStyle(document.body).fontFamily !== '';
        });
      }
    });
  });
});

// Responsive design tests
test.describe('Responsive Design', () => {
  TEST_SCENARIOS.RESPONSIVE_BREAKPOINTS.forEach(({ width, height, name }) => {
    test(`Layout adapts correctly on ${name} (${width}x${height})`, async ({ page }) => {
      await page.setViewportSize({ width, height });
      await page.goto('/');
      
      // Check mobile menu behavior
      if (width < 768) {
        await expect(page.locator('[data-testid="mobile-menu-button"]')).toBeVisible();
        await expect(page.locator('[data-testid="desktop-sidebar"]')).toBeHidden();
      } else {
        await expect(page.locator('[data-testid="desktop-sidebar"]')).toBeVisible();
      }
      
      // Check text readability
      const bodyText = page.locator('body');
      const fontSize = await bodyText.evaluate((el) => {
        return window.getComputedStyle(el).fontSize;
      });
      
      // Ensure minimum font size for readability
      const fontSizeNum = parseInt(fontSize.replace('px', ''));
      expect(fontSizeNum).toBeGreaterThanOrEqual(14);
      
      // Check touch targets on mobile
      if (width < 768) {
        const buttons = page.locator('button, a[role="button"]');
        const buttonCount = await buttons.count();
        
        for (let i = 0; i < Math.min(buttonCount, 5); i++) {
          const button = buttons.nth(i);
          const box = await button.boundingBox();
          if (box) {
            // Touch targets should be at least 44px
            expect(Math.min(box.width, box.height)).toBeGreaterThanOrEqual(44);
          }
        }
      }
    });
  });
});

// Network condition tests
test.describe('Network Conditions', () => {
  test('App works on slow 3G connection', async ({ page, context }) => {
    // Simulate slow 3G
    await context.route('**/*', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 500)); // Add delay
      await route.continue();
    });
    
    await page.goto('/');
    
    // Check loading states are shown
    await expect(page.locator('[data-testid="loading-spinner"]')).toBeVisible({ timeout: 1000 });
    
    // Wait for content to load
    await page.waitForLoadState('networkidle', { timeout: 30000 });
    
    // Verify content is displayed
    await expect(page.locator('main')).toBeVisible();
  });
  
  test('App handles offline state gracefully', async ({ page, context }) => {
    await page.goto('/');
    
    // Go offline
    await context.setOffline(true);
    
    // Try to navigate
    await page.click('a[href="/en/itinerary"]');
    
    // Should show offline message or cached content
    await expect(
      page.locator('text=offline').or(page.locator('[data-testid="cached-content"]'))
    ).toBeVisible({ timeout: 5000 });
  });
});

// Form interaction tests
test.describe('Form Interactions', () => {
  test('Search form works across different input methods', async ({ page }) => {
    await page.goto('/');
    
    const searchInput = page.locator('[data-testid="search-input"]');
    await expect(searchInput).toBeVisible();
    
    // Test keyboard input
    await searchInput.fill('Paris');
    await expect(searchInput).toHaveValue('Paris');
    
    // Test form submission
    await page.keyboard.press('Enter');
    
    // Should navigate or show results
    await page.waitForURL(/search|results/, { timeout: 10000 });
  });
  
  test('Forms validate input correctly', async ({ page }) => {
    await page.goto('/en/contact');
    
    // Try to submit empty form
    await page.click('[type="submit"]');
    
    // Should show validation errors
    await expect(page.locator('.error, [aria-invalid="true"]')).toBeVisible();
    
    // Fill required fields
    await page.fill('[name="email"]', 'test@example.com');
    await page.fill('[name="message"]', 'Test message');
    
    // Submit should work now
    await page.click('[type="submit"]');
    
    // Should show success message or redirect
    await expect(
      page.locator('text=success').or(page.locator('[data-testid="success-message"]'))
    ).toBeVisible({ timeout: 10000 });
  });
});

// Performance tests
test.describe('Performance', () => {
  test('Page load performance meets thresholds', async ({ page }) => {
    const startTime = Date.now();
    
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    const loadTime = Date.now() - startTime;
    
    // Should load within 3 seconds
    expect(loadTime).toBeLessThan(3000);
    
    // Check Core Web Vitals
    const metrics = await page.evaluate(() => {
      return new Promise((resolve) => {
        new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const lcp = entries.find(entry => entry.entryType === 'largest-contentful-paint');
          const fid = entries.find(entry => entry.entryType === 'first-input');
          const cls = entries.find(entry => entry.entryType === 'layout-shift');
          
          resolve({
            lcp: lcp?.startTime || 0,
            fid: fid?.processingStart - fid?.startTime || 0,
            cls: cls?.value || 0,
          });
        }).observe({ entryTypes: ['largest-contentful-paint', 'first-input', 'layout-shift'] });
        
        // Fallback timeout
        setTimeout(() => resolve({ lcp: 0, fid: 0, cls: 0 }), 5000);
      });
    });
    
    // Core Web Vitals thresholds
    if (metrics.lcp > 0) expect(metrics.lcp).toBeLessThan(2500);
    if (metrics.fid > 0) expect(metrics.fid).toBeLessThan(100);
    if (metrics.cls > 0) expect(metrics.cls).toBeLessThan(0.1);
  });
});

// Accessibility tests
test.describe('Accessibility', () => {
  test('Keyboard navigation works correctly', async ({ page }) => {
    await page.goto('/');
    
    // Test tab navigation
    await page.keyboard.press('Tab');
    
    // Should focus on first interactive element
    const focusedElement = await page.locator(':focus');
    await expect(focusedElement).toBeVisible();
    
    // Test skip links
    await page.keyboard.press('Tab');
    const skipLink = page.locator('[href="#main-content"]');
    if (await skipLink.isVisible()) {
      await page.keyboard.press('Enter');
      
      // Should focus main content
      const mainContent = page.locator('#main-content');
      await expect(mainContent).toBeFocused();
    }
  });
  
  test('Screen reader compatibility', async ({ page }) => {
    await page.goto('/');
    
    // Check for proper heading structure
    const h1 = page.locator('h1');
    await expect(h1).toBeVisible();
    
    // Check for alt text on images
    const images = page.locator('img');
    const imageCount = await images.count();
    
    for (let i = 0; i < imageCount; i++) {
      const img = images.nth(i);
      const alt = await img.getAttribute('alt');
      expect(alt).toBeTruthy();
    }
    
    // Check for proper form labels
    const inputs = page.locator('input[type="text"], input[type="email"], textarea');
    const inputCount = await inputs.count();
    
    for (let i = 0; i < inputCount; i++) {
      const input = inputs.nth(i);
      const hasLabel = await input.evaluate((el) => {
        const id = el.id;
        const ariaLabel = el.getAttribute('aria-label');
        const ariaLabelledBy = el.getAttribute('aria-labelledby');
        const label = id ? document.querySelector(`label[for="${id}"]`) : null;
        
        return !!(ariaLabel || ariaLabelledBy || label);
      });
      
      expect(hasLabel).toBeTruthy();
    }
  });
});
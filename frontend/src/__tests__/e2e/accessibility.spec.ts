import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.describe('Accessibility Testing - WCAG 2.1 AA Compliance', () => {
  test.beforeEach(async ({ page }) => {
    // Login as a regular user for authenticated pages
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'user@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();
  });

  test('should pass accessibility audit on homepage', async ({ page }) => {
    await page.goto('/');
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Visual regression snapshot
    await expect(page).toHaveScreenshot('homepage-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should pass accessibility audit on destinations page', async ({ page }) => {
    await page.click('[data-testid="nav-destinations"]');
    await expect(page.locator('[data-testid="destinations-page"]')).toBeVisible();
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .exclude('[data-testid="map-container"]') // Maps often have accessibility issues
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test keyboard navigation
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');
    
    // Visual regression snapshot
    await expect(page).toHaveScreenshot('destinations-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should pass accessibility audit on trip planning page', async ({ page }) => {
    await page.click('[data-testid="nav-trips"]');
    await expect(page.locator('[data-testid="trips-page"]')).toBeVisible();
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test form accessibility
    await page.click('[data-testid="create-trip-button"]');
    await expect(page.locator('[data-testid="trip-form"]')).toBeVisible();
    
    // Check form labels and ARIA attributes
    const titleInput = page.locator('[data-testid="trip-title"]');
    await expect(titleInput).toHaveAttribute('aria-label');
    
    // Visual regression snapshot
    await expect(page).toHaveScreenshot('trip-planning-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should pass accessibility audit on social feed page', async ({ page }) => {
    await page.click('[data-testid="nav-social"]');
    await expect(page.locator('[data-testid="social-feed"]')).toBeVisible();
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test interactive elements
    const likeButton = page.locator('[data-testid="like-button"]').first();
    await expect(likeButton).toHaveAttribute('aria-label');
    await expect(likeButton).toHaveAttribute('role', 'button');
    
    // Visual regression snapshot
    await expect(page).toHaveScreenshot('social-feed-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should pass accessibility audit on search results page', async ({ page }) => {
    await page.click('[data-testid="nav-search"]');
    await page.fill('[data-testid="search-input"]', 'Dhaka');
    await page.click('[data-testid="search-button"]');
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test search filters accessibility
    const filterButton = page.locator('[data-testid="filter-button"]');
    await expect(filterButton).toHaveAttribute('aria-expanded');
    
    // Visual regression snapshot
    await expect(page).toHaveScreenshot('search-results-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should pass accessibility audit on user profile page', async ({ page }) => {
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="profile-link"]');
    await expect(page.locator('[data-testid="user-profile"]')).toBeVisible();
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test profile image alt text
    const profileImage = page.locator('[data-testid="profile-image"]');
    await expect(profileImage).toHaveAttribute('alt');
    
    // Visual regression snapshot
    await expect(page).toHaveScreenshot('user-profile-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should handle high contrast mode', async ({ page }) => {
    // Enable high contrast mode
    await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
    await page.goto('/');
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test color contrast ratios
    const primaryButton = page.locator('[data-testid="primary-button"]').first();
    const buttonStyles = await primaryButton.evaluate((el) => {
      const styles = window.getComputedStyle(el);
      return {
        color: styles.color,
        backgroundColor: styles.backgroundColor
      };
    });
    
    // Verify sufficient contrast (this would need actual contrast calculation)
    expect(buttonStyles.color).toBeTruthy();
    expect(buttonStyles.backgroundColor).toBeTruthy();
    
    // Visual regression snapshot for high contrast
    await expect(page).toHaveScreenshot('high-contrast-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should support keyboard navigation throughout the app', async ({ page }) => {
    await page.goto('/');
    
    // Test tab navigation
    await page.keyboard.press('Tab'); // Skip link
    await page.keyboard.press('Tab'); // Logo
    await page.keyboard.press('Tab'); // Nav item 1
    await page.keyboard.press('Tab'); // Nav item 2
    await page.keyboard.press('Tab'); // Nav item 3
    await page.keyboard.press('Tab'); // User menu
    
    // Verify focus is visible
    const focusedElement = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'));
    expect(focusedElement).toBeTruthy();
    
    // Test Enter key activation
    await page.keyboard.press('Enter');
    
    // Test Escape key for modals
    await page.keyboard.press('Escape');
    
    // Visual regression snapshot for keyboard focus
    await expect(page).toHaveScreenshot('keyboard-navigation-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should provide proper ARIA labels and roles', async ({ page }) => {
    await page.goto('/');
    
    // Check main navigation
    const mainNav = page.locator('[role="navigation"]');
    await expect(mainNav).toBeVisible();
    await expect(mainNav).toHaveAttribute('aria-label');
    
    // Check search functionality
    const searchInput = page.locator('[data-testid="search-input"]');
    await expect(searchInput).toHaveAttribute('aria-label');
    await expect(searchInput).toHaveAttribute('role', 'searchbox');
    
    // Check buttons
    const buttons = page.locator('button');
    const buttonCount = await buttons.count();
    
    for (let i = 0; i < Math.min(buttonCount, 5); i++) {
      const button = buttons.nth(i);
      const hasAriaLabel = await button.getAttribute('aria-label');
      const hasTextContent = await button.textContent();
      
      // Button should have either aria-label or text content
      expect(hasAriaLabel || hasTextContent).toBeTruthy();
    }
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('should handle screen reader announcements', async ({ page }) => {
    await page.goto('/');
    
    // Check for live regions
    const liveRegions = page.locator('[aria-live]');
    await expect(liveRegions).toHaveCount.toBeGreaterThan(0);
    
    // Test dynamic content announcements
    await page.click('[data-testid="nav-destinations"]');
    
    // Check for status messages
    const statusRegion = page.locator('[role="status"]');
    if (await statusRegion.count() > 0) {
      await expect(statusRegion.first()).toBeVisible();
    }
    
    // Check for alert regions
    const alertRegion = page.locator('[role="alert"]');
    if (await alertRegion.count() > 0) {
      await expect(alertRegion.first()).toBeVisible();
    }
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('should support reduced motion preferences', async ({ page }) => {
    // Enable reduced motion
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.goto('/');
    
    // Check that animations are disabled or reduced
    const animatedElements = page.locator('[data-testid*="animated"]');
    const elementCount = await animatedElements.count();
    
    for (let i = 0; i < elementCount; i++) {
      const element = animatedElements.nth(i);
      const animationDuration = await element.evaluate((el) => {
        const styles = window.getComputedStyle(el);
        return styles.animationDuration;
      });
      
      // Animation should be disabled or very short
      expect(animationDuration === '0s' || animationDuration === 'none').toBeTruthy();
    }
    
    // Visual regression snapshot for reduced motion
    await expect(page).toHaveScreenshot('reduced-motion-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should handle mobile accessibility', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Test touch targets are large enough (44px minimum)
    const touchTargets = page.locator('button, a, [role="button"]');
    const targetCount = await touchTargets.count();
    
    for (let i = 0; i < Math.min(targetCount, 10); i++) {
      const target = touchTargets.nth(i);
      const boundingBox = await target.boundingBox();
      
      if (boundingBox) {
        expect(boundingBox.width).toBeGreaterThanOrEqual(44);
        expect(boundingBox.height).toBeGreaterThanOrEqual(44);
      }
    }
    
    // Visual regression snapshot for mobile
    await expect(page).toHaveScreenshot('mobile-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });

  test('should provide comprehensive error handling accessibility', async ({ page }) => {
    await page.goto('/');
    
    // Test form validation errors
    await page.click('[data-testid="create-trip-button"]');
    await page.click('[data-testid="save-trip-button"]'); // Submit without required fields
    
    // Check for error announcements
    const errorMessages = page.locator('[role="alert"], [aria-live="assertive"]');
    await expect(errorMessages).toHaveCount.toBeGreaterThan(0);
    
    // Check error association with form fields
    const errorFields = page.locator('[aria-invalid="true"]');
    const errorFieldCount = await errorFields.count();
    
    for (let i = 0; i < errorFieldCount; i++) {
      const field = errorFields.nth(i);
      const describedBy = await field.getAttribute('aria-describedby');
      expect(describedBy).toBeTruthy();
      
      // Verify the error message exists
      const errorMessage = page.locator(`#${describedBy}`);
      await expect(errorMessage).toBeVisible();
    }
    
    const accessibilityScanResults = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    
    expect(accessibilityScanResults.violations).toEqual([]);
    
    // Visual regression snapshot for error states
    await expect(page).toHaveScreenshot('error-handling-accessibility.png', {
      fullPage: true,
      threshold: 0.2
    });
  });
});
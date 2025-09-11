import { test, expect } from '@playwright/test';
import { createAccessibilityTester, runAccessibilityTestSuite } from '../utils/accessibility';

test.describe('Accessibility Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Set up accessibility testing
    await page.goto('/');
  });

  test('Homepage accessibility compliance', async ({ page }) => {
    const results = await runAccessibilityTestSuite(page, 'Homepage');
    
    // Verify no critical accessibility violations
    expect(results.keyboard.trapIssues).toHaveLength(0);
    expect(results.images.missingAlt).toHaveLength(0);
    expect(results.forms.unlabeledInputs).toHaveLength(0);
  });

  test('Navigation accessibility', async ({ page }) => {
    const tester = createAccessibilityTester(page);
    await tester.initialize();

    // Test main navigation
    await tester.checkElement('nav');
    
    // Test keyboard navigation through menu items
    const keyboardTest = await tester.testKeyboardNavigation();
    expect(keyboardTest.focusableElements).toBeGreaterThan(0);
    expect(keyboardTest.trapIssues).toHaveLength(0);
  });

  test('Search functionality accessibility', async ({ page }) => {
    // Navigate to search page or trigger search
    await page.goto('/search');
    
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Check search form accessibility
    const formTest = await tester.checkFormAccessibility();
    expect(formTest.unlabeledInputs).toHaveLength(0);
    
    // Test search input keyboard navigation
    await page.keyboard.press('Tab');
    const searchInput = page.locator('input[type="search"], input[placeholder*="search" i]').first();
    await expect(searchInput).toBeFocused();
  });

  test('Booking form accessibility', async ({ page }) => {
    // Navigate to booking page
    await page.goto('/booking');
    
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Run comprehensive accessibility checks
    await tester.checkAccessibility();
    
    // Check form-specific accessibility
    const formTest = await tester.checkFormAccessibility();
    expect(formTest.unlabeledInputs).toHaveLength(0);
    expect(formTest.invalidAriaLabels).toHaveLength(0);
  });

  test('User profile accessibility', async ({ page }) => {
    // Navigate to profile page (may require authentication)
    await page.goto('/profile');
    
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Check profile form accessibility
    const formTest = await tester.checkFormAccessibility();
    const imageTest = await tester.checkImageAltText();
    
    expect(formTest.unlabeledInputs).toHaveLength(0);
    expect(imageTest.missingAlt).toHaveLength(0);
  });

  test('Modal dialogs accessibility', async ({ page }) => {
    const tester = createAccessibilityTester(page);
    await tester.initialize();

    // Look for modal triggers
    const modalTrigger = page.locator('button:has-text("Login"), button:has-text("Sign up"), button:has-text("Book now")').first();
    
    if (await modalTrigger.count() > 0) {
      await modalTrigger.click();
      
      // Wait for modal to appear
      await page.waitForSelector('[role="dialog"], .modal, [aria-modal="true"]', { timeout: 5000 });
      
      // Check modal accessibility
      await tester.checkElement('[role="dialog"], .modal, [aria-modal="true"]');
      
      // Test focus management
      const focusedElement = await page.evaluate(() => document.activeElement?.tagName);
      expect(focusedElement).toBeTruthy();
      
      // Test escape key functionality
      await page.keyboard.press('Escape');
      await expect(page.locator('[role="dialog"], .modal, [aria-modal="true"]')).toHaveCount(0);
    }
  });

  test('Color contrast compliance', async ({ page }) => {
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Test color contrast on main elements
    const contrastTest = await tester.testColorContrast();
    
    // Log contrast violations for manual review
    if (contrastTest.violations.length > 0) {
      console.warn('Color contrast violations found:', contrastTest.violations);
    }
    
    // Run axe-core color contrast checks
    await tester.checkAccessibility({
      tags: ['wcag2aa']
    });
  });

  test('Skip links functionality', async ({ page }) => {
    // Test skip to content link
    await page.keyboard.press('Tab');
    
    const skipLink = page.locator('a:has-text("Skip to content"), a:has-text("Skip to main")').first();
    
    if (await skipLink.count() > 0) {
      await expect(skipLink).toBeFocused();
      await skipLink.press('Enter');
      
      // Verify focus moved to main content
      const mainContent = page.locator('main, #main-content, [role="main"]').first();
      await expect(mainContent).toBeFocused();
    }
  });

  test('ARIA landmarks and headings', async ({ page }) => {
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Check for proper landmark structure
    const landmarks = await page.evaluate(() => {
      const landmarks = {
        main: document.querySelectorAll('main, [role="main"]').length,
        navigation: document.querySelectorAll('nav, [role="navigation"]').length,
        banner: document.querySelectorAll('header, [role="banner"]').length,
        contentinfo: document.querySelectorAll('footer, [role="contentinfo"]').length,
        complementary: document.querySelectorAll('aside, [role="complementary"]').length
      };
      return landmarks;
    });
    
    expect(landmarks.main).toBeGreaterThanOrEqual(1);
    expect(landmarks.navigation).toBeGreaterThanOrEqual(1);
    
    // Check heading hierarchy
    const headings = await page.evaluate(() => {
      const headings = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6'));
      return headings.map(h => ({
        level: parseInt(h.tagName.charAt(1)),
        text: h.textContent?.trim() || ''
      }));
    });
    
    expect(headings.filter(h => h.level === 1)).toHaveLength(1); // Should have exactly one h1
    
    // Check heading hierarchy (no skipping levels)
    for (let i = 1; i < headings.length; i++) {
      const current = headings[i];
      const previous = headings[i - 1];
      
      if (current.level > previous.level) {
        expect(current.level - previous.level).toBeLessThanOrEqual(1);
      }
    }
  });

  test('Form validation accessibility', async ({ page }) => {
    // Navigate to a form page
    await page.goto('/contact');
    
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Find form inputs
    const inputs = page.locator('input[required], textarea[required], select[required]');
    const inputCount = await inputs.count();
    
    if (inputCount > 0) {
      // Try to submit form without filling required fields
      const submitButton = page.locator('button[type="submit"], input[type="submit"]').first();
      
      if (await submitButton.count() > 0) {
        await submitButton.click();
        
        // Check for accessible error messages
        const errorMessages = page.locator('[role="alert"], .error, [aria-live="polite"], [aria-live="assertive"]');
        
        if (await errorMessages.count() > 0) {
          // Verify error messages are properly associated with inputs
          const firstInput = inputs.first();
          const ariaDescribedBy = await firstInput.getAttribute('aria-describedby');
          const ariaInvalid = await firstInput.getAttribute('aria-invalid');
          
          expect(ariaInvalid).toBe('true');
          if (ariaDescribedBy) {
            const errorElement = page.locator(`#${ariaDescribedBy}`);
            await expect(errorElement).toBeVisible();
          }
        }
      }
    }
  });

  test('Mobile accessibility', async ({ page, isMobile }) => {
    if (!isMobile) {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 });
    }
    
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Run accessibility checks on mobile
    await tester.checkAccessibility();
    
    // Test touch targets (minimum 44px)
    const touchTargets = await page.evaluate(() => {
      const interactive = document.querySelectorAll('button, a, input, select, textarea, [role="button"], [tabindex="0"]');
      const smallTargets: string[] = [];
      
      interactive.forEach((el, index) => {
        const rect = el.getBoundingClientRect();
        if (rect.width < 44 || rect.height < 44) {
          const selector = el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + (el.className ? '.' + Array.from(el.classList).join('.') : '');
          smallTargets.push(`${selector} (${Math.round(rect.width)}x${Math.round(rect.height)})`);
        }
      });
      
      return smallTargets;
    });
    
    if (touchTargets.length > 0) {
      console.warn('Small touch targets found:', touchTargets);
    }
  });

  test('Screen reader compatibility', async ({ page }) => {
    const tester = createAccessibilityTester(page);
    await tester.initialize();
    
    // Check for screen reader specific attributes
    const screenReaderElements = await page.evaluate(() => {
      const elements = {
        ariaLabels: document.querySelectorAll('[aria-label]').length,
        ariaDescriptions: document.querySelectorAll('[aria-describedby]').length,
        ariaLabelledBy: document.querySelectorAll('[aria-labelledby]').length,
        srOnlyText: document.querySelectorAll('.sr-only, .screen-reader-only, .visually-hidden').length,
        ariaHidden: document.querySelectorAll('[aria-hidden="true"]').length
      };
      return elements;
    });
    
    // Verify presence of screen reader enhancements
    expect(screenReaderElements.ariaLabels + screenReaderElements.ariaLabelledBy).toBeGreaterThan(0);
    
    // Run comprehensive accessibility check
    await tester.checkAccessibility({
      tags: ['wcag2a', 'wcag2aa', 'section508']
    });
  });
});

// Test configuration for different browsers and devices
test.describe('Cross-browser Accessibility', () => {
  ['chromium', 'firefox', 'webkit'].forEach(browserName => {
    test(`${browserName} accessibility compliance`, async ({ page, browserName: currentBrowser }) => {
      test.skip(currentBrowser !== browserName, `Skipping ${browserName} test`);
      
      const tester = createAccessibilityTester(page);
      await tester.initialize();
      
      // Run basic accessibility checks
      await tester.checkAccessibility();
      
      // Browser-specific checks
      if (browserName === 'webkit') {
        // Safari-specific accessibility features
        const voiceOverSupport = await page.evaluate(() => {
          return 'speechSynthesis' in window;
        });
        expect(voiceOverSupport).toBe(true);
      }
    });
  });
});
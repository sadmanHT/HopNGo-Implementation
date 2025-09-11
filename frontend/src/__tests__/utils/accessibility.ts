import { Page } from '@playwright/test';
import { injectAxe, checkA11y, getViolations, configureAxe } from '@axe-core/playwright';

/**
 * Accessibility testing utilities for Playwright tests
 */
export class AccessibilityTester {
  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Initialize axe-core on the page
   */
  async initialize(): Promise<void> {
    await injectAxe(this.page);
    
    // Configure axe with WCAG AA standards
    await configureAxe(this.page, {
      rules: {
        // Enable WCAG 2.1 AA rules
        'color-contrast': { enabled: true },
        'keyboard-navigation': { enabled: true },
        'focus-order-semantics': { enabled: true },
        'aria-required-attr': { enabled: true },
        'aria-valid-attr-value': { enabled: true },
        'button-name': { enabled: true },
        'form-field-multiple-labels': { enabled: true },
        'html-has-lang': { enabled: true },
        'image-alt': { enabled: true },
        'input-image-alt': { enabled: true },
        'label': { enabled: true },
        'link-name': { enabled: true },
        'page-has-heading-one': { enabled: true },
        'region': { enabled: true },
        'skip-link': { enabled: true },
        'tabindex': { enabled: true },
        'valid-lang': { enabled: true }
      },
      tags: ['wcag2a', 'wcag2aa', 'wcag21aa']
    });
  }

  /**
   * Run accessibility checks on the current page
   */
  async checkAccessibility(options?: {
    include?: string[];
    exclude?: string[];
    tags?: string[];
  }): Promise<void> {
    await checkA11y(this.page, undefined, {
      detailedReport: true,
      detailedReportOptions: {
        html: true
      },
      ...options
    });
  }

  /**
   * Get accessibility violations without throwing
   */
  async getViolations(options?: {
    include?: string[];
    exclude?: string[];
    tags?: string[];
  }) {
    return await getViolations(this.page, undefined, {
      ...options
    });
  }

  /**
   * Check specific element for accessibility issues
   */
  async checkElement(selector: string): Promise<void> {
    await checkA11y(this.page, selector, {
      detailedReport: true,
      detailedReportOptions: {
        html: true
      }
    });
  }

  /**
   * Test keyboard navigation
   */
  async testKeyboardNavigation(): Promise<{
    focusableElements: number;
    tabOrder: string[];
    trapIssues: string[];
  }> {
    const focusableElements = await this.page.evaluate(() => {
      const focusable = Array.from(document.querySelectorAll(
        'a[href], button, input, textarea, select, details, [tabindex]:not([tabindex="-1"])'
      )).filter(el => {
        const style = window.getComputedStyle(el);
        return style.display !== 'none' && style.visibility !== 'hidden' && !el.hasAttribute('disabled');
      });
      return focusable.length;
    });

    const tabOrder: string[] = [];
    const trapIssues: string[] = [];

    // Test tab navigation
    await this.page.keyboard.press('Tab');
    let currentElement = await this.page.evaluate(() => {
      const active = document.activeElement;
      return active ? active.tagName + (active.id ? '#' + active.id : '') + (active.className ? '.' + active.className.split(' ').join('.') : '') : null;
    });

    let tabCount = 0;
    const maxTabs = 50; // Prevent infinite loops

    while (currentElement && tabCount < maxTabs) {
      tabOrder.push(currentElement);
      await this.page.keyboard.press('Tab');
      
      const nextElement = await this.page.evaluate(() => {
        const active = document.activeElement;
        return active ? active.tagName + (active.id ? '#' + active.id : '') + (active.className ? '.' + active.className.split(' ').join('.') : '') : null;
      });

      if (nextElement === currentElement) {
        trapIssues.push(`Focus trap detected at: ${currentElement}`);
        break;
      }

      currentElement = nextElement;
      tabCount++;
    }

    return {
      focusableElements,
      tabOrder,
      trapIssues
    };
  }

  /**
   * Test color contrast ratios
   */
  async testColorContrast(): Promise<{
    violations: Array<{
      element: string;
      contrast: number;
      expected: number;
      color: string;
      backgroundColor: string;
    }>;
  }> {
    const violations = await this.page.evaluate(() => {
      const elements = document.querySelectorAll('*');
      const contrastViolations: Array<{
        element: string;
        contrast: number;
        expected: number;
        color: string;
        backgroundColor: string;
      }> = [];

      elements.forEach(el => {
        const style = window.getComputedStyle(el);
        const color = style.color;
        const backgroundColor = style.backgroundColor;
        
        if (color && backgroundColor && color !== 'rgba(0, 0, 0, 0)' && backgroundColor !== 'rgba(0, 0, 0, 0)') {
          // Simple contrast check (would need proper color contrast library for accurate calculation)
          const tagName = el.tagName.toLowerCase();
          const selector = tagName + (el.id ? '#' + el.id : '') + (el.className ? '.' + Array.from(el.classList).join('.') : '');
          
          // This is a simplified check - in real implementation, use proper contrast calculation
          if (color === backgroundColor) {
            contrastViolations.push({
              element: selector,
              contrast: 1,
              expected: 4.5,
              color,
              backgroundColor
            });
          }
        }
      });

      return contrastViolations;
    });

    return { violations };
  }

  /**
   * Check for missing alt text on images
   */
  async checkImageAltText(): Promise<{
    missingAlt: string[];
    emptyAlt: string[];
    decorativeImages: string[];
  }> {
    return await this.page.evaluate(() => {
      const images = document.querySelectorAll('img');
      const missingAlt: string[] = [];
      const emptyAlt: string[] = [];
      const decorativeImages: string[] = [];

      images.forEach((img, index) => {
        const src = img.src || `image-${index}`;
        
        if (!img.hasAttribute('alt')) {
          missingAlt.push(src);
        } else if (img.alt === '') {
          if (img.hasAttribute('aria-hidden') && img.getAttribute('aria-hidden') === 'true') {
            decorativeImages.push(src);
          } else {
            emptyAlt.push(src);
          }
        }
      });

      return {
        missingAlt,
        emptyAlt,
        decorativeImages
      };
    });
  }

  /**
   * Check form accessibility
   */
  async checkFormAccessibility(): Promise<{
    unlabeledInputs: string[];
    missingFieldsets: string[];
    invalidAriaLabels: string[];
  }> {
    return await this.page.evaluate(() => {
      const inputs = document.querySelectorAll('input, textarea, select');
      const unlabeledInputs: string[] = [];
      const missingFieldsets: string[] = [];
      const invalidAriaLabels: string[] = [];

      inputs.forEach((input, index) => {
        const id = input.id || `input-${index}`;
        
        // Check for labels
        const hasLabel = document.querySelector(`label[for="${input.id}"]`) || 
                        input.closest('label') ||
                        input.hasAttribute('aria-label') ||
                        input.hasAttribute('aria-labelledby');
        
        if (!hasLabel) {
          unlabeledInputs.push(id);
        }

        // Check aria-labelledby references
        const ariaLabelledBy = input.getAttribute('aria-labelledby');
        if (ariaLabelledBy) {
          const referencedElement = document.getElementById(ariaLabelledBy);
          if (!referencedElement) {
            invalidAriaLabels.push(`${id} references non-existent element: ${ariaLabelledBy}`);
          }
        }
      });

      // Check for fieldsets in forms with multiple related inputs
      const forms = document.querySelectorAll('form');
      forms.forEach((form, index) => {
        const radioGroups = form.querySelectorAll('input[type="radio"]');
        const checkboxGroups = form.querySelectorAll('input[type="checkbox"]');
        
        if ((radioGroups.length > 1 || checkboxGroups.length > 1) && !form.querySelector('fieldset')) {
          missingFieldsets.push(`form-${index}`);
        }
      });

      return {
        unlabeledInputs,
        missingFieldsets,
        invalidAriaLabels
      };
    });
  }
}

/**
 * Helper function to create accessibility tester
 */
export const createAccessibilityTester = (page: Page): AccessibilityTester => {
  return new AccessibilityTester(page);
};

/**
 * Common accessibility test suite
 */
export const runAccessibilityTestSuite = async (page: Page, testName: string) => {
  const tester = createAccessibilityTester(page);
  await tester.initialize();

  console.log(`Running accessibility tests for: ${testName}`);

  // Run axe-core checks
  await tester.checkAccessibility();

  // Run custom checks
  const keyboardTest = await tester.testKeyboardNavigation();
  const imageTest = await tester.checkImageAltText();
  const formTest = await tester.checkFormAccessibility();
  const contrastTest = await tester.testColorContrast();

  // Log results
  console.log('Keyboard Navigation:', keyboardTest);
  console.log('Image Alt Text:', imageTest);
  console.log('Form Accessibility:', formTest);
  console.log('Color Contrast:', contrastTest);

  return {
    keyboard: keyboardTest,
    images: imageTest,
    forms: formTest,
    contrast: contrastTest
  };
};
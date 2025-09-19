// Accessibility Testing Configuration for HopNGo
// This file configures axe-core rules and settings for WCAG 2.1 AA compliance

module.exports = {
  // Axe-core configuration
  axeConfig: {
    rules: {
      // WCAG 2.1 AA specific rules
      'color-contrast': { enabled: true },
      'color-contrast-enhanced': { enabled: false }, // AAA level, not required for AA
      'focus-order-semantics': { enabled: true },
      'hidden-content': { enabled: true },
      'keyboard-navigation': { enabled: true },
      'landmark-banner-is-top-level': { enabled: true },
      'landmark-complementary-is-top-level': { enabled: true },
      'landmark-contentinfo-is-top-level': { enabled: true },
      'landmark-main-is-top-level': { enabled: true },
      'landmark-no-duplicate-banner': { enabled: true },
      'landmark-no-duplicate-contentinfo': { enabled: true },
      'landmark-one-main': { enabled: true },
      'link-in-text-block': { enabled: true },
      'meta-refresh': { enabled: true },
      'meta-viewport': { enabled: true },
      'region': { enabled: true },
      'scope-attr-valid': { enabled: true },
      'server-side-image-map': { enabled: true },
      'skip-link': { enabled: true },
      'tabindex': { enabled: true },
      'valid-lang': { enabled: true }
    },
    tags: ['wcag2a', 'wcag2aa', 'wcag21aa'],
    locale: 'en'
  },

  // Visual regression testing configuration
  visualRegression: {
    threshold: 0.2,
    fullPage: true,
    animations: 'disabled',
    clip: null,
    mask: []
  },

  // Accessibility test scenarios
  testScenarios: {
    // Core pages to test
    pages: [
      { name: 'homepage', path: '/', authenticated: false },
      { name: 'destinations', path: '/destinations', authenticated: true },
      { name: 'trips', path: '/trips', authenticated: true },
      { name: 'social', path: '/social', authenticated: true },
      { name: 'search', path: '/search', authenticated: true },
      { name: 'profile', path: '/profile', authenticated: true }
    ],

    // Responsive breakpoints to test
    viewports: [
      { name: 'mobile', width: 375, height: 667 },
      { name: 'tablet', width: 768, height: 1024 },
      { name: 'desktop', width: 1920, height: 1080 }
    ],

    // Accessibility features to test
    features: [
      'keyboard-navigation',
      'screen-reader-support',
      'high-contrast-mode',
      'reduced-motion',
      'focus-management',
      'error-handling',
      'form-validation',
      'live-regions'
    ]
  },

  // Elements to exclude from accessibility testing
  excludeSelectors: [
    '[data-testid="map-container"]', // Maps often have accessibility issues
    '.third-party-widget',
    '[data-ignore-a11y="true"]'
  ],

  // Custom accessibility rules
  customRules: {
    // Custom rule for touch target size on mobile
    'touch-target-size': {
      enabled: true,
      minSize: 44, // 44px minimum as per WCAG guidelines
      selector: 'button, a, [role="button"], input[type="checkbox"], input[type="radio"]'
    },

    // Custom rule for focus indicators
    'focus-indicator': {
      enabled: true,
      minContrast: 3, // 3:1 contrast ratio for focus indicators
      selector: ':focus, :focus-visible'
    }
  },

  // Reporting configuration
  reporting: {
    format: ['html', 'json'],
    outputDir: './test-results/accessibility',
    includeScreenshots: true,
    includeViolationDetails: true
  }
};
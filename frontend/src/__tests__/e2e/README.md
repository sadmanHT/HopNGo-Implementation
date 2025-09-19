# HopNGo E2E Testing Suite

This directory contains comprehensive end-to-end tests for the HopNGo travel platform, including accessibility testing with WCAG 2.1 AA compliance.

## Test Structure

### Core E2E Tests

1. **Authentication Flow (`auth.spec.ts`)**
   - User registration with validation
   - Login/logout functionality
   - Two-Factor Authentication (2FA)
   - Password reset flow
   - Session management
   - Social login (OAuth)
   - Account verification
   - User role handling (USER, PROVIDER, ADMIN)

2. **Social Features (`social.spec.ts`)**
   - Post creation and management
   - Image upload and processing
   - Feed interactions (like, comment, share)
   - User following/unfollowing
   - Content moderation
   - Real-time notifications
   - Privacy settings

3. **Search Functionality (`search.spec.ts`)**
   - Basic text search
   - Advanced filters (location, price, category)
   - Sorting and display options
   - Performance monitoring
   - Cumulative Layout Shift (CLS) guards
   - Mobile responsiveness
   - Search suggestions and autocomplete

4. **Trip Planning (`trip-planning.spec.ts`)**
   - Trip creation and management
   - Itinerary building
   - Activity scheduling
   - Budget tracking
   - Collaboration features
   - Visual search functionality
   - AI-powered recommendations
   - Weather-aware planning
   - Offline access

5. **Visual Search (`trip-planning.spec.ts`)**
   - Image upload for destination discovery
   - Camera-based real-time search
   - AI-powered image analysis
   - Similar destination recommendations
   - Visual similarity matching

6. **Accessibility Testing (`accessibility.spec.ts`)**
   - WCAG 2.1 AA compliance testing
   - Keyboard navigation
   - Screen reader support
   - High contrast mode
   - Reduced motion preferences
   - Touch target sizing
   - Error handling accessibility
   - Visual regression testing

## Test Configuration

### Playwright Configuration
- **Config File**: `playwright.config.ts`
- **Test Directory**: `./src/__tests__/e2e`
- **Browsers**: Chromium, Firefox, WebKit
- **Parallel Execution**: Enabled
- **Retries**: 2 on CI, 0 locally
- **Screenshots**: On failure
- **Video**: On failure
- **Trace**: On first retry

### Accessibility Configuration
- **Config File**: `accessibility.config.js`
- **Framework**: axe-core with Playwright integration
- **Standards**: WCAG 2.1 AA compliance
- **Visual Regression**: Enabled with 0.2 threshold
- **Custom Rules**: Touch targets, focus indicators

## Running Tests

### All E2E Tests
```bash
npm run test:e2e
```

### Specific Test Files
```bash
# Authentication tests
npx playwright test auth.spec.ts

# Social features tests
npx playwright test social.spec.ts

# Search functionality tests
npx playwright test search.spec.ts

# Trip planning tests
npx playwright test trip-planning.spec.ts

# Accessibility tests
npx playwright test accessibility.spec.ts
```

### Interactive Mode
```bash
npm run test:e2e:ui
```

### Debug Mode
```bash
npm run test:e2e:debug
```

### Headed Mode (See Browser)
```bash
npm run test:e2e:headed
```

## Test Data and Fixtures

### Test Fixtures
- **Location**: `tests/fixtures/`
- **Sample Images**: `sample-travel-photo.jpg` (SVG format)
- **Test Data**: Created via global setup

### Test Users
- `user@example.com` - Regular user
- `provider@example.com` - Service provider
- `admin@example.com` - Administrator
- `friend@example.com` - Secondary user for collaboration tests

### Test Data
- Sample destinations (Sundarbans, Cox's Bazar, Ahsan Manzil)
- Test trips and itineraries
- Mock social posts and interactions

## Global Setup and Teardown

### Global Setup (`utils/global-setup.ts`)
- Creates test users
- Sets up test destinations
- Initializes test trips
- Prepares application state

### Global Teardown (`utils/global-teardown.ts`)
- Cleans up test data
- Clears browser storage
- Resets application state

## Accessibility Testing Details

### WCAG 2.1 AA Compliance
- **Color Contrast**: 4.5:1 for normal text, 3:1 for large text
- **Keyboard Navigation**: All interactive elements accessible
- **Screen Reader Support**: Proper ARIA labels and roles
- **Focus Management**: Visible focus indicators
- **Touch Targets**: Minimum 44px size on mobile

### Visual Regression Testing
- **Screenshots**: Full page captures
- **Threshold**: 0.2 (20% difference tolerance)
- **Viewports**: Mobile (375x667), Tablet (768x1024), Desktop (1920x1080)
- **Modes**: Light/dark theme, high contrast, reduced motion

### Accessibility Features Tested
- Keyboard navigation throughout the application
- Screen reader announcements and live regions
- High contrast mode support
- Reduced motion preferences
- Mobile accessibility and touch targets
- Form validation and error handling
- Modal and dialog accessibility

## Test Reports

### HTML Report
- **Location**: `playwright-report/`
- **Command**: `npx playwright show-report`

### JSON Report
- **Location**: `test-results/results.json`
- **Format**: Machine-readable test results

### JUnit Report
- **Location**: `test-results/junit.xml`
- **Format**: CI/CD integration compatible

### Accessibility Report
- **Location**: `test-results/accessibility/`
- **Format**: HTML and JSON reports
- **Content**: WCAG violations, screenshots, recommendations

## Best Practices

### Test Organization
- One test file per major feature area
- Descriptive test names following "should [action] [expected result]" pattern
- Proper use of `beforeEach` for common setup
- Clean separation of concerns

### Selectors
- Use `data-testid` attributes for reliable element selection
- Avoid CSS selectors that may change with styling updates
- Use semantic selectors when appropriate (roles, labels)

### Assertions
- Use specific assertions (`toBeVisible()`, `toHaveText()`, etc.)
- Wait for elements before asserting
- Use appropriate timeouts for different operations

### Accessibility Testing
- Test with keyboard navigation
- Verify ARIA attributes and roles
- Check color contrast ratios
- Test with different viewport sizes
- Validate form error handling

## Troubleshooting

### Common Issues

1. **Test Timeouts**
   - Increase timeout for slow operations
   - Use `waitForSelector` with appropriate timeouts
   - Check network conditions and server response times

2. **Flaky Tests**
   - Add proper waits for dynamic content
   - Use `waitForLoadState` for page transitions
   - Implement retry logic for unstable operations

3. **Accessibility Violations**
   - Review axe-core reports for specific issues
   - Check WCAG guidelines for compliance requirements
   - Test with actual assistive technologies

4. **Visual Regression Failures**
   - Review screenshot diffs in test reports
   - Adjust threshold if minor differences are acceptable
   - Update baseline screenshots when UI changes are intentional

### Debug Commands

```bash
# Run specific test with debug
npx playwright test auth.spec.ts --debug

# Generate trace for failed tests
npx playwright test --trace on

# Show trace viewer
npx playwright show-trace trace.zip

# Update visual regression baselines
npx playwright test --update-snapshots
```

## Contributing

When adding new tests:

1. Follow the existing naming conventions
2. Add appropriate `data-testid` attributes to new UI elements
3. Include accessibility testing for new features
4. Update this README with new test descriptions
5. Ensure tests are deterministic and not flaky
6. Add visual regression tests for UI changes

## Dependencies

- `@playwright/test`: E2E testing framework
- `@axe-core/playwright`: Accessibility testing integration
- Test fixtures and utilities in `tests/` directory

For more information, see the [Playwright documentation](https://playwright.dev/) and [axe-core documentation](https://github.com/dequelabs/axe-core).
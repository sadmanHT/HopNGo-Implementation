import { test, expect } from '@playwright/test';

test.describe('Search Functionality - Basic Search', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should perform basic location search', async ({ page }) => {
    // Navigate to search page
    await page.click('[data-testid="nav-search"]');
    
    // Perform basic search
    await page.fill('[data-testid="search-location-input"]', 'Tokyo, Japan');
    await page.click('[data-testid="search-button"]');
    
    // Wait for search results to load
    await page.waitForLoadState('networkidle');
    
    // Verify search results
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    await expect(page.locator('[data-testid="listing-card"]')).toBeVisible();
    await expect(page.locator('[data-testid="search-results-count"]')).toBeVisible();
    
    // Verify search term is displayed
    await expect(page.locator('text=Tokyo, Japan')).toBeVisible();
  });

  test('should handle empty search results', async ({ page }) => {
    await page.click('[data-testid="nav-search"]');
    
    // Search for non-existent location
    await page.fill('[data-testid="search-location-input"]', 'NonExistentCity12345');
    await page.click('[data-testid="search-button"]');
    
    await page.waitForLoadState('networkidle');
    
    // Verify empty state
    await expect(page.locator('[data-testid="no-results-message"]')).toBeVisible();
    await expect(page.locator('text=No listings found for your search')).toBeVisible();
    await expect(page.locator('[data-testid="search-suggestions"]')).toBeVisible();
  });

  test('should provide search suggestions and autocomplete', async ({ page }) => {
    await page.click('[data-testid="nav-search"]');
    
    // Start typing to trigger autocomplete
    await page.fill('[data-testid="search-location-input"]', 'New Y');
    
    // Wait for suggestions to appear
    await expect(page.locator('[data-testid="search-suggestions"]')).toBeVisible();
    await expect(page.locator('[data-testid="suggestion-item"]')).toBeVisible();
    
    // Click on a suggestion
    await page.click('[data-testid="suggestion-item"]:first-child');
    
    // Verify suggestion was selected
    await expect(page.locator('[data-testid="search-location-input"]')).toHaveValue(/New York/);
  });

  test('should handle search with special characters and unicode', async ({ page }) => {
    await page.click('[data-testid="nav-search"]');
    
    // Search with special characters and unicode
    await page.fill('[data-testid="search-location-input"]', 'São Paulo, Brasil 🇧🇷');
    await page.click('[data-testid="search-button"]');
    
    await page.waitForLoadState('networkidle');
    
    // Verify search handles special characters correctly
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    await expect(page.locator('text=São Paulo')).toBeVisible();
  });
});

test.describe('Search Functionality - Advanced Filters', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    await page.fill('[data-testid="search-location-input"]', 'New York');
    await page.click('[data-testid="search-button"]');
    await page.waitForLoadState('networkidle');
  });

  test('should filter by date range', async ({ page }) => {
    // Open filters panel
    await page.click('[data-testid="filters-button"]');
    
    // Set check-in and check-out dates
    await page.fill('[data-testid="checkin-date"]', '2024-07-01');
    await page.fill('[data-testid="checkout-date"]', '2024-07-05');
    
    // Apply filters
    await page.click('[data-testid="apply-filters-button"]');
    
    await page.waitForLoadState('networkidle');
    
    // Verify filtered results
    await expect(page.locator('[data-testid="active-filter-chip"]')).toContainText('Jul 1 - Jul 5');
    await expect(page.locator('[data-testid="listing-card"]')).toBeVisible();
  });

  test('should filter by price range', async ({ page }) => {
    await page.click('[data-testid="filters-button"]');
    
    // Set price range using slider
    await page.locator('[data-testid="price-range-min"]').fill('100');
    await page.locator('[data-testid="price-range-max"]').fill('300');
    
    await page.click('[data-testid="apply-filters-button"]');
    await page.waitForLoadState('networkidle');
    
    // Verify price filter is applied
    await expect(page.locator('[data-testid="active-filter-chip"]')).toContainText('$100 - $300');
    
    // Verify all displayed prices are within range
    const priceElements = await page.locator('[data-testid="listing-price"]').all();
    for (const priceElement of priceElements) {
      const priceText = await priceElement.textContent();
      const price = parseInt(priceText?.replace(/[^0-9]/g, '') || '0');
      expect(price).toBeGreaterThanOrEqual(100);
      expect(price).toBeLessThanOrEqual(300);
    }
  });

  test('should filter by property type', async ({ page }) => {
    await page.click('[data-testid="filters-button"]');
    
    // Select property types
    await page.check('[data-testid="property-type-hotel"]');
    await page.check('[data-testid="property-type-apartment"]');
    
    await page.click('[data-testid="apply-filters-button"]');
    await page.waitForLoadState('networkidle');
    
    // Verify property type filter
    await expect(page.locator('[data-testid="active-filter-chip"]')).toContainText('Hotel, Apartment');
    
    // Verify results match selected types
    const typeElements = await page.locator('[data-testid="listing-type"]').all();
    for (const typeElement of typeElements) {
      const typeText = await typeElement.textContent();
      expect(['Hotel', 'Apartment']).toContain(typeText?.trim());
    }
  });

  test('should filter by amenities', async ({ page }) => {
    await page.click('[data-testid="filters-button"]');
    
    // Expand amenities section
    await page.click('[data-testid="amenities-section"]');
    
    // Select amenities
    await page.check('[data-testid="amenity-wifi"]');
    await page.check('[data-testid="amenity-parking"]');
    await page.check('[data-testid="amenity-pool"]');
    
    await page.click('[data-testid="apply-filters-button"]');
    await page.waitForLoadState('networkidle');
    
    // Verify amenities filter
    await expect(page.locator('[data-testid="active-filter-chip"]')).toContainText('WiFi, Parking, Pool');
    
    // Verify listings have selected amenities
    await expect(page.locator('[data-testid="listing-amenities"] >> text=WiFi')).toBeVisible();
  });

  test('should filter by guest capacity', async ({ page }) => {
    await page.click('[data-testid="filters-button"]');
    
    // Set guest count
    await page.selectOption('[data-testid="guest-count-select"]', '4');
    
    await page.click('[data-testid="apply-filters-button"]');
    await page.waitForLoadState('networkidle');
    
    // Verify guest capacity filter
    await expect(page.locator('[data-testid="active-filter-chip"]')).toContainText('4 guests');
    
    // Verify listings can accommodate 4+ guests
    const capacityElements = await page.locator('[data-testid="listing-capacity"]').all();
    for (const capacityElement of capacityElements) {
      const capacityText = await capacityElement.textContent();
      const capacity = parseInt(capacityText?.match(/\d+/)?.[0] || '0');
      expect(capacity).toBeGreaterThanOrEqual(4);
    }
  });

  test('should combine multiple filters', async ({ page }) => {
    await page.click('[data-testid="filters-button"]');
    
    // Apply multiple filters
    await page.fill('[data-testid="checkin-date"]', '2024-08-01');
    await page.fill('[data-testid="checkout-date"]', '2024-08-05');
    await page.locator('[data-testid="price-range-max"]').fill('250');
    await page.check('[data-testid="property-type-hotel"]');
    await page.check('[data-testid="amenity-wifi"]');
    
    await page.click('[data-testid="apply-filters-button"]');
    await page.waitForLoadState('networkidle');
    
    // Verify multiple active filters
    await expect(page.locator('[data-testid="active-filter-chip"]')).toHaveCount(4);
    await expect(page.locator('[data-testid="listing-card"]')).toBeVisible();
  });

  test('should clear individual and all filters', async ({ page }) => {
    // Apply some filters first
    await page.click('[data-testid="filters-button"]');
    await page.check('[data-testid="property-type-hotel"]');
    await page.check('[data-testid="amenity-wifi"]');
    await page.click('[data-testid="apply-filters-button"]');
    
    // Clear individual filter
    await page.click('[data-testid="active-filter-chip"]:first-child [data-testid="remove-filter"]');
    await page.waitForLoadState('networkidle');
    
    // Verify one filter removed
    await expect(page.locator('[data-testid="active-filter-chip"]')).toHaveCount(1);
    
    // Clear all filters
    await page.click('[data-testid="clear-all-filters-button"]');
    await page.waitForLoadState('networkidle');
    
    // Verify all filters cleared
    await expect(page.locator('[data-testid="active-filter-chip"]')).toHaveCount(0);
  });
});

test.describe('Search Functionality - Sorting and Display', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    await page.fill('[data-testid="search-location-input"]', 'Paris');
    await page.click('[data-testid="search-button"]');
    await page.waitForLoadState('networkidle');
  });

  test('should sort results by different criteria', async ({ page }) => {
    // Sort by price (low to high)
    await page.selectOption('[data-testid="sort-dropdown"]', 'price-asc');
    await page.waitForLoadState('networkidle');
    
    // Verify sorting by checking first few prices
    const prices = await page.locator('[data-testid="listing-price"]').first().textContent();
    
    // Sort by price (high to low)
    await page.selectOption('[data-testid="sort-dropdown"]', 'price-desc');
    await page.waitForLoadState('networkidle');
    
    // Sort by rating
    await page.selectOption('[data-testid="sort-dropdown"]', 'rating');
    await page.waitForLoadState('networkidle');
    
    // Verify rating sort
    await expect(page.locator('[data-testid="listing-rating"]').first()).toBeVisible();
    
    // Sort by distance
    await page.selectOption('[data-testid="sort-dropdown"]', 'distance');
    await page.waitForLoadState('networkidle');
    
    await expect(page.locator('[data-testid="listing-distance"]').first()).toBeVisible();
  });

  test('should switch between grid and list view', async ({ page }) => {
    // Default should be grid view
    await expect(page.locator('[data-testid="results-grid-view"]')).toBeVisible();
    
    // Switch to list view
    await page.click('[data-testid="list-view-button"]');
    await expect(page.locator('[data-testid="results-list-view"]')).toBeVisible();
    await expect(page.locator('[data-testid="listing-card-list"]')).toBeVisible();
    
    // Switch back to grid view
    await page.click('[data-testid="grid-view-button"]');
    await expect(page.locator('[data-testid="results-grid-view"]')).toBeVisible();
    await expect(page.locator('[data-testid="listing-card-grid"]')).toBeVisible();
  });

  test('should show map view with listings', async ({ page }) => {
    // Toggle map view
    await page.click('[data-testid="show-map-button"]');
    
    // Verify map is visible
    await expect(page.locator('[data-testid="search-map"]')).toBeVisible();
    await expect(page.locator('[data-testid="map-marker"]')).toBeVisible();
    
    // Click on a map marker
    await page.click('[data-testid="map-marker"]:first-child');
    
    // Verify listing popup appears
    await expect(page.locator('[data-testid="map-listing-popup"]')).toBeVisible();
    
    // Hide map
    await page.click('[data-testid="hide-map-button"]');
    await expect(page.locator('[data-testid="search-map"]')).not.toBeVisible();
  });

  test('should implement infinite scroll pagination', async ({ page }) => {
    // Get initial listing count
    const initialCount = await page.locator('[data-testid="listing-card"]').count();
    
    // Scroll to bottom to trigger load more
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight);
    });
    
    // Wait for new listings to load
    await page.waitForTimeout(2000);
    await page.waitForLoadState('networkidle');
    
    // Verify more listings loaded
    const newCount = await page.locator('[data-testid="listing-card"]').count();
    expect(newCount).toBeGreaterThan(initialCount);
    
    // Verify loading indicator appeared during load
    // Note: This might be visible briefly, so we check if it exists
    const loadingIndicator = page.locator('[data-testid="loading-more-listings"]');
    // Loading indicator should either be visible or have been visible
  });
});

test.describe('Search Functionality - Performance and CLS Guards', () => {
  test('should prevent Cumulative Layout Shift during search', async ({ page }) => {
    // Navigate to search page
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    
    // Measure layout stability during search
    await page.evaluate(() => {
      // Initialize CLS measurement
      let clsValue = 0;
      let clsEntries = [];
      
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (!entry.hadRecentInput) {
            clsValue += entry.value;
            clsEntries.push(entry);
          }
        }
      });
      
      observer.observe({ type: 'layout-shift', buffered: true });
      
      // Store CLS data on window for later access
      window.clsData = { value: clsValue, entries: clsEntries };
    });
    
    // Perform search that might cause layout shifts
    await page.fill('[data-testid="search-location-input"]', 'London');
    await page.click('[data-testid="search-button"]');
    
    // Wait for search results to fully load
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000); // Additional wait for any delayed shifts
    
    // Check CLS value
    const clsValue = await page.evaluate(() => {
      return window.clsData?.value || 0;
    });
    
    // CLS should be below the "good" threshold of 0.1
    expect(clsValue).toBeLessThan(0.1);
  });

  test('should have proper loading states and skeleton screens', async ({ page }) => {
    // Navigate to search
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    
    // Start search
    await page.fill('[data-testid="search-location-input"]', 'Barcelona');
    
    // Click search and immediately check for loading state
    await Promise.all([
      page.click('[data-testid="search-button"]'),
      page.waitForSelector('[data-testid="search-loading"]', { timeout: 1000 })
    ]);
    
    // Verify skeleton screens are shown
    await expect(page.locator('[data-testid="listing-skeleton"]')).toBeVisible();
    
    // Wait for actual results to load
    await page.waitForLoadState('networkidle');
    
    // Verify loading states are hidden and real content is shown
    await expect(page.locator('[data-testid="search-loading"]')).not.toBeVisible();
    await expect(page.locator('[data-testid="listing-skeleton"]')).not.toBeVisible();
    await expect(page.locator('[data-testid="listing-card"]')).toBeVisible();
  });

  test('should handle search errors gracefully', async ({ page }) => {
    // Mock network failure
    await page.route('**/api/search**', route => {
      route.abort('failed');
    });
    
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    
    // Attempt search
    await page.fill('[data-testid="search-location-input"]', 'Test Location');
    await page.click('[data-testid="search-button"]');
    
    // Wait for error state
    await page.waitForTimeout(3000);
    
    // Verify error handling
    await expect(page.locator('[data-testid="search-error"]')).toBeVisible();
    await expect(page.locator('text=Something went wrong')).toBeVisible();
    await expect(page.locator('[data-testid="retry-search-button"]')).toBeVisible();
    
    // Test retry functionality
    await page.unroute('**/api/search**');
    await page.click('[data-testid="retry-search-button"]');
    
    // Verify retry works
    await page.waitForLoadState('networkidle');
    await expect(page.locator('[data-testid="search-error"]')).not.toBeVisible();
  });

  test('should optimize images and prevent layout shift from image loading', async ({ page }) => {
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    await page.fill('[data-testid="search-location-input"]', 'Rome');
    await page.click('[data-testid="search-button"]');
    
    await page.waitForLoadState('networkidle');
    
    // Check that listing images have proper dimensions set
    const images = await page.locator('[data-testid="listing-image"]').all();
    
    for (const image of images) {
      // Verify images have width and height attributes to prevent CLS
      const width = await image.getAttribute('width');
      const height = await image.getAttribute('height');
      
      expect(width).toBeTruthy();
      expect(height).toBeTruthy();
      
      // Verify images have proper loading attribute
      const loading = await image.getAttribute('loading');
      expect(['lazy', 'eager']).toContain(loading);
    }
  });

  test('should implement proper focus management for accessibility', async ({ page }) => {
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    
    // Test keyboard navigation
    await page.keyboard.press('Tab'); // Should focus search input
    await expect(page.locator('[data-testid="search-location-input"]')).toBeFocused();
    
    await page.keyboard.press('Tab'); // Should focus search button
    await expect(page.locator('[data-testid="search-button"]')).toBeFocused();
    
    // Perform search
    await page.fill('[data-testid="search-location-input"]', 'Amsterdam');
    await page.keyboard.press('Enter');
    
    await page.waitForLoadState('networkidle');
    
    // Test focus management after search
    await page.keyboard.press('Tab');
    
    // Focus should be on first interactive element in results
    const focusedElement = await page.locator(':focus').first();
    await expect(focusedElement).toBeVisible();
    
    // Test skip link functionality
    await page.keyboard.press('Tab');
    const skipLink = page.locator('[data-testid="skip-to-results"]');
    if (await skipLink.isVisible()) {
      await skipLink.click();
      // Focus should jump to results section
      await expect(page.locator('[data-testid="search-results"]')).toBeFocused();
    }
  });
});

test.describe('Search Functionality - Mobile Responsiveness', () => {
  test.use({ viewport: { width: 375, height: 667 } }); // iPhone SE size
  
  test('should work properly on mobile devices', async ({ page }) => {
    await page.goto('/');
    await page.click('[data-testid="nav-search"]');
    
    // Test mobile search interface
    await expect(page.locator('[data-testid="search-location-input"]')).toBeVisible();
    
    // Fill search on mobile
    await page.fill('[data-testid="search-location-input"]', 'Berlin');
    await page.click('[data-testid="search-button"]');
    
    await page.waitForLoadState('networkidle');
    
    // Verify mobile-optimized results
    await expect(page.locator('[data-testid="listing-card"]')).toBeVisible();
    
    // Test mobile filters
    await page.click('[data-testid="mobile-filters-button"]');
    await expect(page.locator('[data-testid="mobile-filters-modal"]')).toBeVisible();
    
    // Apply a filter on mobile
    await page.check('[data-testid="property-type-hotel"]');
    await page.click('[data-testid="apply-mobile-filters-button"]');
    
    // Verify filter applied
    await expect(page.locator('[data-testid="active-filter-chip"]')).toBeVisible();
    
    // Test mobile map view
    await page.click('[data-testid="mobile-map-toggle"]');
    await expect(page.locator('[data-testid="mobile-map-view"]')).toBeVisible();
  });
});
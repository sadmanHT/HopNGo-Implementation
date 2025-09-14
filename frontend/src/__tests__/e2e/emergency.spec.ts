import { test, expect } from '@playwright/test';

test.describe('Emergency Contact System', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'customer@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await expect(page.locator('[data-testid="user-dashboard"]')).toBeVisible();
  });

  test('should set up emergency contacts', async ({ page }) => {
    // Navigate to emergency settings
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="emergency-settings"]');
    await expect(page.locator('[data-testid="emergency-settings-page"]')).toBeVisible();

    // Add primary emergency contact
    await page.click('[data-testid="add-emergency-contact"]');
    await page.fill('[data-testid="contact-name"]', 'John Doe');
    await page.fill('[data-testid="contact-phone"]', '+8801712345678');
    await page.fill('[data-testid="contact-email"]', 'john.doe@example.com');
    await page.selectOption('[data-testid="contact-relationship"]', 'FAMILY');
    await page.check('[data-testid="primary-contact"]');
    await page.click('[data-testid="save-contact-button"]');

    // Add secondary emergency contact
    await page.click('[data-testid="add-emergency-contact"]');
    await page.fill('[data-testid="contact-name"]', 'Jane Smith');
    await page.fill('[data-testid="contact-phone"]', '+8801987654321');
    await page.fill('[data-testid="contact-email"]', 'jane.smith@example.com');
    await page.selectOption('[data-testid="contact-relationship"]', 'FRIEND');
    await page.click('[data-testid="save-contact-button"]');

    // Verify contacts are saved
    await expect(page.locator('[data-testid="emergency-contact-item"]')).toHaveCount(2);
    await expect(page.locator('text=John Doe')).toBeVisible();
    await expect(page.locator('text=Jane Smith')).toBeVisible();
    await expect(page.locator('[data-testid="primary-badge"]')).toBeVisible();
  });

  test('should configure emergency trigger settings', async ({ page }) => {
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="emergency-settings"]');
    await page.click('[data-testid="trigger-settings-tab"]');

    // Configure automatic triggers
    await page.check('[data-testid="enable-location-trigger"]');
    await page.fill('[data-testid="location-timeout"]', '30'); // 30 minutes
    
    await page.check('[data-testid="enable-checkin-trigger"]');
    await page.fill('[data-testid="checkin-interval"]', '24'); // 24 hours
    
    await page.check('[data-testid="enable-panic-button"]');
    await page.selectOption('[data-testid="panic-activation"]', 'DOUBLE_TAP');

    // Configure emergency message template
    await page.fill('[data-testid="emergency-message"]', 
      'EMERGENCY: I need help! My last known location was {location}. Please contact me immediately or call local authorities.');

    await page.click('[data-testid="save-trigger-settings"]');
    await expect(page.locator('text=Emergency settings saved')).toBeVisible();
  });

  test('should trigger manual emergency alert', async ({ page }) => {
    // Navigate to a trip or location where emergency button is available
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    
    // Verify emergency button is visible
    await expect(page.locator('[data-testid="emergency-button"]')).toBeVisible();

    // Trigger emergency alert
    await page.click('[data-testid="emergency-button"]');
    await expect(page.locator('[data-testid="emergency-confirmation-modal"]')).toBeVisible();
    
    // Confirm emergency
    await page.click('[data-testid="confirm-emergency-button"]');
    
    // Verify emergency alert is sent
    await expect(page.locator('[data-testid="emergency-active-indicator"]')).toBeVisible();
    await expect(page.locator('text=Emergency alert sent to your contacts')).toBeVisible();
    
    // Verify emergency status
    await expect(page.locator('[data-testid="emergency-status"]')).toContainText('ACTIVE');
    await expect(page.locator('[data-testid="emergency-timestamp"]')).toBeVisible();
  });

  test('should handle panic button double-tap', async ({ page }) => {
    // Navigate to map or location-based page
    await page.click('[data-testid="nav-map"]');
    await expect(page.locator('[data-testid="map-container"]')).toBeVisible();

    // Simulate double-tap on panic button (if enabled)
    const panicButton = page.locator('[data-testid="panic-button"]');
    await panicButton.dblclick();
    
    // Should trigger immediate emergency without confirmation
    await expect(page.locator('[data-testid="emergency-active-indicator"]')).toBeVisible();
    await expect(page.locator('text=PANIC ALERT ACTIVATED')).toBeVisible();
    
    // Verify high-priority emergency status
    await expect(page.locator('[data-testid="emergency-priority"]')).toContainText('HIGH');
  });

  test('should handle location-based emergency trigger', async ({ page }) => {
    // Mock geolocation to simulate being in a remote area
    await page.context().grantPermissions(['geolocation']);
    await page.setGeolocation({ latitude: 23.8103, longitude: 90.4125 }); // Dhaka coordinates
    
    await page.click('[data-testid="nav-map"]');
    
    // Simulate user not moving for extended period (mock scenario)
    await page.evaluate(() => {
      // Simulate location timeout trigger
      window.dispatchEvent(new CustomEvent('emergency:location-timeout', {
        detail: {
          lastKnownLocation: { lat: 23.8103, lng: 90.4125 },
          timeoutDuration: 30 // minutes
        }
      }));
    });
    
    // Should show location timeout warning
    await expect(page.locator('[data-testid="location-timeout-warning"]')).toBeVisible();
    await expect(page.locator('text=You haven\'t moved for 30 minutes')).toBeVisible();
    
    // User doesn't respond, should trigger emergency
    await page.waitForTimeout(5000); // Wait for auto-trigger
    
    await expect(page.locator('[data-testid="emergency-active-indicator"]')).toBeVisible();
    await expect(page.locator('text=Location-based emergency triggered')).toBeVisible();
  });

  test('should handle check-in reminder and missed check-in', async ({ page }) => {
    // Navigate to trip details
    await page.click('[data-testid="nav-trips"]');
    await page.click('[data-testid="trip-card"]:first-child');
    
    // Simulate check-in reminder
    await page.evaluate(() => {
      window.dispatchEvent(new CustomEvent('emergency:checkin-reminder', {
        detail: {
          nextCheckinTime: new Date(Date.now() + 60000).toISOString() // 1 minute from now
        }
      }));
    });
    
    // Should show check-in reminder
    await expect(page.locator('[data-testid="checkin-reminder"]')).toBeVisible();
    await expect(page.locator('text=Time for your safety check-in')).toBeVisible();
    
    // Perform check-in
    await page.click('[data-testid="checkin-button"]');
    await page.fill('[data-testid="checkin-status"]', 'All good! Enjoying the trip.');
    await page.click('[data-testid="submit-checkin"]');
    
    // Verify check-in recorded
    await expect(page.locator('text=Check-in recorded successfully')).toBeVisible();
    await expect(page.locator('[data-testid="last-checkin-time"]')).toBeVisible();
  });

  test('should handle missed check-in emergency', async ({ page }) => {
    // Simulate missed check-in scenario
    await page.evaluate(() => {
      window.dispatchEvent(new CustomEvent('emergency:missed-checkin', {
        detail: {
          missedTime: new Date(Date.now() - 3600000).toISOString(), // 1 hour ago
          scheduledTime: new Date(Date.now() - 1800000).toISOString() // 30 minutes ago
        }
      }));
    });
    
    // Should show missed check-in alert
    await expect(page.locator('[data-testid="missed-checkin-alert"]')).toBeVisible();
    await expect(page.locator('text=You missed your scheduled check-in')).toBeVisible();
    
    // Provide options to respond
    await expect(page.locator('[data-testid="late-checkin-button"]')).toBeVisible();
    await expect(page.locator('[data-testid="emergency-help-button"]')).toBeVisible();
    
    // User indicates they need help
    await page.click('[data-testid="emergency-help-button"]');
    
    // Should trigger emergency alert
    await expect(page.locator('[data-testid="emergency-active-indicator"]')).toBeVisible();
    await expect(page.locator('text=Emergency triggered due to missed check-in')).toBeVisible();
  });

  test('should send emergency notifications to contacts', async ({ page }) => {
    // Trigger emergency
    await page.click('[data-testid="emergency-button"]');
    await page.click('[data-testid="confirm-emergency-button"]');
    
    // Check emergency notification status
    await page.click('[data-testid="emergency-details-button"]');
    await expect(page.locator('[data-testid="emergency-details-modal"]')).toBeVisible();
    
    // Verify notification attempts
    await expect(page.locator('[data-testid="notification-log"]')).toBeVisible();
    await expect(page.locator('text=SMS sent to +8801712345678')).toBeVisible();
    await expect(page.locator('text=Email sent to john.doe@example.com')).toBeVisible();
    await expect(page.locator('text=Push notification sent')).toBeVisible();
    
    // Verify location sharing
    await expect(page.locator('[data-testid="shared-location"]')).toBeVisible();
    await expect(page.locator('[data-testid="location-map"]')).toBeVisible();
  });

  test('should handle emergency contact response', async ({ page, context }) => {
    // Create emergency contact browser context
    const contactPage = await context.newPage();
    
    // Trigger emergency from main user
    await page.click('[data-testid="emergency-button"]');
    await page.click('[data-testid="confirm-emergency-button"]');
    
    // Simulate emergency contact receiving notification and responding
    const emergencyId = await page.locator('[data-testid="emergency-id"]').textContent();
    
    // Contact accesses emergency response page
    await contactPage.goto(`/emergency-response/${emergencyId}`);
    await expect(contactPage.locator('[data-testid="emergency-response-page"]')).toBeVisible();
    
    // Contact can see emergency details
    await expect(contactPage.locator('[data-testid="emergency-user-info"]')).toBeVisible();
    await expect(contactPage.locator('[data-testid="emergency-location"]')).toBeVisible();
    await expect(contactPage.locator('[data-testid="emergency-timestamp"]')).toBeVisible();
    
    // Contact responds to emergency
    await contactPage.click('[data-testid="respond-to-emergency"]');
    await contactPage.fill('[data-testid="response-message"]', 'I have received your emergency alert. Calling you now and contacting local authorities.');
    await contactPage.click('[data-testid="send-response"]');
    
    // Verify response is recorded
    await expect(contactPage.locator('text=Response sent successfully')).toBeVisible();
    
    // Main user should see the response
    await page.reload();
    await page.click('[data-testid="emergency-details-button"]');
    await expect(page.locator('text=I have received your emergency alert')).toBeVisible();
    await expect(page.locator('[data-testid="contact-response"]')).toBeVisible();
  });

  test('should resolve emergency situation', async ({ page }) => {
    // Trigger emergency first
    await page.click('[data-testid="emergency-button"]');
    await page.click('[data-testid="confirm-emergency-button"]');
    
    // Wait for emergency to be active
    await expect(page.locator('[data-testid="emergency-active-indicator"]')).toBeVisible();
    
    // Resolve emergency
    await page.click('[data-testid="resolve-emergency-button"]');
    await expect(page.locator('[data-testid="resolve-emergency-modal"]')).toBeVisible();
    
    await page.fill('[data-testid="resolution-message"]', 'False alarm - I am safe and no longer need assistance.');
    await page.click('[data-testid="confirm-resolution"]');
    
    // Verify emergency is resolved
    await expect(page.locator('text=Emergency resolved successfully')).toBeVisible();
    await expect(page.locator('[data-testid="emergency-status"]')).toContainText('RESOLVED');
    await expect(page.locator('[data-testid="emergency-active-indicator"]')).not.toBeVisible();
    
    // Verify resolution notification sent to contacts
    await expect(page.locator('text=Resolution notification sent to emergency contacts')).toBeVisible();
  });

  test('should handle emergency escalation', async ({ page }) => {
    // Trigger emergency
    await page.click('[data-testid="emergency-button"]');
    await page.click('[data-testid="confirm-emergency-button"]');
    
    // Simulate no response from contacts for extended period
    await page.evaluate(() => {
      window.dispatchEvent(new CustomEvent('emergency:escalation-timeout', {
        detail: {
          emergencyId: 'test-emergency-123',
          timeoutDuration: 15 // minutes
        }
      }));
    });
    
    // Should show escalation warning
    await expect(page.locator('[data-testid="escalation-warning"]')).toBeVisible();
    await expect(page.locator('text=No response from emergency contacts')).toBeVisible();
    
    // Should automatically escalate to authorities
    await expect(page.locator('[data-testid="authority-notification"]')).toBeVisible();
    await expect(page.locator('text=Local authorities have been notified')).toBeVisible();
    
    // Verify escalation status
    await expect(page.locator('[data-testid="emergency-priority"]')).toContainText('CRITICAL');
    await expect(page.locator('[data-testid="escalation-status"]')).toContainText('ESCALATED');
  });

  test('should test emergency system with mock scenarios', async ({ page }) => {
    // Navigate to emergency testing page (admin/testing feature)
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="emergency-settings"]');
    await page.click('[data-testid="test-emergency-system"]');
    
    // Test different emergency scenarios
    await page.click('[data-testid="test-location-timeout"]');
    await expect(page.locator('text=Location timeout test completed')).toBeVisible();
    
    await page.click('[data-testid="test-missed-checkin"]');
    await expect(page.locator('text=Missed check-in test completed')).toBeVisible();
    
    await page.click('[data-testid="test-panic-button"]');
    await expect(page.locator('text=Panic button test completed')).toBeVisible();
    
    // Verify all tests passed
    await expect(page.locator('[data-testid="test-results"]')).toContainText('All emergency systems functioning correctly');
  });
});
import { test, expect } from '@playwright/test';

test.describe('Social Features - Post Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'social-user@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await page.click('[data-testid="nav-social"]');
  });

  test('should create a text post with tags and location', async ({ page }) => {
    await page.click('[data-testid="create-post-button"]');
    
    // Fill post content
    await page.fill('[data-testid="post-content"]', 'Just discovered this amazing hidden gem in Tokyo! 🗾 The cherry blossoms are absolutely stunning this time of year.');
    await page.fill('[data-testid="post-tags"]', 'tokyo,japan,cherryblossoms,travel,photography');
    await page.fill('[data-testid="post-location"]', 'Shinjuku Park, Tokyo, Japan');
    
    // Set post visibility
    await page.selectOption('[data-testid="post-visibility"]', 'public');
    
    await page.click('[data-testid="submit-post-button"]');
    
    // Verify post creation
    await expect(page.locator('text=Post created successfully')).toBeVisible();
    await expect(page.locator('text=Just discovered this amazing hidden gem in Tokyo!')).toBeVisible();
    await expect(page.locator('[data-testid="post-location"] >> text=Shinjuku Park, Tokyo, Japan')).toBeVisible();
    await expect(page.locator('[data-testid="post-tags"] >> text=tokyo')).toBeVisible();
  });

  test('should create a post with image upload', async ({ page }) => {
    await page.click('[data-testid="create-post-button"]');
    
    // Upload image
    await page.setInputFiles('[data-testid="post-image-upload"]', 'tests/fixtures/sample-travel-photo.jpg');
    
    // Wait for image preview
    await expect(page.locator('[data-testid="image-preview"]')).toBeVisible();
    
    await page.fill('[data-testid="post-content"]', 'Sunset views from my hotel balcony 🌅');
    await page.fill('[data-testid="post-tags"]', 'sunset,hotel,vacation,photography');
    
    await page.click('[data-testid="submit-post-button"]');
    
    // Verify post with image
    await expect(page.locator('text=Post created successfully')).toBeVisible();
    await expect(page.locator('[data-testid="post-image"]')).toBeVisible();
    await expect(page.locator('text=Sunset views from my hotel balcony')).toBeVisible();
  });

  test('should edit an existing post', async ({ page }) => {
    // Create a post first
    await page.click('[data-testid="create-post-button"]');
    await page.fill('[data-testid="post-content"]', 'Original post content');
    await page.click('[data-testid="submit-post-button"]');
    
    // Edit the post
    await page.click('[data-testid="post-menu-button"]:first-child');
    await page.click('[data-testid="edit-post-option"]');
    
    await page.fill('[data-testid="post-content"]', 'Updated post content with more details about my amazing trip!');
    await page.fill('[data-testid="post-tags"]', 'updated,travel,experience');
    
    await page.click('[data-testid="save-post-button"]');
    
    // Verify post update
    await expect(page.locator('text=Post updated successfully')).toBeVisible();
    await expect(page.locator('text=Updated post content with more details')).toBeVisible();
    await expect(page.locator('[data-testid="post-edited-indicator"]')).toBeVisible();
  });

  test('should delete a post', async ({ page }) => {
    // Create a post first
    await page.click('[data-testid="create-post-button"]');
    await page.fill('[data-testid="post-content"]', 'Post to be deleted');
    await page.click('[data-testid="submit-post-button"]');
    
    // Delete the post
    await page.click('[data-testid="post-menu-button"]:first-child');
    await page.click('[data-testid="delete-post-option"]');
    
    // Confirm deletion
    await page.click('[data-testid="confirm-delete-button"]');
    
    // Verify post deletion
    await expect(page.locator('text=Post deleted successfully')).toBeVisible();
    await expect(page.locator('text=Post to be deleted')).not.toBeVisible();
  });
});

test.describe('Social Features - Post Interactions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'social-user@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await page.click('[data-testid="nav-social"]');
  });

  test('should like and unlike posts', async ({ page }) => {
    // Like a post
    await page.click('[data-testid="like-button"]:first-child');
    
    // Verify like count increased
    await expect(page.locator('[data-testid="like-count"]:first-child')).toContainText('1');
    await expect(page.locator('[data-testid="like-button"]:first-child')).toHaveClass(/liked/);
    
    // Unlike the post
    await page.click('[data-testid="like-button"]:first-child');
    
    // Verify like count decreased
    await expect(page.locator('[data-testid="like-count"]:first-child')).toContainText('0');
    await expect(page.locator('[data-testid="like-button"]:first-child')).not.toHaveClass(/liked/);
  });

  test('should add and view comments', async ({ page }) => {
    // Click on comments section
    await page.click('[data-testid="comments-button"]:first-child');
    
    // Add a comment
    await page.fill('[data-testid="comment-input"]', 'This looks amazing! I need to visit this place.');
    await page.click('[data-testid="submit-comment-button"]');
    
    // Verify comment appears
    await expect(page.locator('text=This looks amazing! I need to visit this place.')).toBeVisible();
    await expect(page.locator('[data-testid="comment-count"]:first-child')).toContainText('1');
    
    // Add another comment
    await page.fill('[data-testid="comment-input"]', 'Thanks for sharing! What was your favorite part?');
    await page.click('[data-testid="submit-comment-button"]');
    
    // Verify second comment
    await expect(page.locator('text=Thanks for sharing! What was your favorite part?')).toBeVisible();
    await expect(page.locator('[data-testid="comment-count"]:first-child')).toContainText('2');
  });

  test('should reply to comments', async ({ page }) => {
    // Open comments
    await page.click('[data-testid="comments-button"]:first-child');
    
    // Add initial comment
    await page.fill('[data-testid="comment-input"]', 'Great photo!');
    await page.click('[data-testid="submit-comment-button"]');
    
    // Reply to the comment
    await page.click('[data-testid="reply-button"]:first-child');
    await page.fill('[data-testid="reply-input"]', 'Thank you! It was taken during golden hour.');
    await page.click('[data-testid="submit-reply-button"]');
    
    // Verify reply appears
    await expect(page.locator('text=Thank you! It was taken during golden hour.')).toBeVisible();
    await expect(page.locator('[data-testid="reply-indicator"]')).toBeVisible();
  });

  test('should share posts', async ({ page }) => {
    // Click share button
    await page.click('[data-testid="share-button"]:first-child');
    
    // Verify share modal opens
    await expect(page.locator('[data-testid="share-modal"]')).toBeVisible();
    
    // Test copy link functionality
    await page.click('[data-testid="copy-link-button"]');
    await expect(page.locator('text=Link copied to clipboard')).toBeVisible();
    
    // Test social media sharing options
    await expect(page.locator('[data-testid="share-twitter"]')).toBeVisible();
    await expect(page.locator('[data-testid="share-facebook"]')).toBeVisible();
    await expect(page.locator('[data-testid="share-instagram"]')).toBeVisible();
    
    // Close modal
    await page.click('[data-testid="close-share-modal"]');
    await expect(page.locator('[data-testid="share-modal"]')).not.toBeVisible();
  });

  test('should bookmark posts', async ({ page }) => {
    // Bookmark a post
    await page.click('[data-testid="bookmark-button"]:first-child');
    
    // Verify bookmark status
    await expect(page.locator('[data-testid="bookmark-button"]:first-child')).toHaveClass(/bookmarked/);
    await expect(page.locator('text=Post bookmarked')).toBeVisible();
    
    // Navigate to bookmarks page
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="my-bookmarks"]');
    
    // Verify post appears in bookmarks
    await expect(page.locator('[data-testid="bookmarked-post"]')).toBeVisible();
    
    // Remove bookmark
    await page.click('[data-testid="bookmark-button"]:first-child');
    await expect(page.locator('text=Bookmark removed')).toBeVisible();
  });
});

test.describe('Social Features - Feed Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'social-user@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await page.click('[data-testid="nav-social"]');
  });

  test('should filter feed by tags', async ({ page }) => {
    // Apply tag filter
    await page.click('[data-testid="filter-button"]');
    await page.fill('[data-testid="tag-filter-input"]', 'travel');
    await page.click('[data-testid="apply-filter-button"]');
    
    // Verify filtered results
    await expect(page.locator('[data-testid="post-item"]')).toBeVisible();
    await expect(page.locator('[data-testid="post-tags"] >> text=travel')).toBeVisible();
    
    // Clear filter
    await page.click('[data-testid="clear-filters-button"]');
    await expect(page.locator('[data-testid="filter-active-indicator"]')).not.toBeVisible();
  });

  test('should sort feed by different criteria', async ({ page }) => {
    // Sort by most recent
    await page.selectOption('[data-testid="sort-dropdown"]', 'recent');
    await page.waitForLoadState('networkidle');
    
    // Verify posts are sorted by date
    const firstPostTime = await page.locator('[data-testid="post-timestamp"]:first-child').textContent();
    const secondPostTime = await page.locator('[data-testid="post-timestamp"]:nth-child(2)').textContent();
    
    // Sort by most popular
    await page.selectOption('[data-testid="sort-dropdown"]', 'popular');
    await page.waitForLoadState('networkidle');
    
    // Verify posts are sorted by engagement
    await expect(page.locator('[data-testid="post-item"]:first-child [data-testid="like-count"]')).toBeVisible();
  });

  test('should load more posts with infinite scroll', async ({ page }) => {
    // Get initial post count
    const initialPosts = await page.locator('[data-testid="post-item"]').count();
    
    // Scroll to bottom to trigger load more
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    
    // Wait for new posts to load
    await page.waitForLoadState('networkidle');
    
    // Verify more posts loaded
    const newPostCount = await page.locator('[data-testid="post-item"]').count();
    expect(newPostCount).toBeGreaterThan(initialPosts);
  });

  test('should search posts by content', async ({ page }) => {
    // Use search functionality
    await page.fill('[data-testid="search-posts-input"]', 'Tokyo');
    await page.click('[data-testid="search-posts-button"]');
    
    // Verify search results
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    await expect(page.locator('text=Tokyo')).toBeVisible();
    
    // Clear search
    await page.click('[data-testid="clear-search-button"]');
    await expect(page.locator('[data-testid="search-results"]')).not.toBeVisible();
  });
});

test.describe('Social Features - Moderation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'moderator@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await page.click('[data-testid="nav-social"]');
  });

  test('should report inappropriate content', async ({ page }) => {
    // Report a post
    await page.click('[data-testid="post-menu-button"]:first-child');
    await page.click('[data-testid="report-post-option"]');
    
    // Fill report form
    await page.selectOption('[data-testid="report-reason"]', 'inappropriate-content');
    await page.fill('[data-testid="report-details"]', 'This post contains inappropriate language and violates community guidelines.');
    await page.click('[data-testid="submit-report-button"]');
    
    // Verify report submission
    await expect(page.locator('text=Report submitted successfully')).toBeVisible();
    await expect(page.locator('text=Thank you for helping keep our community safe')).toBeVisible();
  });

  test('should block and unblock users', async ({ page }) => {
    // Block a user
    await page.click('[data-testid="post-author-menu"]:first-child');
    await page.click('[data-testid="block-user-option"]');
    
    // Confirm block action
    await page.click('[data-testid="confirm-block-button"]');
    
    // Verify user is blocked
    await expect(page.locator('text=User blocked successfully')).toBeVisible();
    
    // Navigate to blocked users list
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="privacy-settings"]');
    await page.click('[data-testid="blocked-users-tab"]');
    
    // Verify user appears in blocked list
    await expect(page.locator('[data-testid="blocked-user-item"]')).toBeVisible();
    
    // Unblock the user
    await page.click('[data-testid="unblock-user-button"]');
    await expect(page.locator('text=User unblocked successfully')).toBeVisible();
  });

  test('should hide posts from specific users', async ({ page }) => {
    // Hide posts from a user
    await page.click('[data-testid="post-author-menu"]:first-child');
    await page.click('[data-testid="hide-posts-option"]');
    
    // Confirm hide action
    await page.click('[data-testid="confirm-hide-button"]');
    
    // Verify posts are hidden
    await expect(page.locator('text=Posts from this user will no longer appear in your feed')).toBeVisible();
    
    // Refresh feed and verify posts are not visible
    await page.reload();
    await page.click('[data-testid="nav-social"]');
    
    // The specific user's posts should not be visible
    await expect(page.locator('[data-testid="hidden-content-indicator"]')).not.toBeVisible();
  });
});

test.describe('Social Features - User Following', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.click('text=Sign In');
    await page.fill('[data-testid="login-email"]', 'social-user@example.com');
    await page.fill('[data-testid="login-password"]', 'password123');
    await page.click('[data-testid="login-button"]');
  });

  test('should follow and unfollow users', async ({ page }) => {
    // Navigate to a user's profile
    await page.click('[data-testid="nav-social"]');
    await page.click('[data-testid="post-author-link"]:first-child');
    
    // Follow the user
    await page.click('[data-testid="follow-user-button"]');
    
    // Verify follow status
    await expect(page.locator('[data-testid="follow-user-button"]')).toContainText('Following');
    await expect(page.locator('text=You are now following this user')).toBeVisible();
    
    // Unfollow the user
    await page.click('[data-testid="follow-user-button"]');
    await page.click('[data-testid="confirm-unfollow-button"]');
    
    // Verify unfollow status
    await expect(page.locator('[data-testid="follow-user-button"]')).toContainText('Follow');
    await expect(page.locator('text=You unfollowed this user')).toBeVisible();
  });

  test('should view followers and following lists', async ({ page }) => {
    // Navigate to own profile
    await page.click('[data-testid="user-menu"]');
    await page.click('[data-testid="my-profile"]');
    
    // View followers
    await page.click('[data-testid="followers-count"]');
    await expect(page.locator('[data-testid="followers-modal"]')).toBeVisible();
    await expect(page.locator('[data-testid="follower-item"]')).toBeVisible();
    
    // Close followers modal
    await page.click('[data-testid="close-followers-modal"]');
    
    // View following
    await page.click('[data-testid="following-count"]');
    await expect(page.locator('[data-testid="following-modal"]')).toBeVisible();
    await expect(page.locator('[data-testid="following-item"]')).toBeVisible();
    
    // Close following modal
    await page.click('[data-testid="close-following-modal"]');
  });

  test('should see posts from followed users in feed', async ({ page }) => {
    // Navigate to social feed
    await page.click('[data-testid="nav-social"]');
    
    // Switch to "Following" feed
    await page.click('[data-testid="following-feed-tab"]');
    
    // Verify only posts from followed users appear
    await expect(page.locator('[data-testid="post-item"]')).toBeVisible();
    await expect(page.locator('[data-testid="following-indicator"]')).toBeVisible();
    
    // Switch back to "All" feed
    await page.click('[data-testid="all-feed-tab"]');
    
    // Verify all posts appear
    await expect(page.locator('[data-testid="post-item"]')).toBeVisible();
  });
});
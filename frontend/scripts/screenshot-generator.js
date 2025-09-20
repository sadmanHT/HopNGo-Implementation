const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// Configuration
const BASE_URL = 'http://localhost:3000';
const SCREENSHOT_DIR = path.join(__dirname, '../public/screenshots');
const DEMO_MODE = '?demo=1&demo-user=traveler';

// Screen sizes for responsive screenshots
const SCREEN_SIZES = {
  desktop: { width: 1920, height: 1080, name: 'desktop' },
  tablet: { width: 768, height: 1024, name: 'tablet' },
  mobile: { width: 375, height: 812, name: 'mobile' }
};

// Pages to screenshot
const PAGES = [
  {
    name: 'home',
    path: '/dashboard',
    title: 'Home Dashboard',
    waitFor: '[data-testid="dashboard-content"], .dashboard-grid, main',
    actions: []
  },
  {
    name: 'discover',
    path: '/explore',
    title: 'Discover Destinations',
    waitFor: '[data-testid="explore-content"], .destination-grid, .explore-section',
    actions: []
  },
  {
    name: 'map-heatmap',
    path: '/map',
    title: 'Interactive Map & Heatmap',
    waitFor: '[data-testid="map-container"], .map-wrapper, #map',
    actions: [
      { type: 'wait', duration: 3000 } // Wait for map to load
    ]
  },
  {
    name: 'visual-search',
    path: '/search?type=visual',
    title: 'Visual Search',
    waitFor: '[data-testid="visual-search"], .visual-search-container, .search-interface',
    actions: []
  },
  {
    name: 'listing-detail',
    path: '/destinations/demo-dest-001',
    title: 'Destination Detail - Srimangal Tea Gardens',
    waitFor: '[data-testid="destination-detail"], .destination-header, .listing-content',
    actions: [
      { type: 'scroll', y: 500 },
      { type: 'wait', duration: 1000 }
    ]
  },
  {
    name: 'checkout',
    path: '/checkout',
    title: 'Booking Checkout',
    waitFor: '[data-testid="checkout-form"], .checkout-container, .payment-section',
    actions: []
  },
  {
    name: 'chat',
    path: '/messages',
    title: 'Chat & Messages',
    waitFor: '[data-testid="messages-container"], .chat-interface, .conversation-list',
    actions: []
  },
  {
    name: 'itinerary',
    path: '/itinerary/demo-itin-001',
    title: 'AI-Generated Itinerary',
    waitFor: '[data-testid="itinerary-content"], .itinerary-timeline, .trip-plan',
    actions: [
      { type: 'scroll', y: 300 }
    ]
  },
  {
    name: 'provider-analytics',
    path: '/provider/analytics',
    title: 'Provider Analytics Dashboard',
    waitFor: '[data-testid="analytics-dashboard"], .analytics-grid, .stats-container',
    actions: [],
    demoUser: 'provider'
  },
  {
    name: 'admin-moderation',
    path: '/admin/moderation',
    title: 'Admin Moderation Panel',
    waitFor: '[data-testid="admin-panel"], .moderation-interface, .admin-content',
    actions: [],
    requiresAdmin: true
  },
  {
    name: 'marketplace',
    path: '/marketplace',
    title: 'Travel Gear Marketplace',
    waitFor: '[data-testid="marketplace-grid"], .product-grid, .marketplace-content',
    actions: []
  },
  {
    name: 'profile',
    path: '/profile',
    title: 'User Profile',
    waitFor: '[data-testid="profile-content"], .profile-header, .user-stats',
    actions: []
  }
];

// Utility functions
const ensureDirectoryExists = (dirPath) => {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
};

const sanitizeFilename = (filename) => {
  return filename.replace(/[^a-z0-9-]/gi, '-').toLowerCase();
};

const performAction = async (page, action) => {
  switch (action.type) {
    case 'wait':
      await page.waitForTimeout(action.duration);
      break;
    case 'scroll':
      await page.evaluate((y) => window.scrollTo(0, y), action.y);
      await page.waitForTimeout(500); // Wait for scroll to complete
      break;
    case 'click':
      await page.click(action.selector);
      await page.waitForTimeout(1000);
      break;
    case 'hover':
      await page.hover(action.selector);
      await page.waitForTimeout(500);
      break;
  }
};

// Main screenshot function
const takeScreenshot = async (browser, pageConfig, screenSize, demoUser = 'traveler') => {
  const page = await browser.newPage();
  
  // Set viewport
  await page.setViewport({
    width: screenSize.width,
    height: screenSize.height,
    deviceScaleFactor: screenSize.name === 'mobile' ? 2 : 1
  });
  
  try {
    // Construct URL with demo mode
    const demoParam = `?demo=1&demo-user=${demoUser}`;
    const url = `${BASE_URL}${pageConfig.path}${pageConfig.path.includes('?') ? '&' : '?'}demo=1&demo-user=${demoUser}`;
    
    console.log(`📸 Taking screenshot: ${pageConfig.name} (${screenSize.name}) - ${url}`);
    
    // Navigate to page
    await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
    
    // Wait for specific elements to load
    try {
      await page.waitForSelector(pageConfig.waitFor, { timeout: 10000 });
    } catch (error) {
      console.warn(`⚠️  Warning: Could not find selector ${pageConfig.waitFor} for ${pageConfig.name}`);
      // Continue anyway, might still get a useful screenshot
    }
    
    // Wait for any loading states to complete
    await page.waitForTimeout(2000);
    
    // Perform any custom actions
    for (const action of pageConfig.actions) {
      await performAction(page, action);
    }
    
    // Hide demo banner for cleaner screenshots
    await page.addStyleTag({
      content: `
        .demo-mode .fixed.top-0 { display: none !important; }
        body.demo-mode { padding-top: 0 !important; }
        .demo-mode .pt-12 { padding-top: 0 !important; }
      `
    });
    
    // Take screenshot
    const filename = `${sanitizeFilename(pageConfig.name)}-${screenSize.name}.png`;
    const filepath = path.join(SCREENSHOT_DIR, filename);
    
    await page.screenshot({
      path: filepath,
      fullPage: screenSize.name === 'desktop', // Full page for desktop, viewport for mobile/tablet
      quality: 90
    });
    
    console.log(`✅ Screenshot saved: ${filename}`);
    
    return {
      page: pageConfig.name,
      size: screenSize.name,
      filename,
      filepath,
      title: pageConfig.title,
      url
    };
    
  } catch (error) {
    console.error(`❌ Error taking screenshot for ${pageConfig.name} (${screenSize.name}):`, error.message);
    return null;
  } finally {
    await page.close();
  }
};

// Generate screenshot manifest
const generateManifest = (screenshots) => {
  const manifest = {
    generated: new Date().toISOString(),
    baseUrl: BASE_URL,
    demoMode: true,
    totalScreenshots: screenshots.length,
    screenSizes: Object.keys(SCREEN_SIZES),
    pages: PAGES.map(p => p.name),
    screenshots: screenshots.filter(s => s !== null).map(s => ({
      page: s.page,
      title: s.title,
      size: s.size,
      filename: s.filename,
      url: s.url
    }))
  };
  
  const manifestPath = path.join(SCREENSHOT_DIR, 'manifest.json');
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
  
  console.log(`📋 Screenshot manifest saved: ${manifestPath}`);
  return manifest;
};

// Generate HTML gallery
const generateGallery = (manifest) => {
  const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HopNGo Screenshots Gallery</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f5f5;
        }
        .header {
            text-align: center;
            margin-bottom: 40px;
        }
        .header h1 {
            color: #333;
            margin-bottom: 10px;
        }
        .header p {
            color: #666;
            font-size: 16px;
        }
        .stats {
            display: flex;
            justify-content: center;
            gap: 30px;
            margin: 20px 0;
            flex-wrap: wrap;
        }
        .stat {
            text-align: center;
            padding: 15px;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .stat-number {
            font-size: 24px;
            font-weight: bold;
            color: #10b981;
        }
        .stat-label {
            font-size: 14px;
            color: #666;
            margin-top: 5px;
        }
        .gallery {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 30px;
            max-width: 1400px;
            margin: 0 auto;
        }
        .page-section {
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .page-title {
            font-size: 18px;
            font-weight: 600;
            color: #333;
            margin-bottom: 15px;
            border-bottom: 2px solid #10b981;
            padding-bottom: 10px;
        }
        .screenshots {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }
        .screenshot {
            border: 1px solid #e5e5e5;
            border-radius: 8px;
            overflow: hidden;
            transition: transform 0.2s;
        }
        .screenshot:hover {
            transform: scale(1.02);
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        }
        .screenshot img {
            width: 100%;
            height: auto;
            display: block;
        }
        .screenshot-info {
            padding: 10px;
            background: #f8f9fa;
            font-size: 14px;
            color: #666;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .size-badge {
            background: #10b981;
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 500;
        }
        .desktop { background: #3b82f6; }
        .tablet { background: #f59e0b; }
        .mobile { background: #ef4444; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🎭 HopNGo Screenshots Gallery</h1>
        <p>Demo Mode Screenshots - Generated on ${new Date(manifest.generated).toLocaleString()}</p>
        
        <div class="stats">
            <div class="stat">
                <div class="stat-number">${manifest.totalScreenshots}</div>
                <div class="stat-label">Total Screenshots</div>
            </div>
            <div class="stat">
                <div class="stat-number">${manifest.pages.length}</div>
                <div class="stat-label">Pages Captured</div>
            </div>
            <div class="stat">
                <div class="stat-number">${manifest.screenSizes.length}</div>
                <div class="stat-label">Screen Sizes</div>
            </div>
        </div>
    </div>
    
    <div class="gallery">
        ${manifest.pages.map(pageName => {
          const pageScreenshots = manifest.screenshots.filter(s => s.page === pageName);
          if (pageScreenshots.length === 0) return '';
          
          const pageTitle = pageScreenshots[0].title;
          
          return `
            <div class="page-section">
                <div class="page-title">${pageTitle}</div>
                <div class="screenshots">
                    ${pageScreenshots.map(screenshot => `
                        <div class="screenshot">
                            <img src="${screenshot.filename}" alt="${screenshot.title} - ${screenshot.size}" loading="lazy">
                            <div class="screenshot-info">
                                <span>${screenshot.size}</span>
                                <span class="size-badge ${screenshot.size}">${screenshot.size.toUpperCase()}</span>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
          `;
        }).join('')}
    </div>
</body>
</html>
  `;
  
  const galleryPath = path.join(SCREENSHOT_DIR, 'gallery.html');
  fs.writeFileSync(galleryPath, html);
  
  console.log(`🖼️  Screenshot gallery saved: ${galleryPath}`);
};

// Main execution function
const generateScreenshots = async () => {
  console.log('🚀 Starting HopNGo screenshot generation...');
  console.log(`📁 Screenshots will be saved to: ${SCREENSHOT_DIR}`);
  
  // Ensure screenshot directory exists
  ensureDirectoryExists(SCREENSHOT_DIR);
  
  // Launch browser
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-web-security']
  });
  
  const screenshots = [];
  
  try {
    // Take screenshots for each page and screen size
    for (const pageConfig of PAGES) {
      for (const [sizeName, screenSize] of Object.entries(SCREEN_SIZES)) {
        const demoUser = pageConfig.demoUser || 'traveler';
        
        // Skip admin pages if not specifically testing admin
        if (pageConfig.requiresAdmin && demoUser !== 'admin') {
          console.log(`⏭️  Skipping ${pageConfig.name} (requires admin access)`);
          continue;
        }
        
        const result = await takeScreenshot(browser, pageConfig, screenSize, demoUser);
        if (result) {
          screenshots.push(result);
        }
        
        // Small delay between screenshots
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
    }
    
    // Generate manifest and gallery
    const manifest = generateManifest(screenshots);
    generateGallery(manifest);
    
    console.log(`\n✅ Screenshot generation complete!`);
    console.log(`📊 Generated ${screenshots.length} screenshots across ${PAGES.length} pages`);
    console.log(`🖼️  View gallery: ${path.join(SCREENSHOT_DIR, 'gallery.html')}`);
    
  } catch (error) {
    console.error('❌ Error during screenshot generation:', error);
  } finally {
    await browser.close();
  }
};

// CLI execution
if (require.main === module) {
  generateScreenshots().catch(console.error);
}

module.exports = {
  generateScreenshots,
  PAGES,
  SCREEN_SIZES
};
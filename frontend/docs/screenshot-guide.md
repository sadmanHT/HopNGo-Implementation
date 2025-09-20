# HopNGo Screenshot Guide

## Manual Screenshot Instructions

Since automated screenshot generation is having dependency issues, here's a manual guide to capture all required screenshots for the submission package.

### Demo Mode Setup

1. **Start the development server**: `npm run dev`
2. **Enable demo mode**: Add `?demo=1&demo-user=traveler` to any URL
3. **Switch users**: Use `?demo=1&demo-user=provider` for provider views

### Required Screenshots

Capture each page at three screen sizes:
- **Desktop**: 1920x1080 (full browser window)
- **Tablet**: 768x1024 (use browser dev tools)
- **Mobile**: 375x812 (use browser dev tools)

### Pages to Screenshot

#### 1. Home Dashboard
- **URL**: `http://localhost:3000/dashboard?demo=1&demo-user=traveler`
- **Focus**: Main dashboard with travel stats, recent bookings, recommendations
- **Filename**: `home-[size].png`

#### 2. Discover Destinations
- **URL**: `http://localhost:3000/explore?demo=1&demo-user=traveler`
- **Focus**: Destination grid, filters, search functionality
- **Filename**: `discover-[size].png`

#### 3. Interactive Map & Heatmap
- **URL**: `http://localhost:3000/map?demo=1&demo-user=traveler`
- **Focus**: Map interface with location pins and heatmap overlay
- **Wait**: 3-5 seconds for map to fully load
- **Filename**: `map-heatmap-[size].png`

#### 4. Visual Search
- **URL**: `http://localhost:3000/search?type=visual&demo=1&demo-user=traveler`
- **Focus**: Visual search interface, image upload area
- **Filename**: `visual-search-[size].png`

#### 5. Listing Detail - Srimangal Tea Gardens
- **URL**: `http://localhost:3000/destinations/demo-dest-001?demo=1&demo-user=traveler`
- **Focus**: Destination details, image gallery, booking options
- **Scroll**: Capture both header and details sections
- **Filename**: `listing-detail-[size].png`

#### 6. Booking Checkout
- **URL**: `http://localhost:3000/checkout?demo=1&demo-user=traveler`
- **Focus**: Checkout form, payment options, booking summary
- **Filename**: `checkout-[size].png`

#### 7. Chat & Messages
- **URL**: `http://localhost:3000/messages?demo=1&demo-user=traveler`
- **Focus**: Chat interface with demo conversation
- **Filename**: `chat-[size].png`

#### 8. AI-Generated Itinerary
- **URL**: `http://localhost:3000/itinerary/demo-itin-001?demo=1&demo-user=traveler`
- **Focus**: Itinerary timeline, day-by-day plan
- **Filename**: `itinerary-[size].png`

#### 9. Provider Analytics Dashboard
- **URL**: `http://localhost:3000/provider/analytics?demo=1&demo-user=provider`
- **Focus**: Analytics charts, booking stats, revenue data
- **User**: Switch to provider demo user
- **Filename**: `provider-analytics-[size].png`

#### 10. Admin Moderation Panel
- **URL**: `http://localhost:3000/admin/moderation?demo=1&demo-user=provider`
- **Focus**: Content moderation interface, flagged items
- **Note**: May require admin access setup
- **Filename**: `admin-moderation-[size].png`

#### 11. Travel Gear Marketplace
- **URL**: `http://localhost:3000/marketplace?demo=1&demo-user=traveler`
- **Focus**: Product grid, shopping interface
- **Filename**: `marketplace-[size].png`

#### 12. User Profile
- **URL**: `http://localhost:3000/profile?demo=1&demo-user=traveler`
- **Focus**: User profile, travel stats, preferences
- **Filename**: `profile-[size].png`

### Screenshot Best Practices

1. **Clean Interface**: Hide demo banner by adding this CSS in dev tools:
   ```css
   .demo-mode .fixed.top-0 { display: none !important; }
   body.demo-mode { padding-top: 0 !important; }
   ```

2. **Consistent Timing**: Wait 2-3 seconds after page load for all content to render

3. **Mobile Screenshots**: Use Chrome DevTools device emulation:
   - Press F12 → Click device icon → Select iPhone 12 Pro or similar
   - Set custom size: 375x812

4. **Tablet Screenshots**: Use iPad dimensions (768x1024)

5. **Full Page vs Viewport**:
   - Desktop: Capture full page (scroll if needed)
   - Mobile/Tablet: Capture viewport only

### File Organization

Save all screenshots to: `frontend/public/screenshots/`

```
screenshots/
├── home-desktop.png
├── home-tablet.png
├── home-mobile.png
├── discover-desktop.png
├── discover-tablet.png
├── discover-mobile.png
└── ... (continue pattern)
```

### Quality Settings

- **Format**: PNG for UI screenshots
- **Quality**: High (90-100%)
- **Resolution**: Native screen resolution
- **File Size**: Optimize but maintain clarity

### Automated Alternative

Once npm issues are resolved, run:
```bash
node scripts/screenshot-generator.js
```

This will generate all screenshots automatically and create an HTML gallery.

### Demo Story Flow

For the demo video, follow this user journey:
1. **Home** → Browse destinations
2. **Visual Search** → Upload tea garden photo
3. **Search Results** → Find Srimangal
4. **Destination Detail** → View tea garden resort
5. **AI Itinerary** → Generate 3-day plan
6. **Booking** → Book accommodation
7. **Confirmation** → Receive booking confirmation
8. **Messages** → Chat with host
9. **Weather** → Check weather update
10. **Profile** → View saved itinerary

This flow demonstrates the core hackathon story: "Tea garden photo → Visual search → Srimangal → AI plan 3 days → Book cozy stay → Notification → Chat host → Weather tip → Saved route → Emergency safety."
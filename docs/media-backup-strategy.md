# HopNGo Media Backup Strategy

## Overview

This document outlines the backup and recovery strategy for media assets stored in external cloud services (Cloudinary and Firebase Storage). Unlike database backups that are handled by our automated backup system, media backups require different approaches due to the nature of cloud storage services.

## Media Storage Architecture

### Cloudinary (Primary Image Storage)
- **Usage**: User profile pictures, property images, thumbnails
- **Storage**: Cloudinary cloud storage with CDN delivery
- **Transformations**: On-the-fly image processing and optimization
- **Backup Strategy**: Export URLs + metadata, lifecycle policies

### Firebase Storage (Document Storage)
- **Usage**: User documents, property documents, contracts
- **Storage**: Google Cloud Storage buckets via Firebase
- **Security**: Firebase security rules and authentication
- **Backup Strategy**: Export files + metadata, lifecycle management

## Backup Strategies

### 1. Cloudinary Backup Strategy

#### 1.1 Automated Metadata Export

**Daily Export Script** (`scripts/cloudinary-export.js`):
```javascript
#!/usr/bin/env node
const cloudinary = require('cloudinary').v2;
const fs = require('fs');
const path = require('path');

// Configure Cloudinary
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

async function exportCloudinaryAssets() {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const exportDir = `/backups/media/cloudinary/${timestamp}`;
  
  // Create export directory
  fs.mkdirSync(exportDir, { recursive: true });
  
  try {
    // Export all resources with metadata
    const resources = await cloudinary.api.resources({
      type: 'upload',
      max_results: 500,
      metadata: true,
      context: true,
      tags: true
    });
    
    // Save metadata to JSON file
    const metadata = {
      export_date: new Date().toISOString(),
      total_resources: resources.resources.length,
      resources: resources.resources.map(resource => ({
        public_id: resource.public_id,
        version: resource.version,
        format: resource.format,
        resource_type: resource.resource_type,
        type: resource.type,
        created_at: resource.created_at,
        bytes: resource.bytes,
        width: resource.width,
        height: resource.height,
        url: resource.url,
        secure_url: resource.secure_url,
        tags: resource.tags,
        context: resource.context,
        metadata: resource.metadata
      }))
    };
    
    fs.writeFileSync(
      path.join(exportDir, 'cloudinary-metadata.json'),
      JSON.stringify(metadata, null, 2)
    );
    
    console.log(`Exported ${resources.resources.length} Cloudinary assets to ${exportDir}`);
    
    // Generate download URLs for critical assets
    const criticalAssets = resources.resources.filter(r => 
      r.tags && (r.tags.includes('critical') || r.tags.includes('profile'))
    );
    
    const downloadUrls = criticalAssets.map(asset => ({
      public_id: asset.public_id,
      download_url: cloudinary.url(asset.public_id, {
        sign_url: true,
        type: 'authenticated',
        expires_at: Math.floor(Date.now() / 1000) + 86400 // 24 hours
      })
    }));
    
    fs.writeFileSync(
      path.join(exportDir, 'critical-assets-urls.json'),
      JSON.stringify(downloadUrls, null, 2)
    );
    
  } catch (error) {
    console.error('Cloudinary export failed:', error);
    process.exit(1);
  }
}

exportCloudinaryAssets();
```

#### 1.2 Cloudinary Lifecycle Policies

**Backup Retention Configuration**:
```javascript
// Configure auto-backup in Cloudinary dashboard
const backupConfig = {
  // Enable auto-backup for critical folders
  auto_backup: {
    enabled: true,
    folders: ['profiles', 'properties', 'documents'],
    frequency: 'daily',
    retention_days: 90
  },
  
  // Configure lifecycle rules
  lifecycle_rules: [
    {
      condition: { tags: ['temporary'] },
      action: { delete_after_days: 30 }
    },
    {
      condition: { tags: ['archive'] },
      action: { move_to_cold_storage: true }
    }
  ]
};
```

#### 1.3 Critical Asset Download

**Download Script** (`scripts/download-critical-assets.js`):
```javascript
#!/usr/bin/env node
const https = require('https');
const fs = require('fs');
const path = require('path');

async function downloadCriticalAssets(metadataFile) {
  const metadata = JSON.parse(fs.readFileSync(metadataFile, 'utf8'));
  const downloadDir = path.dirname(metadataFile) + '/downloads';
  
  fs.mkdirSync(downloadDir, { recursive: true });
  
  for (const asset of metadata.resources) {
    if (asset.tags && (asset.tags.includes('critical') || asset.tags.includes('profile'))) {
      const filename = `${asset.public_id.replace(/\//g, '_')}.${asset.format}`;
      const filepath = path.join(downloadDir, filename);
      
      try {
        await downloadFile(asset.secure_url, filepath);
        console.log(`Downloaded: ${filename}`);
      } catch (error) {
        console.error(`Failed to download ${filename}:`, error.message);
      }
    }
  }
}

function downloadFile(url, filepath) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(filepath);
    https.get(url, (response) => {
      response.pipe(file);
      file.on('finish', () => {
        file.close();
        resolve();
      });
    }).on('error', (error) => {
      fs.unlink(filepath, () => {});
      reject(error);
    });
  });
}

// Usage: node download-critical-assets.js /path/to/cloudinary-metadata.json
if (process.argv[2]) {
  downloadCriticalAssets(process.argv[2]);
}
```

### 2. Firebase Storage Backup Strategy

#### 2.1 Firebase Export Script

**Export Script** (`scripts/firebase-export.js`):
```javascript
#!/usr/bin/env node
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin
const serviceAccount = require('../config/firebase-service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: process.env.FIREBASE_STORAGE_BUCKET
});

const bucket = admin.storage().bucket();

async function exportFirebaseStorage() {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const exportDir = `/backups/media/firebase/${timestamp}`;
  
  fs.mkdirSync(exportDir, { recursive: true });
  
  try {
    // List all files in the bucket
    const [files] = await bucket.getFiles();
    
    const fileMetadata = [];
    
    for (const file of files) {
      const [metadata] = await file.getMetadata();
      
      fileMetadata.push({
        name: file.name,
        bucket: file.bucket.name,
        generation: metadata.generation,
        metageneration: metadata.metageneration,
        contentType: metadata.contentType,
        size: metadata.size,
        md5Hash: metadata.md5Hash,
        crc32c: metadata.crc32c,
        etag: metadata.etag,
        created: metadata.timeCreated,
        updated: metadata.updated,
        downloadUrl: await file.getSignedUrl({
          action: 'read',
          expires: Date.now() + 24 * 60 * 60 * 1000 // 24 hours
        })[0]
      });
    }
    
    // Save metadata
    const exportData = {
      export_date: new Date().toISOString(),
      bucket_name: bucket.name,
      total_files: files.length,
      total_size: fileMetadata.reduce((sum, file) => sum + parseInt(file.size), 0),
      files: fileMetadata
    };
    
    fs.writeFileSync(
      path.join(exportDir, 'firebase-storage-metadata.json'),
      JSON.stringify(exportData, null, 2)
    );
    
    console.log(`Exported ${files.length} Firebase Storage files to ${exportDir}`);
    
    // Download critical files
    const criticalFiles = files.filter(file => 
      file.name.includes('documents/') || 
      file.name.includes('contracts/') ||
      file.name.includes('legal/')
    );
    
    const downloadDir = path.join(exportDir, 'critical-files');
    fs.mkdirSync(downloadDir, { recursive: true });
    
    for (const file of criticalFiles) {
      const destination = path.join(downloadDir, file.name.replace(/\//g, '_'));
      await file.download({ destination });
      console.log(`Downloaded critical file: ${file.name}`);
    }
    
  } catch (error) {
    console.error('Firebase Storage export failed:', error);
    process.exit(1);
  }
}

exportFirebaseStorage();
```

#### 2.2 Firebase Lifecycle Management

**Lifecycle Configuration** (`scripts/setup-firebase-lifecycle.js`):
```javascript
#!/usr/bin/env node
const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('../config/firebase-service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  storageBucket: process.env.FIREBASE_STORAGE_BUCKET
});

const bucket = admin.storage().bucket();

async function setupLifecycleRules() {
  const lifecycleRules = [
    {
      condition: {
        age: 90, // days
        matchesPrefix: ['temp/', 'cache/']
      },
      action: {
        type: 'Delete'
      }
    },
    {
      condition: {
        age: 365, // days
        matchesPrefix: ['archives/']
      },
      action: {
        type: 'SetStorageClass',
        storageClass: 'COLDLINE'
      }
    },
    {
      condition: {
        age: 30, // days
        matchesPrefix: ['uploads/temp/']
      },
      action: {
        type: 'Delete'
      }
    }
  ];
  
  try {
    await bucket.setMetadata({
      lifecycle: {
        rule: lifecycleRules
      }
    });
    
    console.log('Firebase Storage lifecycle rules configured successfully');
  } catch (error) {
    console.error('Failed to configure lifecycle rules:', error);
  }
}

setupLifecycleRules();
```

## Backup Automation

### Cron Schedule Integration

Add to `infra/compose/backup/crontab`:
```bash
# Media backup exports (daily at 2:00 AM)
0 2 * * * cd /scripts && node cloudinary-export.js
0 2 * * * cd /scripts && node firebase-export.js

# Weekly critical asset download (Sundays at 3:00 AM)
0 3 * * 0 cd /scripts && node download-critical-assets.js /backups/media/cloudinary/latest/cloudinary-metadata.json

# Monthly cleanup of old media exports (1st of month at 5:00 AM)
0 5 1 * * find /backups/media -type d -mtime +90 -exec rm -rf {} \;
```

### Docker Integration

Update `infra/compose/backup/Dockerfile`:
```dockerfile
# Add Node.js for media backup scripts
RUN apk add --no-cache nodejs npm

# Install required packages
RUN npm install -g cloudinary firebase-admin

# Copy media backup scripts
COPY scripts/media/ /scripts/media/
RUN chmod +x /scripts/media/*.js
```

## Recovery Procedures

### 1. Cloudinary Recovery

#### 1.1 Restore from Metadata
```bash
# Navigate to backup directory
cd /backups/media/cloudinary/YYYYMMDD-HHMM/

# Review metadata
cat cloudinary-metadata.json | jq '.resources | length'

# Restore critical assets
node /scripts/media/restore-cloudinary.js cloudinary-metadata.json
```

#### 1.2 Manual Asset Upload
```javascript
// restore-cloudinary.js
const cloudinary = require('cloudinary').v2;
const fs = require('fs');

async function restoreAssets(metadataFile) {
  const metadata = JSON.parse(fs.readFileSync(metadataFile, 'utf8'));
  const downloadDir = './downloads';
  
  for (const asset of metadata.resources) {
    const localFile = `${downloadDir}/${asset.public_id.replace(/\//g, '_')}.${asset.format}`;
    
    if (fs.existsSync(localFile)) {
      try {
        const result = await cloudinary.uploader.upload(localFile, {
          public_id: asset.public_id,
          tags: asset.tags,
          context: asset.context,
          overwrite: true
        });
        
        console.log(`Restored: ${asset.public_id}`);
      } catch (error) {
        console.error(`Failed to restore ${asset.public_id}:`, error.message);
      }
    }
  }
}
```

### 2. Firebase Storage Recovery

#### 2.1 Restore from Backup
```bash
# Navigate to backup directory
cd /backups/media/firebase/YYYYMMDD-HHMM/

# Review metadata
cat firebase-storage-metadata.json | jq '.total_files'

# Restore critical files
node /scripts/media/restore-firebase.js firebase-storage-metadata.json
```

#### 2.2 Bulk Upload Script
```javascript
// restore-firebase.js
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

async function restoreFiles(metadataFile) {
  const metadata = JSON.parse(fs.readFileSync(metadataFile, 'utf8'));
  const bucket = admin.storage().bucket();
  const criticalDir = './critical-files';
  
  for (const fileInfo of metadata.files) {
    const localFile = path.join(criticalDir, fileInfo.name.replace(/\//g, '_'));
    
    if (fs.existsSync(localFile)) {
      try {
        await bucket.upload(localFile, {
          destination: fileInfo.name,
          metadata: {
            contentType: fileInfo.contentType,
            metadata: {
              originalName: fileInfo.name,
              restoredAt: new Date().toISOString()
            }
          }
        });
        
        console.log(`Restored: ${fileInfo.name}`);
      } catch (error) {
        console.error(`Failed to restore ${fileInfo.name}:`, error.message);
      }
    }
  }
}
```

## Monitoring and Alerting

### Backup Health Checks

```bash
#!/bin/bash
# media-backup-health-check.sh

# Check if recent backups exist
CLOUDINARY_BACKUP=$(find /backups/media/cloudinary -name "*.json" -mtime -1 | wc -l)
FIREBASE_BACKUP=$(find /backups/media/firebase -name "*.json" -mtime -1 | wc -l)

if [ $CLOUDINARY_BACKUP -eq 0 ]; then
  echo "WARNING: No recent Cloudinary backup found"
  # Send alert
fi

if [ $FIREBASE_BACKUP -eq 0 ]; then
  echo "WARNING: No recent Firebase backup found"
  # Send alert
fi

echo "Media backup health check completed"
```

### Storage Usage Monitoring

```javascript
// media-usage-monitor.js
const cloudinary = require('cloudinary').v2;
const admin = require('firebase-admin');

async function checkStorageUsage() {
  // Check Cloudinary usage
  const cloudinaryUsage = await cloudinary.api.usage();
  console.log('Cloudinary Usage:', {
    credits: cloudinaryUsage.credits,
    objects: cloudinaryUsage.objects,
    bandwidth: cloudinaryUsage.bandwidth,
    storage: cloudinaryUsage.storage
  });
  
  // Check Firebase Storage usage
  const bucket = admin.storage().bucket();
  const [files] = await bucket.getFiles();
  const totalSize = files.reduce((sum, file) => {
    return sum + parseInt(file.metadata.size || 0);
  }, 0);
  
  console.log('Firebase Storage Usage:', {
    files: files.length,
    totalSize: `${(totalSize / 1024 / 1024 / 1024).toFixed(2)} GB`
  });
}

checkStorageUsage();
```

## Best Practices

### 1. Data Classification
- **Critical**: User documents, contracts, legal files
- **Important**: Profile pictures, property images
- **Standard**: Generated thumbnails, cached images
- **Temporary**: Upload previews, temporary files

### 2. Retention Policies
- **Critical files**: Permanent retention with multiple backups
- **Important files**: 7-year retention with regular backups
- **Standard files**: 3-year retention with periodic backups
- **Temporary files**: 30-day retention, no backup required

### 3. Security Considerations
- Use signed URLs for temporary access
- Implement proper IAM roles and permissions
- Encrypt sensitive files at rest
- Regular security audits of access patterns

### 4. Cost Optimization
- Use lifecycle policies to move old files to cheaper storage
- Implement automatic cleanup of temporary files
- Monitor usage and set up billing alerts
- Use CDN caching to reduce bandwidth costs

## Emergency Procedures

### Complete Media Loss Scenario

1. **Immediate Actions**
   - Assess scope of data loss
   - Notify stakeholders
   - Prevent further data loss

2. **Recovery Steps**
   - Identify most recent backup
   - Restore critical files first
   - Restore important files second
   - Rebuild thumbnails and cached content

3. **Validation**
   - Verify file integrity
   - Test application functionality
   - Confirm user access to media

4. **Post-Recovery**
   - Document lessons learned
   - Improve backup procedures
   - Update monitoring and alerting

---

**Document Version**: 1.0  
**Last Updated**: $(date)  
**Next Review**: $(date -d "+6 months")  
**Owner**: Infrastructure Team
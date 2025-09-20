#!/usr/bin/env node

/**
 * HopNGo SEO Validation Script
 * Validates SEO metadata, sitemaps, and search engine optimization
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Colors for console output
const colors = {
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  reset: '\x1b[0m',
  bold: '\x1b[1m'
};

const log = {
  success: (msg) => console.log(`${colors.green}✅ ${msg}${colors.reset}`),
  error: (msg) => console.log(`${colors.red}❌ ${msg}${colors.reset}`),
  warning: (msg) => console.log(`${colors.yellow}⚠️  ${msg}${colors.reset}`),
  info: (msg) => console.log(`${colors.blue}ℹ️  ${msg}${colors.reset}`),
  header: (msg) => console.log(`\n${colors.bold}${colors.blue}🔍 ${msg}${colors.reset}\n`)
};

class SEOValidator {
  constructor() {
    this.errors = [];
    this.warnings = [];
    this.successes = [];
    this.baseUrl = 'http://localhost:3000';
    this.frontendPath = path.join(__dirname, '../frontend');
  }

  // Validate meta tags in HTML files
  validateMetaTags() {
    log.header('Validating Meta Tags');
    
    const requiredMetaTags = [
      { name: 'description', required: true },
      { name: 'keywords', required: true },
      { property: 'og:title', required: true },
      { property: 'og:description', required: true },
      { property: 'og:image', required: true },
      { property: 'og:url', required: true },
      { property: 'og:type', required: true },
      { name: 'twitter:card', required: true },
      { name: 'twitter:title', required: true },
      { name: 'twitter:description', required: true },
      { name: 'twitter:image', required: true },
      { name: 'viewport', required: true },
      { name: 'robots', required: false },
      { name: 'author', required: false },
      { name: 'theme-color', required: false }
    ];

    // Check layout files for meta tags
    const layoutFiles = [
      path.join(this.frontendPath, 'src/app/layout.tsx'),
      path.join(this.frontendPath, 'src/app/[locale]/(app)/layout.tsx'),
      path.join(this.frontendPath, 'public/index.html')
    ];

    layoutFiles.forEach(filePath => {
      if (fs.existsSync(filePath)) {
        const content = fs.readFileSync(filePath, 'utf8');
        
        requiredMetaTags.forEach(tag => {
          const metaPattern = tag.name 
            ? new RegExp(`<meta\\s+name=["']${tag.name}["']`, 'i')
            : new RegExp(`<meta\\s+property=["']${tag.property}["']`, 'i');
          
          if (metaPattern.test(content)) {
            this.successes.push(`Meta tag found: ${tag.name || tag.property}`);
            log.success(`Meta tag found: ${tag.name || tag.property}`);
          } else if (tag.required) {
            this.errors.push(`Missing required meta tag: ${tag.name || tag.property}`);
            log.error(`Missing required meta tag: ${tag.name || tag.property}`);
          } else {
            this.warnings.push(`Optional meta tag missing: ${tag.name || tag.property}`);
            log.warning(`Optional meta tag missing: ${tag.name || tag.property}`);
          }
        });
      }
    });
  }

  // Validate structured data (JSON-LD)
  validateStructuredData() {
    log.header('Validating Structured Data');
    
    const structuredDataTypes = [
      'Organization',
      'WebSite',
      'BreadcrumbList',
      'TravelAgency',
      'LocalBusiness'
    ];

    const layoutFiles = [
      path.join(this.frontendPath, 'src/app/layout.tsx'),
      path.join(this.frontendPath, 'src/components/seo')
    ];

    let foundStructuredData = false;

    layoutFiles.forEach(filePath => {
      if (fs.existsSync(filePath)) {
        const isDirectory = fs.statSync(filePath).isDirectory();
        
        if (isDirectory) {
          const files = fs.readdirSync(filePath);
          files.forEach(file => {
            const fullPath = path.join(filePath, file);
            if (fs.statSync(fullPath).isFile()) {
              const content = fs.readFileSync(fullPath, 'utf8');
              if (content.includes('application/ld+json')) {
                foundStructuredData = true;
                log.success(`Structured data found in: ${file}`);
              }
            }
          });
        } else {
          const content = fs.readFileSync(filePath, 'utf8');
          if (content.includes('application/ld+json')) {
            foundStructuredData = true;
            log.success(`Structured data found in: ${path.basename(filePath)}`);
          }
        }
      }
    });

    if (!foundStructuredData) {
      this.warnings.push('No structured data (JSON-LD) found');
      log.warning('No structured data (JSON-LD) found');
    }
  }

  // Validate sitemap.xml
  validateSitemap() {
    log.header('Validating Sitemap');
    
    const sitemapPath = path.join(this.frontendPath, 'public/sitemap.xml');
    
    if (fs.existsSync(sitemapPath)) {
      const sitemapContent = fs.readFileSync(sitemapPath, 'utf8');
      
      // Basic XML validation
      if (sitemapContent.includes('<?xml') && sitemapContent.includes('<urlset')) {
        log.success('Sitemap.xml found and appears valid');
        
        // Count URLs in sitemap
        const urlMatches = sitemapContent.match(/<url>/g);
        const urlCount = urlMatches ? urlMatches.length : 0;
        log.info(`Sitemap contains ${urlCount} URLs`);
        
        // Check for required elements
        if (sitemapContent.includes('<loc>')) {
          log.success('Sitemap contains <loc> elements');
        } else {
          this.errors.push('Sitemap missing <loc> elements');
          log.error('Sitemap missing <loc> elements');
        }
        
        if (sitemapContent.includes('<lastmod>')) {
          log.success('Sitemap contains <lastmod> elements');
        } else {
          this.warnings.push('Sitemap missing <lastmod> elements');
          log.warning('Sitemap missing <lastmod> elements');
        }
        
      } else {
        this.errors.push('Sitemap.xml appears to be malformed');
        log.error('Sitemap.xml appears to be malformed');
      }
    } else {
      this.errors.push('Sitemap.xml not found');
      log.error('Sitemap.xml not found');
    }
  }

  // Validate robots.txt
  validateRobotsTxt() {
    log.header('Validating Robots.txt');
    
    const robotsPath = path.join(this.frontendPath, 'public/robots.txt');
    
    if (fs.existsSync(robotsPath)) {
      const robotsContent = fs.readFileSync(robotsPath, 'utf8');
      
      if (robotsContent.includes('User-agent:')) {
        log.success('Robots.txt found and contains User-agent directive');
        
        if (robotsContent.includes('Sitemap:')) {
          log.success('Robots.txt contains Sitemap reference');
        } else {
          this.warnings.push('Robots.txt missing Sitemap reference');
          log.warning('Robots.txt missing Sitemap reference');
        }
        
        if (robotsContent.includes('Disallow:')) {
          log.success('Robots.txt contains Disallow directives');
        } else {
          this.warnings.push('Robots.txt missing Disallow directives');
          log.warning('Robots.txt missing Disallow directives');
        }
        
      } else {
        this.errors.push('Robots.txt appears to be malformed');
        log.error('Robots.txt appears to be malformed');
      }
    } else {
      this.warnings.push('Robots.txt not found');
      log.warning('Robots.txt not found');
    }
  }

  // Validate page titles and headings
  validatePageStructure() {
    log.header('Validating Page Structure');
    
    const pageFiles = this.findPageFiles();
    
    pageFiles.forEach(filePath => {
      const content = fs.readFileSync(filePath, 'utf8');
      const fileName = path.basename(filePath);
      
      // Check for title tags or title metadata
      if (content.includes('<title>') || content.includes('title:') || content.includes('metadata')) {
        log.success(`Title found in: ${fileName}`);
      } else {
        this.warnings.push(`No title found in: ${fileName}`);
        log.warning(`No title found in: ${fileName}`);
      }
      
      // Check for H1 tags
      if (content.includes('<h1') || content.includes('className="h1"') || content.includes('text-4xl')) {
        log.success(`H1 heading found in: ${fileName}`);
      } else {
        this.warnings.push(`No H1 heading found in: ${fileName}`);
        log.warning(`No H1 heading found in: ${fileName}`);
      }
    });
  }

  // Find all page files
  findPageFiles() {
    const pageFiles = [];
    const appDir = path.join(this.frontendPath, 'src/app');
    
    if (fs.existsSync(appDir)) {
      this.findFilesRecursively(appDir, pageFiles, ['page.tsx', 'layout.tsx']);
    }
    
    return pageFiles;
  }

  // Recursively find files
  findFilesRecursively(dir, fileList, targetFiles) {
    const files = fs.readdirSync(dir);
    
    files.forEach(file => {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      
      if (stat.isDirectory()) {
        this.findFilesRecursively(filePath, fileList, targetFiles);
      } else if (targetFiles.includes(file)) {
        fileList.push(filePath);
      }
    });
  }

  // Validate image alt texts
  validateImageAltTexts() {
    log.header('Validating Image Alt Texts');
    
    const componentFiles = [];
    const srcDir = path.join(this.frontendPath, 'src');
    
    if (fs.existsSync(srcDir)) {
      this.findFilesRecursively(srcDir, componentFiles, []);
      const tsxFiles = componentFiles.filter(file => file.endsWith('.tsx'));
      
      let totalImages = 0;
      let imagesWithAlt = 0;
      
      tsxFiles.forEach(filePath => {
        const content = fs.readFileSync(filePath, 'utf8');
        const fileName = path.basename(filePath);
        
        // Find img tags
        const imgMatches = content.match(/<img[^>]*>/g) || [];
        // Find Next.js Image components
        const nextImageMatches = content.match(/<Image[^>]*>/g) || [];
        
        const allImages = [...imgMatches, ...nextImageMatches];
        totalImages += allImages.length;
        
        allImages.forEach(imgTag => {
          if (imgTag.includes('alt=')) {
            imagesWithAlt++;
          } else {
            this.errors.push(`Image without alt text in: ${fileName}`);
            log.error(`Image without alt text in: ${fileName}`);
          }
        });
      });
      
      if (totalImages > 0) {
        const altPercentage = Math.round((imagesWithAlt / totalImages) * 100);
        log.info(`Images with alt text: ${imagesWithAlt}/${totalImages} (${altPercentage}%)`);
        
        if (altPercentage === 100) {
          log.success('All images have alt text');
        } else if (altPercentage >= 90) {
          this.warnings.push(`${100 - altPercentage}% of images missing alt text`);
          log.warning(`${100 - altPercentage}% of images missing alt text`);
        } else {
          this.errors.push(`${100 - altPercentage}% of images missing alt text`);
          log.error(`${100 - altPercentage}% of images missing alt text`);
        }
      }
    }
  }

  // Validate internal links
  validateInternalLinks() {
    log.header('Validating Internal Links');
    
    const componentFiles = [];
    const srcDir = path.join(this.frontendPath, 'src');
    
    if (fs.existsSync(srcDir)) {
      this.findFilesRecursively(srcDir, componentFiles, []);
      const tsxFiles = componentFiles.filter(file => file.endsWith('.tsx'));
      
      let totalLinks = 0;
      let validLinks = 0;
      
      tsxFiles.forEach(filePath => {
        const content = fs.readFileSync(filePath, 'utf8');
        const fileName = path.basename(filePath);
        
        // Find Link components and href attributes
        const linkMatches = content.match(/href=["']([^"']*)["']/g) || [];
        
        linkMatches.forEach(linkMatch => {
          const href = linkMatch.match(/href=["']([^"']*)["']/)[1];
          totalLinks++;
          
          // Check if it's an internal link
          if (href.startsWith('/') || href.startsWith('#')) {
            validLinks++;
          } else if (href.startsWith('http')) {
            // External link - could validate if needed
            validLinks++;
          } else {
            this.warnings.push(`Potentially invalid link in ${fileName}: ${href}`);
            log.warning(`Potentially invalid link in ${fileName}: ${href}`);
          }
        });
      });
      
      if (totalLinks > 0) {
        log.info(`Total links found: ${totalLinks}`);
        log.success(`Valid links: ${validLinks}/${totalLinks}`);
      }
    }
  }

  // Generate SEO report
  generateReport() {
    log.header('SEO Validation Report');
    
    console.log(`\n${colors.bold}📊 Summary:${colors.reset}`);
    console.log(`${colors.green}✅ Successes: ${this.successes.length}${colors.reset}`);
    console.log(`${colors.yellow}⚠️  Warnings: ${this.warnings.length}${colors.reset}`);
    console.log(`${colors.red}❌ Errors: ${this.errors.length}${colors.reset}`);
    
    if (this.errors.length > 0) {
      console.log(`\n${colors.bold}${colors.red}🚨 Critical Issues:${colors.reset}`);
      this.errors.forEach(error => console.log(`  • ${error}`));
    }
    
    if (this.warnings.length > 0) {
      console.log(`\n${colors.bold}${colors.yellow}⚠️  Warnings:${colors.reset}`);
      this.warnings.forEach(warning => console.log(`  • ${warning}`));
    }
    
    // Calculate SEO score
    const totalChecks = this.successes.length + this.warnings.length + this.errors.length;
    const score = totalChecks > 0 ? Math.round(((this.successes.length + (this.warnings.length * 0.5)) / totalChecks) * 100) : 0;
    
    console.log(`\n${colors.bold}🎯 SEO Score: ${score}%${colors.reset}`);
    
    if (score >= 90) {
      console.log(`${colors.green}🎉 Excellent SEO optimization!${colors.reset}`);
    } else if (score >= 70) {
      console.log(`${colors.yellow}👍 Good SEO, but room for improvement${colors.reset}`);
    } else {
      console.log(`${colors.red}🔧 SEO needs significant improvement${colors.reset}`);
    }
    
    return {
      score,
      errors: this.errors.length,
      warnings: this.warnings.length,
      successes: this.successes.length
    };
  }

  // Run all validations
  async runAll() {
    console.log(`${colors.bold}${colors.blue}🔍 HopNGo SEO Validation Suite${colors.reset}\n`);
    
    try {
      this.validateMetaTags();
      this.validateStructuredData();
      this.validateSitemap();
      this.validateRobotsTxt();
      this.validatePageStructure();
      this.validateImageAltTexts();
      this.validateInternalLinks();
      
      const report = this.generateReport();
      
      // Save report to file
      const reportPath = path.join(__dirname, '../reports/seo-validation-report.json');
      const reportsDir = path.dirname(reportPath);
      
      if (!fs.existsSync(reportsDir)) {
        fs.mkdirSync(reportsDir, { recursive: true });
      }
      
      fs.writeFileSync(reportPath, JSON.stringify({
        timestamp: new Date().toISOString(),
        score: report.score,
        summary: {
          errors: report.errors,
          warnings: report.warnings,
          successes: report.successes
        },
        details: {
          errors: this.errors,
          warnings: this.warnings,
          successes: this.successes
        }
      }, null, 2));
      
      log.info(`Report saved to: ${reportPath}`);
      
      // Exit with error code if there are critical issues
      if (this.errors.length > 0) {
        process.exit(1);
      }
      
    } catch (error) {
      log.error(`Validation failed: ${error.message}`);
      process.exit(1);
    }
  }
}

// Run validation if called directly
if (require.main === module) {
  const validator = new SEOValidator();
  validator.runAll();
}

module.exports = SEOValidator;
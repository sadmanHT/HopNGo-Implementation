#!/usr/bin/env node

/**
 * HopNGo Final Validation Suite
 * Comprehensive validation for submission readiness
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const SEOValidator = require('./seo-validation');

// Colors for console output
const colors = {
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
  reset: '\x1b[0m',
  bold: '\x1b[1m'
};

const log = {
  success: (msg) => console.log(`${colors.green}✅ ${msg}${colors.reset}`),
  error: (msg) => console.log(`${colors.red}❌ ${msg}${colors.reset}`),
  warning: (msg) => console.log(`${colors.yellow}⚠️  ${msg}${colors.reset}`),
  info: (msg) => console.log(`${colors.blue}ℹ️  ${msg}${colors.reset}`),
  header: (msg) => console.log(`\n${colors.bold}${colors.cyan}🚀 ${msg}${colors.reset}\n`),
  section: (msg) => console.log(`\n${colors.bold}${colors.magenta}📋 ${msg}${colors.reset}\n`)
};

class FinalValidator {
  constructor() {
    this.errors = [];
    this.warnings = [];
    this.successes = [];
    this.projectRoot = path.resolve(__dirname, '..');
    this.frontendPath = path.join(this.projectRoot, 'frontend');
    this.backendPath = this.projectRoot;
  }

  // Validate demo mode implementation
  async validateDemoMode() {
    log.section('Demo Mode Validation');
    
    // Check for demo toggle implementation
    const demoFiles = [
      path.join(this.frontendPath, 'src/lib/demo.ts'),
      path.join(this.frontendPath, 'src/hooks/useDemo.ts'),
      path.join(this.frontendPath, 'src/components/demo')
    ];
    
    let demoImplemented = false;
    
    demoFiles.forEach(filePath => {
      if (fs.existsSync(filePath)) {
        demoImplemented = true;
        log.success(`Demo implementation found: ${path.basename(filePath)}`);
      }
    });
    
    if (!demoImplemented) {
      this.warnings.push('Demo mode implementation not found');
      log.warning('Demo mode implementation not found');
    }
    
    // Check for demo credentials in README
    const readmePath = path.join(this.projectRoot, 'README.md');
    if (fs.existsSync(readmePath)) {
      const readmeContent = fs.readFileSync(readmePath, 'utf8');
      if (readmeContent.includes('demo') && (readmeContent.includes('credentials') || readmeContent.includes('login'))) {
        log.success('Demo credentials documented in README');
      } else {
        this.warnings.push('Demo credentials not documented in README');
        log.warning('Demo credentials not documented in README');
      }
    }
  }

  // Validate screenshots and media
  validateScreenshots() {
    log.section('Screenshots & Media Validation');
    
    const screenshotDirs = [
      path.join(this.projectRoot, 'screenshots'),
      path.join(this.projectRoot, 'media'),
      path.join(this.frontendPath, 'public/screenshots')
    ];
    
    let screenshotsFound = false;
    
    screenshotDirs.forEach(dir => {
      if (fs.existsSync(dir)) {
        const files = fs.readdirSync(dir);
        const imageFiles = files.filter(file => 
          /\.(png|jpg|jpeg|webp|svg)$/i.test(file)
        );
        
        if (imageFiles.length > 0) {
          screenshotsFound = true;
          log.success(`Screenshots found in ${path.basename(dir)}: ${imageFiles.length} files`);
        }
      }
    });
    
    if (!screenshotsFound) {
      this.warnings.push('No screenshots found');
      log.warning('No screenshots found');
    }
    
    // Check for video files
    const videoExtensions = ['.mp4', '.webm', '.mov', '.avi'];
    let videoFound = false;
    
    screenshotDirs.forEach(dir => {
      if (fs.existsSync(dir)) {
        const files = fs.readdirSync(dir);
        const videoFiles = files.filter(file => 
          videoExtensions.some(ext => file.toLowerCase().endsWith(ext))
        );
        
        if (videoFiles.length > 0) {
          videoFound = true;
          log.success(`Demo video found: ${videoFiles.join(', ')}`);
        }
      }
    });
    
    if (!videoFound) {
      this.warnings.push('No demo video found');
      log.warning('No demo video found');
    }
  }

  // Validate documentation
  validateDocumentation() {
    log.section('Documentation Validation');
    
    const requiredDocs = [
      { file: 'README.md', required: true },
      { file: 'QUICKSTART.md', required: true },
      { file: 'FEATURES.md', required: true },
      { file: 'docs/HopNGo-One-Pager.md', required: true },
      { file: 'docs/ARCHITECTURE.md', required: false },
      { file: 'docs/SECURITY_CHECKLIST.md', required: false }
    ];
    
    requiredDocs.forEach(doc => {
      const docPath = path.join(this.projectRoot, doc.file);
      if (fs.existsSync(docPath)) {
        const content = fs.readFileSync(docPath, 'utf8');
        if (content.length > 100) {
          log.success(`Documentation complete: ${doc.file}`);
        } else {
          this.warnings.push(`Documentation too short: ${doc.file}`);
          log.warning(`Documentation too short: ${doc.file}`);
        }
      } else if (doc.required) {
        this.errors.push(`Missing required documentation: ${doc.file}`);
        log.error(`Missing required documentation: ${doc.file}`);
      } else {
        this.warnings.push(`Optional documentation missing: ${doc.file}`);
        log.warning(`Optional documentation missing: ${doc.file}`);
      }
    });
  }

  // Validate code quality
  async validateCodeQuality() {
    log.section('Code Quality Validation');
    
    try {
      // Check for TypeScript errors
      log.info('Checking TypeScript compilation...');
      try {
        execSync('npm run type-check', { 
          cwd: this.frontendPath, 
          stdio: 'pipe' 
        });
        log.success('TypeScript compilation successful');
      } catch (error) {
        this.errors.push('TypeScript compilation errors found');
        log.error('TypeScript compilation errors found');
      }
      
      // Check for linting errors
      log.info('Running ESLint...');
      try {
        execSync('npm run lint', { 
          cwd: this.frontendPath, 
          stdio: 'pipe' 
        });
        log.success('ESLint passed');
      } catch (error) {
        this.warnings.push('ESLint warnings/errors found');
        log.warning('ESLint warnings/errors found');
      }
      
    } catch (error) {
      this.warnings.push(`Code quality check failed: ${error.message}`);
      log.warning(`Code quality check failed: ${error.message}`);
    }
  }

  // Validate build process
  async validateBuild() {
    log.section('Build Process Validation');
    
    try {
      log.info('Testing frontend build...');
      execSync('npm run build', { 
        cwd: this.frontendPath, 
        stdio: 'pipe',
        timeout: 120000 // 2 minutes timeout
      });
      log.success('Frontend build successful');
      
      // Check if build output exists
      const buildDir = path.join(this.frontendPath, '.next');
      if (fs.existsSync(buildDir)) {
        log.success('Build output directory exists');
      } else {
        this.errors.push('Build output directory not found');
        log.error('Build output directory not found');
      }
      
    } catch (error) {
      this.errors.push('Frontend build failed');
      log.error('Frontend build failed');
    }
  }

  // Validate accessibility
  validateAccessibility() {
    log.section('Accessibility Validation');
    
    const componentFiles = [];
    const srcDir = path.join(this.frontendPath, 'src');
    
    if (fs.existsSync(srcDir)) {
      this.findFilesRecursively(srcDir, componentFiles, ['.tsx']);
      
      let totalComponents = 0;
      let accessibleComponents = 0;
      
      componentFiles.forEach(filePath => {
        const content = fs.readFileSync(filePath, 'utf8');
        const fileName = path.basename(filePath);
        totalComponents++;
        
        // Check for accessibility attributes
        const accessibilityPatterns = [
          /aria-label=/,
          /aria-describedby=/,
          /role=/,
          /alt=/,
          /tabIndex=/,
          /aria-expanded=/,
          /aria-hidden=/
        ];
        
        const hasAccessibility = accessibilityPatterns.some(pattern => 
          pattern.test(content)
        );
        
        if (hasAccessibility) {
          accessibleComponents++;
        }
      });
      
      const accessibilityPercentage = totalComponents > 0 
        ? Math.round((accessibleComponents / totalComponents) * 100) 
        : 0;
      
      log.info(`Components with accessibility attributes: ${accessibleComponents}/${totalComponents} (${accessibilityPercentage}%)`);
      
      if (accessibilityPercentage >= 80) {
        log.success('Good accessibility coverage');
      } else if (accessibilityPercentage >= 50) {
        this.warnings.push('Moderate accessibility coverage');
        log.warning('Moderate accessibility coverage');
      } else {
        this.errors.push('Poor accessibility coverage');
        log.error('Poor accessibility coverage');
      }
    }
  }

  // Validate internationalization
  validateI18n() {
    log.section('Internationalization Validation');
    
    const i18nDirs = [
      path.join(this.frontendPath, 'src/locales'),
      path.join(this.frontendPath, 'locales'),
      path.join(this.frontendPath, 'public/locales')
    ];
    
    let i18nFound = false;
    
    i18nDirs.forEach(dir => {
      if (fs.existsSync(dir)) {
        const files = fs.readdirSync(dir);
        const jsonFiles = files.filter(file => file.endsWith('.json'));
        
        if (jsonFiles.length > 0) {
          i18nFound = true;
          log.success(`I18n files found: ${jsonFiles.join(', ')}`);
          
          // Check for required languages
          const requiredLanguages = ['en', 'bn'];
          const foundLanguages = jsonFiles.map(file => file.replace('.json', ''));
          
          requiredLanguages.forEach(lang => {
            if (foundLanguages.includes(lang)) {
              log.success(`Language support found: ${lang}`);
            } else {
              this.warnings.push(`Missing language support: ${lang}`);
              log.warning(`Missing language support: ${lang}`);
            }
          });
        }
      }
    });
    
    if (!i18nFound) {
      this.warnings.push('No internationalization files found');
      log.warning('No internationalization files found');
    }
  }

  // Validate security
  validateSecurity() {
    log.section('Security Validation');
    
    // Check for environment variables
    const envFiles = [
      path.join(this.frontendPath, '.env.example'),
      path.join(this.projectRoot, '.env.example')
    ];
    
    let envExampleFound = false;
    
    envFiles.forEach(envFile => {
      if (fs.existsSync(envFile)) {
        envExampleFound = true;
        log.success(`Environment example found: ${path.basename(envFile)}`);
        
        const content = fs.readFileSync(envFile, 'utf8');
        
        // Check for sensitive data patterns
        const sensitivePatterns = [
          /password\s*=\s*[^\s]+/i,
          /secret\s*=\s*[^\s]+/i,
          /key\s*=\s*[^\s]+/i
        ];
        
        const hasSensitiveData = sensitivePatterns.some(pattern => 
          pattern.test(content)
        );
        
        if (hasSensitiveData) {
          this.errors.push('Sensitive data found in .env.example');
          log.error('Sensitive data found in .env.example');
        } else {
          log.success('No sensitive data in .env.example');
        }
      }
    });
    
    if (!envExampleFound) {
      this.warnings.push('No .env.example file found');
      log.warning('No .env.example file found');
    }
    
    // Check for security headers
    const securityFiles = [
      path.join(this.frontendPath, 'next.config.js'),
      path.join(this.frontendPath, 'next.config.mjs')
    ];
    
    securityFiles.forEach(configFile => {
      if (fs.existsSync(configFile)) {
        const content = fs.readFileSync(configFile, 'utf8');
        
        if (content.includes('headers') || content.includes('security')) {
          log.success('Security headers configuration found');
        } else {
          this.warnings.push('No security headers configuration found');
          log.warning('No security headers configuration found');
        }
      }
    });
  }

  // Validate performance
  validatePerformance() {
    log.section('Performance Validation');
    
    // Check for performance optimizations
    const nextConfigPath = path.join(this.frontendPath, 'next.config.js');
    const nextConfigMjsPath = path.join(this.frontendPath, 'next.config.mjs');
    
    const configPath = fs.existsSync(nextConfigPath) ? nextConfigPath : nextConfigMjsPath;
    
    if (fs.existsSync(configPath)) {
      const content = fs.readFileSync(configPath, 'utf8');
      
      const performanceFeatures = [
        { feature: 'images', pattern: /images\s*:/ },
        { feature: 'compression', pattern: /compress/ },
        { feature: 'bundleAnalyzer', pattern: /bundleAnalyzer/ },
        { feature: 'swcMinify', pattern: /swcMinify/ }
      ];
      
      performanceFeatures.forEach(({ feature, pattern }) => {
        if (pattern.test(content)) {
          log.success(`Performance optimization found: ${feature}`);
        } else {
          this.warnings.push(`Performance optimization missing: ${feature}`);
          log.warning(`Performance optimization missing: ${feature}`);
        }
      });
    }
    
    // Check for lazy loading
    const componentFiles = [];
    const srcDir = path.join(this.frontendPath, 'src');
    
    if (fs.existsSync(srcDir)) {
      this.findFilesRecursively(srcDir, componentFiles, ['.tsx']);
      
      let lazyLoadingFound = false;
      
      componentFiles.forEach(filePath => {
        const content = fs.readFileSync(filePath, 'utf8');
        
        if (content.includes('lazy') || content.includes('Suspense') || content.includes('dynamic')) {
          lazyLoadingFound = true;
        }
      });
      
      if (lazyLoadingFound) {
        log.success('Lazy loading implementation found');
      } else {
        this.warnings.push('No lazy loading implementation found');
        log.warning('No lazy loading implementation found');
      }
    }
  }

  // Find files recursively
  findFilesRecursively(dir, fileList, extensions) {
    const files = fs.readdirSync(dir);
    
    files.forEach(file => {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      
      if (stat.isDirectory() && !file.startsWith('.') && file !== 'node_modules') {
        this.findFilesRecursively(filePath, fileList, extensions);
      } else if (extensions.some(ext => file.endsWith(ext))) {
        fileList.push(filePath);
      }
    });
  }

  // Generate final report
  generateFinalReport() {
    log.header('Final Validation Report');
    
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
    
    // Calculate overall readiness score
    const totalChecks = this.successes.length + this.warnings.length + this.errors.length;
    const score = totalChecks > 0 ? Math.round(((this.successes.length + (this.warnings.length * 0.5)) / totalChecks) * 100) : 0;
    
    console.log(`\n${colors.bold}🎯 Submission Readiness Score: ${score}%${colors.reset}`);
    
    if (score >= 90) {
      console.log(`${colors.green}🎉 Ready for submission!${colors.reset}`);
    } else if (score >= 70) {
      console.log(`${colors.yellow}👍 Nearly ready, address warnings for best results${colors.reset}`);
    } else {
      console.log(`${colors.red}🔧 Needs significant work before submission${colors.reset}`);
    }
    
    // Save detailed report
    const reportPath = path.join(this.projectRoot, 'reports/final-validation-report.json');
    const reportsDir = path.dirname(reportPath);
    
    if (!fs.existsSync(reportsDir)) {
      fs.mkdirSync(reportsDir, { recursive: true });
    }
    
    fs.writeFileSync(reportPath, JSON.stringify({
      timestamp: new Date().toISOString(),
      score,
      submissionReady: score >= 70,
      summary: {
        errors: this.errors.length,
        warnings: this.warnings.length,
        successes: this.successes.length
      },
      details: {
        errors: this.errors,
        warnings: this.warnings,
        successes: this.successes
      }
    }, null, 2));
    
    log.info(`Detailed report saved to: ${reportPath}`);
    
    return {
      score,
      submissionReady: score >= 70,
      errors: this.errors.length,
      warnings: this.warnings.length
    };
  }

  // Run all validations
  async runAll() {
    console.log(`${colors.bold}${colors.cyan}🚀 HopNGo Final Validation Suite${colors.reset}\n`);
    console.log(`${colors.blue}Validating submission readiness...${colors.reset}\n`);
    
    try {
      await this.validateDemoMode();
      this.validateScreenshots();
      this.validateDocumentation();
      await this.validateCodeQuality();
      await this.validateBuild();
      this.validateAccessibility();
      this.validateI18n();
      this.validateSecurity();
      this.validatePerformance();
      
      // Run SEO validation
      log.section('SEO Validation');
      const seoValidator = new SEOValidator();
      await seoValidator.runAll();
      
      const report = this.generateFinalReport();
      
      // Exit with appropriate code
      if (report.errors > 0) {
        console.log(`\n${colors.red}❌ Validation failed with ${report.errors} critical errors${colors.reset}`);
        process.exit(1);
      } else if (report.warnings > 5) {
        console.log(`\n${colors.yellow}⚠️  Validation completed with ${report.warnings} warnings${colors.reset}`);
        process.exit(0);
      } else {
        console.log(`\n${colors.green}✅ Validation successful!${colors.reset}`);
        process.exit(0);
      }
      
    } catch (error) {
      log.error(`Validation suite failed: ${error.message}`);
      process.exit(1);
    }
  }
}

// Run validation if called directly
if (require.main === module) {
  const validator = new FinalValidator();
  validator.runAll();
}

module.exports = FinalValidator;
# Performance Optimization Commit Script
# Commits all performance improvements with proper message

param(
    [string]$Message = "perf(scale): autoscaling, load & chaos tests, tuning"
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Committing Performance Optimization Changes..." -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Blue

# Check if we're in a git repository
if (!(Test-Path ".git")) {
    Write-Host "❌ Not in a git repository. Please run from project root." -ForegroundColor Red
    exit 1
}

# Add all performance-related files
Write-Host "📁 Adding performance optimization files..." -ForegroundColor Blue

$performanceFiles = @(
    "infra/k8s/base/autoscaling/",
    "infra/k8s/base/performance-tuning/",
    "infra/k8s/base/market-service/redis-cache-config.yaml",
    "infra/k8s/base/frontend/cache-config.yaml",
    "infra/k8s/base/monitoring/cache-alerts.yaml",
    "infra/grafana/dashboards/cache-performance-dashboard.json",
    "tests/load/",
    "tests/chaos/",
    "scripts/performance-audit.ps1",
    "scripts/performance-audit.sh",
    "scripts/commit-performance-changes.ps1",
    "booking-service/src/main/java/com/hopngo/booking/service/BookingService.java",
    "booking-service/src/main/java/com/hopngo/booking/entity/Booking.java",
    "booking-service/src/main/java/com/hopngo/booking/repository/BookingRepository.java",
    "booking-service/src/main/resources/application.yml"
)

foreach ($file in $performanceFiles) {
    if (Test-Path $file) {
        Write-Host "  ✅ Adding $file" -ForegroundColor Green
        git add $file
    }
    else {
        Write-Host "  ⚠️  File not found: $file" -ForegroundColor Yellow
    }
}

# Check git status
Write-Host "`n📊 Git Status:" -ForegroundColor Blue
git status --porcelain

# Show what will be committed
Write-Host "`n📝 Files to be committed:" -ForegroundColor Blue
git diff --cached --name-only

# Commit the changes
Write-Host "`nCommitting changes..." -ForegroundColor Blue
try {
    git commit -m $Message
    Write-Host "Performance optimizations committed successfully!" -ForegroundColor Green
}
catch {
    Write-Host "❌ Failed to commit changes: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Show commit summary
Write-Host "`n📈 Commit Summary:" -ForegroundColor Blue
git log --oneline -1

Write-Host "`nPerformance Optimization Complete!" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Blue
Write-Host "Autoscaling configurations applied" -ForegroundColor White
Write-Host "Load testing scripts created" -ForegroundColor White
Write-Host "Chaos engineering tests implemented" -ForegroundColor White
Write-Host "Cache optimization configured" -ForegroundColor White
Write-Host "Performance tuning applied" -ForegroundColor White
Write-Host "N+1 query issues fixed" -ForegroundColor White
Write-Host "Console errors addressed" -ForegroundColor White
Write-Host "Database indexes optimized" -ForegroundColor White
Write-Host "JVM and thread pool tuning" -ForegroundColor White
Write-Host "Monitoring and alerting enhanced" -ForegroundColor White

Write-Host "`nNext Steps:" -ForegroundColor Yellow
Write-Host "1. Deploy changes to staging environment" -ForegroundColor White
Write-Host "2. Run load tests to validate performance" -ForegroundColor White
Write-Host "3. Execute chaos tests to verify resilience" -ForegroundColor White
Write-Host "4. Monitor system metrics and adjust as needed" -ForegroundColor White
Write-Host "5. Deploy to production with gradual rollout" -ForegroundColor White

Write-Host "`nPerformance Improvements Summary:" -ForegroundColor Cyan
Write-Host "HPA configured for all services with CPU + custom metrics" -ForegroundColor White
Write-Host "Redis, RabbitMQ, MongoDB connection pools optimized" -ForegroundColor White
Write-Host "HikariCP tuned for optimal database performance" -ForegroundColor White
Write-Host "k6 load testing scripts for critical endpoints" -ForegroundColor White
Write-Host "LitmusChaos experiments for resilience testing" -ForegroundColor White
Write-Host "Cache hit ratio monitoring and warming strategies" -ForegroundColor White
Write-Host "N+1 query prevention with batch loading" -ForegroundColor White
Write-Host "JVM garbage collection and heap optimization" -ForegroundColor White
Write-Host "Thread pool sizing and async task configuration" -ForegroundColor White
Write-Host "Database indexes for query optimization" -ForegroundColor White
Write-Host "Comprehensive performance monitoring dashboards" -ForegroundColor White
Write-Host "Automated performance audit and improvement tools" -ForegroundColor White
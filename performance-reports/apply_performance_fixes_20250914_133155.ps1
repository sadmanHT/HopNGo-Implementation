# Performance Improvement Script
param([string]$Namespace = "hopngo-prod")

Write-Host "Applying performance improvements..." -ForegroundColor Green

# Apply performance configurations
kubectl apply -f ../infra/k8s/base/performance-tuning/performance-config.yaml

# Restart services
$services = @("booking-service", "market-service", "social-service", "notification-service")
foreach ($service in $services) {
    Write-Host "Restarting $service..." -ForegroundColor Yellow
    kubectl rollout restart deployment $service -n $Namespace
}

Write-Host "Performance improvements applied!" -ForegroundColor Green

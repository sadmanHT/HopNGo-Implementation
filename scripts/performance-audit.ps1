# Performance Audit Script for HopNGo
param(
    [string]$Namespace = "hopngo-prod",
    [string]$ReportDir = "./performance-reports"
)

$ErrorActionPreference = "Stop"

Write-Host "Starting HopNGo Performance Audit..." -ForegroundColor Blue

# Configuration
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ReportFile = "$ReportDir/performance_audit_$Timestamp.md"

# Create report directory
if (!(Test-Path $ReportDir)) {
    New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
}

# Initialize report
"# HopNGo Performance Audit Report`nGenerated: $(Get-Date)`n" | Out-File -FilePath $ReportFile -Encoding UTF8

# Function to log with timestamp
function Write-Log {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    $timestamped = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Write-Host $timestamped -ForegroundColor $Color
    $timestamped | Out-File -FilePath $ReportFile -Append -Encoding UTF8
}

# Function to check if kubectl is available
function Test-Kubectl {
    try {
        kubectl version --client --short | Out-Null
        return $true
    }
    catch {
        Write-Log "kubectl not found. Please install kubectl first." "Red"
        return $false
    }
}

# Function to check namespace
function Test-Namespace {
    try {
        kubectl get namespace $Namespace 2>$null | Out-Null
    }
    catch {
        Write-Log "Namespace $Namespace not found. Using default namespace." "Yellow"
        $script:Namespace = "default"
    }
}

# Function to get pod metrics
function Get-PodMetrics {
    Write-Log "Pod Resource Usage Analysis"
    
    try {
        $metrics = kubectl top pods -n $Namespace --sort-by=cpu 2>$null
        if ($metrics) {
            $metrics | Select-Object -First 10 | ForEach-Object { Write-Log $_ }
        }
        else {
            Write-Log "Metrics server not available. Skipping resource usage." "Yellow"
        }
    }
    catch {
        Write-Log "Unable to get pod metrics." "Yellow"
    }
}

# Function to check for OOMKilled pods
function Test-OOMKills {
    Write-Log "Checking for OOMKilled pods..."
    
    try {
        $pods = kubectl get pods -n $Namespace -o json 2>$null | ConvertFrom-Json
        $oomPods = $pods.items | Where-Object { 
            $_.status.containerStatuses -and 
            $_.status.containerStatuses[0].lastState.terminated.reason -eq "OOMKilled" 
        }
        
        if ($oomPods) {
            Write-Log "Found OOMKilled pods:" "Red"
            $oomPods | ForEach-Object {
                Write-Log "- $($_.metadata.name)" "Red"
            }
        }
        else {
            Write-Log "No OOMKilled pods found" "Green"
        }
    }
    catch {
        Write-Log "Unable to check for OOMKilled pods." "Yellow"
    }
}

# Function to analyze application logs for errors
function Test-ApplicationLogs {
    Write-Log "Analyzing application logs for errors..."
    
    $services = @("booking-service", "market-service", "social-service", "notification-service")
    
    foreach ($service in $services) {
        Write-Log "Checking $service logs..."
        
        try {
            $pods = kubectl get pods -n $Namespace -l app=$service -o jsonpath='{.items[*].metadata.name}' 2>$null
            
            if ($pods) {
                $podList = $pods -split ' '
                foreach ($pod in $podList) {
                    if ($pod) {
                        try {
                            $logs = kubectl logs $pod -n $Namespace --tail=500 2>$null
                            
                            if ($logs) {
                                # Count errors
                                $errorLines = $logs | Select-String -Pattern "ERROR|Exception|Failed" -AllMatches
                                $errorCount = ($errorLines | Measure-Object).Count
                                
                                if ($errorCount -gt 0) {
                                    Write-Log "Found $errorCount errors in $pod" "Red"
                                    $errorLines | Select-Object -First 3 | ForEach-Object {
                                        Write-Log "  $($_.Line)" "Red"
                                    }
                                }
                                else {
                                    Write-Log "No errors found in $pod" "Green"
                                }
                                
                                # Check for console output
                                $consoleLines = $logs | Select-String -Pattern "System\.(out|err)\.print" -AllMatches
                                $consoleCount = ($consoleLines | Measure-Object).Count
                                
                                if ($consoleCount -gt 0) {
                                    Write-Log "Found $consoleCount console statements in $pod" "Yellow"
                                }
                            }
                        }
                        catch {
                            Write-Log "Unable to get logs for pod $pod" "Yellow"
                        }
                    }
                }
            }
            else {
                Write-Log "No pods found for service $service" "Yellow"
            }
        }
        catch {
            Write-Log "Unable to check service $service" "Yellow"
        }
    }
}

# Function to generate recommendations
function Write-Recommendations {
    Write-Log "Performance Optimization Recommendations"
    
    Write-Log "Immediate Actions:"
    Write-Log "1. Memory Optimization - Review JVM heap sizes"
    Write-Log "2. Database Optimization - Apply indexes and tune queries"
    Write-Log "3. Cache Optimization - Monitor Redis hit ratios"
    Write-Log "4. Application Code - Replace console output with proper logging"
    
    Write-Log "Long-term Improvements:"
    Write-Log "1. Monitoring - Set up comprehensive metrics"
    Write-Log "2. Scalability - Configure HPA and circuit breakers"
    Write-Log "3. Resource Management - Set proper limits and requests"
}

# Function to create improvement script
function New-ImprovementScript {
    $scriptFile = "$ReportDir/apply_performance_fixes_$Timestamp.ps1"
    
    $content = @'
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
'@

    $content | Out-File -FilePath $scriptFile -Encoding UTF8
    Write-Log "Generated improvement script: $scriptFile"
}

# Main execution
function Main {
    if (!(Test-Kubectl)) { 
        Write-Host "Exiting due to missing kubectl" -ForegroundColor Red
        return 
    }
    
    Test-Namespace
    Write-Log "Starting performance audit for namespace: $Namespace" "Blue"
    
    Get-PodMetrics
    Test-OOMKills
    Test-ApplicationLogs
    Write-Recommendations
    New-ImprovementScript
    
    Write-Log "Performance audit completed!" "Green"
    Write-Log "Report saved to: $ReportFile" "Blue"
    
    Write-Host "Performance Audit Summary" -ForegroundColor Blue
    Write-Host "Report: $ReportFile" -ForegroundColor White
    Write-Host "Improvement script: $ReportDir/apply_performance_fixes_$Timestamp.ps1" -ForegroundColor White
}

# Run main function
Main
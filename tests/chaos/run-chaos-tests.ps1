# HopNGo Chaos Engineering Test Runner (PowerShell)
# This script orchestrates chaos experiments using LitmusChaos on Windows

param(
    [string[]]$TestNames = @(),
    [string]$Namespace = "hopngo-prod",
    [string]$LitmusNamespace = "litmus",
    [switch]$ListTests,
    [switch]$SetupMonitoring,
    [switch]$CleanupOnly,
    [switch]$SkipPrerequisites,
    [switch]$Help
)

# Configuration
$ChaosDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResultsDir = Join-Path $ChaosDir "results"
$LogFile = Join-Path $ResultsDir "chaos-test-$(Get-Date -Format 'yyyyMMdd-HHmmss').log"
$ReportFile = Join-Path $ResultsDir "chaos-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"

# Test configuration
$ChaosTests = @{
    "pod-kill" = "litmus-pod-kill.yaml"
    "db-outage" = "litmus-db-outage.yaml"
    "rabbitmq-chaos" = "litmus-rabbitmq-chaos.yaml"
}

$TestDescriptions = @{
    "pod-kill" = "Pod termination chaos for market-service, frontend, and booking-service"
    "db-outage" = "Database outage simulation for PostgreSQL, MongoDB, and Redis"
    "rabbitmq-chaos" = "RabbitMQ failure scenarios including node failures and network partitions"
}

$TestPriorities = @{
    "pod-kill" = "high"
    "db-outage" = "high"
    "rabbitmq-chaos" = "medium"
}

# Utility functions
function Write-Log {
    param(
        [string]$Level,
        [string]$Message
    )
    
    $Timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $LogMessage = "[$Level] $Timestamp - $Message"
    
    switch ($Level) {
        "INFO" { Write-Host $LogMessage -ForegroundColor Blue }
        "WARN" { Write-Host $LogMessage -ForegroundColor Yellow }
        "ERROR" { Write-Host $LogMessage -ForegroundColor Red }
        "SUCCESS" { Write-Host $LogMessage -ForegroundColor Green }
    }
    
    Add-Content -Path $LogFile -Value $LogMessage
}

function Test-Prerequisites {
    Write-Log "INFO" "Checking prerequisites..."
    
    # Check kubectl
    try {
        kubectl version --client | Out-Null
    } catch {
        Write-Log "ERROR" "kubectl is not installed or not in PATH"
        return $false
    }
    
    # Check cluster connectivity
    try {
        kubectl cluster-info | Out-Null
    } catch {
        Write-Log "ERROR" "Cannot connect to Kubernetes cluster"
        return $false
    }
    
    # Check if namespace exists
    try {
        kubectl get namespace $Namespace | Out-Null
    } catch {
        Write-Log "ERROR" "Namespace $Namespace does not exist"
        return $false
    }
    
    # Check if Litmus is installed
    try {
        kubectl get namespace $LitmusNamespace | Out-Null
    } catch {
        Write-Log "ERROR" "Litmus namespace $LitmusNamespace does not exist. Please install LitmusChaos first."
        Write-Log "INFO" "Install Litmus: kubectl apply -f https://litmuschaos.github.io/litmus/litmus-operator-v2.14.0.yaml"
        return $false
    }
    
    # Check if chaos experiments CRDs exist
    try {
        kubectl get crd chaosengines.litmuschaos.io | Out-Null
    } catch {
        Write-Log "ERROR" "LitmusChaos CRDs not found. Please install LitmusChaos operator."
        return $false
    }
    
    # Check if litmus-admin service account exists
    try {
        kubectl get serviceaccount litmus-admin -n $LitmusNamespace | Out-Null
    } catch {
        Write-Log "WARN" "litmus-admin service account not found. Creating it..."
        New-LitmusRBAC
    }
    
    Write-Log "SUCCESS" "All prerequisites satisfied"
    return $true
}

function New-LitmusRBAC {
    Write-Log "INFO" "Creating Litmus RBAC resources..."
    
    $RBACManifest = @"
apiVersion: v1
kind: ServiceAccount
metadata:
  name: litmus-admin
  namespace: $LitmusNamespace
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: litmus-admin
rules:
- apiGroups: [""]
  resources: ["pods", "events", "configmaps", "secrets", "services"]
  verbs: ["create", "delete", "get", "list", "patch", "update", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "statefulsets", "replicasets"]
  verbs: ["create", "delete", "get", "list", "patch", "update", "watch"]
- apiGroups: ["litmuschaos.io"]
  resources: ["*"]
  verbs: ["*"]
- apiGroups: ["batch"]
  resources: ["jobs"]
  verbs: ["create", "delete", "get", "list", "patch", "update", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: litmus-admin
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: litmus-admin
subjects:
- kind: ServiceAccount
  name: litmus-admin
  namespace: $LitmusNamespace
"@
    
    $RBACManifest | kubectl apply -f -
    Write-Log "SUCCESS" "Litmus RBAC resources created"
}

function Set-ChaosMonitoring {
    Write-Log "INFO" "Setting up chaos monitoring..."
    
    # Create monitoring namespace if it doesn't exist
    kubectl create namespace chaos-monitoring --dry-run=client -o yaml | kubectl apply -f -
    
    # Deploy chaos monitoring dashboard
    $MonitoringManifest = @"
apiVersion: v1
kind: ConfigMap
metadata:
  name: chaos-monitoring-config
  namespace: chaos-monitoring
data:
  monitor.ps1: |
    while (`$true) {
      Write-Host "$(Get-Date): Monitoring chaos experiments..."
      kubectl get chaosengines -A -o wide
      kubectl get chaosresults -A -o wide
      Start-Sleep 30
    }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chaos-monitor
  namespace: chaos-monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: chaos-monitor
  template:
    metadata:
      labels:
        app: chaos-monitor
    spec:
      serviceAccountName: default
      containers:
      - name: monitor
        image: bitnami/kubectl:latest
        command: ["/bin/bash", "-c", "while true; do echo 'Monitoring...'; kubectl get chaosengines -A; sleep 30; done"]
"@
    
    $MonitoringManifest | kubectl apply -f -
    Write-Log "SUCCESS" "Chaos monitoring setup complete"
}

function Invoke-ChaosTest {
    param(
        [string]$TestName,
        [string]$TestFile,
        [string]$TestDescription
    )
    
    Write-Log "INFO" "Starting chaos test: $TestName"
    Write-Log "INFO" "Description: $TestDescription"
    
    # Apply chaos experiment
    try {
        kubectl apply -f (Join-Path $ChaosDir $TestFile)
        Write-Log "SUCCESS" "Chaos experiment $TestName applied successfully"
    } catch {
        Write-Log "ERROR" "Failed to apply chaos experiment $TestName"
        return $false
    }
    
    # Wait for chaos engines to be created
    Start-Sleep 10
    
    # Monitor chaos engines from the test file
    try {
        $ChaosEngines = kubectl get chaosengines -n $Namespace -l app=hopngo --no-headers -o custom-columns=":metadata.name" 2>$null
        
        if (-not $ChaosEngines) {
            Write-Log "WARN" "No chaos engines found for test $TestName"
            return $false
        }
        
        Write-Log "INFO" "Monitoring chaos engines: $($ChaosEngines -join ', ')"
        
        # Monitor each chaos engine
        $AllCompleted = $false
        $Timeout = 600  # 10 minutes timeout
        $Elapsed = 0
        $CheckInterval = 15
        
        while ($Elapsed -lt $Timeout -and -not $AllCompleted) {
            $AllCompleted = $true
            
            foreach ($Engine in $ChaosEngines) {
                try {
                    $Status = kubectl get chaosengine $Engine -n $Namespace -o jsonpath='{.status.engineStatus}' 2>$null
                    if (-not $Status) { $Status = "unknown" }
                    
                    Write-Log "INFO" "Chaos engine $Engine status: $Status"
                    
                    if ($Status -ne "completed" -and $Status -ne "stopped") {
                        $AllCompleted = $false
                    }
                    
                    # Check for any failures
                    $Experiments = kubectl get chaosengine $Engine -n $Namespace -o jsonpath='{.status.experiments[*].name}' 2>$null
                    
                    if ($Experiments) {
                        foreach ($Exp in $Experiments.Split(' ')) {
                            if ($Exp) {
                                try {
                                    $ExpStatus = kubectl get chaosresult "$Engine-$Exp" -n $Namespace -o jsonpath='{.status.experimentStatus.verdict}' 2>$null
                                    
                                    if ($ExpStatus -eq "Fail") {
                                        Write-Log "ERROR" "Experiment $Exp in engine $Engine failed"
                                    } elseif ($ExpStatus -eq "Pass") {
                                        Write-Log "SUCCESS" "Experiment $Exp in engine $Engine passed"
                                    }
                                } catch {
                                    # Ignore errors for individual experiment status checks
                                }
                            }
                        }
                    }
                } catch {
                    Write-Log "WARN" "Failed to get status for chaos engine $Engine"
                }
            }
            
            if (-not $AllCompleted) {
                Start-Sleep $CheckInterval
                $Elapsed += $CheckInterval
            }
        }
        
        if ($Elapsed -ge $Timeout) {
            Write-Log "WARN" "Chaos test $TestName timed out after $Timeout seconds"
        } else {
            Write-Log "SUCCESS" "Chaos test $TestName completed"
        }
        
        # Collect results
        Save-TestResults $TestName $ChaosEngines
        
        # Cleanup chaos engines
        Write-Log "INFO" "Cleaning up chaos engines for test $TestName"
        kubectl delete -f (Join-Path $ChaosDir $TestFile) --ignore-not-found=true
        
        # Wait for cleanup
        Start-Sleep 30
        
        return $true
    } catch {
        Write-Log "ERROR" "Error during chaos test execution: $($_.Exception.Message)"
        return $false
    }
}

function Save-TestResults {
    param(
        [string]$TestName,
        [string[]]$ChaosEngines
    )
    
    Write-Log "INFO" "Collecting results for test $TestName"
    
    $ResultsFile = Join-Path $ResultsDir "$TestName-results.json"
    
    $Results = @{
        testName = $TestName
        timestamp = (Get-Date -Format 'yyyy-MM-ddTHH:mm:ssZ')
        chaosEngines = @()
        chaosResults = @()
    }
    
    foreach ($Engine in $ChaosEngines) {
        try {
            $EngineStatus = kubectl get chaosengine $Engine -n $Namespace -o json 2>$null | ConvertFrom-Json
            $Results.chaosEngines += @{
                name = $Engine
                status = $EngineStatus
            }
        } catch {
            Write-Log "WARN" "Failed to collect status for chaos engine $Engine"
        }
    }
    
    # Collect chaos results
    try {
        $ChaosResultsJson = kubectl get chaosresults -n $Namespace -o json 2>$null | ConvertFrom-Json
        $Results.chaosResults = $ChaosResultsJson.items
    } catch {
        Write-Log "WARN" "Failed to collect chaos results"
    }
    
    $Results | ConvertTo-Json -Depth 10 | Set-Content -Path $ResultsFile
    Write-Log "SUCCESS" "Results collected in $ResultsFile"
}

function New-FinalReport {
    Write-Log "INFO" "Generating final chaos testing report..."
    
    $TotalTests = $ChaosTests.Count
    $SuccessfulTests = 0
    $FailedTests = 0
    $TestResults = @()
    
    foreach ($TestName in $ChaosTests.Keys) {
        $ResultsFile = Join-Path $ResultsDir "$TestName-results.json"
        $TestStatus = if (Test-Path $ResultsFile) { "completed"; $SuccessfulTests++ } else { "failed"; $FailedTests++ }
        
        $TestResults += @{
            testName = $TestName
            description = $TestDescriptions[$TestName]
            priority = $TestPriorities[$TestName]
            status = $TestStatus
            resultsFile = $ResultsFile
        }
    }
    
    $Report = @{
        chaosTestingSummary = @{
            timestamp = (Get-Date -Format 'yyyy-MM-ddTHH:mm:ssZ')
            totalTests = $TotalTests
            namespace = $Namespace
            litmusNamespace = $LitmusNamespace
            testResults = $TestResults
            summary = @{
                successful = $SuccessfulTests
                failed = $FailedTests
                successRate = "$([math]::Round($SuccessfulTests * 100 / $TotalTests, 2))%"
            }
        }
    }
    
    $Report | ConvertTo-Json -Depth 10 | Set-Content -Path $ReportFile
    Write-Log "SUCCESS" "Final report generated: $ReportFile"
    
    # Print summary to console
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "🎯 CHAOS TESTING SUMMARY" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "Total Tests: $TotalTests" -ForegroundColor White
    Write-Host "Successful: $SuccessfulTests" -ForegroundColor Green
    Write-Host "Failed: $FailedTests" -ForegroundColor Red
    Write-Host "Success Rate: $([math]::Round($SuccessfulTests * 100 / $TotalTests, 2))%" -ForegroundColor Yellow
    Write-Host "Report: $ReportFile" -ForegroundColor White
    Write-Host "Logs: $LogFile" -ForegroundColor White
    Write-Host "======================================" -ForegroundColor Cyan
}

function Remove-ChaosResources {
    Write-Log "INFO" "Performing cleanup..."
    
    # Clean up any remaining chaos engines
    kubectl delete chaosengines -n $Namespace -l app=hopngo --ignore-not-found=true
    
    # Clean up chaos results
    kubectl delete chaosresults -n $Namespace -l app=hopngo --ignore-not-found=true
    
    Write-Log "SUCCESS" "Cleanup completed"
}

function Show-Usage {
    Write-Host "Usage: .\run-chaos-tests.ps1 [options] [test-names...]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Help                   Show this help message"
    Write-Host "  -ListTests              List available chaos tests"
    Write-Host "  -Namespace NAME         Target namespace (default: $Namespace)"
    Write-Host "  -LitmusNamespace NAME   Litmus namespace (default: $LitmusNamespace)"
    Write-Host "  -SetupMonitoring        Setup chaos monitoring"
    Write-Host "  -CleanupOnly            Only perform cleanup"
    Write-Host "  -SkipPrerequisites      Skip prerequisite checks"
    Write-Host ""
    Write-Host "Available Tests:"
    foreach ($TestName in $ChaosTests.Keys) {
        Write-Host "  $TestName - $($TestDescriptions[$TestName])"
    }
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\run-chaos-tests.ps1                    # Run all chaos tests"
    Write-Host "  .\run-chaos-tests.ps1 pod-kill db-outage # Run specific tests"
    Write-Host "  .\run-chaos-tests.ps1 -SetupMonitoring   # Setup monitoring only"
    Write-Host "  .\run-chaos-tests.ps1 -CleanupOnly       # Cleanup only"
}

function Show-TestList {
    Write-Host "Available Chaos Tests:"
    Write-Host ""
    foreach ($TestName in $ChaosTests.Keys) {
        Write-Host "Test: $TestName"
        Write-Host "  Description: $($TestDescriptions[$TestName])"
        Write-Host "  Priority: $($TestPriorities[$TestName])"
        Write-Host "  File: $($ChaosTests[$TestName])"
        Write-Host ""
    }
}

# Main execution
function Main {
    # Handle help and list options
    if ($Help) {
        Show-Usage
        return
    }
    
    if ($ListTests) {
        Show-TestList
        return
    }
    
    # Create results directory
    if (-not (Test-Path $ResultsDir)) {
        New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null
    }
    
    # Initialize log file
    "Chaos Testing Log - $(Get-Date)" | Set-Content -Path $LogFile
    
    Write-Log "INFO" "Starting HopNGo Chaos Engineering Tests"
    Write-Log "INFO" "Target namespace: $Namespace"
    Write-Log "INFO" "Litmus namespace: $LitmusNamespace"
    Write-Log "INFO" "Results directory: $ResultsDir"
    
    # Handle special modes
    if ($CleanupOnly) {
        Remove-ChaosResources
        return
    }
    
    if ($SetupMonitoring) {
        Set-ChaosMonitoring
        return
    }
    
    # Prerequisites check
    if (-not $SkipPrerequisites) {
        if (-not (Test-Prerequisites)) {
            Write-Log "ERROR" "Prerequisites check failed"
            return
        }
    }
    
    # Setup monitoring
    Set-ChaosMonitoring
    
    # Determine tests to run
    $SelectedTests = if ($TestNames.Count -eq 0) { $ChaosTests.Keys } else { $TestNames }
    
    # Validate test names
    foreach ($TestName in $SelectedTests) {
        if (-not $ChaosTests.ContainsKey($TestName)) {
            Write-Log "ERROR" "Unknown test: $TestName"
            Show-TestList
            return
        }
    }
    
    Write-Log "INFO" "Running $($SelectedTests.Count) chaos tests: $($SelectedTests -join ', ')"
    
    # Run chaos tests
    $TestResults = @()
    foreach ($TestName in $SelectedTests) {
        $TestFile = $ChaosTests[$TestName]
        $TestDescription = $TestDescriptions[$TestName]
        
        Write-Log "INFO" "`n=== Starting Chaos Test: $TestName ==="
        
        if (Invoke-ChaosTest $TestName $TestFile $TestDescription) {
            $TestResults += "$TestName:success"
            Write-Log "SUCCESS" "Chaos test $TestName completed successfully"
        } else {
            $TestResults += "$TestName:failed"
            Write-Log "ERROR" "Chaos test $TestName failed"
        }
        
        # Wait between tests
        if ($TestName -ne $SelectedTests[-1]) {
            Write-Log "INFO" "Waiting 60 seconds before next test..."
            Start-Sleep 60
        }
    }
    
    # Generate final report
    New-FinalReport
    
    # Final cleanup
    Remove-ChaosResources
    
    # Exit with appropriate code
    $FailedCount = ($TestResults | Where-Object { $_ -like "*:failed" }).Count
    
    if ($FailedCount -gt 0) {
        Write-Log "ERROR" "$FailedCount chaos tests failed"
        exit 1
    } else {
        Write-Log "SUCCESS" "All chaos tests completed successfully"
        exit 0
    }
}

# Run main function
Main
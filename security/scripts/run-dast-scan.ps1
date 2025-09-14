# OWASP ZAP DAST Security Scan for HopNGo Platform
# Performs dynamic application security testing and generates reports

param(
    [Parameter(Mandatory=$false)]
    [string]$TargetUrl = "http://localhost:3000",
    
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = "http://localhost:8080",
    
    [Parameter(Mandatory=$false)]
    [string]$ZapPath = "C:\Program Files\ZAP\Zed Attack Proxy\zap.bat",
    
    [Parameter(Mandatory=$false)]
    [string]$ReportDir = "D:\projects\HopNGo\security\reports",
    
    [Parameter(Mandatory=$false)]
    [switch]$QuickScan = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$FullScan = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$ApiScan = $false
)

# Configuration
$Config = @{
    ZapPort = 8090
    ZapApiKey = "hopngo-security-scan-2024"
    ScanTimeout = 3600  # 1 hour
    MaxAlerts = 1000
    ReportFormats = @("html", "json", "xml")
    ExcludedPaths = @(
        ".*/logout.*",
        ".*/admin/delete.*",
        ".*/api/v1/payments/process.*"
    )
    AuthConfig = @{
        LoginUrl = "$TargetUrl/auth/login"
        Username = "test@hopngo.com"
        Password = "TestPassword123!"
        UsernameField = "email"
        PasswordField = "password"
        LoginIndicator = "dashboard"
        LogoutIndicator = "login"
    }
    ScanPolicies = @{
        Quick = @(
            "Cross Site Scripting (Reflected)",
            "SQL Injection",
            "Path Traversal",
            "Remote File Inclusion"
        )
        Full = @(
            "Cross Site Scripting (Reflected)",
            "Cross Site Scripting (Persistent)",
            "SQL Injection",
            "Path Traversal",
            "Remote File Inclusion",
            "Command Injection",
            "LDAP Injection",
            "Header Injection",
            "Parameter Pollution",
            "Server Side Include",
            "Cross-Domain Misconfiguration",
            "Source Code Disclosure",
            "Remote Code Execution",
            "Buffer Overflow",
            "Format String Error",
            "Integer Overflow Error"
        )
    }
}

# Logging functions
function Write-Log {
    param(
        [string]$Message,
        [string]$Level = "INFO"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch ($Level) {
        "ERROR" { "Red" }
        "WARN" { "Yellow" }
        "SUCCESS" { "Green" }
        "CRITICAL" { "Magenta" }
        default { "White" }
    }
    
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

function Write-ScanLog {
    param(
        [string]$Action,
        [string]$Status,
        [string]$Details = ""
    )
    
    $logEntry = @{
        timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        action = $Action
        status = $Status
        details = $Details
        target_url = $TargetUrl
        scan_type = if ($QuickScan) { "Quick" } elseif ($FullScan) { "Full" } else { "Standard" }
    } | ConvertTo-Json -Compress
    
    $logFile = Join-Path $ReportDir "dast-scan.log"
    Add-Content -Path $logFile -Value $logEntry
}

# ZAP API functions
function Invoke-ZapApi {
    param(
        [string]$Endpoint,
        [hashtable]$Parameters = @{},
        [string]$Method = "GET"
    )
    
    try {
        $baseUrl = "http://localhost:$($Config.ZapPort)"
        $url = "$baseUrl/JSON/$Endpoint"
        
        $Parameters["apikey"] = $Config.ZapApiKey
        
        if ($Method -eq "GET") {
            $queryString = ($Parameters.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Web.HttpUtility]::UrlEncode($_.Value))" }) -join "&"
            if ($queryString) {
                $url += "?$queryString"
            }
            $response = Invoke-RestMethod -Uri $url -Method GET
        } else {
            $response = Invoke-RestMethod -Uri $url -Method POST -Body $Parameters
        }
        
        return $response
        
    } catch {
        Write-Log "ZAP API call failed: $Endpoint - $($_.Exception.Message)" "ERROR"
        throw
    }
}

function Start-ZapProxy {
    Write-Log "Starting OWASP ZAP proxy..."
    Write-ScanLog "ZAP_START" "INITIATED"
    
    try {
        if (!(Test-Path $ZapPath)) {
            throw "ZAP not found at: $ZapPath. Please install OWASP ZAP or update the path."
        }
        
        # Start ZAP in daemon mode
        $zapArgs = @(
            "-daemon",
            "-port", $Config.ZapPort,
            "-config", "api.key=$($Config.ZapApiKey)",
            "-config", "api.addrs.addr.name=.*",
            "-config", "api.addrs.addr.regex=true"
        )
        
        $zapProcess = Start-Process -FilePath $ZapPath -ArgumentList $zapArgs -PassThru -WindowStyle Hidden
        
        # Wait for ZAP to start
        $maxWait = 60
        $waited = 0
        
        do {
            Start-Sleep -Seconds 2
            $waited += 2
            
            try {
                $status = Invoke-ZapApi "core/view/version"
                if ($status) {
                    Write-Log "ZAP started successfully (version: $($status.version))" "SUCCESS"
                    Write-ScanLog "ZAP_START" "SUCCESS" "Version: $($status.version)"
                    return $zapProcess
                }
            } catch {
                # ZAP not ready yet
            }
            
        } while ($waited -lt $maxWait)
        
        throw "ZAP failed to start within $maxWait seconds"
        
    } catch {
        Write-Log "Failed to start ZAP: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "ZAP_START" "FAILED" $_.Exception.Message
        throw
    }
}

function Stop-ZapProxy {
    param([System.Diagnostics.Process]$ZapProcess)
    
    Write-Log "Stopping ZAP proxy..."
    
    try {
        # Try graceful shutdown first
        Invoke-ZapApi "core/action/shutdown" -Method POST
        
        # Wait for process to exit
        if ($ZapProcess -and !$ZapProcess.HasExited) {
            $ZapProcess.WaitForExit(30000)  # 30 seconds
            
            if (!$ZapProcess.HasExited) {
                Write-Log "Force killing ZAP process" "WARN"
                $ZapProcess.Kill()
            }
        }
        
        Write-Log "ZAP stopped successfully" "SUCCESS"
        Write-ScanLog "ZAP_STOP" "SUCCESS"
        
    } catch {
        Write-Log "Error stopping ZAP: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "ZAP_STOP" "FAILED" $_.Exception.Message
    }
}

function Set-ZapContext {
    Write-Log "Configuring ZAP context and authentication..."
    Write-ScanLog "CONTEXT_SETUP" "INITIATED"
    
    try {
        # Create context
        $contextName = "HopNGo-Context"
        $context = Invoke-ZapApi "context/action/newContext" @{ contextName = $contextName } "POST"
        $contextId = $context.contextId
        
        # Include URLs in context
        Invoke-ZapApi "context/action/includeInContext" @{ 
            contextName = $contextName
            regex = "$TargetUrl.*"
        } "POST"
        
        if ($ApiScan) {
            Invoke-ZapApi "context/action/includeInContext" @{ 
                contextName = $contextName
                regex = "$ApiUrl.*"
            } "POST"
        }
        
        # Exclude sensitive paths
        foreach ($excludePath in $Config.ExcludedPaths) {
            Invoke-ZapApi "context/action/excludeFromContext" @{ 
                contextName = $contextName
                regex = $excludePath
            } "POST"
        }
        
        # Configure authentication
        $authMethod = Invoke-ZapApi "authentication/action/setAuthenticationMethod" @{
            contextId = $contextId
            authMethodName = "formBasedAuthentication"
            authMethodConfigParams = "loginUrl=$($Config.AuthConfig.LoginUrl)&loginRequestData=$($Config.AuthConfig.UsernameField)%3D%7B%25username%25%7D%26$($Config.AuthConfig.PasswordField)%3D%7B%25password%25%7D"
        } "POST"
        
        # Set login/logout indicators
        Invoke-ZapApi "authentication/action/setLoggedInIndicator" @{
            contextId = $contextId
            loggedInIndicatorRegex = $Config.AuthConfig.LoginIndicator
        } "POST"
        
        Invoke-ZapApi "authentication/action/setLoggedOutIndicator" @{
            contextId = $contextId
            loggedOutIndicatorRegex = $Config.AuthConfig.LogoutIndicator
        } "POST"
        
        # Create user
        $user = Invoke-ZapApi "users/action/newUser" @{
            contextId = $contextId
            name = "testuser"
        } "POST"
        
        $userId = $user.userId
        
        # Set user credentials
        Invoke-ZapApi "users/action/setAuthenticationCredentials" @{
            contextId = $contextId
            userId = $userId
            authCredentialsConfigParams = "username=$($Config.AuthConfig.Username)&password=$($Config.AuthConfig.Password)"
        } "POST"
        
        Invoke-ZapApi "users/action/setUserEnabled" @{
            contextId = $contextId
            userId = $userId
            enabled = "true"
        } "POST"
        
        Write-Log "ZAP context configured successfully" "SUCCESS"
        Write-ScanLog "CONTEXT_SETUP" "SUCCESS" "Context ID: $contextId, User ID: $userId"
        
        return @{ ContextId = $contextId; UserId = $userId }
        
    } catch {
        Write-Log "Failed to configure ZAP context: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "CONTEXT_SETUP" "FAILED" $_.Exception.Message
        throw
    }
}

function Start-SpiderScan {
    param([hashtable]$Context)
    
    Write-Log "Starting spider scan..."
    Write-ScanLog "SPIDER_SCAN" "INITIATED"
    
    try {
        # Start spider scan
        $spider = Invoke-ZapApi "spider/action/scan" @{
            url = $TargetUrl
            contextName = "HopNGo-Context"
            recurse = "true"
        } "POST"
        
        $spiderId = $spider.scan
        
        # Monitor spider progress
        do {
            Start-Sleep -Seconds 5
            $status = Invoke-ZapApi "spider/view/status" @{ scanId = $spiderId }
            $progress = [int]$status.status
            Write-Log "Spider scan progress: $progress%"
        } while ($progress -lt 100)
        
        # Get spider results
        $results = Invoke-ZapApi "spider/view/results" @{ scanId = $spiderId }
        $urlCount = $results.results.Count
        
        Write-Log "Spider scan completed: $urlCount URLs discovered" "SUCCESS"
        Write-ScanLog "SPIDER_SCAN" "SUCCESS" "URLs discovered: $urlCount"
        
        return $urlCount
        
    } catch {
        Write-Log "Spider scan failed: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "SPIDER_SCAN" "FAILED" $_.Exception.Message
        throw
    }
}

function Start-ActiveScan {
    param([hashtable]$Context)
    
    Write-Log "Starting active security scan..."
    Write-ScanLog "ACTIVE_SCAN" "INITIATED"
    
    try {
        # Configure scan policy
        $policyName = "HopNGo-Policy"
        Invoke-ZapApi "ascan/action/addScanPolicy" @{ scanPolicyName = $policyName } "POST"
        
        # Enable/disable specific rules based on scan type
        $scanRules = if ($QuickScan) { $Config.ScanPolicies.Quick } else { $Config.ScanPolicies.Full }
        
        foreach ($rule in $scanRules) {
            try {
                Invoke-ZapApi "ascan/action/enableScanners" @{
                    scanPolicyName = $policyName
                    ids = $rule
                } "POST"
            } catch {
                Write-Log "Warning: Could not enable scan rule: $rule" "WARN"
            }
        }
        
        # Start active scan
        $scan = Invoke-ZapApi "ascan/action/scan" @{
            url = $TargetUrl
            recurse = "true"
            inScopeOnly = "true"
            scanPolicyName = $policyName
            contextId = $Context.ContextId
            userId = $Context.UserId
        } "POST"
        
        $scanId = $scan.scan
        
        # Monitor scan progress
        $startTime = Get-Date
        do {
            Start-Sleep -Seconds 10
            $status = Invoke-ZapApi "ascan/view/status" @{ scanId = $scanId }
            $progress = [int]$status.status
            
            $elapsed = (Get-Date) - $startTime
            Write-Log "Active scan progress: $progress% (elapsed: $($elapsed.ToString('hh\:mm\:ss')))"
            
            # Check timeout
            if ($elapsed.TotalSeconds -gt $Config.ScanTimeout) {
                Write-Log "Scan timeout reached, stopping scan" "WARN"
                Invoke-ZapApi "ascan/action/stop" @{ scanId = $scanId } "POST"
                break
            }
            
        } while ($progress -lt 100)
        
        Write-Log "Active scan completed" "SUCCESS"
        Write-ScanLog "ACTIVE_SCAN" "SUCCESS" "Scan ID: $scanId"
        
        return $scanId
        
    } catch {
        Write-Log "Active scan failed: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "ACTIVE_SCAN" "FAILED" $_.Exception.Message
        throw
    }
}

function Get-ScanResults {
    Write-Log "Retrieving scan results..."
    Write-ScanLog "RESULTS_RETRIEVAL" "INITIATED"
    
    try {
        # Get all alerts
        $alerts = Invoke-ZapApi "core/view/alerts" @{ baseurl = $TargetUrl }
        
        if (!$alerts.alerts) {
            Write-Log "No security alerts found" "SUCCESS"
            return @()
        }
        
        $alertList = $alerts.alerts
        $totalAlerts = $alertList.Count
        
        # Categorize alerts by risk level
        $riskCounts = @{
            High = ($alertList | Where-Object { $_.risk -eq "High" }).Count
            Medium = ($alertList | Where-Object { $_.risk -eq "Medium" }).Count
            Low = ($alertList | Where-Object { $_.risk -eq "Low" }).Count
            Informational = ($alertList | Where-Object { $_.risk -eq "Informational" }).Count
        }
        
        Write-Log "Scan results: $totalAlerts total alerts" "INFO"
        Write-Log "  High: $($riskCounts.High), Medium: $($riskCounts.Medium), Low: $($riskCounts.Low), Info: $($riskCounts.Informational)" "INFO"
        
        if ($riskCounts.High -gt 0) {
            Write-Log "CRITICAL: $($riskCounts.High) high-risk vulnerabilities found!" "CRITICAL"
        }
        
        Write-ScanLog "RESULTS_RETRIEVAL" "SUCCESS" "Total alerts: $totalAlerts, High: $($riskCounts.High), Medium: $($riskCounts.Medium)"
        
        return $alertList
        
    } catch {
        Write-Log "Failed to retrieve scan results: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "RESULTS_RETRIEVAL" "FAILED" $_.Exception.Message
        throw
    }
}

function Export-ScanReports {
    param([array]$Alerts)
    
    Write-Log "Generating scan reports..."
    Write-ScanLog "REPORT_GENERATION" "INITIATED"
    
    try {
        # Ensure report directory exists
        if (!(Test-Path $ReportDir)) {
            New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
        }
        
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $reportBase = "dast-scan-$timestamp"
        
        foreach ($format in $Config.ReportFormats) {
            $reportFile = Join-Path $ReportDir "$reportBase.$format"
            
            try {
                switch ($format) {
                    "html" {
                        $report = Invoke-ZapApi "core/other/htmlreport"
                        $report | Out-File -FilePath $reportFile -Encoding UTF8
                    }
                    "json" {
                        $report = Invoke-ZapApi "core/view/alerts" @{ baseurl = $TargetUrl }
                        $report | ConvertTo-Json -Depth 10 | Out-File -FilePath $reportFile -Encoding UTF8
                    }
                    "xml" {
                        $report = Invoke-ZapApi "core/other/xmlreport"
                        $report | Out-File -FilePath $reportFile -Encoding UTF8
                    }
                }
                
                Write-Log "Report generated: $reportFile" "SUCCESS"
                
            } catch {
                Write-Log "Failed to generate $format report: $($_.Exception.Message)" "ERROR"
            }
        }
        
        # Generate summary report
        $summaryFile = Join-Path $ReportDir "$reportBase-summary.txt"
        $summary = Generate-ScanSummary $Alerts
        $summary | Out-File -FilePath $summaryFile -Encoding UTF8
        
        Write-Log "Scan reports generated in: $ReportDir" "SUCCESS"
        Write-ScanLog "REPORT_GENERATION" "SUCCESS" "Reports: $($Config.ReportFormats -join ', ')"
        
    } catch {
        Write-Log "Failed to generate reports: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "REPORT_GENERATION" "FAILED" $_.Exception.Message
    }
}

function Generate-ScanSummary {
    param([array]$Alerts)
    
    $summary = @()
    $summary += "HopNGo DAST Security Scan Summary"
    $summary += "=" * 50
    $summary += "Scan Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    $summary += "Target URL: $TargetUrl"
    $summary += "Scan Type: $(if ($QuickScan) { 'Quick' } elseif ($FullScan) { 'Full' } else { 'Standard' })"
    $summary += ""
    
    if ($Alerts.Count -eq 0) {
        $summary += "[SUCCESS] No security vulnerabilities found!"
        return $summary
    }
    
    # Risk level summary
    $riskCounts = @{
        High = ($Alerts | Where-Object { $_.risk -eq "High" }).Count
        Medium = ($Alerts | Where-Object { $_.risk -eq "Medium" }).Count
        Low = ($Alerts | Where-Object { $_.risk -eq "Low" }).Count
        Informational = ($Alerts | Where-Object { $_.risk -eq "Informational" }).Count
    }
    
    $summary += "Risk Level Summary:"
    $summary += "  [HIGH] High Risk: $($riskCounts.High)"
    $summary += "  [MED]  Medium Risk: $($riskCounts.Medium)"
    $summary += "  [LOW]  Low Risk: $($riskCounts.Low)"
    $summary += "  [INFO] Informational: $($riskCounts.Informational)"
    $summary += ""
    
    # High-risk vulnerabilities detail
    if ($riskCounts.High -gt 0) {
        $summary += "[CRITICAL] HIGH RISK VULNERABILITIES (IMMEDIATE ACTION REQUIRED):"
        $summary += "-" * 60
        
        $highRiskAlerts = $Alerts | Where-Object { $_.risk -eq "High" }
        foreach ($alert in $highRiskAlerts) {
            $summary += "• $($alert.alert) - $($alert.url)"
            $summary += "  Description: $($alert.description)"
            $summary += "  Solution: $($alert.solution)"
            $summary += ""
        }
    }
    
    # Recommendations
    $summary += "RECOMMENDATIONS:"
    $summary += "-" * 20
    
    if ($riskCounts.High -gt 0) {
        $summary += "1. [CRITICAL] Fix all high-risk vulnerabilities immediately"
        $summary += "2. [ACTION] Review and implement suggested solutions"
        $summary += "3. [VERIFY] Re-run scan after fixes to verify remediation"
    } elseif ($riskCounts.Medium -gt 0) {
        $summary += "1. [WARNING] Address medium-risk vulnerabilities"
        $summary += "2. [REVIEW] Review security configurations"
        $summary += "3. [SCHEDULE] Schedule regular security scans"
    } else {
        $summary += "1. [SUCCESS] Good security posture maintained"
        $summary += "2. [CONTINUE] Continue regular security scanning"
        $summary += "3. [LEARN] Stay updated on security best practices"
    }
    
    return $summary
}

# Main execution function
function Start-DastScan {
    Write-Log "Starting OWASP ZAP DAST scan for HopNGo platform" "INFO"
    Write-Log "Target: $TargetUrl" "INFO"
    
    if ($ApiScan) {
        Write-Log "API Target: $ApiUrl" "INFO"
    }
    
    $scanType = if ($QuickScan) { "Quick" } elseif ($FullScan) { "Full" } else { "Standard" }
    Write-Log "Scan Type: $scanType" "INFO"
    
    Write-ScanLog "DAST_SCAN_START" "INITIATED" "Type: $scanType, Target: $TargetUrl"
    
    $zapProcess = $null
    
    try {
        # Ensure report directory exists
        if (!(Test-Path $ReportDir)) {
            New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
        }
        
        # Start ZAP
        $zapProcess = Start-ZapProxy
        
        # Configure context and authentication
        $context = Set-ZapContext
        
        # Run spider scan to discover URLs
        $urlCount = Start-SpiderScan $context
        
        # Run active security scan
        $scanId = Start-ActiveScan $context
        
        # Get scan results
        $alerts = Get-ScanResults
        
        # Generate reports
        Export-ScanReports $alerts
        
        # Determine overall result
        $highRiskCount = ($alerts | Where-Object { $_.risk -eq "High" }).Count
        $mediumRiskCount = ($alerts | Where-Object { $_.risk -eq "Medium" }).Count
        
        if ($highRiskCount -gt 0) {
            Write-Log "DAST scan completed with $highRiskCount HIGH RISK vulnerabilities!" "CRITICAL"
            Write-ScanLog "DAST_SCAN_COMPLETE" "CRITICAL" "High risk: $highRiskCount, Medium risk: $mediumRiskCount"
            exit 1
        } elseif ($mediumRiskCount -gt 0) {
            Write-Log "DAST scan completed with $mediumRiskCount medium risk vulnerabilities" "WARN"
            Write-ScanLog "DAST_SCAN_COMPLETE" "WARNING" "Medium risk: $mediumRiskCount"
        } else {
            Write-Log "DAST scan completed successfully - no high/medium risk vulnerabilities found!" "SUCCESS"
            Write-ScanLog "DAST_SCAN_COMPLETE" "SUCCESS" "No significant vulnerabilities found"
        }
        
    } catch {
        Write-Log "DAST scan failed: $($_.Exception.Message)" "ERROR"
        Write-ScanLog "DAST_SCAN_COMPLETE" "FAILED" $_.Exception.Message
        throw
        
    } finally {
        # Always stop ZAP
        if ($zapProcess) {
            Stop-ZapProxy $zapProcess
        }
    }
}

# Execute if running as script
if ($MyInvocation.InvocationName -ne '.') {
    Start-DastScan
}
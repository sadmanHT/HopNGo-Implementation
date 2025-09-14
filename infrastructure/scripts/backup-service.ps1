# HopNGo Encrypted Backup Service
# Handles automated backups with encryption, retention, and audit logging

param(
    [Parameter(Mandatory=$false)]
    [string]$BackupType = "incremental",
    
    [Parameter(Mandatory=$false)]
    [string]$Environment = "production",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$Verify = $false
)

# Configuration
$Config = @{
    BackupRoot = "D:\backups\hopngo"
    EncryptionKey = $env:BACKUP_ENCRYPTION_KEY
    RetentionDays = @{
        daily = 30
        weekly = 90
        monthly = 365
        yearly = 2555  # 7 years
    }
    Databases = @(
        @{ Name = "hopngo_users"; Type = "postgresql"; Classification = "SENSITIVE" }
        @{ Name = "hopngo_bookings"; Type = "postgresql"; Classification = "INTERNAL" }
        @{ Name = "hopngo_payments"; Type = "postgresql"; Classification = "CONFIDENTIAL" }
        @{ Name = "hopngo_analytics"; Type = "postgresql"; Classification = "INTERNAL" }
    )
    FileSystems = @(
        @{ Path = "D:\projects\HopNGo\uploads"; Classification = "SENSITIVE" }
        @{ Path = "D:\projects\HopNGo\logs"; Classification = "INTERNAL" }
        @{ Path = "D:\projects\HopNGo\config"; Classification = "CONFIDENTIAL" }
    )
    AuditLog = "D:\backups\audit\backup-audit.log"
    NotificationWebhook = $env:BACKUP_NOTIFICATION_WEBHOOK
}

# Logging functions
function Write-AuditLog {
    param(
        [string]$Action,
        [string]$Resource,
        [string]$Status,
        [string]$Details = "",
        [string]$Classification = "INTERNAL"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logEntry = @{
        timestamp = $timestamp
        action = $Action
        resource = $Resource
        status = $Status
        details = $Details
        classification = $Classification
        user = $env:USERNAME
        machine = $env:COMPUTERNAME
        process_id = $PID
    } | ConvertTo-Json -Compress
    
    # Ensure audit directory exists
    $auditDir = Split-Path $Config.AuditLog -Parent
    if (!(Test-Path $auditDir)) {
        New-Item -ItemType Directory -Path $auditDir -Force | Out-Null
    }
    
    Add-Content -Path $Config.AuditLog -Value $logEntry
    Write-Host "[AUDIT] $Action - $Resource - $Status" -ForegroundColor Cyan
}

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
        default { "White" }
    }
    
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

# Encryption functions
function Encrypt-BackupFile {
    param(
        [string]$FilePath,
        [string]$Classification
    )
    
    if ([string]::IsNullOrEmpty($Config.EncryptionKey)) {
        Write-Log "Encryption key not configured - skipping encryption" "WARN"
        return $FilePath
    }
    
    try {
        $encryptedPath = "$FilePath.enc"
        
        # Use AES-256-GCM encryption
        $key = [System.Convert]::FromBase64String($Config.EncryptionKey)
        $aes = [System.Security.Cryptography.AesGcm]::new($key)
        
        $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
        $nonce = New-Object byte[] 12
        [System.Security.Cryptography.RandomNumberGenerator]::Fill($nonce)
        
        $ciphertext = New-Object byte[] $fileBytes.Length
        $tag = New-Object byte[] 16
        
        $aes.Encrypt($nonce, $fileBytes, $ciphertext, $tag)
        
        # Combine nonce + tag + ciphertext
        $encryptedData = $nonce + $tag + $ciphertext
        [System.IO.File]::WriteAllBytes($encryptedPath, $encryptedData)
        
        # Remove original file
        Remove-Item $FilePath -Force
        
        Write-AuditLog "ENCRYPT" $FilePath "SUCCESS" "File encrypted with AES-256-GCM" $Classification
        return $encryptedPath
        
    } catch {
        Write-Log "Failed to encrypt file: $FilePath - $($_.Exception.Message)" "ERROR"
        Write-AuditLog "ENCRYPT" $FilePath "FAILED" $_.Exception.Message $Classification
        throw
    }
}

function Decrypt-BackupFile {
    param(
        [string]$EncryptedFilePath,
        [string]$OutputPath
    )
    
    if ([string]::IsNullOrEmpty($Config.EncryptionKey)) {
        Write-Log "Encryption key not configured - cannot decrypt" "ERROR"
        return $false
    }
    
    try {
        $key = [System.Convert]::FromBase64String($Config.EncryptionKey)
        $aes = [System.Security.Cryptography.AesGcm]::new($key)
        
        $encryptedData = [System.IO.File]::ReadAllBytes($EncryptedFilePath)
        
        # Extract nonce, tag, and ciphertext
        $nonce = $encryptedData[0..11]
        $tag = $encryptedData[12..27]
        $ciphertext = $encryptedData[28..($encryptedData.Length-1)]
        
        $plaintext = New-Object byte[] $ciphertext.Length
        $aes.Decrypt($nonce, $ciphertext, $tag, $plaintext)
        
        [System.IO.File]::WriteAllBytes($OutputPath, $plaintext)
        
        Write-AuditLog "DECRYPT" $EncryptedFilePath "SUCCESS" "File decrypted successfully"
        return $true
        
    } catch {
        Write-Log "Failed to decrypt file: $EncryptedFilePath - $($_.Exception.Message)" "ERROR"
        Write-AuditLog "DECRYPT" $EncryptedFilePath "FAILED" $_.Exception.Message
        return $false
    }
}

# Database backup functions
function Backup-Database {
    param(
        [hashtable]$DatabaseConfig
    )
    
    $dbName = $DatabaseConfig.Name
    $dbType = $DatabaseConfig.Type
    $classification = $DatabaseConfig.Classification
    
    Write-Log "Starting backup for database: $dbName ($classification)"
    Write-AuditLog "BACKUP_START" "DATABASE:$dbName" "INITIATED" "Type: $dbType, Classification: $classification" $classification
    
    try {
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $backupDir = Join-Path $Config.BackupRoot "databases\$dbName\$timestamp"
        
        if (!(Test-Path $backupDir)) {
            New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
        }
        
        $backupFile = Join-Path $backupDir "$dbName`_$timestamp.sql"
        
        if ($DryRun) {
            Write-Log "[DRY RUN] Would backup database $dbName to $backupFile" "INFO"
            return @{ Success = $true; Path = $backupFile; Size = 0 }
        }
        
        # PostgreSQL backup
        if ($dbType -eq "postgresql") {
            $pgDumpCmd = "pg_dump"
            $connectionString = "postgresql://localhost:5432/$dbName"
            
            & $pgDumpCmd --dbname=$connectionString --file=$backupFile --verbose --format=custom --compress=9
            
            if ($LASTEXITCODE -ne 0) {
                throw "pg_dump failed with exit code $LASTEXITCODE"
            }
        }
        
        # Verify backup file exists and has content
        if (!(Test-Path $backupFile) -or (Get-Item $backupFile).Length -eq 0) {
            throw "Backup file is missing or empty"
        }
        
        $fileSize = (Get-Item $backupFile).Length
        Write-Log "Database backup completed: $backupFile ($fileSize bytes)" "SUCCESS"
        
        # Encrypt the backup
        $encryptedFile = Encrypt-BackupFile $backupFile $classification
        
        Write-AuditLog "BACKUP_COMPLETE" "DATABASE:$dbName" "SUCCESS" "Size: $fileSize bytes, Encrypted: $encryptedFile" $classification
        
        return @{
            Success = $true
            Path = $encryptedFile
            Size = $fileSize
            Classification = $classification
        }
        
    } catch {
        Write-Log "Database backup failed: $dbName - $($_.Exception.Message)" "ERROR"
        Write-AuditLog "BACKUP_COMPLETE" "DATABASE:$dbName" "FAILED" $_.Exception.Message $classification
        
        return @{
            Success = $false
            Error = $_.Exception.Message
            Classification = $classification
        }
    }
}

# File system backup functions
function Backup-FileSystem {
    param(
        [hashtable]$FileSystemConfig
    )
    
    $sourcePath = $FileSystemConfig.Path
    $classification = $FileSystemConfig.Classification
    
    Write-Log "Starting backup for filesystem: $sourcePath ($classification)"
    Write-AuditLog "BACKUP_START" "FILESYSTEM:$sourcePath" "INITIATED" "Classification: $classification" $classification
    
    try {
        if (!(Test-Path $sourcePath)) {
            Write-Log "Source path does not exist: $sourcePath" "WARN"
            return @{ Success = $true; Skipped = $true }
        }
        
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $backupName = (Split-Path $sourcePath -Leaf) -replace '[^a-zA-Z0-9]', '_'
        $backupDir = Join-Path $Config.BackupRoot "filesystems\$backupName\$timestamp"
        
        if (!(Test-Path $backupDir)) {
            New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
        }
        
        $archiveFile = Join-Path $backupDir "$backupName`_$timestamp.zip"
        
        if ($DryRun) {
            Write-Log "[DRY RUN] Would backup filesystem $sourcePath to $archiveFile" "INFO"
            return @{ Success = $true; Path = $archiveFile; Size = 0 }
        }
        
        # Create compressed archive
        Compress-Archive -Path "$sourcePath\*" -DestinationPath $archiveFile -CompressionLevel Optimal
        
        $fileSize = (Get-Item $archiveFile).Length
        Write-Log "Filesystem backup completed: $archiveFile ($fileSize bytes)" "SUCCESS"
        
        # Encrypt the backup
        $encryptedFile = Encrypt-BackupFile $archiveFile $classification
        
        Write-AuditLog "BACKUP_COMPLETE" "FILESYSTEM:$sourcePath" "SUCCESS" "Size: $fileSize bytes, Encrypted: $encryptedFile" $classification
        
        return @{
            Success = $true
            Path = $encryptedFile
            Size = $fileSize
            Classification = $classification
        }
        
    } catch {
        Write-Log "Filesystem backup failed: $sourcePath - $($_.Exception.Message)" "ERROR"
        Write-AuditLog "BACKUP_COMPLETE" "FILESYSTEM:$sourcePath" "FAILED" $_.Exception.Message $classification
        
        return @{
            Success = $false
            Error = $_.Exception.Message
            Classification = $classification
        }
    }
}

# Retention management
function Remove-ExpiredBackups {
    Write-Log "Starting cleanup of expired backups"
    Write-AuditLog "CLEANUP_START" "RETENTION" "INITIATED" "Retention policy enforcement"
    
    $totalRemoved = 0
    $totalSize = 0
    
    try {
        foreach ($retentionType in $Config.RetentionDays.Keys) {
            $retentionDays = $Config.RetentionDays[$retentionType]
            $cutoffDate = (Get-Date).AddDays(-$retentionDays)
            
            $searchPath = Join-Path $Config.BackupRoot "*\*"
            $expiredBackups = Get-ChildItem -Path $searchPath -Directory | Where-Object {
                $_.CreationTime -lt $cutoffDate
            }
            
            foreach ($backup in $expiredBackups) {
                try {
                    $backupSize = (Get-ChildItem -Path $backup.FullName -Recurse -File | Measure-Object -Property Length -Sum).Sum
                    
                    if ($DryRun) {
                        Write-Log "[DRY RUN] Would remove expired backup: $($backup.FullName) ($backupSize bytes)" "INFO"
                    } else {
                        Remove-Item -Path $backup.FullName -Recurse -Force
                        Write-Log "Removed expired backup: $($backup.FullName) ($backupSize bytes)" "SUCCESS"
                    }
                    
                    $totalRemoved++
                    $totalSize += $backupSize
                    
                    Write-AuditLog "CLEANUP_REMOVE" $backup.FullName "SUCCESS" "Size: $backupSize bytes, Age: $retentionType"
                    
                } catch {
                    Write-Log "Failed to remove backup: $($backup.FullName) - $($_.Exception.Message)" "ERROR"
                    Write-AuditLog "CLEANUP_REMOVE" $backup.FullName "FAILED" $_.Exception.Message
                }
            }
        }
        
        Write-Log "Cleanup completed: Removed $totalRemoved backups ($totalSize bytes)" "SUCCESS"
        Write-AuditLog "CLEANUP_COMPLETE" "RETENTION" "SUCCESS" "Removed: $totalRemoved backups, Size: $totalSize bytes"
        
    } catch {
        Write-Log "Cleanup failed: $($_.Exception.Message)" "ERROR"
        Write-AuditLog "CLEANUP_COMPLETE" "RETENTION" "FAILED" $_.Exception.Message
    }
}

# Verification functions
function Test-BackupIntegrity {
    param(
        [string]$BackupPath
    )
    
    Write-Log "Verifying backup integrity: $BackupPath"
    Write-AuditLog "VERIFY_START" $BackupPath "INITIATED" "Integrity verification"
    
    try {
        if (!(Test-Path $BackupPath)) {
            throw "Backup file not found: $BackupPath"
        }
        
        # For encrypted files, try to decrypt to temp location
        if ($BackupPath.EndsWith(".enc")) {
            $tempFile = [System.IO.Path]::GetTempFileName()
            
            if (Decrypt-BackupFile $BackupPath $tempFile) {
                # Verify the decrypted file
                if ((Get-Item $tempFile).Length -gt 0) {
                    Remove-Item $tempFile -Force
                    Write-Log "Backup integrity verified: $BackupPath" "SUCCESS"
                    Write-AuditLog "VERIFY_COMPLETE" $BackupPath "SUCCESS" "Integrity check passed"
                    return $true
                } else {
                    Remove-Item $tempFile -Force -ErrorAction SilentlyContinue
                    throw "Decrypted file is empty"
                }
            } else {
                throw "Failed to decrypt backup file"
            }
        } else {
            # For unencrypted files, just check if readable
            $fileInfo = Get-Item $BackupPath
            if ($fileInfo.Length -gt 0) {
                Write-Log "Backup integrity verified: $BackupPath" "SUCCESS"
                Write-AuditLog "VERIFY_COMPLETE" $BackupPath "SUCCESS" "Integrity check passed"
                return $true
            } else {
                throw "Backup file is empty"
            }
        }
        
    } catch {
        Write-Log "Backup integrity check failed: $BackupPath - $($_.Exception.Message)" "ERROR"
        Write-AuditLog "VERIFY_COMPLETE" $BackupPath "FAILED" $_.Exception.Message
        return $false
    }
}

# Notification functions
function Send-BackupNotification {
    param(
        [hashtable]$Results
    )
    
    if ([string]::IsNullOrEmpty($Config.NotificationWebhook)) {
        return
    }
    
    try {
        $summary = @{
            timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
            environment = $Environment
            backup_type = $BackupType
            total_backups = $Results.Count
            successful = ($Results | Where-Object { $_.Success }).Count
            failed = ($Results | Where-Object { -not $_.Success }).Count
            total_size = ($Results | Where-Object { $_.Success } | Measure-Object -Property Size -Sum).Sum
        }
        
        $payload = @{
            text = "HopNGo Backup Report - $Environment"
            attachments = @(
                @{
                    color = if ($summary.failed -eq 0) { "good" } else { "warning" }
                    fields = @(
                        @{ title = "Environment"; value = $summary.environment; short = $true }
                        @{ title = "Backup Type"; value = $summary.backup_type; short = $true }
                        @{ title = "Successful"; value = $summary.successful; short = $true }
                        @{ title = "Failed"; value = $summary.failed; short = $true }
                        @{ title = "Total Size"; value = "$([math]::Round($summary.total_size / 1MB, 2)) MB"; short = $true }
                        @{ title = "Timestamp"; value = $summary.timestamp; short = $true }
                    )
                }
            )
        } | ConvertTo-Json -Depth 10
        
        Invoke-RestMethod -Uri $Config.NotificationWebhook -Method Post -Body $payload -ContentType "application/json"
        Write-Log "Backup notification sent successfully" "SUCCESS"
        
    } catch {
        Write-Log "Failed to send backup notification: $($_.Exception.Message)" "ERROR"
    }
}

# Main execution
function Start-BackupProcess {
    Write-Log "Starting HopNGo backup process - Type: $BackupType, Environment: $Environment" "INFO"
    
    if ($DryRun) {
        Write-Log "DRY RUN MODE - No actual backups will be performed" "WARN"
    }
    
    Write-AuditLog "BACKUP_SESSION_START" "SYSTEM" "INITIATED" "Type: $BackupType, Environment: $Environment, DryRun: $DryRun"
    
    $results = @()
    
    try {
        # Ensure backup root directory exists
        if (!(Test-Path $Config.BackupRoot)) {
            New-Item -ItemType Directory -Path $Config.BackupRoot -Force | Out-Null
        }
        
        # Backup databases
        Write-Log "Starting database backups..."
        foreach ($db in $Config.Databases) {
            $result = Backup-Database $db
            $results += $result
        }
        
        # Backup file systems
        Write-Log "Starting filesystem backups..."
        foreach ($fs in $Config.FileSystems) {
            $result = Backup-FileSystem $fs
            $results += $result
        }
        
        # Verify backups if requested
        if ($Verify) {
            Write-Log "Starting backup verification..."
            foreach ($result in $results) {
                if ($result.Success -and $result.Path) {
                    Test-BackupIntegrity $result.Path
                }
            }
        }
        
        # Clean up expired backups
        if ($BackupType -eq "maintenance") {
            Remove-ExpiredBackups
        }
        
        # Send notification
        Send-BackupNotification $results
        
        $successCount = ($results | Where-Object { $_.Success }).Count
        $totalCount = $results.Count
        
        Write-Log "Backup process completed: $successCount/$totalCount successful" "SUCCESS"
        Write-AuditLog "BACKUP_SESSION_COMPLETE" "SYSTEM" "SUCCESS" "Completed: $successCount/$totalCount backups"
        
    } catch {
        Write-Log "Backup process failed: $($_.Exception.Message)" "ERROR"
        Write-AuditLog "BACKUP_SESSION_COMPLETE" "SYSTEM" "FAILED" $_.Exception.Message
        throw
    }
}

# Execute if running as script
if ($MyInvocation.InvocationName -ne '.') {
    Start-BackupProcess
}
# JWT Key Rotation Script for HopNGo Auth Service (PowerShell)
# This script generates new RSA key pairs for JWT signing on Windows

param(
    [int]$KeySize = 2048,
    [string]$KeysDir = "./keys",
    [switch]$Force
)

# Configuration
$BackupDir = "$KeysDir/backup"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# Colors for output
$Colors = @{
    Red = "Red"
    Green = "Green"
    Yellow = "Yellow"
    Blue = "Cyan"
    White = "White"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Colors[$Color]
}

Write-ColorOutput "=== JWT Key Rotation Script ===" "Blue"
Write-ColorOutput "Timestamp: $Timestamp" "Blue"
Write-Host

# Create directories
if (-not (Test-Path $KeysDir)) {
    New-Item -ItemType Directory -Path $KeysDir -Force | Out-Null
}
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

# Function to check if OpenSSL is available
function Test-OpenSSL {
    try {
        $null = Get-Command openssl -ErrorAction Stop
        return $true
    }
    catch {
        Write-ColorOutput "❌ OpenSSL not found in PATH" "Red"
        Write-ColorOutput "Please install OpenSSL or use Windows Subsystem for Linux (WSL)" "Yellow"
        Write-ColorOutput "Download from: https://slproweb.com/products/Win32OpenSSL.html" "Yellow"
        return $false
    }
}

# Function to backup existing keys
function Backup-ExistingKeys {
    $privateKeyPath = "$KeysDir/private_key.pem"
    $publicKeyPath = "$KeysDir/public_key.pem"
    
    if ((Test-Path $privateKeyPath) -or (Test-Path $publicKeyPath)) {
        Write-ColorOutput "Backing up existing keys..." "Yellow"
        
        if (Test-Path $privateKeyPath) {
            Copy-Item $privateKeyPath "$BackupDir/private_key_$Timestamp.pem"
            Write-ColorOutput "  ✓ Backed up private key to $BackupDir/private_key_$Timestamp.pem" "Green"
        }
        
        if (Test-Path $publicKeyPath) {
            Copy-Item $publicKeyPath "$BackupDir/public_key_$Timestamp.pem"
            Write-ColorOutput "  ✓ Backed up public key to $BackupDir/public_key_$Timestamp.pem" "Green"
        }
        Write-Host
    }
}

# Function to generate new RSA key pair
function New-RSAKeyPair {
    Write-ColorOutput "Generating new RSA key pair ($KeySize bits)..." "Green"
    
    $privateKeyPath = "$KeysDir/private_key.pem"
    $publicKeyPath = "$KeysDir/public_key.pem"
    
    try {
        # Generate private key
        $genRsaResult = & openssl genrsa -out $privateKeyPath $KeySize 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to generate private key: $genRsaResult"
        }
        Write-ColorOutput "  ✓ Generated private key: $privateKeyPath" "Green"
        
        # Extract public key from private key
        $extractPubResult = & openssl rsa -in $privateKeyPath -pubout -out $publicKeyPath 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to extract public key: $extractPubResult"
        }
        Write-ColorOutput "  ✓ Generated public key: $publicKeyPath" "Green"
        Write-Host
        
        return $true
    }
    catch {
        Write-ColorOutput "❌ Key generation failed: $_" "Red"
        return $false
    }
}

# Function to convert keys to base64 for environment variables
function New-EnvironmentVariables {
    Write-ColorOutput "Generating environment variables..." "Green"
    
    try {
        $privateKeyPath = "$KeysDir/private_key.pem"
        $publicKeyPath = "$KeysDir/public_key.pem"
        
        # Read and process private key
        $privateKeyContent = Get-Content $privateKeyPath | Where-Object { 
            $_ -notmatch "-----BEGIN" -and $_ -notmatch "-----END" 
        } | Join-String
        
        # Read and process public key
        $publicKeyContent = Get-Content $publicKeyPath | Where-Object { 
            $_ -notmatch "-----BEGIN" -and $_ -notmatch "-----END" 
        } | Join-String
        
        # Create environment file
        $envFile = "$KeysDir/jwt-keys.env"
        $envContent = @"
# JWT RSA Keys for HopNGo Auth Service
# Generated on: $(Get-Date)
# Key size: $KeySize bits

# Private key for JWT signing
JWT_RSA_PRIVATE_KEY=$privateKeyContent

# Public key for JWT verification
JWT_RSA_PUBLIC_KEY=$publicKeyContent
"@
        
        Set-Content -Path $envFile -Value $envContent -Encoding UTF8
        Write-ColorOutput "  ✓ Environment variables saved to: $envFile" "Green"
        
        # Create PowerShell environment file
        $psEnvFile = "$KeysDir/jwt-keys.ps1"
        $psEnvContent = @"
# JWT RSA Keys for HopNGo Auth Service (PowerShell)
# Generated on: $(Get-Date)
# Usage: . .\jwt-keys.ps1

`$env:JWT_RSA_PRIVATE_KEY = "$privateKeyContent"
`$env:JWT_RSA_PUBLIC_KEY = "$publicKeyContent"

Write-Host "JWT environment variables loaded" -ForegroundColor Green
"@
        
        Set-Content -Path $psEnvFile -Value $psEnvContent -Encoding UTF8
        Write-ColorOutput "  ✓ PowerShell environment file saved to: $psEnvFile" "Green"
        Write-Host
        
        return $true
    }
    catch {
        Write-ColorOutput "❌ Failed to generate environment variables: $_" "Red"
        return $false
    }
}

# Function to create Kubernetes secret manifest
function New-KubernetesSecret {
    Write-ColorOutput "Generating Kubernetes secret manifest..." "Green"
    
    try {
        $privateKeyPath = "$KeysDir/private_key.pem"
        $publicKeyPath = "$KeysDir/public_key.pem"
        
        # Get base64 encoded keys for Kubernetes
        $privateKeyContent = Get-Content $privateKeyPath | Where-Object { 
            $_ -notmatch "-----BEGIN" -and $_ -notmatch "-----END" 
        } | Join-String
        $privateKeyB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($privateKeyContent))
        
        $publicKeyContent = Get-Content $publicKeyPath | Where-Object { 
            $_ -notmatch "-----BEGIN" -and $_ -notmatch "-----END" 
        } | Join-String
        $publicKeyB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($publicKeyContent))
        
        $k8sSecretFile = "$KeysDir/jwt-keys-secret.yaml"
        $k8sContent = @"
apiVersion: v1
kind: Secret
metadata:
  name: jwt-keys
  namespace: hopngo
type: Opaque
data:
  JWT_RSA_PRIVATE_KEY: $privateKeyB64
  JWT_RSA_PUBLIC_KEY: $publicKeyB64
"@
        
        Set-Content -Path $k8sSecretFile -Value $k8sContent -Encoding UTF8
        Write-ColorOutput "  ✓ Kubernetes secret manifest saved to: $k8sSecretFile" "Green"
        Write-Host
        
        return $true
    }
    catch {
        Write-ColorOutput "❌ Failed to generate Kubernetes secret: $_" "Red"
        return $false
    }
}

# Function to verify key generation
function Test-GeneratedKeys {
    Write-ColorOutput "Verifying generated keys..." "Green"
    
    try {
        $privateKeyPath = "$KeysDir/private_key.pem"
        $publicKeyPath = "$KeysDir/public_key.pem"
        
        # Test private key validity
        $privateKeyCheck = & openssl rsa -in $privateKeyPath -check -noout 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Private key validation failed: $privateKeyCheck"
        }
        Write-ColorOutput "  ✓ Private key is valid" "Green"
        
        # Test public key validity
        $publicKeyCheck = & openssl rsa -in $publicKeyPath -pubin -text -noout 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Public key validation failed: $publicKeyCheck"
        }
        Write-ColorOutput "  ✓ Public key is valid" "Green"
        
        # Test that keys match
        $privateModulus = & openssl rsa -in $privateKeyPath -modulus -noout 2>&1
        $publicModulus = & openssl rsa -in $publicKeyPath -pubin -modulus -noout 2>&1
        
        if ($privateModulus -eq $publicModulus) {
            Write-ColorOutput "  ✓ Private and public keys match" "Green"
        } else {
            throw "Private and public keys do not match"
        }
        
        Write-Host
        return $true
    }
    catch {
        Write-ColorOutput "❌ Key verification failed: $_" "Red"
        return $false
    }
}

# Function to display deployment instructions
function Show-DeploymentInstructions {
    Write-ColorOutput "=== Deployment Instructions ===" "Blue"
    Write-Host
    
    Write-ColorOutput "1. For Docker/Docker Compose:" "Yellow"
    Write-Host "   Add these environment variables to your docker-compose.yml or .env file:"
    Write-Host "   (Copy from: $KeysDir/jwt-keys.env)"
    Write-Host
    
    Write-ColorOutput "2. For Kubernetes:" "Yellow"
    Write-Host "   Apply the secret manifest:"
    Write-Host "   kubectl apply -f $KeysDir/jwt-keys-secret.yaml"
    Write-Host
    
    Write-ColorOutput "3. For Local Development (PowerShell):" "Yellow"
    Write-Host "   Load the environment variables:"
    Write-Host "   . .\$KeysDir\jwt-keys.ps1"
    Write-Host "   Or add to your IDE's run configuration"
    Write-Host
    
    Write-ColorOutput "4. For Local Development (Command Prompt):" "Yellow"
    Write-Host "   Set environment variables manually or use the .env file"
    Write-Host
    
    Write-ColorOutput "5. Rolling Deployment Strategy:" "Yellow"
    Write-Host "   a) Deploy new keys to all instances"
    Write-Host "   b) Restart auth-service instances one by one"
    Write-Host "   c) Verify JWT validation works across all services"
    Write-Host "   d) Monitor for authentication errors"
    Write-Host
    
    Write-ColorOutput "⚠️  IMPORTANT SECURITY NOTES:" "Red"
    Write-Host "   • Keep the private key secure and never commit to version control"
    Write-Host "   • Rotate keys regularly (recommended: every 90 days)"
    Write-Host "   • Use different keys for different environments"
    Write-Host "   • Monitor for any authentication failures after rotation"
    Write-Host "   • Keep backup keys until all old tokens expire"
    Write-Host
}

# Main execution
Write-ColorOutput "This script will generate new RSA keys for JWT signing." "Yellow"
Write-ColorOutput "Existing keys will be backed up automatically." "Yellow"
Write-Host

if (-not $Force) {
    $confirmation = Read-Host "Continue? (y/N)"
    if ($confirmation -notmatch "^[Yy]$") {
        Write-Host "Operation cancelled."
        exit 0
    }
}

# Check prerequisites
if (-not (Test-OpenSSL)) {
    exit 1
}

# Execute key rotation steps
try {
    Backup-ExistingKeys
    
    if (-not (New-RSAKeyPair)) {
        throw "Key generation failed"
    }
    
    if (-not (Test-GeneratedKeys)) {
        throw "Key verification failed"
    }
    
    if (-not (New-EnvironmentVariables)) {
        throw "Environment variable generation failed"
    }
    
    if (-not (New-KubernetesSecret)) {
        throw "Kubernetes secret generation failed"
    }
    
    Show-DeploymentInstructions
    
    Write-ColorOutput "✅ JWT key rotation completed successfully!" "Green"
    Write-ColorOutput "New keys generated with timestamp: $Timestamp" "Green"
}
catch {
    Write-ColorOutput "❌ Key rotation failed: $_" "Red"
    exit 1
}
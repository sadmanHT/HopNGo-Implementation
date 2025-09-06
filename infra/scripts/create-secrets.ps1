# HopNGo Kubernetes Secrets Creation Script (PowerShell)
# This script creates all necessary secrets for the HopNGo application

param(
    [Parameter(Position=0)]
    [string]$Environment = "dev",
    [switch]$Help
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Red
}

# Function to check if kubectl is available
function Test-Kubectl {
    try {
        kubectl version --client --output=json | Out-Null
        Write-Success "kubectl is available"
        return $true
    }
    catch {
        Write-Error "kubectl is not installed or not in PATH"
        return $false
    }
}

# Function to create namespace if it doesn't exist
function New-Namespace {
    param([string]$Namespace)
    
    try {
        kubectl get namespace $Namespace 2>$null | Out-Null
        Write-Warning "Namespace $Namespace already exists"
    }
    catch {
        kubectl create namespace $Namespace
        Write-Success "Created namespace: $Namespace"
    }
}

# Function to create database secret
function New-DatabaseSecret {
    param([string]$Namespace)
    
    $username = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "hopngo" }
    $password = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "hopngo123" }
    
    Write-Status "Creating database secret in namespace: $Namespace"
    
    kubectl create secret generic database-secret `
        --from-literal=username="$username" `
        --from-literal=password="$password" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "Database secret created"
}

# Function to create MongoDB secret
function New-MongoDBSecret {
    param([string]$Namespace)
    
    $connectionString = if ($env:MONGODB_CONNECTION_STRING) { $env:MONGODB_CONNECTION_STRING } else { "mongodb://hopngo:hopngo123@mongodb:27017/hopngo" }
    
    Write-Status "Creating MongoDB secret in namespace: $Namespace"
    
    kubectl create secret generic mongodb-secret `
        --from-literal=connection-string="$connectionString" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "MongoDB secret created"
}

# Function to create Redis secret
function New-RedisSecret {
    param([string]$Namespace)
    
    $password = if ($env:REDIS_PASSWORD) { $env:REDIS_PASSWORD } else { "hopngoredis" }
    
    Write-Status "Creating Redis secret in namespace: $Namespace"
    
    kubectl create secret generic redis-secret `
        --from-literal=password="$password" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "Redis secret created"
}

# Function to create RabbitMQ secret
function New-RabbitMQSecret {
    param([string]$Namespace)
    
    $username = if ($env:RABBITMQ_USERNAME) { $env:RABBITMQ_USERNAME } else { "hopngo" }
    $password = if ($env:RABBITMQ_PASSWORD) { $env:RABBITMQ_PASSWORD } else { "hopngorabbit" }
    
    Write-Status "Creating RabbitMQ secret in namespace: $Namespace"
    
    kubectl create secret generic rabbitmq-secret `
        --from-literal=username="$username" `
        --from-literal=password="$password" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "RabbitMQ secret created"
}

# Function to create JWT secret
function New-JWTSecret {
    param([string]$Namespace)
    
    $privateKeyFile = if ($env:JWT_PRIVATE_KEY_FILE) { $env:JWT_PRIVATE_KEY_FILE } else { ".\keys\jwt-private.key" }
    $publicKeyFile = if ($env:JWT_PUBLIC_KEY_FILE) { $env:JWT_PUBLIC_KEY_FILE } else { ".\keys\jwt-public.key" }
    
    Write-Status "Creating JWT secret in namespace: $Namespace"
    
    if ((Test-Path $privateKeyFile) -and (Test-Path $publicKeyFile)) {
        kubectl create secret generic jwt-secret `
            --from-file=private-key="$privateKeyFile" `
            --from-file=public-key="$publicKeyFile" `
            --namespace="$Namespace" `
            --dry-run=client -o yaml | kubectl apply -f -
    }
    else {
        Write-Warning "JWT key files not found. Creating with placeholder values."
        kubectl create secret generic jwt-secret `
            --from-literal=private-key="placeholder-private-key" `
            --from-literal=public-key="placeholder-public-key" `
            --namespace="$Namespace" `
            --dry-run=client -o yaml | kubectl apply -f -
    }
    
    Write-Success "JWT secret created"
}

# Function to create payment secret
function New-PaymentSecret {
    param([string]$Namespace)
    
    $stripeSecretKey = if ($env:STRIPE_SECRET_KEY) { $env:STRIPE_SECRET_KEY } else { "sk_test_placeholder" }
    $stripeWebhookSecret = if ($env:STRIPE_WEBHOOK_SECRET) { $env:STRIPE_WEBHOOK_SECRET } else { "whsec_placeholder" }
    
    Write-Status "Creating payment secret in namespace: $Namespace"
    
    kubectl create secret generic payment-secret `
        --from-literal=stripe-secret-key="$stripeSecretKey" `
        --from-literal=stripe-webhook-secret="$stripeWebhookSecret" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "Payment secret created"
}

# Function to create OpenAI secret
function New-OpenAISecret {
    param([string]$Namespace)
    
    $apiKey = if ($env:OPENAI_API_KEY) { $env:OPENAI_API_KEY } else { "sk-placeholder" }
    
    Write-Status "Creating OpenAI secret in namespace: $Namespace"
    
    kubectl create secret generic openai-secret `
        --from-literal=api-key="$apiKey" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "OpenAI secret created"
}

# Function to create Google Maps secret
function New-MapsSecret {
    param([string]$Namespace)
    
    $apiKey = if ($env:GOOGLE_MAPS_API_KEY) { $env:GOOGLE_MAPS_API_KEY } else { "AIzaSyA_placeholder" }
    
    Write-Status "Creating Google Maps secret in namespace: $Namespace"
    
    kubectl create secret generic maps-secret `
        --from-literal=google-api-key="$apiKey" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "Google Maps secret created"
}

# Function to create Firebase secret
function New-FirebaseSecret {
    param([string]$Namespace)
    
    $credentialsFile = if ($env:FIREBASE_CREDENTIALS_FILE) { $env:FIREBASE_CREDENTIALS_FILE } else { ".\keys\firebase-credentials.json" }
    
    Write-Status "Creating Firebase secret in namespace: $Namespace"
    
    if (Test-Path $credentialsFile) {
        kubectl create secret generic firebase-secret `
            --from-file=credentials="$credentialsFile" `
            --namespace="$Namespace" `
            --dry-run=client -o yaml | kubectl apply -f -
    }
    else {
        Write-Warning "Firebase credentials file not found. Creating with placeholder."
        kubectl create secret generic firebase-secret `
            --from-literal=credentials='{"type": "service_account", "project_id": "hopngo-notifications"}' `
            --namespace="$Namespace" `
            --dry-run=client -o yaml | kubectl apply -f -
    }
    
    Write-Success "Firebase secret created"
}

# Function to create frontend secret
function New-FrontendSecret {
    param([string]$Namespace)
    
    $nextauthSecret = if ($env:NEXTAUTH_SECRET) { $env:NEXTAUTH_SECRET } else { "nextauth_secret_placeholder" }
    
    Write-Status "Creating frontend secret in namespace: $Namespace"
    
    kubectl create secret generic frontend-secret `
        --from-literal=nextauth-secret="$nextauthSecret" `
        --namespace="$Namespace" `
        --dry-run=client -o yaml | kubectl apply -f -
    
    Write-Success "Frontend secret created"
}

# Help function
function Show-Help {
    Write-Host "Usage: .\create-secrets.ps1 [ENVIRONMENT]"
    Write-Host ""
    Write-Host "Creates Kubernetes secrets for HopNGo application"
    Write-Host ""
    Write-Host "Arguments:"
    Write-Host "  ENVIRONMENT    Target environment (dev, staging, production). Default: dev"
    Write-Host ""
    Write-Host "Environment Variables:"
    Write-Host "  DB_USERNAME              Database username (default: hopngo)"
    Write-Host "  DB_PASSWORD              Database password (default: hopngo123)"
    Write-Host "  MONGODB_CONNECTION_STRING MongoDB connection string"
    Write-Host "  REDIS_PASSWORD           Redis password (default: hopngoredis)"
    Write-Host "  RABBITMQ_USERNAME        RabbitMQ username (default: hopngo)"
    Write-Host "  RABBITMQ_PASSWORD        RabbitMQ password (default: hopngorabbit)"
    Write-Host "  JWT_PRIVATE_KEY_FILE     Path to JWT private key file"
    Write-Host "  JWT_PUBLIC_KEY_FILE      Path to JWT public key file"
    Write-Host "  STRIPE_SECRET_KEY        Stripe secret key"
    Write-Host "  STRIPE_WEBHOOK_SECRET    Stripe webhook secret"
    Write-Host "  OPENAI_API_KEY           OpenAI API key"
    Write-Host "  GOOGLE_MAPS_API_KEY      Google Maps API key"
    Write-Host "  FIREBASE_CREDENTIALS_FILE Path to Firebase credentials JSON file"
    Write-Host "  NEXTAUTH_SECRET          NextAuth secret"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\create-secrets.ps1 dev"
    Write-Host "  .\create-secrets.ps1 staging"
    Write-Host "  .\create-secrets.ps1 production"
}

# Main function
function Main {
    param([string]$Environment)
    
    if ($Help) {
        Show-Help
        return
    }
    
    $namespace = "hopngo-$Environment"
    
    Write-Status "Creating secrets for environment: $Environment"
    Write-Status "Target namespace: $namespace"
    
    # Check prerequisites
    if (-not (Test-Kubectl)) {
        exit 1
    }
    
    # Create namespace
    New-Namespace $namespace
    
    # Create all secrets
    try {
        New-DatabaseSecret $namespace
        New-MongoDBSecret $namespace
        New-RedisSecret $namespace
        New-RabbitMQSecret $namespace
        New-JWTSecret $namespace
        New-PaymentSecret $namespace
        New-OpenAISecret $namespace
        New-MapsSecret $namespace
        New-FirebaseSecret $namespace
        New-FrontendSecret $namespace
        
        Write-Success "All secrets created successfully for environment: $Environment"
        Write-Status "You can now deploy the application using: kubectl apply -k infra/k8s/overlays/$Environment"
    }
    catch {
        Write-Error "Failed to create secrets: $($_.Exception.Message)"
        exit 1
    }
}

# Run main function
Main -Environment $Environment
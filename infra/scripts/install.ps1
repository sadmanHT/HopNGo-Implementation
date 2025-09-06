# HopNGo Infrastructure Installation Script (PowerShell)
# This script installs and manages the HopNGo application infrastructure

param(
    [Parameter(Position=0)]
    [string]$Command = "help",
    [Parameter(Position=1)]
    [string]$Environment = "dev",
    [switch]$SkipSecrets,
    [switch]$SkipInfra,
    [switch]$Help
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"
$Magenta = "Magenta"

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

function Write-Header {
    param([string]$Message)
    Write-Host "\n=== $Message ===" -ForegroundColor $Magenta
}

# Function to check prerequisites
function Test-Prerequisites {
    Write-Header "Checking Prerequisites"
    
    $allGood = $true
    
    # Check kubectl
    try {
        kubectl version --client --output=json | Out-Null
        Write-Success "kubectl is available"
    }
    catch {
        Write-Error "kubectl is not installed or not in PATH"
        $allGood = $false
    }
    
    # Check helm
    try {
        helm version --short | Out-Null
        Write-Success "helm is available"
    }
    catch {
        Write-Error "helm is not installed or not in PATH"
        $allGood = $false
    }
    
    # Check kustomize
    try {
        kubectl kustomize --help | Out-Null
        Write-Success "kustomize is available (via kubectl)"
    }
    catch {
        Write-Error "kustomize is not available"
        $allGood = $false
    }
    
    if (-not $allGood) {
        Write-Error "Prerequisites not met. Please install missing tools."
        exit 1
    }
    
    Write-Success "All prerequisites met"
}

# Function to install infrastructure components
function Install-Infrastructure {
    param([string]$Environment)
    
    if ($SkipInfra) {
        Write-Warning "Skipping infrastructure installation"
        return
    }
    
    Write-Header "Installing Infrastructure Components"
    
    $namespace = "hopngo-$Environment"
    
    # Create namespace if it doesn't exist
    try {
        kubectl get namespace $namespace 2>$null | Out-Null
        Write-Warning "Namespace $namespace already exists"
    }
    catch {
        kubectl create namespace $namespace
        Write-Success "Created namespace: $namespace"
    }
    
    # Add Helm repositories
    Write-Status "Adding Helm repositories..."
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    helm repo add jetstack https://charts.jetstack.io
    helm repo update
    
    Write-Success "Helm repositories updated"
    
    # Install infrastructure using Helm chart
    Write-Status "Installing infrastructure components..."
    
    $valuesFile = "infra\helm\values.yaml"
    if (-not (Test-Path $valuesFile)) {
        Write-Error "Values file not found: $valuesFile"
        exit 1
    }
    
    try {
        helm upgrade --install hopngo-infra infra\helm `
            --namespace $namespace `
            --values $valuesFile `
            --set global.environment=$Environment `
            --timeout 10m `
            --wait
        
        Write-Success "Infrastructure components installed successfully"
    }
    catch {
        Write-Error "Failed to install infrastructure: $($_.Exception.Message)"
        exit 1
    }
}

# Function to create secrets
function New-Secrets {
    param([string]$Environment)
    
    if ($SkipSecrets) {
        Write-Warning "Skipping secrets creation"
        return
    }
    
    Write-Header "Creating Kubernetes Secrets"
    
    $secretsScript = "infra\scripts\create-secrets.ps1"
    if (-not (Test-Path $secretsScript)) {
        Write-Error "Secrets script not found: $secretsScript"
        exit 1
    }
    
    try {
        & $secretsScript $Environment
        Write-Success "Secrets created successfully"
    }
    catch {
        Write-Error "Failed to create secrets: $($_.Exception.Message)"
        exit 1
    }
}

# Function to deploy application
function Deploy-Application {
    param([string]$Environment)
    
    Write-Header "Deploying HopNGo Application"
    
    $overlayPath = "infra\k8s\overlays\$Environment"
    if (-not (Test-Path $overlayPath)) {
        Write-Error "Overlay path not found: $overlayPath"
        exit 1
    }
    
    try {
        # Validate manifests first
        Write-Status "Validating Kubernetes manifests..."
        kubectl kustomize $overlayPath | kubectl apply --dry-run=client -f -
        Write-Success "Manifests validation passed"
        
        # Apply manifests
        Write-Status "Applying Kubernetes manifests..."
        kubectl apply -k $overlayPath
        
        Write-Success "Application deployed successfully"
    }
    catch {
        Write-Error "Failed to deploy application: $($_.Exception.Message)"
        exit 1
    }
}

# Function to verify installation
function Test-Installation {
    param([string]$Environment)
    
    Write-Header "Verifying Installation"
    
    $namespace = "hopngo-$Environment"
    
    Write-Status "Checking pods status..."
    kubectl get pods -n $namespace
    
    Write-Status "Checking services..."
    kubectl get services -n $namespace
    
    Write-Status "Checking ingress..."
    kubectl get ingress -n $namespace
    
    # Wait for pods to be ready
    Write-Status "Waiting for pods to be ready (timeout: 5 minutes)..."
    try {
        kubectl wait --for=condition=ready pod --all -n $namespace --timeout=300s
        Write-Success "All pods are ready"
    }
    catch {
        Write-Warning "Some pods may not be ready yet. Check with: kubectl get pods -n $namespace"
    }
    
    # Show ingress information
    Write-Status "Getting ingress information..."
    $ingress = kubectl get ingress -n $namespace -o jsonpath='{.items[0].spec.rules[0].host}' 2>$null
    if ($ingress) {
        Write-Success "Application should be available at: http://$ingress"
    }
    else {
        Write-Warning "No ingress found. Check ingress configuration."
    }
}

# Function to show logs
function Show-Logs {
    param([string]$Environment, [string]$Service = "")
    
    $namespace = "hopngo-$Environment"
    
    if ($Service) {
        Write-Header "Showing logs for service: $Service"
        kubectl logs -n $namespace -l app=$Service --tail=100 -f
    }
    else {
        Write-Header "Available services in namespace: $namespace"
        kubectl get pods -n $namespace -o custom-columns=NAME:.metadata.name,APP:.metadata.labels.app
        Write-Status "Use: .\install.ps1 logs $Environment <service-name> to view specific service logs"
    }
}

# Function to uninstall
function Uninstall-Application {
    param([string]$Environment)
    
    Write-Header "Uninstalling HopNGo Application"
    
    $namespace = "hopngo-$Environment"
    
    Write-Warning "This will delete all resources in namespace: $namespace"
    $confirmation = Read-Host "Are you sure? (y/N)"
    
    if ($confirmation -eq "y" -or $confirmation -eq "Y") {
        try {
            # Delete application resources
            $overlayPath = "infra\k8s\overlays\$Environment"
            if (Test-Path $overlayPath) {
                kubectl delete -k $overlayPath
            }
            
            # Uninstall Helm chart
            helm uninstall hopngo-infra -n $namespace 2>$null
            
            # Delete namespace
            kubectl delete namespace $namespace
            
            Write-Success "Application uninstalled successfully"
        }
        catch {
            Write-Error "Failed to uninstall: $($_.Exception.Message)"
            exit 1
        }
    }
    else {
        Write-Status "Uninstall cancelled"
    }
}

# Function to show help
function Show-Help {
    Write-Host "HopNGo Infrastructure Management Script"
    Write-Host ""
    Write-Host "Usage: .\install.ps1 <command> [environment] [options]"
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  install     Install infrastructure and deploy application"
    Write-Host "  deploy      Deploy application only (skip infrastructure)"
    Write-Host "  verify      Verify installation status"
    Write-Host "  logs        Show application logs"
    Write-Host "  uninstall   Uninstall application and infrastructure"
    Write-Host "  help        Show this help message"
    Write-Host ""
    Write-Host "Environments:"
    Write-Host "  dev         Development environment (default)"
    Write-Host "  staging     Staging environment"
    Write-Host "  production  Production environment"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipSecrets    Skip secrets creation"
    Write-Host "  -SkipInfra      Skip infrastructure installation"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\install.ps1 install dev"
    Write-Host "  .\install.ps1 deploy staging -SkipSecrets"
    Write-Host "  .\install.ps1 verify production"
    Write-Host "  .\install.ps1 logs dev gateway"
    Write-Host "  .\install.ps1 uninstall dev"
    Write-Host ""
    Write-Host "Prerequisites:"
    Write-Host "  - kubectl (Kubernetes CLI)"
    Write-Host "  - helm (Helm package manager)"
    Write-Host "  - kustomize (via kubectl)"
    Write-Host "  - Access to Kubernetes cluster"
}

# Main function
function Main {
    param([string]$Command, [string]$Environment)
    
    if ($Help -or $Command -eq "help") {
        Show-Help
        return
    }
    
    # Validate environment
    if ($Environment -notin @("dev", "staging", "production")) {
        Write-Error "Invalid environment: $Environment. Must be one of: dev, staging, production"
        exit 1
    }
    
    Write-Header "HopNGo Infrastructure Management"
    Write-Status "Command: $Command"
    Write-Status "Environment: $Environment"
    
    switch ($Command) {
        "install" {
            Test-Prerequisites
            Install-Infrastructure $Environment
            New-Secrets $Environment
            Deploy-Application $Environment
            Test-Installation $Environment
        }
        "deploy" {
            Test-Prerequisites
            if (-not $SkipInfra) {
                Install-Infrastructure $Environment
            }
            if (-not $SkipSecrets) {
                New-Secrets $Environment
            }
            Deploy-Application $Environment
            Test-Installation $Environment
        }
        "verify" {
            Test-Installation $Environment
        }
        "logs" {
            $service = if ($args.Count -gt 2) { $args[2] } else { "" }
            Show-Logs $Environment $service
        }
        "uninstall" {
            Uninstall-Application $Environment
        }
        default {
            Write-Error "Unknown command: $Command"
            Show-Help
            exit 1
        }
    }
}

# Run main function
Main -Command $Command -Environment $Environment
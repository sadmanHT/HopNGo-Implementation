# Production deployment script for HopNGo (PowerShell)
# Deploys the application to production Kubernetes cluster

param(
    [switch]$SkipValidation,
    [switch]$Rollback,
    [switch]$Help
)

# Configuration
$NAMESPACE = "hopngo-prod"
$KUSTOMIZE_DIR = "../k8s/overlays/production"
$CLUSTER_NAME = "hopngo-prod-cluster"
$REGION = "us-central1"
$PROJECT_ID = "hopngo-production"

# Error handling
$ErrorActionPreference = "Stop"

# Logging functions
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Show help
function Show-Help {
    Write-Host "Usage: .\deploy-production.ps1 [OPTIONS]"
    Write-Host "Options:"
    Write-Host "  -SkipValidation    Skip manifest validation"
    Write-Host "  -Rollback          Rollback to previous deployment"
    Write-Host "  -Help              Show this help message"
}

# Check prerequisites
function Test-Prerequisites {
    Write-Info "Checking prerequisites..."
    
    # Check if kubectl is installed
    try {
        kubectl version --client --output=json | Out-Null
    }
    catch {
        Write-Error "kubectl is not installed. Please install kubectl first."
        exit 1
    }
    
    # Check if kustomize is installed
    try {
        kustomize version | Out-Null
    }
    catch {
        Write-Error "kustomize is not installed. Please install kustomize first."
        exit 1
    }
    
    # Check if gcloud is installed (for GKE)
    try {
        gcloud version | Out-Null
        # Authenticate with GKE cluster
        Write-Info "Authenticating with GKE cluster..."
        gcloud container clusters get-credentials $CLUSTER_NAME --region=$REGION --project=$PROJECT_ID
    }
    catch {
        Write-Warning "gcloud is not installed. Skipping GKE authentication."
    }
    
    # Check if cert-manager is installed
    try {
        kubectl get crd certificates.cert-manager.io | Out-Null
    }
    catch {
        Write-Warning "cert-manager CRDs not found. Installing cert-manager..."
        kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
        kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager -n cert-manager
        kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-cainjector -n cert-manager
        kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-webhook -n cert-manager
    }
    
    # Check if external-secrets-operator is installed
    try {
        kubectl get crd externalsecrets.external-secrets.io | Out-Null
    }
    catch {
        Write-Warning "External Secrets Operator CRDs not found. Installing ESO..."
        helm repo add external-secrets https://charts.external-secrets.io
        helm repo update
        helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
        kubectl wait --for=condition=Available --timeout=300s deployment/external-secrets -n external-secrets-system
        kubectl wait --for=condition=Available --timeout=300s deployment/external-secrets-cert-controller -n external-secrets-system
        kubectl wait --for=condition=Available --timeout=300s deployment/external-secrets-webhook -n external-secrets-system
    }
    
    Write-Success "Prerequisites check completed"
}

# Validate Kubernetes manifests
function Test-Manifests {
    Write-Info "Validating Kubernetes manifests..."
    
    Push-Location $KUSTOMIZE_DIR
    
    try {
        # Build and validate kustomize configuration
        kustomize build . | Out-File -FilePath "$env:TEMP\hopngo-prod-manifests.yaml" -Encoding UTF8
        
        # Validate manifests with kubectl
        kubectl apply --dry-run=client -f "$env:TEMP\hopngo-prod-manifests.yaml"
        
        Write-Success "Manifest validation completed"
    }
    catch {
        Write-Error "Manifest validation failed: $_"
        exit 1
    }
    finally {
        Pop-Location
    }
}

# Deploy infrastructure components
function Deploy-Infrastructure {
    Write-Info "Deploying infrastructure components..."
    
    # Create namespace first
    kubectl apply -f "$KUSTOMIZE_DIR\namespace.yaml"
    
    # Deploy cert-manager resources
    kubectl apply -f "$KUSTOMIZE_DIR\cert-manager.yaml"
    
    # Wait for ClusterIssuers to be ready
    Write-Info "Waiting for cert-manager ClusterIssuers to be ready..."
    kubectl wait --for=condition=Ready --timeout=300s clusterissuer/letsencrypt-staging
    kubectl wait --for=condition=Ready --timeout=300s clusterissuer/letsencrypt-prod
    
    # Deploy external secrets
    kubectl apply -f "$KUSTOMIZE_DIR\production-secrets.yaml"
    
    # Wait for external secrets to sync
    Write-Info "Waiting for external secrets to sync..."
    Start-Sleep -Seconds 30
    
    Write-Success "Infrastructure deployment completed"
}

# Deploy application
function Deploy-Application {
    Write-Info "Deploying HopNGo application..."
    
    Push-Location $KUSTOMIZE_DIR
    
    try {
        # Apply the complete configuration
        kubectl apply -k .
        
        # Wait for deployments to be ready
        Write-Info "Waiting for deployments to be ready..."
        kubectl wait --for=condition=Available --timeout=600s deployment --all -n $NAMESPACE
        
        Write-Success "Application deployment completed"
    }
    catch {
        Write-Error "Application deployment failed: $_"
        exit 1
    }
    finally {
        Pop-Location
    }
}

# Verify deployment
function Test-Deployment {
    Write-Info "Verifying deployment..."
    
    # Check pod status
    Write-Info "Pod status:"
    kubectl get pods -n $NAMESPACE
    
    # Check service status
    Write-Info "Service status:"
    kubectl get services -n $NAMESPACE
    
    # Check ingress status
    Write-Info "Ingress status:"
    kubectl get ingress -n $NAMESPACE
    
    # Check certificate status
    Write-Info "Certificate status:"
    kubectl get certificates -n $NAMESPACE
    
    # Check external secrets status
    Write-Info "External secrets status:"
    kubectl get externalsecrets -n $NAMESPACE
    
    # Perform health checks
    Write-Info "Performing health checks..."
    
    # Wait for ingress to get external IP
    Write-Info "Waiting for ingress to get external IP..."
    $timeout = 300
    $elapsed = 0
    do {
        try {
            $ingressIP = kubectl get ingress hopngo-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>$null
            if ($ingressIP -match '^\d+\.\d+\.\d+\.\d+$') {
                break
            }
        }
        catch {
            # Continue waiting
        }
        Start-Sleep -Seconds 5
        $elapsed += 5
    } while ($elapsed -lt $timeout)
    
    if ($elapsed -ge $timeout) {
        Write-Warning "Ingress IP not ready within timeout"
        $ingressIP = "pending"
    }
    
    Write-Info "Ingress IP: $ingressIP"
    
    Write-Success "Deployment verification completed"
}

# Rollback function
function Invoke-Rollback {
    Write-Warning "Rolling back deployment..."
    
    try {
        # Get previous revision
        $historyOutput = kubectl rollout history deployment -n $NAMESPACE
        $lines = $historyOutput -split "`n"
        if ($lines.Count -gt 2) {
            $previousRevision = ($lines[-2] -split "\s+")[0]
            
            if ($previousRevision) {
                kubectl rollout undo deployment --to-revision=$previousRevision -n $NAMESPACE
                kubectl rollout status deployment -n $NAMESPACE --timeout=300s
                Write-Success "Rollback completed to revision $previousRevision"
            }
            else {
                Write-Error "No previous revision found for rollback"
            }
        }
        else {
            Write-Error "No previous revision found for rollback"
        }
    }
    catch {
        Write-Error "Rollback failed: $_"
        exit 1
    }
}

# Main deployment function
function Main {
    if ($Help) {
        Show-Help
        return
    }
    
    Write-Info "Starting HopNGo production deployment..."
    
    # Handle rollback
    if ($Rollback) {
        Invoke-Rollback
        return
    }
    
    try {
        # Execute deployment steps
        Test-Prerequisites
        
        if (-not $SkipValidation) {
            Test-Manifests
        }
        
        Deploy-Infrastructure
        Deploy-Application
        Test-Deployment
        
        Write-Success "🎉 HopNGo production deployment completed successfully!"
        Write-Info "Application should be available at: https://hopngo.com"
        Write-Info "Monitor the deployment with: kubectl get pods -n $NAMESPACE -w"
    }
    catch {
        Write-Error "Deployment failed: $_"
        Write-Error "Consider running with -Rollback to revert changes."
        exit 1
    }
}

# Run main function
Main
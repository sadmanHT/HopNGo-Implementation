#!/bin/bash
# Production deployment script for HopNGo
# Deploys the application to production Kubernetes cluster

set -euo pipefail

# Configuration
NAMESPACE="hopngo-prod"
KUSTOMIZE_DIR="../k8s/overlays/production"
CLUSTER_NAME="hopngo-prod-cluster"
REGION="us-central1"
PROJECT_ID="hopngo-production"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if kustomize is installed
    if ! command -v kustomize &> /dev/null; then
        log_error "kustomize is not installed. Please install kustomize first."
        exit 1
    fi
    
    # Check if gcloud is installed (for GKE)
    if ! command -v gcloud &> /dev/null; then
        log_warning "gcloud is not installed. Skipping GKE authentication."
    else
        # Authenticate with GKE cluster
        log_info "Authenticating with GKE cluster..."
        gcloud container clusters get-credentials "$CLUSTER_NAME" --region="$REGION" --project="$PROJECT_ID"
    fi
    
    # Check if cert-manager is installed
    if ! kubectl get crd certificates.cert-manager.io &> /dev/null; then
        log_warning "cert-manager CRDs not found. Installing cert-manager..."
        kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
        kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager -n cert-manager
        kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-cainjector -n cert-manager
        kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-webhook -n cert-manager
    fi
    
    # Check if external-secrets-operator is installed
    if ! kubectl get crd externalsecrets.external-secrets.io &> /dev/null; then
        log_warning "External Secrets Operator CRDs not found. Installing ESO..."
        helm repo add external-secrets https://charts.external-secrets.io
        helm repo update
        helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
        kubectl wait --for=condition=Available --timeout=300s deployment/external-secrets -n external-secrets-system
        kubectl wait --for=condition=Available --timeout=300s deployment/external-secrets-cert-controller -n external-secrets-system
        kubectl wait --for=condition=Available --timeout=300s deployment/external-secrets-webhook -n external-secrets-system
    fi
    
    log_success "Prerequisites check completed"
}

# Validate Kubernetes manifests
validate_manifests() {
    log_info "Validating Kubernetes manifests..."
    
    cd "$KUSTOMIZE_DIR"
    
    # Build and validate kustomize configuration
    if ! kustomize build . > /tmp/hopngo-prod-manifests.yaml; then
        log_error "Failed to build kustomize configuration"
        exit 1
    fi
    
    # Validate manifests with kubectl
    if ! kubectl apply --dry-run=client -f /tmp/hopngo-prod-manifests.yaml; then
        log_error "Manifest validation failed"
        exit 1
    fi
    
    log_success "Manifest validation completed"
    cd - > /dev/null
}

# Deploy infrastructure components
deploy_infrastructure() {
    log_info "Deploying infrastructure components..."
    
    # Create namespace first
    kubectl apply -f "$KUSTOMIZE_DIR/namespace.yaml"
    
    # Deploy cert-manager resources
    kubectl apply -f "$KUSTOMIZE_DIR/cert-manager.yaml"
    
    # Wait for ClusterIssuers to be ready
    log_info "Waiting for cert-manager ClusterIssuers to be ready..."
    kubectl wait --for=condition=Ready --timeout=300s clusterissuer/letsencrypt-staging
    kubectl wait --for=condition=Ready --timeout=300s clusterissuer/letsencrypt-prod
    
    # Deploy external secrets
    kubectl apply -f "$KUSTOMIZE_DIR/production-secrets.yaml"
    
    # Wait for external secrets to sync
    log_info "Waiting for external secrets to sync..."
    sleep 30
    
    log_success "Infrastructure deployment completed"
}

# Deploy application
deploy_application() {
    log_info "Deploying HopNGo application..."
    
    cd "$KUSTOMIZE_DIR"
    
    # Apply the complete configuration
    kubectl apply -k .
    
    # Wait for deployments to be ready
    log_info "Waiting for deployments to be ready..."
    kubectl wait --for=condition=Available --timeout=600s deployment --all -n "$NAMESPACE"
    
    log_success "Application deployment completed"
    cd - > /dev/null
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."
    
    # Check pod status
    log_info "Pod status:"
    kubectl get pods -n "$NAMESPACE"
    
    # Check service status
    log_info "Service status:"
    kubectl get services -n "$NAMESPACE"
    
    # Check ingress status
    log_info "Ingress status:"
    kubectl get ingress -n "$NAMESPACE"
    
    # Check certificate status
    log_info "Certificate status:"
    kubectl get certificates -n "$NAMESPACE"
    
    # Check external secrets status
    log_info "External secrets status:"
    kubectl get externalsecrets -n "$NAMESPACE"
    
    # Perform health checks
    log_info "Performing health checks..."
    
    # Wait for ingress to get external IP
    log_info "Waiting for ingress to get external IP..."
    timeout 300 bash -c 'until kubectl get ingress hopngo-ingress -n hopngo-prod -o jsonpath="{.status.loadBalancer.ingress[0].ip}" | grep -E "^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$"; do sleep 5; done' || log_warning "Ingress IP not ready within timeout"
    
    # Get ingress IP
    INGRESS_IP=$(kubectl get ingress hopngo-ingress -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")
    log_info "Ingress IP: $INGRESS_IP"
    
    log_success "Deployment verification completed"
}

# Rollback function
rollback() {
    log_warning "Rolling back deployment..."
    
    # Get previous revision
    PREVIOUS_REVISION=$(kubectl rollout history deployment -n "$NAMESPACE" | tail -2 | head -1 | awk '{print $1}')
    
    if [ -n "$PREVIOUS_REVISION" ]; then
        kubectl rollout undo deployment --to-revision="$PREVIOUS_REVISION" -n "$NAMESPACE"
        kubectl rollout status deployment -n "$NAMESPACE" --timeout=300s
        log_success "Rollback completed to revision $PREVIOUS_REVISION"
    else
        log_error "No previous revision found for rollback"
    fi
}

# Main deployment function
main() {
    log_info "Starting HopNGo production deployment..."
    
    # Parse command line arguments
    SKIP_VALIDATION=false
    ROLLBACK=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-validation)
                SKIP_VALIDATION=true
                shift
                ;;
            --rollback)
                ROLLBACK=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --skip-validation    Skip manifest validation"
                echo "  --rollback          Rollback to previous deployment"
                echo "  -h, --help          Show this help message"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Handle rollback
    if [ "$ROLLBACK" = true ]; then
        rollback
        exit 0
    fi
    
    # Trap errors and rollback
    trap 'log_error "Deployment failed. Consider running with --rollback to revert changes."; exit 1' ERR
    
    # Execute deployment steps
    check_prerequisites
    
    if [ "$SKIP_VALIDATION" = false ]; then
        validate_manifests
    fi
    
    deploy_infrastructure
    deploy_application
    verify_deployment
    
    log_success "🎉 HopNGo production deployment completed successfully!"
    log_info "Application should be available at: https://hopngo.com"
    log_info "Monitor the deployment with: kubectl get pods -n $NAMESPACE -w"
}

# Run main function
main "$@"
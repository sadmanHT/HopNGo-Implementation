#!/bin/bash

# HopNGo Kubernetes Installation Script
# This script installs the complete HopNGo application stack

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DEFAULT_ENVIRONMENT="dev"
DEFAULT_NAMESPACE_PREFIX="hopngo"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SCRIPT_DIR")"
K8S_DIR="$INFRA_DIR/k8s"
HELM_DIR="$INFRA_DIR/helm"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "\n${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}\n"
}

# Function to check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    print_success "kubectl is available"
    
    # Check helm
    if ! command -v helm &> /dev/null; then
        print_error "helm is not installed or not in PATH"
        exit 1
    fi
    print_success "helm is available"
    
    # Check kustomize
    if ! command -v kustomize &> /dev/null; then
        print_warning "kustomize not found, using kubectl kustomize"
    else
        print_success "kustomize is available"
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    print_success "Connected to Kubernetes cluster"
}

# Function to install infrastructure components
install_infrastructure() {
    local environment=$1
    
    print_header "Installing Infrastructure Components"
    
    # Add Helm repositories
    print_status "Adding Helm repositories..."
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    helm repo add jetstack https://charts.jetstack.io
    helm repo update
    print_success "Helm repositories added and updated"
    
    # Install infrastructure using Helm
    local namespace="$DEFAULT_NAMESPACE_PREFIX-$environment"
    
    print_status "Installing infrastructure in namespace: $namespace"
    
    # Create namespace if it doesn't exist
    kubectl create namespace "$namespace" --dry-run=client -o yaml | kubectl apply -f -
    
    # Install PostgreSQL
    print_status "Installing PostgreSQL..."
    helm upgrade --install postgresql bitnami/postgresql \
        --namespace "$namespace" \
        --values "$HELM_DIR/values.yaml" \
        --set postgresql.auth.postgresPassword="hopngo123" \
        --set postgresql.auth.username="hopngo" \
        --set postgresql.auth.password="hopngo123" \
        --set postgresql.auth.database="hopngo" \
        --wait
    
    # Install MongoDB
    print_status "Installing MongoDB..."
    helm upgrade --install mongodb bitnami/mongodb \
        --namespace "$namespace" \
        --values "$HELM_DIR/values.yaml" \
        --set auth.rootPassword="hopngo123" \
        --set auth.usernames[0]="hopngo" \
        --set auth.passwords[0]="hopngo123" \
        --set auth.databases[0]="hopngo" \
        --wait
    
    # Install Redis
    print_status "Installing Redis..."
    helm upgrade --install redis bitnami/redis \
        --namespace "$namespace" \
        --values "$HELM_DIR/values.yaml" \
        --set auth.password="hopngoredis" \
        --wait
    
    # Install RabbitMQ
    print_status "Installing RabbitMQ..."
    helm upgrade --install rabbitmq bitnami/rabbitmq \
        --namespace "$namespace" \
        --values "$HELM_DIR/values.yaml" \
        --set auth.username="hopngo" \
        --set auth.password="hopngorabbit" \
        --wait
    
    # Install NGINX Ingress Controller (only for non-production)
    if [[ "$environment" != "production" ]]; then
        print_status "Installing NGINX Ingress Controller..."
        helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
            --namespace ingress-nginx \
            --create-namespace \
            --values "$HELM_DIR/values.yaml" \
            --wait
    fi
    
    # Install Prometheus (only for staging and production)
    if [[ "$environment" != "dev" ]]; then
        print_status "Installing Prometheus..."
        helm upgrade --install prometheus prometheus-community/prometheus \
            --namespace "$namespace" \
            --values "$HELM_DIR/values.yaml" \
            --wait
        
        # Install Grafana
        print_status "Installing Grafana..."
        helm upgrade --install grafana grafana/grafana \
            --namespace "$namespace" \
            --values "$HELM_DIR/values.yaml" \
            --set adminPassword="hopngografana" \
            --wait
    fi
    
    print_success "Infrastructure components installed successfully"
}

# Function to create secrets
create_secrets() {
    local environment=$1
    
    print_header "Creating Application Secrets"
    
    if [[ -f "$SCRIPT_DIR/create-secrets.sh" ]]; then
        bash "$SCRIPT_DIR/create-secrets.sh" "$environment"
    else
        print_error "create-secrets.sh not found"
        exit 1
    fi
}

# Function to deploy application
deploy_application() {
    local environment=$1
    
    print_header "Deploying HopNGo Application"
    
    local overlay_dir="$K8S_DIR/overlays/$environment"
    
    if [[ ! -d "$overlay_dir" ]]; then
        print_error "Overlay directory not found: $overlay_dir"
        exit 1
    fi
    
    print_status "Deploying application for environment: $environment"
    
    # Apply Kustomize configuration
    kubectl apply -k "$overlay_dir"
    
    print_success "Application deployed successfully"
    
    # Wait for deployments to be ready
    print_status "Waiting for deployments to be ready..."
    local namespace="$DEFAULT_NAMESPACE_PREFIX-$environment"
    
    kubectl wait --for=condition=available --timeout=300s deployment --all -n "$namespace"
    
    print_success "All deployments are ready"
}

# Function to verify installation
verify_installation() {
    local environment=$1
    local namespace="$DEFAULT_NAMESPACE_PREFIX-$environment"
    
    print_header "Verifying Installation"
    
    print_status "Checking pods status..."
    kubectl get pods -n "$namespace"
    
    print_status "Checking services..."
    kubectl get services -n "$namespace"
    
    print_status "Checking ingress..."
    kubectl get ingress -n "$namespace"
    
    # Check if all pods are running
    local failed_pods
    failed_pods=$(kubectl get pods -n "$namespace" --field-selector=status.phase!=Running --no-headers 2>/dev/null | wc -l)
    
    if [[ $failed_pods -eq 0 ]]; then
        print_success "All pods are running successfully"
    else
        print_warning "Some pods are not running. Check the status above."
    fi
    
    # Show access information
    print_header "Access Information"
    
    case "$environment" in
        "dev")
            print_status "Development environment access:"
            print_status "  Frontend: http://hopngo.local"
            print_status "  API Gateway: http://hopngo.local/api/gateway"
            ;;
        "staging")
            print_status "Staging environment access:"
            print_status "  Frontend: https://staging.hopngo.com"
            print_status "  API Gateway: https://staging.hopngo.com/api/gateway"
            ;;
        "production")
            print_status "Production environment access:"
            print_status "  Frontend: https://hopngo.com"
            print_status "  API Gateway: https://hopngo.com/api/gateway"
            ;;
    esac
    
    if [[ "$environment" != "dev" ]]; then
        print_status "Monitoring access:"
        print_status "  Grafana: kubectl port-forward svc/grafana 3000:80 -n $namespace"
        print_status "  Prometheus: kubectl port-forward svc/prometheus-server 9090:80 -n $namespace"
    fi
}

# Function to show logs
show_logs() {
    local environment=$1
    local service=$2
    local namespace="$DEFAULT_NAMESPACE_PREFIX-$environment"
    
    if [[ -n "$service" ]]; then
        print_status "Showing logs for $service in $environment environment..."
        kubectl logs -f deployment/"$service" -n "$namespace"
    else
        print_status "Available services in $environment environment:"
        kubectl get deployments -n "$namespace" -o name | sed 's/deployment.apps\///'
    fi
}

# Function to uninstall
uninstall() {
    local environment=$1
    local namespace="$DEFAULT_NAMESPACE_PREFIX-$environment"
    
    print_header "Uninstalling HopNGo Application"
    
    print_warning "This will delete all resources in namespace: $namespace"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Deleting application resources..."
        kubectl delete namespace "$namespace" --ignore-not-found=true
        
        # Delete Helm releases
        helm uninstall postgresql -n "$namespace" --ignore-not-found || true
        helm uninstall mongodb -n "$namespace" --ignore-not-found || true
        helm uninstall redis -n "$namespace" --ignore-not-found || true
        helm uninstall rabbitmq -n "$namespace" --ignore-not-found || true
        helm uninstall prometheus -n "$namespace" --ignore-not-found || true
        helm uninstall grafana -n "$namespace" --ignore-not-found || true
        
        print_success "Uninstallation completed"
    else
        print_status "Uninstallation cancelled"
    fi
}

# Help function
show_help() {
    echo "HopNGo Kubernetes Installation Script"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  install [ENV]     Install HopNGo application (default: dev)"
    echo "  deploy [ENV]      Deploy application only (skip infrastructure)"
    echo "  secrets [ENV]     Create secrets only"
    echo "  verify [ENV]      Verify installation"
    echo "  logs [ENV] [SVC]  Show logs for service"
    echo "  uninstall [ENV]   Uninstall application"
    echo "  help              Show this help message"
    echo ""
    echo "Environments:"
    echo "  dev               Development environment (default)"
    echo "  staging           Staging environment"
    echo "  production        Production environment"
    echo ""
    echo "Examples:"
    echo "  $0 install dev"
    echo "  $0 deploy staging"
    echo "  $0 logs production gateway"
    echo "  $0 verify dev"
    echo "  $0 uninstall dev"
}

# Main function
main() {
    local command=${1:-install}
    local environment=${2:-$DEFAULT_ENVIRONMENT}
    local service=${3:-}
    
    case "$command" in
        "install")
            check_prerequisites
            install_infrastructure "$environment"
            create_secrets "$environment"
            deploy_application "$environment"
            verify_installation "$environment"
            ;;
        "deploy")
            check_prerequisites
            create_secrets "$environment"
            deploy_application "$environment"
            verify_installation "$environment"
            ;;
        "secrets")
            check_prerequisites
            create_secrets "$environment"
            ;;
        "verify")
            check_prerequisites
            verify_installation "$environment"
            ;;
        "logs")
            check_prerequisites
            show_logs "$environment" "$service"
            ;;
        "uninstall")
            check_prerequisites
            uninstall "$environment"
            ;;
        "help"|"--help"|"-h")
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
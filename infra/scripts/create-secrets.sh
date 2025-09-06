#!/bin/bash

# HopNGo Kubernetes Secrets Creation Script
# This script creates all necessary secrets for the HopNGo application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    print_success "kubectl is available"
}

# Function to create namespace if it doesn't exist
create_namespace() {
    local namespace=$1
    if kubectl get namespace "$namespace" &> /dev/null; then
        print_warning "Namespace $namespace already exists"
    else
        kubectl create namespace "$namespace"
        print_success "Created namespace: $namespace"
    fi
}

# Function to create database secret
create_database_secret() {
    local namespace=$1
    local username=${DB_USERNAME:-hopngo}
    local password=${DB_PASSWORD:-hopngo123}
    
    print_status "Creating database secret in namespace: $namespace"
    kubectl create secret generic database-secret \
        --from-literal=username="$username" \
        --from-literal=password="$password" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "Database secret created"
}

# Function to create MongoDB secret
create_mongodb_secret() {
    local namespace=$1
    local connection_string=${MONGODB_CONNECTION_STRING:-mongodb://hopngo:hopngo123@mongodb:27017/hopngo}
    
    print_status "Creating MongoDB secret in namespace: $namespace"
    kubectl create secret generic mongodb-secret \
        --from-literal=connection-string="$connection_string" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "MongoDB secret created"
}

# Function to create Redis secret
create_redis_secret() {
    local namespace=$1
    local password=${REDIS_PASSWORD:-hopngoredis}
    
    print_status "Creating Redis secret in namespace: $namespace"
    kubectl create secret generic redis-secret \
        --from-literal=password="$password" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "Redis secret created"
}

# Function to create RabbitMQ secret
create_rabbitmq_secret() {
    local namespace=$1
    local username=${RABBITMQ_USERNAME:-hopngo}
    local password=${RABBITMQ_PASSWORD:-hopngorabbit}
    
    print_status "Creating RabbitMQ secret in namespace: $namespace"
    kubectl create secret generic rabbitmq-secret \
        --from-literal=username="$username" \
        --from-literal=password="$password" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "RabbitMQ secret created"
}

# Function to create JWT secret
create_jwt_secret() {
    local namespace=$1
    local private_key_file=${JWT_PRIVATE_KEY_FILE:-./keys/jwt-private.key}
    local public_key_file=${JWT_PUBLIC_KEY_FILE:-./keys/jwt-public.key}
    
    print_status "Creating JWT secret in namespace: $namespace"
    
    if [[ -f "$private_key_file" && -f "$public_key_file" ]]; then
        kubectl create secret generic jwt-secret \
            --from-file=private-key="$private_key_file" \
            --from-file=public-key="$public_key_file" \
            --namespace="$namespace" \
            --dry-run=client -o yaml | kubectl apply -f -
    else
        print_warning "JWT key files not found. Creating with placeholder values."
        kubectl create secret generic jwt-secret \
            --from-literal=private-key="placeholder-private-key" \
            --from-literal=public-key="placeholder-public-key" \
            --namespace="$namespace" \
            --dry-run=client -o yaml | kubectl apply -f -
    fi
    print_success "JWT secret created"
}

# Function to create payment secret
create_payment_secret() {
    local namespace=$1
    local stripe_secret_key=${STRIPE_SECRET_KEY:-sk_test_placeholder}
    local stripe_webhook_secret=${STRIPE_WEBHOOK_SECRET:-whsec_placeholder}
    
    print_status "Creating payment secret in namespace: $namespace"
    kubectl create secret generic payment-secret \
        --from-literal=stripe-secret-key="$stripe_secret_key" \
        --from-literal=stripe-webhook-secret="$stripe_webhook_secret" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "Payment secret created"
}

# Function to create OpenAI secret
create_openai_secret() {
    local namespace=$1
    local api_key=${OPENAI_API_KEY:-sk-placeholder}
    
    print_status "Creating OpenAI secret in namespace: $namespace"
    kubectl create secret generic openai-secret \
        --from-literal=api-key="$api_key" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "OpenAI secret created"
}

# Function to create Google Maps secret
create_maps_secret() {
    local namespace=$1
    local api_key=${GOOGLE_MAPS_API_KEY:-AIzaSyA_placeholder}
    
    print_status "Creating Google Maps secret in namespace: $namespace"
    kubectl create secret generic maps-secret \
        --from-literal=google-api-key="$api_key" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "Google Maps secret created"
}

# Function to create Firebase secret
create_firebase_secret() {
    local namespace=$1
    local credentials_file=${FIREBASE_CREDENTIALS_FILE:-./keys/firebase-credentials.json}
    
    print_status "Creating Firebase secret in namespace: $namespace"
    
    if [[ -f "$credentials_file" ]]; then
        kubectl create secret generic firebase-secret \
            --from-file=credentials="$credentials_file" \
            --namespace="$namespace" \
            --dry-run=client -o yaml | kubectl apply -f -
    else
        print_warning "Firebase credentials file not found. Creating with placeholder."
        kubectl create secret generic firebase-secret \
            --from-literal=credentials='{"type": "service_account", "project_id": "hopngo-notifications"}' \
            --namespace="$namespace" \
            --dry-run=client -o yaml | kubectl apply -f -
    fi
    print_success "Firebase secret created"
}

# Function to create frontend secret
create_frontend_secret() {
    local namespace=$1
    local nextauth_secret=${NEXTAUTH_SECRET:-nextauth_secret_placeholder}
    
    print_status "Creating frontend secret in namespace: $namespace"
    kubectl create secret generic frontend-secret \
        --from-literal=nextauth-secret="$nextauth_secret" \
        --namespace="$namespace" \
        --dry-run=client -o yaml | kubectl apply -f -
    print_success "Frontend secret created"
}

# Main function
main() {
    local environment=${1:-dev}
    local namespace="hopngo-$environment"
    
    print_status "Creating secrets for environment: $environment"
    print_status "Target namespace: $namespace"
    
    # Check prerequisites
    check_kubectl
    
    # Create namespace
    create_namespace "$namespace"
    
    # Create all secrets
    create_database_secret "$namespace"
    create_mongodb_secret "$namespace"
    create_redis_secret "$namespace"
    create_rabbitmq_secret "$namespace"
    create_jwt_secret "$namespace"
    create_payment_secret "$namespace"
    create_openai_secret "$namespace"
    create_maps_secret "$namespace"
    create_firebase_secret "$namespace"
    create_frontend_secret "$namespace"
    
    print_success "All secrets created successfully for environment: $environment"
    print_status "You can now deploy the application using: kubectl apply -k infra/k8s/overlays/$environment"
}

# Help function
show_help() {
    echo "Usage: $0 [ENVIRONMENT]"
    echo ""
    echo "Creates Kubernetes secrets for HopNGo application"
    echo ""
    echo "Arguments:"
    echo "  ENVIRONMENT    Target environment (dev, staging, production). Default: dev"
    echo ""
    echo "Environment Variables:"
    echo "  DB_USERNAME              Database username (default: hopngo)"
    echo "  DB_PASSWORD              Database password (default: hopngo123)"
    echo "  MONGODB_CONNECTION_STRING MongoDB connection string"
    echo "  REDIS_PASSWORD           Redis password (default: hopngoredis)"
    echo "  RABBITMQ_USERNAME        RabbitMQ username (default: hopngo)"
    echo "  RABBITMQ_PASSWORD        RabbitMQ password (default: hopngorabbit)"
    echo "  JWT_PRIVATE_KEY_FILE     Path to JWT private key file"
    echo "  JWT_PUBLIC_KEY_FILE      Path to JWT public key file"
    echo "  STRIPE_SECRET_KEY        Stripe secret key"
    echo "  STRIPE_WEBHOOK_SECRET    Stripe webhook secret"
    echo "  OPENAI_API_KEY           OpenAI API key"
    echo "  GOOGLE_MAPS_API_KEY      Google Maps API key"
    echo "  FIREBASE_CREDENTIALS_FILE Path to Firebase credentials JSON file"
    echo "  NEXTAUTH_SECRET          NextAuth secret"
    echo ""
    echo "Examples:"
    echo "  $0 dev"
    echo "  $0 staging"
    echo "  $0 production"
}

# Parse command line arguments
case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac
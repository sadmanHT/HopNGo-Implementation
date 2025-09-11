#!/bin/bash

# JWT Key Rotation Script for HopNGo Auth Service
# This script generates new RSA key pairs for JWT signing

set -e

# Configuration
KEY_SIZE=2048
KEYS_DIR="./keys"
BACKUP_DIR="./keys/backup"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== JWT Key Rotation Script ===${NC}"
echo -e "${BLUE}Timestamp: $TIMESTAMP${NC}"
echo

# Create directories
mkdir -p "$KEYS_DIR"
mkdir -p "$BACKUP_DIR"

# Function to backup existing keys
backup_existing_keys() {
    if [ -f "$KEYS_DIR/private_key.pem" ] || [ -f "$KEYS_DIR/public_key.pem" ]; then
        echo -e "${YELLOW}Backing up existing keys...${NC}"
        
        if [ -f "$KEYS_DIR/private_key.pem" ]; then
            cp "$KEYS_DIR/private_key.pem" "$BACKUP_DIR/private_key_$TIMESTAMP.pem"
            echo "  ✓ Backed up private key to $BACKUP_DIR/private_key_$TIMESTAMP.pem"
        fi
        
        if [ -f "$KEYS_DIR/public_key.pem" ]; then
            cp "$KEYS_DIR/public_key.pem" "$BACKUP_DIR/public_key_$TIMESTAMP.pem"
            echo "  ✓ Backed up public key to $BACKUP_DIR/public_key_$TIMESTAMP.pem"
        fi
        echo
    fi
}

# Function to generate new RSA key pair
generate_keys() {
    echo -e "${GREEN}Generating new RSA key pair (${KEY_SIZE} bits)...${NC}"
    
    # Generate private key
    openssl genrsa -out "$KEYS_DIR/private_key.pem" $KEY_SIZE
    echo "  ✓ Generated private key: $KEYS_DIR/private_key.pem"
    
    # Extract public key from private key
    openssl rsa -in "$KEYS_DIR/private_key.pem" -pubout -out "$KEYS_DIR/public_key.pem"
    echo "  ✓ Generated public key: $KEYS_DIR/public_key.pem"
    echo
}

# Function to convert keys to base64 for environment variables
generate_env_vars() {
    echo -e "${GREEN}Generating environment variables...${NC}"
    
    # Convert private key to base64 (remove headers and newlines)
    PRIVATE_KEY_B64=$(grep -v "BEGIN\|END" "$KEYS_DIR/private_key.pem" | tr -d '\n')
    
    # Convert public key to base64 (remove headers and newlines)
    PUBLIC_KEY_B64=$(grep -v "BEGIN\|END" "$KEYS_DIR/public_key.pem" | tr -d '\n')
    
    # Create environment file
    ENV_FILE="$KEYS_DIR/jwt-keys.env"
    cat > "$ENV_FILE" << EOF
# JWT RSA Keys for HopNGo Auth Service
# Generated on: $(date)
# Key size: $KEY_SIZE bits

# Private key for JWT signing
JWT_RSA_PRIVATE_KEY=$PRIVATE_KEY_B64

# Public key for JWT verification
JWT_RSA_PUBLIC_KEY=$PUBLIC_KEY_B64
EOF
    
    echo "  ✓ Environment variables saved to: $ENV_FILE"
    echo
}

# Function to create Kubernetes secret manifest
generate_k8s_secret() {
    echo -e "${GREEN}Generating Kubernetes secret manifest...${NC}"
    
    PRIVATE_KEY_B64=$(grep -v "BEGIN\|END" "$KEYS_DIR/private_key.pem" | tr -d '\n' | base64 -w 0)
    PUBLIC_KEY_B64=$(grep -v "BEGIN\|END" "$KEYS_DIR/public_key.pem" | tr -d '\n' | base64 -w 0)
    
    K8S_SECRET_FILE="$KEYS_DIR/jwt-keys-secret.yaml"
    cat > "$K8S_SECRET_FILE" << EOF
apiVersion: v1
kind: Secret
metadata:
  name: jwt-keys
  namespace: hopngo
type: Opaque
data:
  JWT_RSA_PRIVATE_KEY: $PRIVATE_KEY_B64
  JWT_RSA_PUBLIC_KEY: $PUBLIC_KEY_B64
EOF
    
    echo "  ✓ Kubernetes secret manifest saved to: $K8S_SECRET_FILE"
    echo
}

# Function to display deployment instructions
show_deployment_instructions() {
    echo -e "${BLUE}=== Deployment Instructions ===${NC}"
    echo
    echo -e "${YELLOW}1. For Docker/Docker Compose:${NC}"
    echo "   Add these environment variables to your docker-compose.yml or .env file:"
    echo "   (Copy from: $KEYS_DIR/jwt-keys.env)"
    echo
    echo -e "${YELLOW}2. For Kubernetes:${NC}"
    echo "   Apply the secret manifest:"
    echo "   kubectl apply -f $KEYS_DIR/jwt-keys-secret.yaml"
    echo
    echo -e "${YELLOW}3. For Local Development:${NC}"
    echo "   Source the environment file:"
    echo "   source $KEYS_DIR/jwt-keys.env"
    echo "   Or add to your IDE's run configuration"
    echo
    echo -e "${YELLOW}4. Rolling Deployment Strategy:${NC}"
    echo "   a) Deploy new keys to all instances"
    echo "   b) Restart auth-service instances one by one"
    echo "   c) Verify JWT validation works across all services"
    echo "   d) Monitor for authentication errors"
    echo
    echo -e "${RED}⚠️  IMPORTANT SECURITY NOTES:${NC}"
    echo "   • Keep the private key secure and never commit to version control"
    echo "   • Rotate keys regularly (recommended: every 90 days)"
    echo "   • Use different keys for different environments"
    echo "   • Monitor for any authentication failures after rotation"
    echo "   • Keep backup keys until all old tokens expire"
    echo
}

# Function to verify key generation
verify_keys() {
    echo -e "${GREEN}Verifying generated keys...${NC}"
    
    # Test that private key is valid
    if openssl rsa -in "$KEYS_DIR/private_key.pem" -check -noout > /dev/null 2>&1; then
        echo "  ✓ Private key is valid"
    else
        echo -e "  ${RED}✗ Private key validation failed${NC}"
        exit 1
    fi
    
    # Test that public key is valid
    if openssl rsa -in "$KEYS_DIR/public_key.pem" -pubin -text -noout > /dev/null 2>&1; then
        echo "  ✓ Public key is valid"
    else
        echo -e "  ${RED}✗ Public key validation failed${NC}"
        exit 1
    fi
    
    # Test that keys match
    PRIVATE_MODULUS=$(openssl rsa -in "$KEYS_DIR/private_key.pem" -modulus -noout)
    PUBLIC_MODULUS=$(openssl rsa -in "$KEYS_DIR/public_key.pem" -pubin -modulus -noout)
    
    if [ "$PRIVATE_MODULUS" = "$PUBLIC_MODULUS" ]; then
        echo "  ✓ Private and public keys match"
    else
        echo -e "  ${RED}✗ Private and public keys do not match${NC}"
        exit 1
    fi
    
    echo
}

# Main execution
echo -e "${YELLOW}This script will generate new RSA keys for JWT signing.${NC}"
echo -e "${YELLOW}Existing keys will be backed up automatically.${NC}"
echo
read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operation cancelled."
    exit 0
fi

# Execute key rotation steps
backup_existing_keys
generate_keys
verify_keys
generate_env_vars
generate_k8s_secret
show_deployment_instructions

echo -e "${GREEN}✅ JWT key rotation completed successfully!${NC}"
echo -e "${GREEN}New keys generated with timestamp: $TIMESTAMP${NC}"
echo
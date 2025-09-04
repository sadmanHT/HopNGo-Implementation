#!/bin/bash

# HopNGo Development Infrastructure Shutdown Script
# This script stops all infrastructure services and optionally cleans up volumes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_DIR="$PROJECT_ROOT/infra/compose"

# Parse command line arguments
CLEAN_VOLUMES=false
FORCE_REMOVE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean-volumes|-c)
            CLEAN_VOLUMES=true
            shift
            ;;
        --force|-f)
            FORCE_REMOVE=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Stop HopNGo development infrastructure services"
            echo ""
            echo "Options:"
            echo "  -c, --clean-volumes    Remove all data volumes (WARNING: This will delete all data!)"
            echo "  -f, --force           Force removal without confirmation"
            echo "  -h, --help            Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}🛑 Stopping HopNGo Development Infrastructure...${NC}"
echo -e "${YELLOW}📁 Project Root: $PROJECT_ROOT${NC}"
echo -e "${YELLOW}🐳 Compose Directory: $COMPOSE_DIR${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Cannot stop services.${NC}"
    exit 1
fi

# Check if docker-compose.yml exists
if [ ! -f "$COMPOSE_DIR/docker-compose.yml" ]; then
    echo -e "${RED}❌ docker-compose.yml not found in $COMPOSE_DIR${NC}"
    exit 1
fi

# Change to compose directory
cd "$COMPOSE_DIR"

# Stop services
echo -e "${BLUE}🛑 Stopping infrastructure services...${NC}"
docker compose down

# Clean volumes if requested
if [ "$CLEAN_VOLUMES" = true ]; then
    if [ "$FORCE_REMOVE" = false ]; then
        echo ""
        echo -e "${YELLOW}⚠️  WARNING: This will permanently delete all data in the following volumes:${NC}"
        echo -e "  • postgres_data (PostgreSQL database)"
        echo -e "  • mongodb_data (MongoDB database)"
        echo -e "  • redis_data (Redis cache)"
        echo -e "  • rabbitmq_data (RabbitMQ messages)"
        echo -e "  • elasticsearch_data (Elasticsearch indices)"
        echo ""
        read -p "Are you sure you want to continue? (y/N): " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}🚫 Volume cleanup cancelled.${NC}"
            exit 0
        fi
    fi
    
    echo -e "${BLUE}🗑️  Removing data volumes...${NC}"
    docker compose down -v
    
    # Remove named volumes
    echo -e "${BLUE}🧹 Cleaning up named volumes...${NC}"
    docker volume rm -f \
        hopngo_postgres_data \
        hopngo_mongodb_data \
        hopngo_mongodb_config \
        hopngo_redis_data \
        hopngo_rabbitmq_data \
        hopngo_elasticsearch_data 2>/dev/null || true
    
    echo -e "${GREEN}✅ Volumes cleaned successfully!${NC}"
fi

# Prune unused networks
echo -e "${BLUE}🌐 Cleaning up unused networks...${NC}"
docker network prune -f

# Show final status
echo ""
echo -e "${GREEN}🎉 Infrastructure services stopped successfully!${NC}"
echo ""

if [ "$CLEAN_VOLUMES" = true ]; then
    echo -e "${YELLOW}💡 All data has been removed. Next startup will create fresh databases.${NC}"
else
    echo -e "${YELLOW}💡 Data volumes preserved. Use 'scripts/dev.sh' to restart services.${NC}"
    echo -e "${YELLOW}💡 Use 'scripts/down.sh --clean-volumes' to remove all data.${NC}"
fi
echo ""
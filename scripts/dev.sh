#!/bin/bash

# HopNGo Development Infrastructure Startup Script
# This script starts all required infrastructure services for local development

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

echo -e "${BLUE}🚀 Starting HopNGo Development Infrastructure...${NC}"
echo -e "${YELLOW}📁 Project Root: $PROJECT_ROOT${NC}"
echo -e "${YELLOW}🐳 Compose Directory: $COMPOSE_DIR${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

# Check if docker-compose.yml exists
if [ ! -f "$COMPOSE_DIR/docker-compose.yml" ]; then
    echo -e "${RED}❌ docker-compose.yml not found in $COMPOSE_DIR${NC}"
    exit 1
fi

# Check if .env file exists
if [ ! -f "$COMPOSE_DIR/.env" ]; then
    echo -e "${RED}❌ .env file not found in $COMPOSE_DIR${NC}"
    echo -e "${YELLOW}💡 Please create the .env file with required environment variables.${NC}"
    exit 1
fi

# Change to compose directory
cd "$COMPOSE_DIR"

# Pull latest images
echo -e "${BLUE}📥 Pulling latest Docker images...${NC}"
docker compose pull

# Start services
echo -e "${BLUE}🏗️  Starting infrastructure services...${NC}"
docker compose up -d

# Wait a moment for services to initialize
echo -e "${YELLOW}⏳ Waiting for services to initialize...${NC}"
sleep 10

# Check service health
echo -e "${BLUE}🔍 Checking service health...${NC}"
echo ""

# Function to check service health
check_service_health() {
    local service_name=$1
    local health_status=$(docker compose ps --format "table {{.Service}}\t{{.Status}}" | grep "$service_name" | awk '{print $2}')
    
    if [[ $health_status == *"healthy"* ]]; then
        echo -e "${GREEN}✅ $service_name: Healthy${NC}"
    elif [[ $health_status == *"starting"* ]]; then
        echo -e "${YELLOW}⏳ $service_name: Starting...${NC}"
    else
        echo -e "${RED}❌ $service_name: $health_status${NC}"
    fi
}

# Check each service
check_service_health "postgres"
check_service_health "mongodb"
check_service_health "redis"
check_service_health "rabbitmq"
check_service_health "mailhog"

echo ""
echo -e "${GREEN}🎉 Infrastructure services started successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Service URLs:${NC}"
echo -e "  🐘 PostgreSQL:     localhost:5432 (hopngo/hopngo_dev_2024!)"
echo -e "  🍃 MongoDB:        localhost:27017 (admin/mongo_dev_2024!)"
echo -e "  🔴 Redis:          localhost:6379 (password: redis_dev_2024!)"
echo -e "  🐰 RabbitMQ:       localhost:5672 (hopngo/rabbit_dev_2024!)"
echo -e "  📧 Mailhog:        http://localhost:8025"
echo -e "  🐰 RabbitMQ UI:    http://localhost:15672"
echo ""
echo -e "${YELLOW}💡 Use 'scripts/down.sh' to stop all services${NC}"
echo -e "${YELLOW}💡 Use 'docker compose logs -f [service]' to view logs${NC}"
echo -e "${YELLOW}💡 Use 'docker compose ps' to check service status${NC}"
echo ""
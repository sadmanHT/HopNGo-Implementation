#!/bin/bash

# PostgreSQL Connection Test Script
# Tests connection to the HopNGo PostgreSQL database

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database connection parameters
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="hopngo"
DB_USER="hopngo"
DB_PASSWORD="hopngo_dev_2024!"

echo -e "${BLUE}🐘 Testing PostgreSQL Connection...${NC}"
echo -e "${YELLOW}📍 Host: $DB_HOST:$DB_PORT${NC}"
echo -e "${YELLOW}🗄️  Database: $DB_NAME${NC}"
echo -e "${YELLOW}👤 User: $DB_USER${NC}"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ psql command not found. Please install PostgreSQL client tools.${NC}"
    echo -e "${YELLOW}💡 On Ubuntu/Debian: sudo apt-get install postgresql-client${NC}"
    echo -e "${YELLOW}💡 On macOS: brew install postgresql${NC}"
    echo -e "${YELLOW}💡 On Windows: Install PostgreSQL or use Docker exec${NC}"
    echo ""
    echo -e "${BLUE}🐳 Alternative: Use Docker exec to connect:${NC}"
    echo -e "${YELLOW}   docker exec -it hopngo-postgres psql -U $DB_USER -d $DB_NAME${NC}"
    exit 1
fi

# Test connection
echo -e "${BLUE}🔌 Testing database connection...${NC}"

# Set password for psql
export PGPASSWORD="$DB_PASSWORD"

# Test basic connection
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Connection successful!${NC}"
else
    echo -e "${RED}❌ Connection failed!${NC}"
    echo -e "${YELLOW}💡 Make sure the PostgreSQL service is running: docker compose ps${NC}"
    exit 1
fi

# Run test queries
echo -e "${BLUE}🔍 Running test queries...${NC}"
echo ""

# Get PostgreSQL version
echo -e "${BLUE}📊 PostgreSQL Version:${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT version();"
echo ""

# Show current database
echo -e "${BLUE}🗄️  Current Database:${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT current_database();"
echo ""

# Show current user
echo -e "${BLUE}👤 Current User:${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT current_user;"
echo ""

# List existing tables
echo -e "${BLUE}📋 Existing Tables:${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "\\dt"
echo ""

# Create a test table and insert data
echo -e "${BLUE}🧪 Creating test table...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
-- Create test table
DROP TABLE IF EXISTS connection_test;
CREATE TABLE connection_test (
    id SERIAL PRIMARY KEY,
    test_message VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data
INSERT INTO connection_test (test_message) VALUES 
    ('PostgreSQL connection test successful!'),
    ('HopNGo database is ready for development');

-- Query test data
SELECT * FROM connection_test;

-- Clean up
DROP TABLE connection_test;
EOF

echo -e "${GREEN}🎉 PostgreSQL test completed successfully!${NC}"
echo ""
echo -e "${YELLOW}💡 Connection details:${NC}"
echo -e "  Host: $DB_HOST:$DB_PORT"
echo -e "  Database: $DB_NAME"
echo -e "  User: $DB_USER"
echo -e "  Connection URL: postgresql://$DB_USER:$DB_PASSWORD@$DB_HOST:$DB_PORT/$DB_NAME"
echo ""
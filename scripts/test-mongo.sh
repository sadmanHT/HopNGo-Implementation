#!/bin/bash

# MongoDB Connection Test Script
# Tests connection to the HopNGo MongoDB database

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database connection parameters
DB_HOST="localhost"
DB_PORT="27017"
DB_NAME="hopngo"
DB_USER="admin"
DB_PASSWORD="mongo_dev_2024!"
AUTH_DB="admin"

echo -e "${BLUE}🍃 Testing MongoDB Connection...${NC}"
echo -e "${YELLOW}📍 Host: $DB_HOST:$DB_PORT${NC}"
echo -e "${YELLOW}🗄️  Database: $DB_NAME${NC}"
echo -e "${YELLOW}👤 User: $DB_USER${NC}"
echo -e "${YELLOW}🔐 Auth Database: $AUTH_DB${NC}"
echo ""

# Check if mongosh is available
if ! command -v mongosh &> /dev/null; then
    echo -e "${RED}❌ mongosh command not found. Trying legacy mongo client...${NC}"
    
    if ! command -v mongo &> /dev/null; then
        echo -e "${RED}❌ mongo command not found either. Please install MongoDB client tools.${NC}"
        echo -e "${YELLOW}💡 Install MongoDB Shell: https://docs.mongodb.com/mongodb-shell/install/${NC}"
        echo -e "${YELLOW}💡 On Ubuntu/Debian: sudo apt-get install mongodb-mongosh${NC}"
        echo -e "${YELLOW}💡 On macOS: brew install mongosh${NC}"
        echo ""
        echo -e "${BLUE}🐳 Alternative: Use Docker exec to connect:${NC}"
        echo -e "${YELLOW}   docker exec -it hopngo-mongodb mongosh -u $DB_USER -p $DB_PASSWORD --authenticationDatabase $AUTH_DB${NC}"
        exit 1
    else
        MONGO_CMD="mongo"
    fi
else
    MONGO_CMD="mongosh"
fi

# Connection string
CONNECTION_STRING="mongodb://$DB_USER:$DB_PASSWORD@$DB_HOST:$DB_PORT/$DB_NAME?authSource=$AUTH_DB"

# Test connection
echo -e "${BLUE}🔌 Testing database connection...${NC}"

if $MONGO_CMD --quiet --eval "db.adminCommand('ping')" "$CONNECTION_STRING" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Connection successful!${NC}"
else
    echo -e "${RED}❌ Connection failed!${NC}"
    echo -e "${YELLOW}💡 Make sure the MongoDB service is running: docker compose ps${NC}"
    exit 1
fi

# Run test queries
echo -e "${BLUE}🔍 Running test queries...${NC}"
echo ""

# Get MongoDB version and server info
echo -e "${BLUE}📊 MongoDB Server Information:${NC}"
$MONGO_CMD --quiet "$CONNECTION_STRING" << 'EOF'
print("MongoDB Version: " + db.version());
print("Server Status:");
db.serverStatus().host && print("  Host: " + db.serverStatus().host);
db.serverStatus().version && print("  Version: " + db.serverStatus().version);
db.serverStatus().uptime && print("  Uptime: " + db.serverStatus().uptime + " seconds");
EOF
echo ""

# Show current database
echo -e "${BLUE}🗄️  Current Database:${NC}"
$MONGO_CMD --quiet "$CONNECTION_STRING" --eval "print('Database: ' + db.getName())"
echo ""

# List existing collections
echo -e "${BLUE}📋 Existing Collections:${NC}"
$MONGO_CMD --quiet "$CONNECTION_STRING" << 'EOF'
var collections = db.getCollectionNames();
if (collections.length === 0) {
    print("No collections found in database.");
} else {
    print("Collections in " + db.getName() + ":");
    collections.forEach(function(collection) {
        print("  - " + collection);
    });
}
EOF
echo ""

# Create a test collection and insert data
echo -e "${BLUE}🧪 Creating test collection...${NC}"
$MONGO_CMD --quiet "$CONNECTION_STRING" << 'EOF'
// Drop test collection if exists
db.connectionTest.drop();

// Insert test documents
var result = db.connectionTest.insertMany([
    {
        message: "MongoDB connection test successful!",
        timestamp: new Date(),
        service: "HopNGo",
        environment: "development"
    },
    {
        message: "HopNGo database is ready for development",
        timestamp: new Date(),
        service: "HopNGo",
        environment: "development",
        features: ["social", "booking", "marketplace"]
    }
]);

print("Inserted " + result.insertedIds.length + " test documents.");

// Query test data
print("\nTest documents:");
db.connectionTest.find().forEach(function(doc) {
    print("  - " + doc.message + " (" + doc.timestamp + ")");
});

// Test aggregation
print("\nAggregation test:");
var count = db.connectionTest.countDocuments();
print("  Total documents: " + count);

// Clean up
db.connectionTest.drop();
print("\nTest collection cleaned up.");
EOF

echo -e "${GREEN}🎉 MongoDB test completed successfully!${NC}"
echo ""
echo -e "${YELLOW}💡 Connection details:${NC}"
echo -e "  Host: $DB_HOST:$DB_PORT"
echo -e "  Database: $DB_NAME"
echo -e "  User: $DB_USER"
echo -e "  Auth Database: $AUTH_DB"
echo -e "  Connection URL: $CONNECTION_STRING"
echo ""
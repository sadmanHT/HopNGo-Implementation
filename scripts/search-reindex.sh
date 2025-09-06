#!/bin/bash

# HopNGo Search Reindex Helper Script
# This script helps reindex posts and listings into OpenSearch

set -e

# Configuration
OPENSEARCH_URL="http://localhost:9200"
SOCIAL_SERVICE_URL="http://localhost:8081"
BOOKING_SERVICE_URL="http://localhost:8082"

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

# Function to check if OpenSearch is running
check_opensearch() {
    print_status "Checking OpenSearch connection..."
    if curl -s "$OPENSEARCH_URL/_cluster/health" > /dev/null; then
        print_success "OpenSearch is running"
        return 0
    else
        print_error "OpenSearch is not accessible at $OPENSEARCH_URL"
        return 1
    fi
}

# Function to check if service is running
check_service() {
    local service_name=$1
    local service_url=$2
    
    print_status "Checking $service_name connection..."
    if curl -s "$service_url/actuator/health" > /dev/null; then
        print_success "$service_name is running"
        return 0
    else
        print_warning "$service_name is not accessible at $service_url"
        return 1
    fi
}

# Function to create indices
create_indices() {
    print_status "Creating search indices..."
    
    # Create posts_v1 index
    curl -X PUT "$OPENSEARCH_URL/posts_v1" -H 'Content-Type: application/json' -d'{
        "settings": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "analysis": {
                "analyzer": {
                    "multilingual_analyzer": {
                        "type": "custom",
                        "tokenizer": "standard",
                        "filter": ["lowercase", "stop"]
                    }
                }
            }
        },
        "mappings": {
            "properties": {
                "id": {"type": "keyword"},
                "authorId": {"type": "keyword"},
                "text": {
                    "type": "text",
                    "analyzer": "multilingual_analyzer"
                },
                "tags": {"type": "keyword"},
                "place": {"type": "text"},
                "createdAt": {"type": "date"},
                "embedding": {
                    "type": "dense_vector",
                    "dims": 1536
                }
            }
        }
    }'
    
    # Create listings_v1 index
    curl -X PUT "$OPENSEARCH_URL/listings_v1" -H 'Content-Type: application/json' -d'{
        "settings": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "analysis": {
                "analyzer": {
                    "multilingual_analyzer": {
                        "type": "custom",
                        "tokenizer": "standard",
                        "filter": ["lowercase", "stop"]
                    }
                }
            }
        },
        "mappings": {
            "properties": {
                "id": {"type": "keyword"},
                "vendorId": {"type": "keyword"},
                "title": {
                    "type": "text",
                    "analyzer": "multilingual_analyzer"
                },
                "description": {
                    "type": "text",
                    "analyzer": "multilingual_analyzer"
                },
                "amenities": {"type": "keyword"},
                "geo": {"type": "geo_point"},
                "price": {"type": "double"},
                "currency": {"type": "keyword"},
                "rating": {"type": "float"}
            }
        }
    }'
    
    print_success "Indices created successfully"
}

# Function to reindex posts
reindex_posts() {
    print_status "Triggering posts reindex..."
    if check_service "Social Service" "$SOCIAL_SERVICE_URL"; then
        curl -X POST "$SOCIAL_SERVICE_URL/admin/reindex/posts" -H 'Content-Type: application/json'
        print_success "Posts reindex triggered"
    else
        print_warning "Skipping posts reindex - service not available"
    fi
}

# Function to reindex listings
reindex_listings() {
    print_status "Triggering listings reindex..."
    if check_service "Booking Service" "$BOOKING_SERVICE_URL"; then
        curl -X POST "$BOOKING_SERVICE_URL/admin/reindex/listings" -H 'Content-Type: application/json'
        print_success "Listings reindex triggered"
    else
        print_warning "Skipping listings reindex - service not available"
    fi
}

# Function to show index status
show_status() {
    print_status "Checking index status..."
    
    echo "Posts index:"
    curl -s "$OPENSEARCH_URL/posts_v1/_count" | jq '.count // "N/A"'
    
    echo "Listings index:"
    curl -s "$OPENSEARCH_URL/listings_v1/_count" | jq '.count // "N/A"'
}

# Function to delete indices
delete_indices() {
    print_warning "Deleting all search indices..."
    curl -X DELETE "$OPENSEARCH_URL/posts_v1" 2>/dev/null || true
    curl -X DELETE "$OPENSEARCH_URL/listings_v1" 2>/dev/null || true
    print_success "Indices deleted"
}

# Main function
main() {
    case "${1:-help}" in
        "setup")
            check_opensearch && create_indices
            ;;
        "reindex")
            check_opensearch && reindex_posts && reindex_listings
            ;;
        "posts")
            check_opensearch && reindex_posts
            ;;
        "listings")
            check_opensearch && reindex_listings
            ;;
        "status")
            check_opensearch && show_status
            ;;
        "reset")
            check_opensearch && delete_indices && create_indices
            ;;
        "help")
            echo "HopNGo Search Reindex Helper"
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  setup     - Create search indices"
            echo "  reindex   - Reindex all posts and listings"
            echo "  posts     - Reindex posts only"
            echo "  listings  - Reindex listings only"
            echo "  status    - Show index status"
            echo "  reset     - Delete and recreate indices"
            echo "  help      - Show this help message"
            ;;
        *)
            print_error "Unknown command: $1"
            main help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
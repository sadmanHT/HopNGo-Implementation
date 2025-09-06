#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
OPENSEARCH_URL="http://localhost:9200"
USERNAME="admin"
PASSWORD="admin"

echo -e "${BLUE}🔍 HopNGo Search Indices Initialization${NC}"
echo "=========================================="

# Function to check if OpenSearch is running
check_opensearch() {
    echo -e "${YELLOW}Checking OpenSearch connection...${NC}"
    
    if curl -s -u "$USERNAME:$PASSWORD" "$OPENSEARCH_URL/_cluster/health" > /dev/null; then
        echo -e "${GREEN}✅ OpenSearch is running${NC}"
        return 0
    else
        echo -e "${RED}❌ OpenSearch is not accessible at $OPENSEARCH_URL${NC}"
        echo "Please ensure OpenSearch is running with: docker-compose up opensearch"
        return 1
    fi
}

# Function to create posts index
create_posts_index() {
    echo -e "${YELLOW}Creating posts_v1 index...${NC}"
    
    curl -X PUT "$OPENSEARCH_URL/posts_v1" \
        -u "$USERNAME:$PASSWORD" \
        -H 'Content-Type: application/json' \
        -d '{
            "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "analysis": {
                    "analyzer": {
                        "multilingual_analyzer": {
                            "type": "custom",
                            "tokenizer": "standard",
                            "filter": ["lowercase", "stop", "bengali_stop", "english_stop"]
                        },
                        "bengali_analyzer": {
                            "type": "custom",
                            "tokenizer": "standard",
                            "filter": ["lowercase", "bengali_stop"]
                        }
                    },
                    "filter": {
                        "bengali_stop": {
                            "type": "stop",
                            "stopwords": ["এবং", "বা", "না", "যে", "এই", "সেই", "তার", "তাদের"]
                        },
                        "english_stop": {
                            "type": "stop",
                            "stopwords": "_english_"
                        }
                    }
                }
            },
            "mappings": {
                "properties": {
                    "id": {
                        "type": "keyword"
                    },
                    "authorId": {
                        "type": "keyword"
                    },
                    "text": {
                        "type": "text",
                        "analyzer": "multilingual_analyzer",
                        "fields": {
                            "bengali": {
                                "type": "text",
                                "analyzer": "bengali_analyzer"
                            },
                            "english": {
                                "type": "text",
                                "analyzer": "english"
                            }
                        }
                    },
                    "tags": {
                        "type": "keyword"
                    },
                    "place": {
                        "type": "text",
                        "analyzer": "multilingual_analyzer"
                    },
                    "createdAt": {
                        "type": "date"
                    },
                    "embedding": {
                        "type": "dense_vector",
                        "dims": 1536,
                        "index": true,
                        "similarity": "cosine"
                    }
                }
            }
        }'
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ posts_v1 index created successfully${NC}"
    else
        echo -e "${RED}❌ Failed to create posts_v1 index${NC}"
    fi
}

# Function to create listings index
create_listings_index() {
    echo -e "${YELLOW}Creating listings_v1 index...${NC}"
    
    curl -X PUT "$OPENSEARCH_URL/listings_v1" \
        -u "$USERNAME:$PASSWORD" \
        -H 'Content-Type: application/json' \
        -d '{
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
                    "id": {
                        "type": "keyword"
                    },
                    "vendorId": {
                        "type": "keyword"
                    },
                    "title": {
                        "type": "text",
                        "analyzer": "multilingual_analyzer"
                    },
                    "description": {
                        "type": "text",
                        "analyzer": "multilingual_analyzer"
                    },
                    "amenities": {
                        "type": "keyword"
                    },
                    "geo": {
                        "type": "geo_point"
                    },
                    "price": {
                        "type": "double"
                    },
                    "currency": {
                        "type": "keyword"
                    },
                    "rating": {
                        "type": "float"
                    },
                    "createdAt": {
                        "type": "date"
                    }
                }
            }
        }'
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ listings_v1 index created successfully${NC}"
    else
        echo -e "${RED}❌ Failed to create listings_v1 index${NC}"
    fi
}

# Function to show index status
show_index_status() {
    echo -e "${BLUE}📊 Index Status${NC}"
    echo "================="
    
    echo -e "${YELLOW}Posts Index:${NC}"
    curl -s -u "$USERNAME:$PASSWORD" "$OPENSEARCH_URL/posts_v1/_stats/docs" | jq '.indices.posts_v1.total.docs' 2>/dev/null || echo "Index not found or jq not available"
    
    echo -e "${YELLOW}Listings Index:${NC}"
    curl -s -u "$USERNAME:$PASSWORD" "$OPENSEARCH_URL/listings_v1/_stats/docs" | jq '.indices.listings_v1.total.docs' 2>/dev/null || echo "Index not found or jq not available"
    
    echo -e "${YELLOW}All Indices:${NC}"
    curl -s -u "$USERNAME:$PASSWORD" "$OPENSEARCH_URL/_cat/indices?v" | grep -E "(posts_v1|listings_v1)"
}

# Function to delete indices (for reset)
delete_indices() {
    echo -e "${RED}🗑️  Deleting existing indices...${NC}"
    
    curl -X DELETE "$OPENSEARCH_URL/posts_v1" -u "$USERNAME:$PASSWORD" 2>/dev/null
    curl -X DELETE "$OPENSEARCH_URL/listings_v1" -u "$USERNAME:$PASSWORD" 2>/dev/null
    
    echo -e "${GREEN}✅ Indices deleted${NC}"
}

# Main execution
case "$1" in
    "reset")
        check_opensearch || exit 1
        delete_indices
        create_posts_index
        create_listings_index
        show_index_status
        ;;
    "status")
        check_opensearch || exit 1
        show_index_status
        ;;
    "delete")
        check_opensearch || exit 1
        delete_indices
        ;;
    *)
        check_opensearch || exit 1
        create_posts_index
        create_listings_index
        show_index_status
        ;;
esac

echo -e "${GREEN}🎉 Search indices initialization completed!${NC}"
echo "You can now use the search functionality in your services."
echo ""
echo "Usage:"
echo "  ./init-search-indices.sh        - Create indices"
echo "  ./init-search-indices.sh reset  - Delete and recreate indices"
echo "  ./init-search-indices.sh status - Show index status"
echo "  ./init-search-indices.sh delete - Delete indices"
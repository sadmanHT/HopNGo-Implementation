#!/bin/bash

# HopNGo Backup Script
# Backs up PostgreSQL, MongoDB, Redis, RabbitMQ, and OpenSearch

set -euo pipefail

# Configuration
BACKUP_DIR="/backups"
TIMESTAMP=$(date +"%Y%m%d-%H%M")
BACKUP_PATH="${BACKUP_DIR}/${TIMESTAMP}"
LOG_FILE="${BACKUP_PATH}/backup.log"
TMP_DIR="/tmp/backup-work"

# Database connection settings
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-hopngo}"
POSTGRES_USER="${POSTGRES_USER:-hopngo}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD}"

MONGO_HOST="${MONGO_HOST:-mongodb}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_DB="${MONGO_DB:-hopngo}"
MONGO_USER="${MONGO_ROOT_USER:-admin}"
MONGO_PASSWORD="${MONGO_ROOT_PASSWORD}"

REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD}"

RABBITMQ_HOST="${RABBITMQ_HOST:-rabbitmq}"
RABBITMQ_PORT="${RABBITMQ_PORT:-15672}"
RABBITMQ_USER="${RABBITMQ_USER:-hopngo}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD}"

OPENSEARCH_HOST="${OPENSEARCH_HOST:-opensearch}"
OPENSEARCH_PORT="${OPENSEARCH_PORT:-9200}"

# Retention settings
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Create backup directory structure
create_backup_structure() {
    log "Creating backup directory structure: $BACKUP_PATH"
    mkdir -p "$BACKUP_PATH"/{postgres,mongodb,redis,rabbitmq,opensearch,metadata}
    mkdir -p "$TMP_DIR"
}

# Backup PostgreSQL
backup_postgres() {
    log "Starting PostgreSQL backup..."
    
    export PGPASSWORD="$POSTGRES_PASSWORD"
    
    # Backup schema and data
    pg_dump -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
        --verbose --no-password --format=custom --compress=9 \
        > "$BACKUP_PATH/postgres/hopngo_${TIMESTAMP}.dump" || error_exit "PostgreSQL backup failed"
    
    # Backup globals (users, roles, etc.)
    pg_dumpall -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" \
        --globals-only --no-password \
        > "$BACKUP_PATH/postgres/globals_${TIMESTAMP}.sql" || error_exit "PostgreSQL globals backup failed"
    
    # Create readable SQL backup as well
    pg_dump -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
        --verbose --no-password --format=plain --inserts \
        | gzip > "$BACKUP_PATH/postgres/hopngo_${TIMESTAMP}.sql.gz" || error_exit "PostgreSQL SQL backup failed"
    
    unset PGPASSWORD
    log "PostgreSQL backup completed successfully"
}

# Backup MongoDB
backup_mongodb() {
    log "Starting MongoDB backup..."
    
    # Create MongoDB URI
    MONGO_URI="mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_HOST}:${MONGO_PORT}/${MONGO_DB}?authSource=admin"
    
    # Backup all databases
    mongodump --uri="$MONGO_URI" --out="$BACKUP_PATH/mongodb" --gzip || error_exit "MongoDB backup failed"
    
    # Create metadata
    echo "MongoDB backup created at $(date)" > "$BACKUP_PATH/mongodb/backup_info.txt"
    echo "URI: $MONGO_HOST:$MONGO_PORT" >> "$BACKUP_PATH/mongodb/backup_info.txt"
    
    log "MongoDB backup completed successfully"
}

# Backup Redis
backup_redis() {
    log "Starting Redis backup..."
    
    # Create Redis snapshot
    if [ -n "$REDIS_PASSWORD" ]; then
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --rdb "$TMP_DIR/redis_${TIMESTAMP}.rdb" || error_exit "Redis backup failed"
    else
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --rdb "$TMP_DIR/redis_${TIMESTAMP}.rdb" || error_exit "Redis backup failed"
    fi
    
    # Compress and move to backup directory
    gzip -c "$TMP_DIR/redis_${TIMESTAMP}.rdb" > "$BACKUP_PATH/redis/redis_${TIMESTAMP}.rdb.gz"
    rm "$TMP_DIR/redis_${TIMESTAMP}.rdb"
    
    # Get Redis info
    if [ -n "$REDIS_PASSWORD" ]; then
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" INFO > "$BACKUP_PATH/redis/redis_info_${TIMESTAMP}.txt" || log "Warning: Could not get Redis info"
    else
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" INFO > "$BACKUP_PATH/redis/redis_info_${TIMESTAMP}.txt" || log "Warning: Could not get Redis info"
    fi
    
    log "Redis backup completed successfully"
}

# Backup RabbitMQ
backup_rabbitmq() {
    log "Starting RabbitMQ backup..."
    
    # Export RabbitMQ definitions (exchanges, queues, bindings, users, etc.)
    curl -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" \
        "http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/definitions" \
        -o "$BACKUP_PATH/rabbitmq/definitions_${TIMESTAMP}.json" || error_exit "RabbitMQ definitions backup failed"
    
    # Get cluster status
    curl -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" \
        "http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/cluster-name" \
        -o "$BACKUP_PATH/rabbitmq/cluster_info_${TIMESTAMP}.json" || log "Warning: Could not get RabbitMQ cluster info"
    
    # Get overview
    curl -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" \
        "http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/overview" \
        -o "$BACKUP_PATH/rabbitmq/overview_${TIMESTAMP}.json" || log "Warning: Could not get RabbitMQ overview"
    
    log "RabbitMQ backup completed successfully"
}

# Backup OpenSearch
backup_opensearch() {
    log "Starting OpenSearch backup..."
    
    # Create snapshot repository if it doesn't exist
    curl -X PUT "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_snapshot/backup_repo" \
        -H 'Content-Type: application/json' \
        -d "{
            \"type\": \"fs\",
            \"settings\": {
                \"location\": \"/usr/share/opensearch/backup\"
            }
        }" || log "Warning: Could not create OpenSearch snapshot repository"
    
    # Create snapshot
    SNAPSHOT_NAME="snapshot_${TIMESTAMP}"
    curl -X PUT "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_snapshot/backup_repo/${SNAPSHOT_NAME}" \
        -H 'Content-Type: application/json' \
        -d "{
            \"indices\": \"*\",
            \"ignore_unavailable\": true,
            \"include_global_state\": true
        }" || log "Warning: Could not create OpenSearch snapshot"
    
    # Export index mappings and settings
    curl -X GET "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_mapping" \
        -o "$BACKUP_PATH/opensearch/mappings_${TIMESTAMP}.json" || log "Warning: Could not export OpenSearch mappings"
    
    curl -X GET "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_settings" \
        -o "$BACKUP_PATH/opensearch/settings_${TIMESTAMP}.json" || log "Warning: Could not export OpenSearch settings"
    
    # Get cluster info
    curl -X GET "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_cluster/health" \
        -o "$BACKUP_PATH/opensearch/cluster_health_${TIMESTAMP}.json" || log "Warning: Could not get OpenSearch cluster health"
    
    log "OpenSearch backup completed successfully"
}

# Create backup metadata
create_metadata() {
    log "Creating backup metadata..."
    
    cat > "$BACKUP_PATH/metadata/backup_info.json" << EOF
{
    "timestamp": "$TIMESTAMP",
    "date": "$(date -Iseconds)",
    "version": "1.0",
    "services": {
        "postgres": {
            "host": "$POSTGRES_HOST",
            "database": "$POSTGRES_DB",
            "user": "$POSTGRES_USER"
        },
        "mongodb": {
            "host": "$MONGO_HOST",
            "database": "$MONGO_DB",
            "user": "$MONGO_USER"
        },
        "redis": {
            "host": "$REDIS_HOST",
            "port": "$REDIS_PORT"
        },
        "rabbitmq": {
            "host": "$RABBITMQ_HOST",
            "user": "$RABBITMQ_USER"
        },
        "opensearch": {
            "host": "$OPENSEARCH_HOST",
            "port": "$OPENSEARCH_PORT"
        }
    },
    "backup_size": "$(du -sh $BACKUP_PATH | cut -f1)",
    "retention_days": "$RETENTION_DAYS"
}
EOF

    # Create checksums
    find "$BACKUP_PATH" -type f -not -path "*/metadata/*" -exec sha256sum {} \; > "$BACKUP_PATH/metadata/checksums.txt"
    
    log "Backup metadata created successfully"
}

# Cleanup old backups
cleanup_old_backups() {
    log "Cleaning up backups older than $RETENTION_DAYS days..."
    
    find "$BACKUP_DIR" -maxdepth 1 -type d -name "[0-9]*-[0-9]*" -mtime +"$RETENTION_DAYS" -exec rm -rf {} \; || log "Warning: Could not clean up old backups"
    
    log "Cleanup completed"
}

# Main backup function
main() {
    log "Starting HopNGo backup process..."
    
    create_backup_structure
    
    # Run backups in parallel where possible
    backup_postgres &
    POSTGRES_PID=$!
    
    backup_mongodb &
    MONGODB_PID=$!
    
    backup_redis &
    REDIS_PID=$!
    
    backup_rabbitmq &
    RABBITMQ_PID=$!
    
    backup_opensearch &
    OPENSEARCH_PID=$!
    
    # Wait for all backups to complete
    wait $POSTGRES_PID || log "Warning: PostgreSQL backup process failed"
    wait $MONGODB_PID || log "Warning: MongoDB backup process failed"
    wait $REDIS_PID || log "Warning: Redis backup process failed"
    wait $RABBITMQ_PID || log "Warning: RabbitMQ backup process failed"
    wait $OPENSEARCH_PID || log "Warning: OpenSearch backup process failed"
    
    create_metadata
    cleanup_old_backups
    
    # Clean up temp directory
    rm -rf "$TMP_DIR"/*
    
    log "Backup process completed successfully"
    log "Backup location: $BACKUP_PATH"
    log "Backup size: $(du -sh $BACKUP_PATH | cut -f1)"
}

# Run main function
main "$@"
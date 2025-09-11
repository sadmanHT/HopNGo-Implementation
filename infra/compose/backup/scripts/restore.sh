#!/bin/bash

# HopNGo Restore Script
# Restores PostgreSQL, MongoDB, Redis, RabbitMQ, and OpenSearch from backup

set -euo pipefail

# Configuration
BACKUP_DIR="/backups"
TMP_DIR="/tmp/restore-work"

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

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Usage function
usage() {
    echo "Usage: $0 [OPTIONS] <backup_timestamp>"
    echo ""
    echo "Options:"
    echo "  -s, --service SERVICE    Restore only specific service (postgres|mongodb|redis|rabbitmq|opensearch)"
    echo "  -f, --force             Force restore without confirmation"
    echo "  -v, --verify            Verify backup integrity before restore"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 20240115-0300                    # Restore all services from backup"
    echo "  $0 -s postgres 20240115-0300        # Restore only PostgreSQL"
    echo "  $0 -f -v 20240115-0300              # Force restore with verification"
    echo ""
    echo "Available backups:"
    ls -1 "$BACKUP_DIR" 2>/dev/null | grep -E '^[0-9]{8}-[0-9]{4}$' | sort -r | head -10 || echo "  No backups found"
}

# Verify backup integrity
verify_backup() {
    local backup_path="$1"
    log "Verifying backup integrity..."
    
    if [ ! -f "$backup_path/metadata/checksums.txt" ]; then
        log "Warning: No checksums file found, skipping verification"
        return 0
    fi
    
    cd "$backup_path"
    if sha256sum -c metadata/checksums.txt --quiet; then
        log "Backup integrity verification passed"
        return 0
    else
        error_exit "Backup integrity verification failed"
    fi
}

# Wait for service to be ready
wait_for_service() {
    local service="$1"
    local max_attempts=30
    local attempt=1
    
    log "Waiting for $service to be ready..."
    
    case "$service" in
        postgres)
            while [ $attempt -le $max_attempts ]; do
                if PGPASSWORD="$POSTGRES_PASSWORD" pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" >/dev/null 2>&1; then
                    log "PostgreSQL is ready"
                    return 0
                fi
                sleep 2
                ((attempt++))
            done
            ;;
        mongodb)
            while [ $attempt -le $max_attempts ]; do
                if mongosh --host "$MONGO_HOST:$MONGO_PORT" --username "$MONGO_USER" --password "$MONGO_PASSWORD" --authenticationDatabase admin --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
                    log "MongoDB is ready"
                    return 0
                fi
                sleep 2
                ((attempt++))
            done
            ;;
        redis)
            while [ $attempt -le $max_attempts ]; do
                if [ -n "$REDIS_PASSWORD" ]; then
                    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping >/dev/null 2>&1; then
                        log "Redis is ready"
                        return 0
                    fi
                else
                    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping >/dev/null 2>&1; then
                        log "Redis is ready"
                        return 0
                    fi
                fi
                sleep 2
                ((attempt++))
            done
            ;;
        rabbitmq)
            while [ $attempt -le $max_attempts ]; do
                if curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" "http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/overview" >/dev/null 2>&1; then
                    log "RabbitMQ is ready"
                    return 0
                fi
                sleep 2
                ((attempt++))
            done
            ;;
        opensearch)
            while [ $attempt -le $max_attempts ]; do
                if curl -s "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_cluster/health" >/dev/null 2>&1; then
                    log "OpenSearch is ready"
                    return 0
                fi
                sleep 2
                ((attempt++))
            done
            ;;
    esac
    
    error_exit "$service is not ready after $max_attempts attempts"
}

# Restore PostgreSQL
restore_postgres() {
    local backup_path="$1"
    log "Starting PostgreSQL restore..."
    
    wait_for_service postgres
    
    export PGPASSWORD="$POSTGRES_PASSWORD"
    
    # Find the backup files
    local dump_file=$(find "$backup_path/postgres" -name "*.dump" | head -1)
    local globals_file=$(find "$backup_path/postgres" -name "globals_*.sql" | head -1)
    
    if [ -z "$dump_file" ]; then
        error_exit "PostgreSQL dump file not found in backup"
    fi
    
    # Drop and recreate database
    log "Dropping and recreating database..."
    psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres \
        -c "DROP DATABASE IF EXISTS \"$POSTGRES_DB\";" || error_exit "Failed to drop database"
    
    psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres \
        -c "CREATE DATABASE \"$POSTGRES_DB\";" || error_exit "Failed to create database"
    
    # Restore globals first if available
    if [ -n "$globals_file" ] && [ -f "$globals_file" ]; then
        log "Restoring PostgreSQL globals..."
        psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d postgres \
            -f "$globals_file" || log "Warning: Failed to restore globals"
    fi
    
    # Restore database
    log "Restoring PostgreSQL database..."
    pg_restore -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
        --verbose --clean --if-exists --no-owner --no-privileges \
        "$dump_file" || error_exit "PostgreSQL restore failed"
    
    unset PGPASSWORD
    log "PostgreSQL restore completed successfully"
}

# Restore MongoDB
restore_mongodb() {
    local backup_path="$1"
    log "Starting MongoDB restore..."
    
    wait_for_service mongodb
    
    # Create MongoDB URI
    local mongo_uri="mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_HOST}:${MONGO_PORT}/${MONGO_DB}?authSource=admin"
    
    # Find backup directory
    local backup_dir="$backup_path/mongodb"
    if [ ! -d "$backup_dir" ]; then
        error_exit "MongoDB backup directory not found"
    fi
    
    # Drop existing database
    log "Dropping existing MongoDB database..."
    mongosh --uri="$mongo_uri" --eval "db.dropDatabase()" || log "Warning: Could not drop existing database"
    
    # Restore database
    log "Restoring MongoDB database..."
    mongorestore --uri="$mongo_uri" --gzip --drop "$backup_dir" || error_exit "MongoDB restore failed"
    
    log "MongoDB restore completed successfully"
}

# Restore Redis
restore_redis() {
    local backup_path="$1"
    log "Starting Redis restore..."
    
    wait_for_service redis
    
    # Find backup file
    local backup_file=$(find "$backup_path/redis" -name "*.rdb.gz" | head -1)
    if [ -z "$backup_file" ]; then
        error_exit "Redis backup file not found"
    fi
    
    # Extract backup file
    mkdir -p "$TMP_DIR"
    gunzip -c "$backup_file" > "$TMP_DIR/dump.rdb"
    
    # Flush existing data
    log "Flushing existing Redis data..."
    if [ -n "$REDIS_PASSWORD" ]; then
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" FLUSHALL || error_exit "Failed to flush Redis data"
    else
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" FLUSHALL || error_exit "Failed to flush Redis data"
    fi
    
    # Note: Redis RDB restore requires stopping Redis, copying the file, and restarting
    # This is a limitation of Redis backup/restore process
    log "Warning: Redis restore requires manual intervention:"
    log "1. Stop Redis service"
    log "2. Copy $TMP_DIR/dump.rdb to Redis data directory"
    log "3. Restart Redis service"
    log "Backup file prepared at: $TMP_DIR/dump.rdb"
    
    log "Redis restore preparation completed"
}

# Restore RabbitMQ
restore_rabbitmq() {
    local backup_path="$1"
    log "Starting RabbitMQ restore..."
    
    wait_for_service rabbitmq
    
    # Find definitions file
    local definitions_file=$(find "$backup_path/rabbitmq" -name "definitions_*.json" | head -1)
    if [ -z "$definitions_file" ]; then
        error_exit "RabbitMQ definitions file not found"
    fi
    
    # Import definitions
    log "Importing RabbitMQ definitions..."
    curl -u "${RABBITMQ_USER}:${RABBITMQ_PASSWORD}" \
        -H "Content-Type: application/json" \
        -X POST \
        -d @"$definitions_file" \
        "http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/definitions" || error_exit "RabbitMQ restore failed"
    
    log "RabbitMQ restore completed successfully"
}

# Restore OpenSearch
restore_opensearch() {
    local backup_path="$1"
    log "Starting OpenSearch restore..."
    
    wait_for_service opensearch
    
    # Find mappings and settings files
    local mappings_file=$(find "$backup_path/opensearch" -name "mappings_*.json" | head -1)
    local settings_file=$(find "$backup_path/opensearch" -name "settings_*.json" | head -1)
    
    if [ -n "$mappings_file" ] && [ -f "$mappings_file" ]; then
        log "Restoring OpenSearch mappings..."
        # Note: This is a simplified restore - in practice, you'd need to recreate indices individually
        log "Warning: OpenSearch restore requires manual intervention for index recreation"
        log "Mappings file available at: $mappings_file"
        log "Settings file available at: $settings_file"
    fi
    
    # Try to restore from snapshot if available
    log "Attempting to restore from OpenSearch snapshot..."
    curl -X POST "${OPENSEARCH_HOST}:${OPENSEARCH_PORT}/_snapshot/backup_repo/*/_restore" \
        -H 'Content-Type: application/json' \
        -d '{
            "ignore_unavailable": true,
            "include_global_state": true
        }' || log "Warning: Could not restore from OpenSearch snapshot"
    
    log "OpenSearch restore completed"
}

# Main restore function
main() {
    local service=""
    local force=false
    local verify=false
    local backup_timestamp=""
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -s|--service)
                service="$2"
                shift 2
                ;;
            -f|--force)
                force=true
                shift
                ;;
            -v|--verify)
                verify=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            -*)
                echo "Unknown option: $1"
                usage
                exit 1
                ;;
            *)
                backup_timestamp="$1"
                shift
                ;;
        esac
    done
    
    # Validate arguments
    if [ -z "$backup_timestamp" ]; then
        echo "Error: Backup timestamp is required"
        usage
        exit 1
    fi
    
    local backup_path="$BACKUP_DIR/$backup_timestamp"
    
    if [ ! -d "$backup_path" ]; then
        error_exit "Backup directory not found: $backup_path"
    fi
    
    # Verify backup if requested
    if [ "$verify" = true ]; then
        verify_backup "$backup_path"
    fi
    
    # Confirmation prompt
    if [ "$force" = false ]; then
        echo "WARNING: This will overwrite existing data!"
        echo "Backup: $backup_timestamp"
        echo "Services: ${service:-all}"
        echo -n "Are you sure you want to continue? (yes/no): "
        read -r confirmation
        if [ "$confirmation" != "yes" ]; then
            log "Restore cancelled by user"
            exit 0
        fi
    fi
    
    log "Starting restore process from backup: $backup_timestamp"
    
    # Create temp directory
    mkdir -p "$TMP_DIR"
    
    # Restore specific service or all services
    case "$service" in
        postgres)
            restore_postgres "$backup_path"
            ;;
        mongodb)
            restore_mongodb "$backup_path"
            ;;
        redis)
            restore_redis "$backup_path"
            ;;
        rabbitmq)
            restore_rabbitmq "$backup_path"
            ;;
        opensearch)
            restore_opensearch "$backup_path"
            ;;
        "")
            # Restore all services
            restore_postgres "$backup_path" &
            POSTGRES_PID=$!
            
            restore_mongodb "$backup_path" &
            MONGODB_PID=$!
            
            restore_redis "$backup_path" &
            REDIS_PID=$!
            
            restore_rabbitmq "$backup_path" &
            RABBITMQ_PID=$!
            
            restore_opensearch "$backup_path" &
            OPENSEARCH_PID=$!
            
            # Wait for all restores to complete
            wait $POSTGRES_PID || log "Warning: PostgreSQL restore failed"
            wait $MONGODB_PID || log "Warning: MongoDB restore failed"
            wait $REDIS_PID || log "Warning: Redis restore failed"
            wait $RABBITMQ_PID || log "Warning: RabbitMQ restore failed"
            wait $OPENSEARCH_PID || log "Warning: OpenSearch restore failed"
            ;;
        *)
            error_exit "Unknown service: $service"
            ;;
    esac
    
    # Clean up temp directory
    rm -rf "$TMP_DIR"
    
    log "Restore process completed successfully"
}

# Run main function
main "$@"
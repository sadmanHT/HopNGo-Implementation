#!/bin/bash

# HopNGo Synthetic Monitoring Checks
# This script performs health checks on public endpoints
# Can be run in CI/CD pipeline or as a cron job

set -euo pipefail

# Configuration
BASE_URL="${HOPNGO_BASE_URL:-https://api.hopngo.com}"
TIMEOUT="${TIMEOUT:-10}"
RETRIES="${RETRIES:-3}"
SLACK_WEBHOOK="${SLACK_WEBHOOK:-}"
EMAIL_ALERT="${EMAIL_ALERT:-alerts@hopngo.com}"
LOG_FILE="${LOG_FILE:-/tmp/synthetic-checks.log}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Metrics
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
START_TIME=$(date +%s)

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] [$level] $message" | tee -a "$LOG_FILE"
}

# Check function with retry logic
check_endpoint() {
    local name="$1"
    local url="$2"
    local expected_status="${3:-200}"
    local expected_content="${4:-}"
    local max_response_time="${5:-2000}" # milliseconds
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    log "INFO" "Checking $name: $url"
    
    local attempt=1
    local success=false
    
    while [ $attempt -le $RETRIES ] && [ "$success" = false ]; do
        if [ $attempt -gt 1 ]; then
            log "WARN" "Retry attempt $attempt for $name"
            sleep 2
        fi
        
        # Perform the check
        local start_time=$(date +%s%3N)
        local response
        local status_code
        local response_time
        
        if response=$(curl -s -w "\n%{http_code}\n%{time_total}" \
                          --max-time "$TIMEOUT" \
                          --retry 0 \
                          --fail-with-body \
                          "$url" 2>/dev/null); then
            
            # Parse response
            local body=$(echo "$response" | head -n -2)
            status_code=$(echo "$response" | tail -n 2 | head -n 1)
            local time_total=$(echo "$response" | tail -n 1)
            response_time=$(echo "$time_total * 1000" | bc | cut -d. -f1)
            
            # Check status code
            if [ "$status_code" = "$expected_status" ]; then
                # Check content if specified
                if [ -n "$expected_content" ] && ! echo "$body" | grep -q "$expected_content"; then
                    log "ERROR" "$name: Content check failed. Expected: '$expected_content'"
                    attempt=$((attempt + 1))
                    continue
                fi
                
                # Check response time
                if [ "$response_time" -gt "$max_response_time" ]; then
                    log "WARN" "$name: Slow response time: ${response_time}ms (threshold: ${max_response_time}ms)"
                fi
                
                log "INFO" "$name: ✓ Status: $status_code, Response time: ${response_time}ms"
                success=true
                PASSED_CHECKS=$((PASSED_CHECKS + 1))
                
                # Send metrics to monitoring system
                send_metrics "$name" "success" "$status_code" "$response_time"
                
            else
                log "ERROR" "$name: Status code mismatch. Expected: $expected_status, Got: $status_code"
                attempt=$((attempt + 1))
            fi
        else
            local end_time=$(date +%s%3N)
            response_time=$((end_time - start_time))
            log "ERROR" "$name: Request failed after ${response_time}ms"
            attempt=$((attempt + 1))
        fi
    done
    
    if [ "$success" = false ]; then
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        log "ERROR" "$name: ✗ All $RETRIES attempts failed"
        send_metrics "$name" "failure" "0" "$response_time"
        send_alert "$name" "$url" "All $RETRIES attempts failed"
    fi
}

# Send metrics to Prometheus pushgateway or similar
send_metrics() {
    local endpoint_name="$1"
    local status="$2"
    local status_code="$3"
    local response_time="$4"
    
    # If Prometheus pushgateway is available
    if [ -n "${PUSHGATEWAY_URL:-}" ]; then
        cat <<EOF | curl -s --data-binary @- "${PUSHGATEWAY_URL}/metrics/job/synthetic-checks/instance/$(hostname)"
# HELP synthetic_check_success Whether the synthetic check succeeded
# TYPE synthetic_check_success gauge
synthetic_check_success{endpoint="$endpoint_name"} $([ "$status" = "success" ] && echo 1 || echo 0)
# HELP synthetic_check_response_time_ms Response time in milliseconds
# TYPE synthetic_check_response_time_ms gauge
synthetic_check_response_time_ms{endpoint="$endpoint_name"} $response_time
# HELP synthetic_check_status_code HTTP status code
# TYPE synthetic_check_status_code gauge
synthetic_check_status_code{endpoint="$endpoint_name"} $status_code
EOF
    fi
}

# Send alert notifications
send_alert() {
    local endpoint_name="$1"
    local url="$2"
    local error_message="$3"
    
    local alert_message="🚨 Synthetic Check Failed\n\nEndpoint: $endpoint_name\nURL: $url\nError: $error_message\nTime: $(date)"
    
    # Send to Slack if webhook is configured
    if [ -n "$SLACK_WEBHOOK" ]; then
        curl -s -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"$alert_message\"}" \
            "$SLACK_WEBHOOK" || true
    fi
    
    # Send email if configured
    if command -v mail >/dev/null 2>&1 && [ -n "$EMAIL_ALERT" ]; then
        echo -e "$alert_message" | mail -s "[HopNGo] Synthetic Check Failed: $endpoint_name" "$EMAIL_ALERT" || true
    fi
}

# Health check endpoints
run_health_checks() {
    log "INFO" "Starting health checks..."
    
    # Gateway health
    check_endpoint "Gateway Health" "$BASE_URL/health" 200 "healthy"
    
    # Auth service health
    check_endpoint "Auth Health" "$BASE_URL/auth/health" 200 "healthy"
    
    # Booking service health
    check_endpoint "Booking Health" "$BASE_URL/bookings/health" 200 "healthy"
    
    # Market service health
    check_endpoint "Market Health" "$BASE_URL/market/health" 200 "healthy"
    
    # Analytics service health
    check_endpoint "Analytics Health" "$BASE_URL/analytics/health" 200 "healthy"
}

# API functionality checks
run_api_checks() {
    log "INFO" "Starting API functionality checks..."
    
    # Public endpoints that don't require authentication
    check_endpoint "Search Destinations" "$BASE_URL/bookings/destinations" 200 "destinations" 1000
    
    check_endpoint "Market Categories" "$BASE_URL/market/categories" 200 "categories" 800
    
    # Check if login endpoint is responsive (should return 400 for empty request)
    check_endpoint "Auth Login Endpoint" "$BASE_URL/auth/login" 400 "" 500
    
    # Newsletter subscription endpoint (should return 400 for empty request)
    check_endpoint "Newsletter Endpoint" "$BASE_URL/analytics/newsletter/subscribe" 400 "" 500
}

# Performance checks
run_performance_checks() {
    log "INFO" "Starting performance checks..."
    
    # Check critical endpoints with strict performance requirements
    check_endpoint "Search Performance" "$BASE_URL/bookings/search?destination=london&checkin=2024-06-01&checkout=2024-06-03" 200 "" 400
    
    check_endpoint "Market Browse Performance" "$BASE_URL/market/products?category=experiences&limit=10" 200 "" 600
}

# Database connectivity checks (through API)
run_database_checks() {
    log "INFO" "Starting database connectivity checks..."
    
    # These endpoints typically hit the database
    check_endpoint "Database via Destinations" "$BASE_URL/bookings/destinations" 200 "" 1500
    
    check_endpoint "Database via Categories" "$BASE_URL/market/categories" 200 "" 1200
}

# External service checks
run_external_service_checks() {
    log "INFO" "Starting external service checks..."
    
    # Check if external dependencies are reachable through our API
    # These might be proxied through our services
    
    # Payment gateway health (if exposed)
    if curl -s --max-time 5 "$BASE_URL/market/payment/health" >/dev/null 2>&1; then
        check_endpoint "Payment Gateway" "$BASE_URL/market/payment/health" 200 "" 2000
    fi
    
    # Email service health (if exposed)
    if curl -s --max-time 5 "$BASE_URL/analytics/email/health" >/dev/null 2>&1; then
        check_endpoint "Email Service" "$BASE_URL/analytics/email/health" 200 "" 1500
    fi
}

# Generate summary report
generate_report() {
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    local success_rate=0
    
    if [ $TOTAL_CHECKS -gt 0 ]; then
        success_rate=$(echo "scale=2; $PASSED_CHECKS * 100 / $TOTAL_CHECKS" | bc)
    fi
    
    log "INFO" "=== Synthetic Checks Summary ==="
    log "INFO" "Total checks: $TOTAL_CHECKS"
    log "INFO" "Passed: $PASSED_CHECKS"
    log "INFO" "Failed: $FAILED_CHECKS"
    log "INFO" "Success rate: ${success_rate}%"
    log "INFO" "Duration: ${duration}s"
    
    # Send summary metrics
    if [ -n "${PUSHGATEWAY_URL:-}" ]; then
        cat <<EOF | curl -s --data-binary @- "${PUSHGATEWAY_URL}/metrics/job/synthetic-checks-summary/instance/$(hostname)"
# HELP synthetic_checks_total Total number of synthetic checks
# TYPE synthetic_checks_total gauge
synthetic_checks_total $TOTAL_CHECKS
# HELP synthetic_checks_passed Number of passed synthetic checks
# TYPE synthetic_checks_passed gauge
synthetic_checks_passed $PASSED_CHECKS
# HELP synthetic_checks_failed Number of failed synthetic checks
# TYPE synthetic_checks_failed gauge
synthetic_checks_failed $FAILED_CHECKS
# HELP synthetic_checks_success_rate Success rate percentage
# TYPE synthetic_checks_success_rate gauge
synthetic_checks_success_rate $success_rate
# HELP synthetic_checks_duration_seconds Duration of all checks
# TYPE synthetic_checks_duration_seconds gauge
synthetic_checks_duration_seconds $duration
EOF
    fi
    
    # Alert if success rate is below threshold
    local threshold=${SUCCESS_RATE_THRESHOLD:-95}
    if [ "$(echo "$success_rate < $threshold" | bc)" -eq 1 ]; then
        send_alert "Overall Health" "$BASE_URL" "Success rate ${success_rate}% is below threshold ${threshold}%"
    fi
    
    # Exit with error code if any checks failed
    if [ $FAILED_CHECKS -gt 0 ]; then
        exit 1
    fi
}

# Main execution
main() {
    log "INFO" "Starting HopNGo synthetic monitoring checks"
    log "INFO" "Base URL: $BASE_URL"
    log "INFO" "Timeout: ${TIMEOUT}s"
    log "INFO" "Retries: $RETRIES"
    
    # Create log directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # Run different types of checks based on arguments
    case "${1:-all}" in
        "health")
            run_health_checks
            ;;
        "api")
            run_api_checks
            ;;
        "performance")
            run_performance_checks
            ;;
        "database")
            run_database_checks
            ;;
        "external")
            run_external_service_checks
            ;;
        "all")
            run_health_checks
            run_api_checks
            run_performance_checks
            run_database_checks
            run_external_service_checks
            ;;
        *)
            echo "Usage: $0 [health|api|performance|database|external|all]"
            exit 1
            ;;
    esac
    
    generate_report
}

# Trap to ensure cleanup
trap 'log "INFO" "Synthetic checks interrupted"' INT TERM

# Check dependencies
if ! command -v curl >/dev/null 2>&1; then
    echo "Error: curl is required but not installed."
    exit 1
fi

if ! command -v bc >/dev/null 2>&1; then
    echo "Error: bc is required but not installed."
    exit 1
fi

# Run main function
main "$@"
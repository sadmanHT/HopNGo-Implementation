#!/bin/bash

# Performance Audit and Optimization Script
# This script identifies and reports performance bottlenecks, memory issues, and console errors

set -e

echo "🔍 Starting HopNGo Performance Audit..."
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="hopngo-prod"
REPORT_DIR="./performance-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/performance_audit_$TIMESTAMP.md"

# Create report directory
mkdir -p "$REPORT_DIR"

echo "# HopNGo Performance Audit Report" > "$REPORT_FILE"
echo "Generated: $(date)" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Function to log with timestamp
log() {
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
    echo "$1" >> "$REPORT_FILE"
}

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        log "${RED}❌ kubectl not found. Please install kubectl first.${NC}"
        exit 1
    fi
}

# Function to check namespace
check_namespace() {
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log "${YELLOW}⚠️  Namespace $NAMESPACE not found. Using default namespace.${NC}"
        NAMESPACE="default"
    fi
}

# Function to get pod metrics
get_pod_metrics() {
    log "\n## 📊 Pod Resource Usage"
    log "\n### CPU and Memory Usage"
    
    kubectl top pods -n "$NAMESPACE" --sort-by=cpu 2>/dev/null | head -20 || {
        log "${YELLOW}⚠️  Metrics server not available. Skipping resource usage.${NC}"
        return
    }
    
    log "\n### Pod Status and Restarts"
    kubectl get pods -n "$NAMESPACE" -o custom-columns=NAME:.metadata.name,STATUS:.status.phase,RESTARTS:.status.containerStatuses[0].restartCount,AGE:.metadata.creationTimestamp --sort-by=.status.containerStatuses[0].restartCount
}

# Function to check for OOMKilled pods
check_oom_kills() {
    log "\n## 💥 OOMKilled Pods Analysis"
    
    local oom_pods=$(kubectl get pods -n "$NAMESPACE" -o json | jq -r '.items[] | select(.status.containerStatuses[]?.lastState.terminated.reason == "OOMKilled") | .metadata.name' 2>/dev/null || echo "")
    
    if [[ -n "$oom_pods" ]]; then
        log "${RED}❌ Found OOMKilled pods:${NC}"
        echo "$oom_pods" | while read -r pod; do
            log "- $pod"
            kubectl describe pod "$pod" -n "$NAMESPACE" | grep -A 10 -B 5 "OOMKilled" || true
        done
    else
        log "${GREEN}✅ No OOMKilled pods found${NC}"
    fi
}

# Function to analyze application logs for errors
analyze_application_logs() {
    log "\n## 📋 Application Error Analysis"
    
    local services=("booking-service" "market-service" "social-service" "notification-service" "frontend")
    
    for service in "${services[@]}"; do
        log "\n### $service Errors"
        
        local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
        
        if [[ -n "$pods" ]]; then
            for pod in $pods; do
                log "\n#### Pod: $pod"
                
                # Check for common error patterns
                local error_count=$(kubectl logs "$pod" -n "$NAMESPACE" --tail=1000 2>/dev/null | grep -i -E "error|exception|failed|timeout|outofmemory" | wc -l || echo "0")
                
                if [[ "$error_count" -gt 0 ]]; then
                    log "${RED}❌ Found $error_count errors in last 1000 log lines${NC}"
                    
                    # Show recent errors
                    kubectl logs "$pod" -n "$NAMESPACE" --tail=1000 2>/dev/null | grep -i -E "error|exception|failed|timeout|outofmemory" | tail -10 | while read -r line; do
                        log "  $line"
                    done
                else
                    log "${GREEN}✅ No recent errors found${NC}"
                fi
                
                # Check for console.log/System.out patterns
                local console_count=$(kubectl logs "$pod" -n "$NAMESPACE" --tail=1000 2>/dev/null | grep -E "System\.(out|err)\.print|console\.(log|error|warn)" | wc -l || echo "0")
                
                if [[ "$console_count" -gt 0 ]]; then
                    log "${YELLOW}⚠️  Found $console_count console output statements${NC}"
                    kubectl logs "$pod" -n "$NAMESPACE" --tail=1000 2>/dev/null | grep -E "System\.(out|err)\.print|console\.(log|error|warn)" | head -5 | while read -r line; do
                        log "  $line"
                    done
                fi
            done
        else
            log "${YELLOW}⚠️  No pods found for service $service${NC}"
        fi
    done
}

# Function to check database performance
check_database_performance() {
    log "\n## 🗄️  Database Performance Analysis"
    
    local db_pod=$(kubectl get pods -n "$NAMESPACE" -l app=postgresql -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    
    if [[ -n "$db_pod" ]]; then
        log "\n### PostgreSQL Metrics"
        
        # Check connection count
        local conn_count=$(kubectl exec "$db_pod" -n "$NAMESPACE" -- psql -U postgres -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | tr -d ' ' || echo "N/A")
        log "Active connections: $conn_count"
        
        # Check for long-running queries
        log "\n### Long-running Queries (>30s)"
        kubectl exec "$db_pod" -n "$NAMESPACE" -- psql -U postgres -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE (now() - pg_stat_activity.query_start) > interval '30 seconds' AND state = 'active';" 2>/dev/null || log "Unable to check long-running queries"
        
        # Check database sizes
        log "\n### Database Sizes"
        kubectl exec "$db_pod" -n "$NAMESPACE" -- psql -U postgres -c "SELECT datname, pg_size_pretty(pg_database_size(datname)) as size FROM pg_database WHERE datistemplate = false;" 2>/dev/null || log "Unable to check database sizes"
        
    else
        log "${YELLOW}⚠️  PostgreSQL pod not found${NC}"
    fi
}

# Function to check Redis performance
check_redis_performance() {
    log "\n## 🔴 Redis Performance Analysis"
    
    local redis_pod=$(kubectl get pods -n "$NAMESPACE" -l app=redis -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    
    if [[ -n "$redis_pod" ]]; then
        log "\n### Redis Info"
        
        # Memory usage
        local memory_info=$(kubectl exec "$redis_pod" -n "$NAMESPACE" -- redis-cli info memory 2>/dev/null | grep -E "used_memory_human|used_memory_peak_human|maxmemory_human" || echo "Unable to get memory info")
        log "$memory_info"
        
        # Hit ratio
        local stats=$(kubectl exec "$redis_pod" -n "$NAMESPACE" -- redis-cli info stats 2>/dev/null | grep -E "keyspace_hits|keyspace_misses" || echo "Unable to get stats")
        log "$stats"
        
        # Connected clients
        local clients=$(kubectl exec "$redis_pod" -n "$NAMESPACE" -- redis-cli info clients 2>/dev/null | grep connected_clients || echo "Unable to get client info")
        log "$clients"
        
        # Slow log
        log "\n### Redis Slow Log (last 10)"
        kubectl exec "$redis_pod" -n "$NAMESPACE" -- redis-cli slowlog get 10 2>/dev/null || log "Unable to get slow log"
        
    else
        log "${YELLOW}⚠️  Redis pod not found${NC}"
    fi
}

# Function to check RabbitMQ performance
check_rabbitmq_performance() {
    log "\n## 🐰 RabbitMQ Performance Analysis"
    
    local rabbitmq_pod=$(kubectl get pods -n "$NAMESPACE" -l app=rabbitmq -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    
    if [[ -n "$rabbitmq_pod" ]]; then
        log "\n### RabbitMQ Overview"
        
        # Queue stats
        kubectl exec "$rabbitmq_pod" -n "$NAMESPACE" -- rabbitmqctl list_queues name messages consumers 2>/dev/null || log "Unable to get queue stats"
        
        # Connection stats
        log "\n### Connection Stats"
        kubectl exec "$rabbitmq_pod" -n "$NAMESPACE" -- rabbitmqctl list_connections 2>/dev/null | wc -l | xargs -I {} log "Active connections: {}"
        
        # Memory usage
        log "\n### Memory Usage"
        kubectl exec "$rabbitmq_pod" -n "$NAMESPACE" -- rabbitmqctl status 2>/dev/null | grep -A 5 -B 5 memory || log "Unable to get memory stats"
        
    else
        log "${YELLOW}⚠️  RabbitMQ pod not found${NC}"
    fi
}

# Function to check JVM heap usage
check_jvm_heap() {
    log "\n## ☕ JVM Heap Analysis"
    
    local services=("booking-service" "market-service" "social-service" "notification-service")
    
    for service in "${services[@]}"; do
        log "\n### $service JVM Metrics"
        
        local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
        
        if [[ -n "$pods" ]]; then
            for pod in $pods; do
                log "\n#### Pod: $pod"
                
                # Check if actuator endpoint is available
                local actuator_check=$(kubectl exec "$pod" -n "$NAMESPACE" -- curl -s -f http://localhost:8080/actuator/health 2>/dev/null || echo "")
                
                if [[ -n "$actuator_check" ]]; then
                    # Get heap metrics
                    kubectl exec "$pod" -n "$NAMESPACE" -- curl -s http://localhost:8080/actuator/metrics/jvm.memory.used 2>/dev/null | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' | xargs -I {} log "Heap used: {} bytes"
                    
                    kubectl exec "$pod" -n "$NAMESPACE" -- curl -s http://localhost:8080/actuator/metrics/jvm.memory.max 2>/dev/null | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' | xargs -I {} log "Heap max: {} bytes"
                    
                    # GC metrics
                    kubectl exec "$pod" -n "$NAMESPACE" -- curl -s http://localhost:8080/actuator/metrics/jvm.gc.pause 2>/dev/null | jq -r '.measurements[] | select(.statistic=="TOTAL_TIME") | .value' | xargs -I {} log "GC total time: {} seconds"
                    
                else
                    log "${YELLOW}⚠️  Actuator endpoint not available${NC}"
                fi
            done
        else
            log "${YELLOW}⚠️  No pods found for service $service${NC}"
        fi
    done
}

# Function to check network performance
check_network_performance() {
    log "\n## 🌐 Network Performance Analysis"
    
    # Check service endpoints
    log "\n### Service Endpoints"
    kubectl get endpoints -n "$NAMESPACE" -o custom-columns=NAME:.metadata.name,ENDPOINTS:.subsets[*].addresses[*].ip
    
    # Check ingress status
    log "\n### Ingress Status"
    kubectl get ingress -n "$NAMESPACE" -o wide 2>/dev/null || log "No ingress resources found"
}

# Function to generate recommendations
generate_recommendations() {
    log "\n## 🎯 Performance Optimization Recommendations"
    
    log "\n### Immediate Actions"
    log "1. **Memory Optimization**"
    log "   - Review and adjust JVM heap sizes based on actual usage"
    log "   - Implement proper garbage collection tuning"
    log "   - Add memory limits to prevent OOMKilled scenarios"
    
    log "\n2. **Database Optimization**"
    log "   - Apply database indexes for frequently queried columns"
    log "   - Optimize connection pool sizes"
    log "   - Enable query performance monitoring"
    
    log "\n3. **Cache Optimization**"
    log "   - Monitor Redis hit ratios and adjust TTL values"
    log "   - Implement cache warming for critical data"
    log "   - Optimize cache key patterns"
    
    log "\n4. **Application Code**"
    log "   - Replace System.out.println with proper logging"
    log "   - Fix N+1 query issues with batch loading"
    log "   - Implement proper error handling"
    
    log "\n### Long-term Improvements"
    log "1. **Monitoring & Alerting**"
    log "   - Set up comprehensive application metrics"
    log "   - Configure alerts for performance degradation"
    log "   - Implement distributed tracing"
    
    log "\n2. **Scalability**"
    log "   - Configure Horizontal Pod Autoscaling (HPA)"
    log "   - Implement circuit breakers for external services"
    log "   - Add request rate limiting"
    
    log "\n3. **Resource Management**"
    log "   - Set appropriate resource requests and limits"
    log "   - Implement pod disruption budgets"
    log "   - Configure quality of service classes"
}

# Function to create performance improvement script
create_improvement_script() {
    local script_file="$REPORT_DIR/apply_performance_fixes_$TIMESTAMP.sh"
    
    cat > "$script_file" << 'EOF'
#!/bin/bash

# Performance Improvement Script
# Auto-generated based on audit findings

set -e

NAMESPACE="hopngo-prod"

echo "🚀 Applying performance improvements..."

# Apply performance tuning configurations
echo "📊 Applying performance configurations..."
kubectl apply -f ../infra/k8s/base/performance-tuning/performance-config.yaml

# Apply database optimizations
echo "🗄️  Running database optimization job..."
kubectl apply -f ../infra/k8s/base/performance-tuning/performance-config.yaml
kubectl wait --for=condition=complete job/database-optimization-job -n "$NAMESPACE" --timeout=300s

# Restart services to apply new configurations
echo "🔄 Restarting services with new configurations..."
services=("booking-service" "market-service" "social-service" "notification-service")

for service in "${services[@]}"; do
    echo "Restarting $service..."
    kubectl rollout restart deployment "$service" -n "$NAMESPACE"
    kubectl rollout status deployment "$service" -n "$NAMESPACE" --timeout=300s
done

echo "✅ Performance improvements applied successfully!"
echo "📈 Monitor the system for 15-30 minutes to see the effects."
EOF

    chmod +x "$script_file"
    log "\n## 🛠️  Performance Improvement Script"
    log "Generated script: $script_file"
    log "Run this script to apply performance optimizations automatically."
}

# Main execution
main() {
    check_kubectl
    check_namespace
    
    log "${BLUE}🔍 Starting performance audit for namespace: $NAMESPACE${NC}"
    
    get_pod_metrics
    check_oom_kills
    analyze_application_logs
    check_database_performance
    check_redis_performance
    check_rabbitmq_performance
    check_jvm_heap
    check_network_performance
    generate_recommendations
    create_improvement_script
    
    log "\n${GREEN}✅ Performance audit completed!${NC}"
    log "${BLUE}📄 Full report saved to: $REPORT_FILE${NC}"
    
    echo ""
    echo "======================================"
    echo "🎯 Performance Audit Summary"
    echo "======================================"
    echo "📄 Report: $REPORT_FILE"
    echo "🛠️  Improvement script: $REPORT_DIR/apply_performance_fixes_$TIMESTAMP.sh"
    echo ""
    echo "Next steps:"
    echo "1. Review the detailed report"
    echo "2. Run the improvement script"
    echo "3. Monitor system performance"
    echo "4. Repeat audit after changes"
}

# Run main function
main "$@"
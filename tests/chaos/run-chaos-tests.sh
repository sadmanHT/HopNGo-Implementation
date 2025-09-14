#!/bin/bash

# HopNGo Chaos Engineering Test Runner
# This script orchestrates chaos experiments using LitmusChaos

set -euo pipefail

# Configuration
NAMESPACE="hopngo-prod"
LITMUS_NAMESPACE="litmus"
CHAOS_DIR="$(dirname "$0")"
RESULTS_DIR="${CHAOS_DIR}/results"
LOG_FILE="${RESULTS_DIR}/chaos-test-$(date +%Y%m%d-%H%M%S).log"
REPORT_FILE="${RESULTS_DIR}/chaos-report-$(date +%Y%m%d-%H%M%S).json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
declare -A CHAOS_TESTS=(
    ["pod-kill"]="litmus-pod-kill.yaml"
    ["db-outage"]="litmus-db-outage.yaml"
    ["rabbitmq-chaos"]="litmus-rabbitmq-chaos.yaml"
)

declare -A TEST_DESCRIPTIONS=(
    ["pod-kill"]="Pod termination chaos for market-service, frontend, and booking-service"
    ["db-outage"]="Database outage simulation for PostgreSQL, MongoDB, and Redis"
    ["rabbitmq-chaos"]="RabbitMQ failure scenarios including node failures and network partitions"
)

declare -A TEST_PRIORITIES=(
    ["pod-kill"]="high"
    ["db-outage"]="high"
    ["rabbitmq-chaos"]="medium"
)

# Utility functions
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")
            echo -e "${BLUE}[INFO]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE"
            ;;
        "WARN")
            echo -e "${YELLOW}[WARN]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE"
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE"
            ;;
        "SUCCESS")
            echo -e "${GREEN}[SUCCESS]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE"
            ;;
    esac
}

check_prerequisites() {
    log "INFO" "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log "ERROR" "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log "ERROR" "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log "ERROR" "Namespace $NAMESPACE does not exist"
        exit 1
    fi
    
    # Check if Litmus is installed
    if ! kubectl get namespace "$LITMUS_NAMESPACE" &> /dev/null; then
        log "ERROR" "Litmus namespace $LITMUS_NAMESPACE does not exist. Please install LitmusChaos first."
        log "INFO" "Install Litmus: kubectl apply -f https://litmuschaos.github.io/litmus/litmus-operator-v2.14.0.yaml"
        exit 1
    fi
    
    # Check if chaos experiments CRDs exist
    if ! kubectl get crd chaosengines.litmuschaos.io &> /dev/null; then
        log "ERROR" "LitmusChaos CRDs not found. Please install LitmusChaos operator."
        exit 1
    fi
    
    # Check if litmus-admin service account exists
    if ! kubectl get serviceaccount litmus-admin -n "$LITMUS_NAMESPACE" &> /dev/null; then
        log "WARN" "litmus-admin service account not found. Creating it..."
        create_litmus_rbac
    fi
    
    log "SUCCESS" "All prerequisites satisfied"
}

create_litmus_rbac() {
    log "INFO" "Creating Litmus RBAC resources..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: litmus-admin
  namespace: $LITMUS_NAMESPACE
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: litmus-admin
rules:
- apiGroups: [""]
  resources: ["pods", "events", "configmaps", "secrets", "services"]
  verbs: ["create", "delete", "get", "list", "patch", "update", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "statefulsets", "replicasets"]
  verbs: ["create", "delete", "get", "list", "patch", "update", "watch"]
- apiGroups: ["litmuschaos.io"]
  resources: ["*"]
  verbs: ["*"]
- apiGroups: ["batch"]
  resources: ["jobs"]
  verbs: ["create", "delete", "get", "list", "patch", "update", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: litmus-admin
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: litmus-admin
subjects:
- kind: ServiceAccount
  name: litmus-admin
  namespace: $LITMUS_NAMESPACE
EOF
    
    log "SUCCESS" "Litmus RBAC resources created"
}

setup_monitoring() {
    log "INFO" "Setting up chaos monitoring..."
    
    # Create monitoring namespace if it doesn't exist
    kubectl create namespace chaos-monitoring --dry-run=client -o yaml | kubectl apply -f -
    
    # Deploy chaos monitoring dashboard (simplified)
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: chaos-monitoring-config
  namespace: chaos-monitoring
data:
  monitor.sh: |
    #!/bin/bash
    while true; do
      echo "\$(date): Monitoring chaos experiments..."
      kubectl get chaosengines -A -o wide
      kubectl get chaosresults -A -o wide
      sleep 30
    done
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chaos-monitor
  namespace: chaos-monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: chaos-monitor
  template:
    metadata:
      labels:
        app: chaos-monitor
    spec:
      serviceAccountName: default
      containers:
      - name: monitor
        image: bitnami/kubectl:latest
        command: ["/bin/bash", "/scripts/monitor.sh"]
        volumeMounts:
        - name: scripts
          mountPath: /scripts
      volumes:
      - name: scripts
        configMap:
          name: chaos-monitoring-config
          defaultMode: 0755
EOF
    
    log "SUCCESS" "Chaos monitoring setup complete"
}

run_chaos_test() {
    local test_name=$1
    local test_file=$2
    local test_description=$3
    
    log "INFO" "Starting chaos test: $test_name"
    log "INFO" "Description: $test_description"
    
    # Apply chaos experiment
    if kubectl apply -f "${CHAOS_DIR}/${test_file}"; then
        log "SUCCESS" "Chaos experiment $test_name applied successfully"
    else
        log "ERROR" "Failed to apply chaos experiment $test_name"
        return 1
    fi
    
    # Wait for chaos engines to be created
    sleep 10
    
    # Monitor chaos engines from the test file
    local chaos_engines
    chaos_engines=$(kubectl get chaosengines -n "$NAMESPACE" -l app=hopngo --no-headers -o custom-columns=":metadata.name" 2>/dev/null || true)
    
    if [[ -z "$chaos_engines" ]]; then
        log "WARN" "No chaos engines found for test $test_name"
        return 1
    fi
    
    log "INFO" "Monitoring chaos engines: $chaos_engines"
    
    # Monitor each chaos engine
    local all_completed=false
    local timeout=600  # 10 minutes timeout
    local elapsed=0
    local check_interval=15
    
    while [[ $elapsed -lt $timeout ]] && [[ $all_completed == false ]]; do
        all_completed=true
        
        for engine in $chaos_engines; do
            local status
            status=$(kubectl get chaosengine "$engine" -n "$NAMESPACE" -o jsonpath='{.status.engineStatus}' 2>/dev/null || echo "unknown")
            
            log "INFO" "Chaos engine $engine status: $status"
            
            if [[ "$status" != "completed" ]] && [[ "$status" != "stopped" ]]; then
                all_completed=false
            fi
            
            # Check for any failures
            local experiments
            experiments=$(kubectl get chaosengine "$engine" -n "$NAMESPACE" -o jsonpath='{.status.experiments[*].name}' 2>/dev/null || echo "")
            
            for exp in $experiments; do
                local exp_status
                exp_status=$(kubectl get chaosresult "${engine}-${exp}" -n "$NAMESPACE" -o jsonpath='{.status.experimentStatus.verdict}' 2>/dev/null || echo "unknown")
                
                if [[ "$exp_status" == "Fail" ]]; then
                    log "ERROR" "Experiment $exp in engine $engine failed"
                elif [[ "$exp_status" == "Pass" ]]; then
                    log "SUCCESS" "Experiment $exp in engine $engine passed"
                fi
            done
        done
        
        if [[ $all_completed == false ]]; then
            sleep $check_interval
            elapsed=$((elapsed + check_interval))
        fi
    done
    
    if [[ $elapsed -ge $timeout ]]; then
        log "WARN" "Chaos test $test_name timed out after $timeout seconds"
    else
        log "SUCCESS" "Chaos test $test_name completed"
    fi
    
    # Collect results
    collect_test_results "$test_name" "$chaos_engines"
    
    # Cleanup chaos engines
    log "INFO" "Cleaning up chaos engines for test $test_name"
    kubectl delete -f "${CHAOS_DIR}/${test_file}" --ignore-not-found=true
    
    # Wait for cleanup
    sleep 30
    
    return 0
}

collect_test_results() {
    local test_name=$1
    local chaos_engines=$2
    
    log "INFO" "Collecting results for test $test_name"
    
    local results_file="${RESULTS_DIR}/${test_name}-results.json"
    
    echo "{" > "$results_file"
    echo "  \"testName\": \"$test_name\"," >> "$results_file"
    echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"," >> "$results_file"
    echo "  \"chaosEngines\": [" >> "$results_file"
    
    local first=true
    for engine in $chaos_engines; do
        if [[ $first == false ]]; then
            echo "," >> "$results_file"
        fi
        first=false
        
        echo "    {" >> "$results_file"
        echo "      \"name\": \"$engine\"," >> "$results_file"
        
        local engine_status
        engine_status=$(kubectl get chaosengine "$engine" -n "$NAMESPACE" -o json 2>/dev/null || echo '{}')
        
        echo "      \"status\": $engine_status" >> "$results_file"
        echo "    }" >> "$results_file"
    done
    
    echo "  ]," >> "$results_file"
    echo "  \"chaosResults\": [" >> "$results_file"
    
    # Collect chaos results
    local results
    results=$(kubectl get chaosresults -n "$NAMESPACE" -o json 2>/dev/null || echo '{"items":[]}')
    echo "    $results" >> "$results_file"
    
    echo "  ]" >> "$results_file"
    echo "}" >> "$results_file"
    
    log "SUCCESS" "Results collected in $results_file"
}

generate_final_report() {
    log "INFO" "Generating final chaos testing report..."
    
    local total_tests=${#CHAOS_TESTS[@]}
    local successful_tests=0
    local failed_tests=0
    
    echo "{" > "$REPORT_FILE"
    echo "  \"chaosTestingSummary\": {" >> "$REPORT_FILE"
    echo "    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"," >> "$REPORT_FILE"
    echo "    \"totalTests\": $total_tests," >> "$REPORT_FILE"
    echo "    \"namespace\": \"$NAMESPACE\"," >> "$REPORT_FILE"
    echo "    \"litmusNamespace\": \"$LITMUS_NAMESPACE\"," >> "$REPORT_FILE"
    echo "    \"testResults\": [" >> "$REPORT_FILE"
    
    local first=true
    for test_name in "${!CHAOS_TESTS[@]}"; do
        if [[ $first == false ]]; then
            echo "," >> "$REPORT_FILE"
        fi
        first=false
        
        local results_file="${RESULTS_DIR}/${test_name}-results.json"
        local test_status="unknown"
        
        if [[ -f "$results_file" ]]; then
            test_status="completed"
            successful_tests=$((successful_tests + 1))
        else
            test_status="failed"
            failed_tests=$((failed_tests + 1))
        fi
        
        echo "      {" >> "$REPORT_FILE"
        echo "        \"testName\": \"$test_name\"," >> "$REPORT_FILE"
        echo "        \"description\": \"${TEST_DESCRIPTIONS[$test_name]}\"," >> "$REPORT_FILE"
        echo "        \"priority\": \"${TEST_PRIORITIES[$test_name]}\"," >> "$REPORT_FILE"
        echo "        \"status\": \"$test_status\"," >> "$REPORT_FILE"
        echo "        \"resultsFile\": \"$results_file\"" >> "$REPORT_FILE"
        echo "      }" >> "$REPORT_FILE"
    done
    
    echo "    ]," >> "$REPORT_FILE"
    echo "    \"summary\": {" >> "$REPORT_FILE"
    echo "      \"successful\": $successful_tests," >> "$REPORT_FILE"
    echo "      \"failed\": $failed_tests," >> "$REPORT_FILE"
    echo "      \"successRate\": \"$(( successful_tests * 100 / total_tests ))%\"" >> "$REPORT_FILE"
    echo "    }" >> "$REPORT_FILE"
    echo "  }" >> "$REPORT_FILE"
    echo "}" >> "$REPORT_FILE"
    
    log "SUCCESS" "Final report generated: $REPORT_FILE"
    
    # Print summary to console
    echo ""
    echo "======================================"
    echo "🎯 CHAOS TESTING SUMMARY"
    echo "======================================"
    echo "Total Tests: $total_tests"
    echo "Successful: $successful_tests"
    echo "Failed: $failed_tests"
    echo "Success Rate: $(( successful_tests * 100 / total_tests ))%"
    echo "Report: $REPORT_FILE"
    echo "Logs: $LOG_FILE"
    echo "======================================"
}

cleanup() {
    log "INFO" "Performing cleanup..."
    
    # Clean up any remaining chaos engines
    kubectl delete chaosengines -n "$NAMESPACE" -l app=hopngo --ignore-not-found=true
    
    # Clean up chaos results
    kubectl delete chaosresults -n "$NAMESPACE" -l app=hopngo --ignore-not-found=true
    
    log "SUCCESS" "Cleanup completed"
}

print_usage() {
    echo "Usage: $0 [options] [test-names...]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -l, --list              List available chaos tests"
    echo "  -n, --namespace NAME    Target namespace (default: $NAMESPACE)"
    echo "  --litmus-ns NAME        Litmus namespace (default: $LITMUS_NAMESPACE)"
    echo "  --setup-monitoring      Setup chaos monitoring"
    echo "  --cleanup-only          Only perform cleanup"
    echo "  --skip-prerequisites    Skip prerequisite checks"
    echo ""
    echo "Available Tests:"
    for test_name in "${!CHAOS_TESTS[@]}"; do
        echo "  $test_name - ${TEST_DESCRIPTIONS[$test_name]}"
    done
    echo ""
    echo "Examples:"
    echo "  $0                      # Run all chaos tests"
    echo "  $0 pod-kill db-outage   # Run specific tests"
    echo "  $0 --setup-monitoring   # Setup monitoring only"
    echo "  $0 --cleanup-only       # Cleanup only"
}

list_tests() {
    echo "Available Chaos Tests:"
    echo ""
    for test_name in "${!CHAOS_TESTS[@]}"; do
        echo "Test: $test_name"
        echo "  Description: ${TEST_DESCRIPTIONS[$test_name]}"
        echo "  Priority: ${TEST_PRIORITIES[$test_name]}"
        echo "  File: ${CHAOS_TESTS[$test_name]}"
        echo ""
    done
}

# Main execution
main() {
    local selected_tests=()
    local setup_monitoring_only=false
    local cleanup_only=false
    local skip_prerequisites=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                print_usage
                exit 0
                ;;
            -l|--list)
                list_tests
                exit 0
                ;;
            -n|--namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            --litmus-ns)
                LITMUS_NAMESPACE="$2"
                shift 2
                ;;
            --setup-monitoring)
                setup_monitoring_only=true
                shift
                ;;
            --cleanup-only)
                cleanup_only=true
                shift
                ;;
            --skip-prerequisites)
                skip_prerequisites=true
                shift
                ;;
            -*)
                log "ERROR" "Unknown option: $1"
                print_usage
                exit 1
                ;;
            *)
                if [[ -n "${CHAOS_TESTS[$1]:-}" ]]; then
                    selected_tests+=("$1")
                else
                    log "ERROR" "Unknown test: $1"
                    list_tests
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    # Initialize log file
    echo "Chaos Testing Log - $(date)" > "$LOG_FILE"
    
    log "INFO" "Starting HopNGo Chaos Engineering Tests"
    log "INFO" "Target namespace: $NAMESPACE"
    log "INFO" "Litmus namespace: $LITMUS_NAMESPACE"
    log "INFO" "Results directory: $RESULTS_DIR"
    
    # Handle special modes
    if [[ $cleanup_only == true ]]; then
        cleanup
        exit 0
    fi
    
    if [[ $setup_monitoring_only == true ]]; then
        setup_monitoring
        exit 0
    fi
    
    # Prerequisites check
    if [[ $skip_prerequisites == false ]]; then
        check_prerequisites
    fi
    
    # Setup monitoring
    setup_monitoring
    
    # Determine tests to run
    if [[ ${#selected_tests[@]} -eq 0 ]]; then
        selected_tests=("${!CHAOS_TESTS[@]}")
    fi
    
    log "INFO" "Running ${#selected_tests[@]} chaos tests: ${selected_tests[*]}"
    
    # Run chaos tests
    local test_results=()
    for test_name in "${selected_tests[@]}"; do
        local test_file="${CHAOS_TESTS[$test_name]}"
        local test_description="${TEST_DESCRIPTIONS[$test_name]}"
        
        log "INFO" "\n=== Starting Chaos Test: $test_name ==="
        
        if run_chaos_test "$test_name" "$test_file" "$test_description"; then
            test_results+=("$test_name:success")
            log "SUCCESS" "Chaos test $test_name completed successfully"
        else
            test_results+=("$test_name:failed")
            log "ERROR" "Chaos test $test_name failed"
        fi
        
        # Wait between tests
        if [[ "$test_name" != "${selected_tests[-1]}" ]]; then
            log "INFO" "Waiting 60 seconds before next test..."
            sleep 60
        fi
    done
    
    # Generate final report
    generate_final_report
    
    # Final cleanup
    cleanup
    
    # Exit with appropriate code
    local failed_count=0
    for result in "${test_results[@]}"; do
        if [[ "$result" == *":failed" ]]; then
            failed_count=$((failed_count + 1))
        fi
    done
    
    if [[ $failed_count -gt 0 ]]; then
        log "ERROR" "$failed_count chaos tests failed"
        exit 1
    else
        log "SUCCESS" "All chaos tests completed successfully"
        exit 0
    fi
}

# Trap for cleanup on exit
trap cleanup EXIT

# Run main function
main "$@"
# HopNGo On-Call Runbook

## Overview

This runbook provides guidance for on-call engineers responding to incidents in the HopNGo platform. It covers common scenarios, diagnostic queries, and remediation procedures.

## Emergency Contacts

- **Primary On-Call**: Check PagerDuty/OpsGenie rotation
- **Secondary On-Call**: Check PagerDuty/OpsGenie rotation
- **Engineering Manager**: manager@hopngo.com
- **CTO**: cto@hopngo.com
- **Infrastructure Team**: infrastructure@hopngo.com

## Severity Levels

### Critical (P0)
- Service completely down
- Data loss or corruption
- Security breach
- Payment processing failures
- **Response Time**: Immediate (< 5 minutes)

### High (P1)
- Significant performance degradation
- Feature unavailable
- High error rates
- **Response Time**: < 15 minutes

### Medium (P2)
- Minor performance issues
- Non-critical feature issues
- **Response Time**: < 1 hour

### Low (P3)
- Cosmetic issues
- Enhancement requests
- **Response Time**: Next business day

## Common Incidents and Remediation

### 1. Service Down

#### Symptoms
- Alert: `ServiceDown`
- Service health check failing
- 5xx errors from load balancer

#### Diagnostic Queries

```promql
# Check service status
up{job="service-name"}

# Check recent deployments
rate(deployment_events_total[1h])

# Check container restarts
increase(kube_pod_container_restart_total[1h])

# Check resource usage
container_memory_usage_bytes{pod=~"service-name.*"}
rate(container_cpu_usage_seconds_total{pod=~"service-name.*"}[5m])
```

#### Remediation Steps

1. **Immediate Response**
   ```bash
   # Check service logs
   kubectl logs -f deployment/service-name --tail=100
   
   # Check pod status
   kubectl get pods -l app=service-name
   
   # Describe problematic pods
   kubectl describe pod <pod-name>
   ```

2. **Common Fixes**
   ```bash
   # Restart deployment
   kubectl rollout restart deployment/service-name
   
   # Scale up replicas
   kubectl scale deployment service-name --replicas=5
   
   # Check for resource constraints
   kubectl top pods
   kubectl describe nodes
   ```

3. **Database Connection Issues**
   ```bash
   # Check database connectivity
   kubectl exec -it deployment/service-name -- nc -zv postgres-service 5432
   
   # Check connection pool
   # Look for "connection pool exhausted" in logs
   ```

### 2. High Latency (P95 > SLO)

#### Symptoms
- Alert: `BookingSearchLatencySLOBurnRateHigh`
- Alert: `MarketCheckoutLatencySLOBurnRateHigh`
- User complaints about slow responses

#### Diagnostic Queries

```promql
# P95 latency by endpoint
histogram_quantile(0.95,
  sum(rate(http_request_duration_seconds_bucket[5m])) by (uri, le)
)

# Request rate by endpoint
sum(rate(http_requests_total[5m])) by (uri)

# Database query performance
histogram_quantile(0.95,
  sum(rate(pg_stat_statements_mean_time_seconds_bucket[5m])) by (le)
)

# JVM GC impact
rate(jvm_gc_collection_seconds_sum[5m])
```

#### Remediation Steps

1. **Identify Bottleneck**
   ```bash
   # Check slow queries
   kubectl exec -it postgres-0 -- psql -U hopngo -c "
   SELECT query, mean_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_time DESC 
   LIMIT 10;"
   
   # Check JVM heap usage
   # Look for high GC activity in metrics
   ```

2. **Quick Fixes**
   ```bash
   # Scale horizontally
   kubectl scale deployment service-name --replicas=10
   
   # Increase JVM heap (if memory available)
   kubectl patch deployment service-name -p '{
     "spec": {
       "template": {
         "spec": {
           "containers": [{
             "name": "service-name",
             "env": [{
               "name": "JAVA_OPTS",
               "value": "-Xmx2g -Xms1g"
             }]
           }]
         }
       }
     }
   }'
   ```

3. **Database Optimization**
   ```sql
   -- Kill long-running queries
   SELECT pg_terminate_backend(pid) 
   FROM pg_stat_activity 
   WHERE state = 'active' 
   AND query_start < now() - interval '5 minutes';
   
   -- Check for missing indexes
   SELECT schemaname, tablename, attname, n_distinct, correlation 
   FROM pg_stats 
   WHERE schemaname = 'public' 
   ORDER BY n_distinct DESC;
   ```

### 3. High Error Rate

#### Symptoms
- Alert: `ServiceHighErrorRate`
- Alert: `MarketCheckoutErrorRateSLOBurnRateHigh`
- Alert: `AuthLoginErrorRateHigh`

#### Diagnostic Queries

```promql
# Error rate by status code
sum(rate(http_requests_total{code=~"5.."}[5m])) by (code, uri)

# Error rate by service
sum(rate(http_requests_total{code=~"5.."}[5m])) by (job)

# Recent error logs
increase(log_entries_total{level="error"}[10m])
```

#### Remediation Steps

1. **Identify Error Source**
   ```bash
   # Check recent error logs
   kubectl logs deployment/service-name --since=10m | grep ERROR
   
   # Check for specific error patterns
   kubectl logs deployment/service-name --since=10m | grep -E "(OutOfMemory|Connection|Timeout)"
   ```

2. **Common Error Patterns**
   
   **Database Connection Errors**
   ```bash
   # Check connection pool settings
   # Look for "connection pool exhausted" errors
   
   # Temporary fix: restart service
   kubectl rollout restart deployment/service-name
   ```
   
   **Memory Errors**
   ```bash
   # Check memory usage
   kubectl top pods
   
   # Increase memory limits
   kubectl patch deployment service-name -p '{
     "spec": {
       "template": {
         "spec": {
           "containers": [{
             "name": "service-name",
             "resources": {
               "limits": {
                 "memory": "2Gi"
               }
             }
           }]
         }
       }
     }
   }'
   ```

### 4. Queue Lag (RabbitMQ)

#### Symptoms
- Alert: `RabbitMQQueueLagHigh`
- Messages piling up in queues
- Processing delays

#### Diagnostic Queries

```promql
# Queue depth by queue
rabbitmq_queue_messages_ready

# Consumer count
rabbitmq_queue_consumers

# Message processing rate
rate(rabbitmq_queue_messages_delivered_total[5m])

# Consumer utilization
rabbitmq_queue_consumer_utilisation
```

#### Remediation Steps

1. **Check Queue Status**
   ```bash
   # Access RabbitMQ management
   kubectl port-forward svc/rabbitmq 15672:15672
   # Open http://localhost:15672 (guest/guest)
   
   # CLI commands
   kubectl exec -it rabbitmq-0 -- rabbitmqctl list_queues name messages consumers
   ```

2. **Clear Stuck Messages**
   ```bash
   # Purge queue (CAUTION: Data loss)
   kubectl exec -it rabbitmq-0 -- rabbitmqctl purge_queue queue-name
   
   # Move messages to DLQ
   kubectl exec -it rabbitmq-0 -- rabbitmqctl eval '
   rabbit_amqqueue:with_or_die(
     {resource, <<"virtual-host">>, queue, <<"queue-name">>},
     fun(Q) -> rabbit_amqqueue:purge(Q) end
   ).'
   ```

3. **Scale Consumers**
   ```bash
   # Scale consumer service
   kubectl scale deployment consumer-service --replicas=5
   
   # Check consumer health
   kubectl logs deployment/consumer-service --tail=50
   ```

### 5. Database Issues

#### Symptoms
- Alert: `PostgreSQLConnectionSaturationHigh`
- Connection timeouts
- Slow queries

#### Diagnostic Queries

```promql
# Connection usage
pg_stat_database_numbackends / pg_settings_max_connections * 100

# Active connections
pg_stat_activity_count{state="active"}

# Lock waits
pg_locks_count{mode="AccessExclusiveLock"}

# Database size
pg_database_size_bytes
```

#### Remediation Steps

1. **Connection Management**
   ```sql
   -- Check active connections
   SELECT pid, usename, application_name, client_addr, state, query_start, query
   FROM pg_stat_activity
   WHERE state = 'active'
   ORDER BY query_start;
   
   -- Kill idle connections
   SELECT pg_terminate_backend(pid)
   FROM pg_stat_activity
   WHERE state = 'idle'
   AND query_start < now() - interval '1 hour';
   ```

2. **Performance Issues**
   ```sql
   -- Find slow queries
   SELECT query, mean_time, calls, total_time
   FROM pg_stat_statements
   ORDER BY mean_time DESC
   LIMIT 10;
   
   -- Check for locks
   SELECT blocked_locks.pid AS blocked_pid,
          blocked_activity.usename AS blocked_user,
          blocking_locks.pid AS blocking_pid,
          blocking_activity.usename AS blocking_user,
          blocked_activity.query AS blocked_statement
   FROM pg_catalog.pg_locks blocked_locks
   JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
   JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
   JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
   WHERE NOT blocked_locks.granted;
   ```

### 6. JVM Memory Issues

#### Symptoms
- Alert: `JVMHeapUtilizationHigh`
- OutOfMemoryError in logs
- Frequent GC activity

#### Diagnostic Queries

```promql
# Heap utilization
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# GC frequency
rate(jvm_gc_collection_seconds_count[5m])

# GC time
rate(jvm_gc_collection_seconds_sum[5m])
```

#### Remediation Steps

1. **Immediate Actions**
   ```bash
   # Restart affected pods
   kubectl delete pod -l app=service-name
   
   # Check for memory leaks in logs
   kubectl logs deployment/service-name | grep -i "outofmemory\|heap"
   ```

2. **Memory Tuning**
   ```bash
   # Increase heap size
   kubectl patch deployment service-name -p '{
     "spec": {
       "template": {
         "spec": {
           "containers": [{
             "name": "service-name",
             "env": [{
               "name": "JAVA_OPTS",
               "value": "-Xmx4g -Xms2g -XX:+UseG1GC"
             }]
           }]
         }
       }
     }
   }'
   ```

## Security Incidents

### Key Rotation

#### When to Rotate
- Suspected key compromise
- Regular rotation schedule
- Employee departure

#### Steps

1. **Database Credentials**
   ```bash
   # Generate new password
   NEW_PASSWORD=$(openssl rand -base64 32)
   
   # Update in database
   kubectl exec -it postgres-0 -- psql -U postgres -c "
   ALTER USER hopngo PASSWORD '$NEW_PASSWORD';"
   
   # Update Kubernetes secret
   kubectl create secret generic postgres-credentials \
     --from-literal=username=hopngo \
     --from-literal=password=$NEW_PASSWORD \
     --dry-run=client -o yaml | kubectl apply -f -
   
   # Restart services
   kubectl rollout restart deployment/booking-service
   kubectl rollout restart deployment/market-service
   ```

2. **API Keys**
   ```bash
   # Update external service API keys
   kubectl create secret generic external-api-keys \
     --from-literal=payment-gateway-key=new-key \
     --from-literal=email-service-key=new-key \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

3. **JWT Secrets**
   ```bash
   # Generate new JWT secret
   JWT_SECRET=$(openssl rand -base64 64)
   
   # Update secret
   kubectl create secret generic jwt-secret \
     --from-literal=secret=$JWT_SECRET \
     --dry-run=client -o yaml | kubectl apply -f -
   
   # Restart auth service
   kubectl rollout restart deployment/auth-service
   ```

## Scaling Procedures

### Scale Up

```bash
# Scale specific service
kubectl scale deployment service-name --replicas=10

# Scale all services
for service in gateway booking-service market-service auth-service; do
  kubectl scale deployment $service --replicas=5
done

# Scale database (if using read replicas)
kubectl scale statefulset postgres-replica --replicas=3
```

### Scale Down

```bash
# Gradual scale down
kubectl scale deployment service-name --replicas=3

# Wait and monitor
sleep 60

# Continue scaling down if metrics are good
kubectl scale deployment service-name --replicas=2
```

## Monitoring and Alerting

### Key Dashboards

- **SLO Overview**: http://grafana.hopngo.com/d/slo-overview
- **Service Health**: http://grafana.hopngo.com/d/service-health
- **Infrastructure**: http://grafana.hopngo.com/d/infrastructure
- **Database**: http://grafana.hopngo.com/d/database
- **RabbitMQ**: http://grafana.hopngo.com/d/rabbitmq

### Useful Queries

```promql
# Overall system health
up{job=~"gateway|booking-service|market-service|auth-service"}

# Request rate
sum(rate(http_requests_total[5m])) by (job)

# Error rate
sum(rate(http_requests_total{code=~"5.."}[5m])) by (job) / sum(rate(http_requests_total[5m])) by (job)

# P95 latency
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (job, le))

# Resource usage
sum(rate(container_cpu_usage_seconds_total[5m])) by (pod)
sum(container_memory_usage_bytes) by (pod)
```

## Communication

### Incident Communication Template

```
Subject: [INCIDENT] Brief description - Status

Incident: #INC-YYYY-MMDD-001
Severity: P0/P1/P2/P3
Status: Investigating/Identified/Monitoring/Resolved
Start Time: YYYY-MM-DD HH:MM UTC
Services Affected: List of affected services
User Impact: Description of user impact

Summary:
Brief description of the issue

Current Status:
What we know and what we're doing

Next Update:
When the next update will be provided

Incident Commander: Name
```

### Escalation Matrix

| Time | Action |
|------|--------|
| 0-5 min | On-call engineer responds |
| 15 min | Escalate to secondary on-call |
| 30 min | Escalate to engineering manager |
| 1 hour | Escalate to CTO |
| 2 hours | Consider external communication |

## Post-Incident

### Immediate Actions
1. Ensure incident is fully resolved
2. Document timeline and actions taken
3. Identify root cause
4. Create follow-up tasks

### Post-Mortem Template

```markdown
# Post-Mortem: [Incident Title]

## Incident Summary
- **Date**: YYYY-MM-DD
- **Duration**: X hours Y minutes
- **Severity**: P0/P1/P2/P3
- **Services Affected**: List
- **User Impact**: Description

## Timeline
- **HH:MM** - Initial alert
- **HH:MM** - Investigation started
- **HH:MM** - Root cause identified
- **HH:MM** - Fix implemented
- **HH:MM** - Service restored

## Root Cause
Detailed explanation of what caused the incident

## Resolution
What was done to resolve the incident

## Action Items
- [ ] Item 1 (Owner: Name, Due: Date)
- [ ] Item 2 (Owner: Name, Due: Date)

## Lessons Learned
- What went well
- What could be improved
- Process improvements
```

## Useful Commands Reference

### Kubernetes
```bash
# Pod management
kubectl get pods -o wide
kubectl describe pod <pod-name>
kubectl logs -f <pod-name>
kubectl exec -it <pod-name> -- /bin/bash

# Deployment management
kubectl rollout status deployment/<deployment-name>
kubectl rollout restart deployment/<deployment-name>
kubectl rollout undo deployment/<deployment-name>

# Resource monitoring
kubectl top nodes
kubectl top pods

# Events
kubectl get events --sort-by=.metadata.creationTimestamp
```

### Docker
```bash
# Container management
docker ps
docker logs -f <container-id>
docker exec -it <container-id> /bin/bash
docker stats

# Image management
docker images
docker pull <image>
docker build -t <tag> .
```

### Database
```sql
-- PostgreSQL monitoring
SELECT * FROM pg_stat_activity;
SELECT * FROM pg_stat_database;
SELECT * FROM pg_locks;

-- Performance
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;
```

## Contact Information

- **Slack**: #incidents, #on-call
- **Email**: oncall@hopngo.com
- **Phone**: Check PagerDuty for current rotation
- **Runbook Updates**: Create PR against this document

---

**Last Updated**: 2024-01-12  
**Version**: 1.0  
**Owner**: Platform Team
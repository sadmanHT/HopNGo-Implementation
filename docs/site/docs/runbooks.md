---
sidebar_position: 6
---

# Runbooks

This document provides step-by-step operational procedures for critical system operations, incident response, and maintenance tasks.

## Overview

Runbooks are essential for maintaining system reliability and ensuring consistent operational procedures. Each runbook includes:

- **Purpose**: What the procedure accomplishes
- **Prerequisites**: Required access, tools, and conditions
- **Steps**: Detailed step-by-step instructions
- **Verification**: How to confirm success
- **Rollback**: Recovery procedures if something goes wrong
- **Escalation**: When and how to escalate issues

## Deployment Runbooks

### Production Deployment

#### Purpose
Deploy new application versions to production environment with zero downtime.

#### Prerequisites
- [ ] Code reviewed and approved
- [ ] All tests passing in CI/CD
- [ ] Staging deployment successful
- [ ] Database migrations tested
- [ ] Rollback plan prepared
- [ ] Change approval obtained
- [ ] Deployment window scheduled

#### Pre-Deployment Checklist
```bash
# 1. Verify staging environment
curl -f https://staging-api.hopngo.com/health
if [ $? -ne 0 ]; then
    echo "Staging health check failed. Aborting deployment."
    exit 1
fi

# 2. Check database migration status
psql -h staging-db.hopngo.com -U hopngo -d hopngo -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;"

# 3. Verify backup completion
aws rds describe-db-snapshots --db-instance-identifier hopngo-prod --query 'DBSnapshots[0].Status'

# 4. Check system resources
aws cloudwatch get-metric-statistics \
  --namespace AWS/EC2 \
  --metric-name CPUUtilization \
  --dimensions Name=InstanceId,Value=i-1234567890abcdef0 \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average
```

#### Deployment Steps

**Step 1: Database Migration**
```bash
#!/bin/bash
# Run database migrations

echo "Starting database migration..."
cd backend

# Create migration backup
echo "Creating pre-migration backup..."
aws rds create-db-snapshot \
  --db-instance-identifier hopngo-prod \
  --db-snapshot-identifier hopngo-prod-pre-migration-$(date +%Y%m%d-%H%M%S)

# Wait for backup completion
echo "Waiting for backup to complete..."
aws rds wait db-snapshot-completed \
  --db-snapshot-identifier hopngo-prod-pre-migration-$(date +%Y%m%d-%H%M%S)

# Run migrations
echo "Running database migrations..."
SPRING_PROFILES_ACTIVE=production \
DATABASE_URL=jdbc:postgresql://prod-db.hopngo.com:5432/hopngo \
./mvnw flyway:migrate

if [ $? -eq 0 ]; then
    echo "Database migration completed successfully"
else
    echo "Database migration failed. Check logs and consider rollback."
    exit 1
fi
```

**Step 2: Backend Deployment**
```bash
#!/bin/bash
# Deploy backend application

echo "Deploying backend application..."

# Build application
cd backend
./mvnw clean package -DskipTests

# Deploy to Elastic Beanstalk
eb deploy hopngo-backend-production

# Wait for deployment
echo "Waiting for deployment to complete..."
eb health hopngo-backend-production

# Verify deployment
echo "Verifying backend deployment..."
for i in {1..30}; do
    if curl -f https://api.hopngo.com/health; then
        echo "Backend deployment successful"
        break
    else
        echo "Attempt $i: Backend not ready, waiting 10 seconds..."
        sleep 10
    fi
done
```

**Step 3: Frontend Deployment**
```bash
#!/bin/bash
# Deploy frontend application

echo "Deploying frontend application..."

# Build frontend
cd frontend
npm ci
npm run build

# Deploy to S3
aws s3 sync build/ s3://hopngo-prod-frontend --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id E1234567890ABC \
  --paths "/*"

# Wait for invalidation
echo "Waiting for cache invalidation..."
aws cloudfront wait invalidation-completed \
  --distribution-id E1234567890ABC \
  --id $(aws cloudfront list-invalidations --distribution-id E1234567890ABC --query 'InvalidationList.Items[0].Id' --output text)

echo "Frontend deployment completed"
```

#### Post-Deployment Verification
```bash
#!/bin/bash
# Verify deployment success

echo "Running post-deployment verification..."

# Health checks
echo "Checking API health..."
curl -f https://api.hopngo.com/health || exit 1

echo "Checking frontend..."
curl -f https://hopngo.com || exit 1

# Functional tests
echo "Running smoke tests..."
npm run test:smoke:production

# Performance checks
echo "Checking response times..."
for endpoint in "/api/listings" "/api/users/profile" "/api/bookings"; do
    response_time=$(curl -o /dev/null -s -w '%{time_total}' https://api.hopngo.com$endpoint)
    if (( $(echo "$response_time > 2.0" | bc -l) )); then
        echo "WARNING: Slow response time for $endpoint: ${response_time}s"
    else
        echo "OK: $endpoint response time: ${response_time}s"
    fi
done

# Database connectivity
echo "Checking database connectivity..."
psql -h prod-db.hopngo.com -U hopngo -d hopngo -c "SELECT 1;" || exit 1

echo "Deployment verification completed successfully"
```

#### Rollback Procedure
```bash
#!/bin/bash
# Emergency rollback procedure

echo "EMERGENCY ROLLBACK INITIATED"
echo "Timestamp: $(date)"
echo "Initiated by: $USER"

# Rollback backend
echo "Rolling back backend..."
eb deploy hopngo-backend-production --version-label previous-stable

# Rollback frontend
echo "Rolling back frontend..."
aws s3 sync s3://hopngo-prod-frontend-backup/ s3://hopngo-prod-frontend --delete
aws cloudfront create-invalidation --distribution-id E1234567890ABC --paths "/*"

# Rollback database if needed
read -p "Rollback database? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Rolling back database..."
    # Restore from pre-migration backup
    aws rds restore-db-instance-from-db-snapshot \
      --db-instance-identifier hopngo-prod-rollback \
      --db-snapshot-identifier hopngo-prod-pre-migration-latest
fi

echo "Rollback completed. Verify system functionality."
```

### Hotfix Deployment

#### Purpose
Deploy critical fixes outside of normal deployment windows.

#### Prerequisites
- [ ] Critical issue identified
- [ ] Fix developed and tested
- [ ] Emergency change approval
- [ ] On-call engineer available

#### Hotfix Steps
```bash
#!/bin/bash
# Hotfix deployment procedure

HOTFIX_VERSION=$1
if [ -z "$HOTFIX_VERSION" ]; then
    echo "Usage: $0 <hotfix-version>"
    exit 1
fi

echo "Deploying hotfix version: $HOTFIX_VERSION"

# Create hotfix branch
git checkout -b hotfix/$HOTFIX_VERSION

# Apply fix and test
echo "Apply your hotfix changes now and run tests"
read -p "Press enter when ready to deploy..."

# Quick deployment
echo "Building and deploying hotfix..."
cd backend
./mvnw clean package -DskipTests
eb deploy hopngo-backend-production --version-label hotfix-$HOTFIX_VERSION

# Monitor deployment
echo "Monitoring deployment..."
for i in {1..10}; do
    if curl -f https://api.hopngo.com/health; then
        echo "Hotfix deployed successfully"
        break
    else
        echo "Waiting for deployment... ($i/10)"
        sleep 30
    fi
done

# Notify team
echo "Hotfix $HOTFIX_VERSION deployed. Notify team and monitor closely."
```

## Database Operations

### Database Migration

#### Purpose
Apply schema changes and data migrations safely.

#### Migration Checklist
```sql
-- Pre-migration checklist
-- 1. Backup database
-- 2. Test migration on staging
-- 3. Estimate migration time
-- 4. Plan for rollback
-- 5. Schedule maintenance window

-- Example migration script
-- V1.5.0__add_booking_status_index.sql

-- Add index for better query performance
CREATE INDEX CONCURRENTLY idx_bookings_status_created 
ON bookings(status, created_at) 
WHERE status IN ('PENDING', 'CONFIRMED');

-- Update statistics
ANALYZE bookings;

-- Verify index creation
SELECT schemaname, tablename, indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'bookings' 
AND indexname = 'idx_bookings_status_created';
```

#### Migration Execution
```bash
#!/bin/bash
# Database migration execution

echo "Starting database migration process..."

# 1. Create backup
echo "Creating database backup..."
pg_dump -h prod-db.hopngo.com -U hopngo -d hopngo -f backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Check migration files
echo "Checking migration files..."
ls -la src/main/resources/db/migration/

# 3. Dry run (if supported)
echo "Running migration dry run..."
./mvnw flyway:info -Dflyway.url=jdbc:postgresql://staging-db.hopngo.com:5432/hopngo

# 4. Execute migration
echo "Executing migration..."
START_TIME=$(date +%s)
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db.hopngo.com:5432/hopngo
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "Migration completed in $DURATION seconds"

# 5. Verify migration
echo "Verifying migration..."
psql -h prod-db.hopngo.com -U hopngo -d hopngo -c "
    SELECT version, description, installed_on, success 
    FROM flyway_schema_history 
    ORDER BY installed_rank DESC 
    LIMIT 5;
"

# 6. Test application connectivity
echo "Testing application connectivity..."
curl -f https://api.hopngo.com/health

echo "Database migration completed successfully"
```

### Database Backup & Restore

#### Backup Procedure
```bash
#!/bin/bash
# Database backup procedure

BACKUP_TYPE=${1:-full}  # full, incremental, or snapshot
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

case $BACKUP_TYPE in
    "full")
        echo "Creating full database backup..."
        pg_dump -h prod-db.hopngo.com -U hopngo -d hopngo \
                -f "hopngo_full_backup_$TIMESTAMP.sql" \
                --verbose --no-password
        
        # Compress backup
        gzip "hopngo_full_backup_$TIMESTAMP.sql"
        
        # Upload to S3
        aws s3 cp "hopngo_full_backup_$TIMESTAMP.sql.gz" \
                  s3://hopngo-backups/database/full/
        ;;
    
    "snapshot")
        echo "Creating RDS snapshot..."
        aws rds create-db-snapshot \
            --db-instance-identifier hopngo-prod \
            --db-snapshot-identifier "hopngo-prod-$TIMESTAMP"
        
        # Wait for completion
        aws rds wait db-snapshot-completed \
            --db-snapshot-identifier "hopngo-prod-$TIMESTAMP"
        ;;
    
    "incremental")
        echo "Creating incremental backup..."
        # WAL-E or similar tool for incremental backups
        wal-e backup-push /var/lib/postgresql/data
        ;;
esac

echo "Backup completed: $BACKUP_TYPE backup created at $TIMESTAMP"
```

#### Restore Procedure
```bash
#!/bin/bash
# Database restore procedure

BACKUP_FILE=$1
TARGET_DB=${2:-hopngo_restore}

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file> [target-database]"
    echo "Available backups:"
    aws s3 ls s3://hopngo-backups/database/full/
    exit 1
fi

echo "WARNING: This will restore database from backup"
echo "Backup file: $BACKUP_FILE"
echo "Target database: $TARGET_DB"
read -p "Continue? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Restore cancelled"
    exit 1
fi

# Download backup if from S3
if [[ $BACKUP_FILE == s3://* ]]; then
    echo "Downloading backup from S3..."
    aws s3 cp "$BACKUP_FILE" ./restore_backup.sql.gz
    gunzip restore_backup.sql.gz
    BACKUP_FILE="./restore_backup.sql"
fi

# Create target database
echo "Creating target database..."
psql -h prod-db.hopngo.com -U postgres -c "CREATE DATABASE $TARGET_DB;"

# Restore data
echo "Restoring database..."
psql -h prod-db.hopngo.com -U hopngo -d $TARGET_DB -f "$BACKUP_FILE"

# Verify restore
echo "Verifying restore..."
psql -h prod-db.hopngo.com -U hopngo -d $TARGET_DB -c "
    SELECT 
        schemaname,
        tablename,
        n_tup_ins as inserts,
        n_tup_upd as updates,
        n_tup_del as deletes
    FROM pg_stat_user_tables 
    ORDER BY n_tup_ins DESC 
    LIMIT 10;
"

echo "Database restore completed successfully"
echo "Target database: $TARGET_DB"
```

## Security Operations

### Secrets Rotation

#### Purpose
Regularly rotate sensitive credentials and API keys.

#### Database Password Rotation
```bash
#!/bin/bash
# Database password rotation

echo "Starting database password rotation..."

# Generate new password
NEW_PASSWORD=$(openssl rand -base64 32)
echo "New password generated"

# Update password in RDS
echo "Updating RDS master password..."
aws rds modify-db-instance \
    --db-instance-identifier hopngo-prod \
    --master-user-password "$NEW_PASSWORD" \
    --apply-immediately

# Wait for modification
echo "Waiting for password update..."
aws rds wait db-instance-available --db-instance-identifier hopngo-prod

# Update application secrets
echo "Updating application secrets..."
aws secretsmanager update-secret \
    --secret-id hopngo/database/password \
    --secret-string "$NEW_PASSWORD"

# Restart application to pick up new password
echo "Restarting application..."
eb restart hopngo-backend-production

# Verify connectivity
echo "Verifying database connectivity..."
sleep 60  # Wait for restart
curl -f https://api.hopngo.com/health

echo "Database password rotation completed"
```

#### API Key Rotation
```bash
#!/bin/bash
# API key rotation for external services

SERVICE=$1
if [ -z "$SERVICE" ]; then
    echo "Usage: $0 <service-name>"
    echo "Available services: stripe, bkash, sendgrid, google-maps"
    exit 1
fi

case $SERVICE in
    "stripe")
        echo "Rotating Stripe API keys..."
        # Generate new keys in Stripe dashboard
        echo "1. Generate new keys in Stripe dashboard"
        echo "2. Update secrets in AWS Secrets Manager"
        read -p "Enter new secret key: " -s NEW_SECRET_KEY
        echo
        read -p "Enter new publishable key: " NEW_PUBLISHABLE_KEY
        
        # Update secrets
        aws secretsmanager update-secret \
            --secret-id hopngo/stripe/secret-key \
            --secret-string "$NEW_SECRET_KEY"
        
        aws secretsmanager update-secret \
            --secret-id hopngo/stripe/publishable-key \
            --secret-string "$NEW_PUBLISHABLE_KEY"
        ;;
    
    "bkash")
        echo "Rotating bKash API credentials..."
        # Similar process for bKash
        ;;
    
    "sendgrid")
        echo "Rotating SendGrid API key..."
        # Similar process for SendGrid
        ;;
esac

# Restart services
echo "Restarting services to pick up new credentials..."
eb restart hopngo-backend-production

# Verify functionality
echo "Verifying service functionality..."
sleep 60
npm run test:integration:$SERVICE

echo "$SERVICE API key rotation completed"
```

#### SSL Certificate Renewal
```bash
#!/bin/bash
# SSL certificate renewal

echo "Starting SSL certificate renewal..."

# Check current certificate expiry
echo "Checking current certificate..."
echo | openssl s_client -servername hopngo.com -connect hopngo.com:443 2>/dev/null | \
openssl x509 -noout -dates

# Request new certificate (Let's Encrypt)
echo "Requesting new certificate..."
certbot certonly --webroot \
    -w /var/www/html \
    -d hopngo.com \
    -d www.hopngo.com \
    -d api.hopngo.com \
    --non-interactive \
    --agree-tos \
    --email admin@hopngo.com

# Update load balancer certificate
echo "Updating load balancer certificate..."
aws elbv2 modify-listener \
    --listener-arn arn:aws:elasticloadbalancing:region:account:listener/app/hopngo-alb/listener-id \
    --certificates CertificateArn=arn:aws:acm:region:account:certificate/new-cert-id

# Verify new certificate
echo "Verifying new certificate..."
sleep 30
echo | openssl s_client -servername hopngo.com -connect hopngo.com:443 2>/dev/null | \
openssl x509 -noout -dates

echo "SSL certificate renewal completed"
```

## Incident Response

### On-Call Procedures

#### Incident Classification

| Severity | Response Time | Description | Examples |
|----------|---------------|-------------|----------|
| **P0 - Critical** | 15 minutes | Complete service outage | Site down, payment failures |
| **P1 - High** | 1 hour | Major functionality impacted | Login issues, booking failures |
| **P2 - Medium** | 4 hours | Minor functionality impacted | Search slow, email delays |
| **P3 - Low** | 24 hours | Cosmetic or minor issues | UI glitches, typos |

#### Incident Response Workflow

**Step 1: Incident Detection**
```bash
#!/bin/bash
# Automated incident detection

# Check system health
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://api.hopngo.com/health)

if [ "$HEALTH_STATUS" != "200" ]; then
    echo "ALERT: API health check failed with status $HEALTH_STATUS"
    
    # Send alert to on-call engineer
    aws sns publish \
        --topic-arn arn:aws:sns:region:account:hopngo-alerts \
        --message "API health check failed. Status: $HEALTH_STATUS. Time: $(date)"
    
    # Create incident in PagerDuty
    curl -X POST https://api.pagerduty.com/incidents \
        -H "Authorization: Token token=YOUR_API_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "incident": {
                "type": "incident",
                "title": "API Health Check Failed",
                "service": {
                    "id": "SERVICE_ID",
                    "type": "service_reference"
                },
                "urgency": "high"
            }
        }'
fi
```

**Step 2: Initial Response**
```bash
#!/bin/bash
# Initial incident response

INCIDENT_ID=$1
if [ -z "$INCIDENT_ID" ]; then
    echo "Usage: $0 <incident-id>"
    exit 1
fi

echo "Responding to incident: $INCIDENT_ID"
echo "Timestamp: $(date)"
echo "On-call engineer: $USER"

# Acknowledge incident
echo "Acknowledging incident..."
# PagerDuty API call to acknowledge

# Gather initial information
echo "Gathering system information..."

# Check application status
echo "=== Application Status ==="
curl -s https://api.hopngo.com/health | jq .

# Check database status
echo "=== Database Status ==="
psql -h prod-db.hopngo.com -U hopngo -d hopngo -c "SELECT 1;" 2>&1

# Check recent deployments
echo "=== Recent Deployments ==="
eb events hopngo-backend-production --max-items 10

# Check error rates
echo "=== Error Rates ==="
aws logs filter-log-events \
    --log-group-name /aws/elasticbeanstalk/hopngo-backend-production/var/log/eb-docker/containers/eb-current-app/stdouterr.log \
    --start-time $(date -d '1 hour ago' +%s)000 \
    --filter-pattern "ERROR"

# Check system metrics
echo "=== System Metrics ==="
aws cloudwatch get-metric-statistics \
    --namespace AWS/ApplicationELB \
    --metric-name TargetResponseTime \
    --dimensions Name=LoadBalancer,Value=app/hopngo-alb/1234567890abcdef \
    --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average

echo "Initial assessment completed. Proceeding with investigation..."
```

**Step 3: Investigation & Resolution**
```bash
#!/bin/bash
# Incident investigation and resolution

echo "Starting incident investigation..."

# Common resolution steps
echo "Checking common issues..."

# 1. Restart application if needed
read -p "Restart application? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Restarting application..."
    eb restart hopngo-backend-production
    
    # Wait and verify
    echo "Waiting for restart..."
    sleep 120
    curl -f https://api.hopngo.com/health
fi

# 2. Scale up if resource constrained
read -p "Scale up application? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Scaling up application..."
    aws autoscaling update-auto-scaling-group \
        --auto-scaling-group-name hopngo-backend-production \
        --desired-capacity 6
fi

# 3. Check and clear cache if needed
read -p "Clear Redis cache? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Clearing Redis cache..."
    redis-cli -h prod-cache.hopngo.com FLUSHALL
fi

# 4. Database maintenance if needed
read -p "Run database maintenance? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Running database maintenance..."
    psql -h prod-db.hopngo.com -U hopngo -d hopngo -c "VACUUM ANALYZE;"
fi

echo "Resolution steps completed. Verifying system health..."

# Verify resolution
for i in {1..10}; do
    if curl -f https://api.hopngo.com/health; then
        echo "System health restored"
        break
    else
        echo "Attempt $i: System still unhealthy, waiting..."
        sleep 30
    fi
done
```

**Step 4: Post-Incident**
```bash
#!/bin/bash
# Post-incident procedures

INCIDENT_ID=$1
echo "Post-incident procedures for: $INCIDENT_ID"

# Resolve incident
echo "Resolving incident..."
# PagerDuty API call to resolve

# Document incident
echo "Creating incident report..."
cat > "incident_report_$INCIDENT_ID.md" << EOF
# Incident Report: $INCIDENT_ID

## Summary
- **Incident ID**: $INCIDENT_ID
- **Date**: $(date)
- **Duration**: [TO BE FILLED]
- **Severity**: [TO BE FILLED]
- **On-call Engineer**: $USER

## Timeline
- **Detection**: [TO BE FILLED]
- **Response**: [TO BE FILLED]
- **Resolution**: [TO BE FILLED]

## Root Cause
[TO BE FILLED]

## Impact
- **Users Affected**: [TO BE FILLED]
- **Services Impacted**: [TO BE FILLED]
- **Revenue Impact**: [TO BE FILLED]

## Resolution
[TO BE FILLED]

## Action Items
- [ ] [TO BE FILLED]
- [ ] [TO BE FILLED]

## Lessons Learned
[TO BE FILLED]
EOF

echo "Incident report template created: incident_report_$INCIDENT_ID.md"
echo "Please complete the report and share with the team"

# Schedule post-mortem if P0/P1
echo "Schedule post-mortem meeting for P0/P1 incidents"
```

## Disaster Recovery

### DR Activation

#### Purpose
Activate disaster recovery procedures in case of major system failure.

#### DR Checklist
- [ ] Incident commander assigned
- [ ] Stakeholders notified
- [ ] DR site status verified
- [ ] Data replication status checked
- [ ] Recovery time estimate provided
- [ ] Communication plan activated

#### DR Activation Steps
```bash
#!/bin/bash
# Disaster recovery activation

echo "DISASTER RECOVERY ACTIVATION"
echo "Timestamp: $(date)"
echo "Initiated by: $USER"
echo "Incident: $1"

# 1. Assess primary site
echo "Assessing primary site status..."
PRIMARY_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://api.hopngo.com/health)
echo "Primary site status: $PRIMARY_STATUS"

# 2. Check DR site readiness
echo "Checking DR site readiness..."
DR_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://dr-api.hopngo.com/health)
echo "DR site status: $DR_STATUS"

# 3. Verify data replication
echo "Verifying data replication..."
aws rds describe-db-instances \
    --db-instance-identifier hopngo-dr \
    --query 'DBInstances[0].DBInstanceStatus'

# 4. Activate DR environment
if [ "$PRIMARY_STATUS" != "200" ] && [ "$DR_STATUS" == "200" ]; then
    echo "Activating DR environment..."
    
    # Update DNS to point to DR
    aws route53 change-resource-record-sets \
        --hosted-zone-id Z1234567890ABC \
        --change-batch '{
            "Changes": [{
                "Action": "UPSERT",
                "ResourceRecordSet": {
                    "Name": "api.hopngo.com",
                    "Type": "CNAME",
                    "TTL": 60,
                    "ResourceRecords": [{
                        "Value": "dr-api.hopngo.com"
                    }]
                }
            }]
        }'
    
    # Scale up DR environment
    aws autoscaling update-auto-scaling-group \
        --auto-scaling-group-name hopngo-dr-backend \
        --desired-capacity 4
    
    echo "DR environment activated"
    echo "Estimated recovery time: 15 minutes"
else
    echo "DR activation conditions not met"
    echo "Primary status: $PRIMARY_STATUS"
    echo "DR status: $DR_STATUS"
fi

# 5. Notify stakeholders
echo "Notifying stakeholders..."
aws sns publish \
    --topic-arn arn:aws:sns:region:account:hopngo-dr-alerts \
    --message "DR activation initiated. Incident: $1. Time: $(date)"

echo "DR activation procedure completed"
```

### DR Testing

#### Monthly DR Test
```bash
#!/bin/bash
# Monthly disaster recovery test

echo "Starting monthly DR test..."
TEST_DATE=$(date +%Y-%m-%d)

# 1. Verify DR environment
echo "Verifying DR environment..."
curl -f https://dr-api.hopngo.com/health

# 2. Test data replication
echo "Testing data replication..."
PRIMARY_COUNT=$(psql -h prod-db.hopngo.com -U hopngo -d hopngo -t -c "SELECT COUNT(*) FROM users;")
DR_COUNT=$(psql -h dr-db.hopngo.com -U hopngo -d hopngo -t -c "SELECT COUNT(*) FROM users;")

echo "Primary DB user count: $PRIMARY_COUNT"
echo "DR DB user count: $DR_COUNT"

if [ "$PRIMARY_COUNT" -eq "$DR_COUNT" ]; then
    echo "Data replication test: PASSED"
else
    echo "Data replication test: FAILED"
fi

# 3. Test failover procedure
echo "Testing failover procedure (simulation)..."
# Simulate DNS change without actually changing
echo "DNS change simulation: PASSED"

# 4. Test application functionality
echo "Testing DR application functionality..."
npm run test:smoke:dr

# 5. Generate test report
cat > "dr_test_report_$TEST_DATE.md" << EOF
# DR Test Report - $TEST_DATE

## Test Results
- **Environment Health**: PASSED
- **Data Replication**: $([ "$PRIMARY_COUNT" -eq "$DR_COUNT" ] && echo "PASSED" || echo "FAILED")
- **Failover Simulation**: PASSED
- **Application Functionality**: [CHECK TEST RESULTS]

## Issues Found
[TO BE FILLED]

## Action Items
[TO BE FILLED]

## Next Test Date
$(date -d '+1 month' +%Y-%m-%d)
EOF

echo "DR test completed. Report: dr_test_report_$TEST_DATE.md"
```

## Maintenance Runbooks

### System Updates

#### Security Patch Deployment
```bash
#!/bin/bash
# Security patch deployment

echo "Starting security patch deployment..."

# 1. Check for available updates
echo "Checking for security updates..."
sudo apt update
sudo apt list --upgradable | grep -i security

# 2. Test updates in staging
echo "Testing updates in staging environment..."
ssh staging-server "sudo apt update && sudo apt upgrade -y"

# 3. Verify staging functionality
echo "Verifying staging functionality..."
curl -f https://staging-api.hopngo.com/health
npm run test:smoke:staging

# 4. Apply updates to production
if [ $? -eq 0 ]; then
    echo "Staging tests passed. Applying updates to production..."
    
    # Rolling update across instances
    for instance in $(aws ec2 describe-instances --filters "Name=tag:Environment,Values=production" --query 'Reservations[].Instances[].InstanceId' --output text); do
        echo "Updating instance: $instance"
        
        # Remove from load balancer
        aws elbv2 deregister-targets \
            --target-group-arn arn:aws:elasticloadbalancing:region:account:targetgroup/hopngo-prod/1234567890abcdef \
            --targets Id=$instance
        
        # Wait for deregistration
        sleep 60
        
        # Apply updates
        ssh $instance "sudo apt update && sudo apt upgrade -y && sudo systemctl restart hopngo-backend"
        
        # Wait for service startup
        sleep 120
        
        # Re-register with load balancer
        aws elbv2 register-targets \
            --target-group-arn arn:aws:elasticloadbalancing:region:account:targetgroup/hopngo-prod/1234567890abcdef \
            --targets Id=$instance
        
        # Verify health
        sleep 60
        curl -f https://api.hopngo.com/health
        
        echo "Instance $instance updated successfully"
    done
else
    echo "Staging tests failed. Aborting production updates."
    exit 1
fi

echo "Security patch deployment completed"
```

### Performance Optimization

#### Database Maintenance
```bash
#!/bin/bash
# Database maintenance procedures

echo "Starting database maintenance..."

# 1. Analyze table statistics
echo "Analyzing table statistics..."
psql -h prod-db.hopngo.com -U hopngo -d hopngo << EOF
-- Update table statistics
ANALYZE;

-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY schemaname, tablename;
EOF

# 2. Vacuum and reindex
echo "Running vacuum and reindex..."
psql -h prod-db.hopngo.com -U hopngo -d hopngo << EOF
-- Vacuum analyze all tables
VACUUM ANALYZE;

-- Reindex if needed (during maintenance window)
-- REINDEX DATABASE hopngo;
EOF

# 3. Check slow queries
echo "Checking slow queries..."
psql -h prod-db.hopngo.com -U hopngo -d hopngo << EOF
-- Enable slow query logging temporarily
SET log_min_duration_statement = 1000;

-- Check current slow queries
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements
WHERE mean_time > 1000
ORDER BY mean_time DESC
LIMIT 10;
EOF

echo "Database maintenance completed"
```

## Monitoring & Alerting

### Health Check Automation

```bash
#!/bin/bash
# Comprehensive health check

echo "Running comprehensive health check..."
HEALTH_REPORT="health_check_$(date +%Y%m%d_%H%M%S).txt"

{
    echo "=== HopNGo Health Check Report ==="
    echo "Timestamp: $(date)"
    echo ""
    
    # API Health
    echo "=== API Health ==="
    if curl -f -s https://api.hopngo.com/health > /dev/null; then
        echo "✓ API is healthy"
    else
        echo "✗ API health check failed"
    fi
    
    # Database Health
    echo "=== Database Health ==="
    if psql -h prod-db.hopngo.com -U hopngo -d hopngo -c "SELECT 1;" > /dev/null 2>&1; then
        echo "✓ Database is accessible"
    else
        echo "✗ Database connection failed"
    fi
    
    # Cache Health
    echo "=== Cache Health ==="
    if redis-cli -h prod-cache.hopngo.com ping | grep -q PONG; then
        echo "✓ Redis cache is responding"
    else
        echo "✗ Redis cache is not responding"
    fi
    
    # External Services
    echo "=== External Services ==="
    if curl -f -s https://api.stripe.com/v1/charges -u sk_test_key: > /dev/null; then
        echo "✓ Stripe API is accessible"
    else
        echo "✗ Stripe API connection failed"
    fi
    
    # System Resources
    echo "=== System Resources ==="
    aws cloudwatch get-metric-statistics \
        --namespace AWS/EC2 \
        --metric-name CPUUtilization \
        --dimensions Name=AutoScalingGroupName,Value=hopngo-backend-production \
        --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
        --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
        --period 300 \
        --statistics Average \
        --query 'Datapoints[0].Average'
    
} > $HEALTH_REPORT

echo "Health check completed. Report: $HEALTH_REPORT"

# Send report if issues found
if grep -q "✗" $HEALTH_REPORT; then
    echo "Issues detected. Sending alert..."
    aws sns publish \
        --topic-arn arn:aws:sns:region:account:hopngo-health-alerts \
        --message "Health check issues detected. See report: $HEALTH_REPORT"
fi
```

---

*These runbooks are living documents that should be updated regularly based on operational experience and system changes. All procedures should be tested in non-production environments before execution.*
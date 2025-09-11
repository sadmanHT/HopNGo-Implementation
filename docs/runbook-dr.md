# HopNGo Disaster Recovery Runbook

## Overview

This document provides step-by-step procedures for disaster recovery of the HopNGo platform. It covers complete system restoration from backups, including databases, message queues, search indices, and application services.

## Recovery Objectives

### RPO (Recovery Point Objective)
- **Critical Data**: 1 hour (PostgreSQL, MongoDB)
- **Cache Data**: 4 hours (Redis)
- **Message Queues**: 1 hour (RabbitMQ)
- **Search Indices**: 4 hours (OpenSearch)
- **Media Files**: 24 hours (Cloudinary/Firebase)

### RTO (Recovery Time Objective)
- **Database Services**: 30 minutes
- **Application Services**: 15 minutes
- **Full System**: 1 hour
- **Search Functionality**: 2 hours

## Prerequisites

### Required Access
- [ ] Docker and Docker Compose installed
- [ ] Access to backup storage location
- [ ] Environment variables file (`.env`)
- [ ] SSL certificates (if using HTTPS)
- [ ] Cloud service credentials (Cloudinary, Firebase)

### Required Information
- Latest backup timestamp
- Database passwords and credentials
- External service API keys
- DNS configuration details

## Disaster Recovery Procedures

### Phase 1: Assessment and Preparation (5-10 minutes)

#### 1.1 Assess the Situation
```bash
# Check what services are affected
docker ps -a
docker-compose ps

# Check system resources
df -h
free -m
```

#### 1.2 Identify Latest Backup
```bash
# List available backups
ls -la /path/to/backups/
# or if using backup container
docker run --rm -v backup_data:/backups alpine ls -la /backups
```

#### 1.3 Prepare Environment
```bash
# Navigate to project directory
cd /path/to/hopngo

# Ensure environment file exists
cp .env.example .env
# Edit .env with correct values
```

### Phase 2: Infrastructure Recovery (10-15 minutes)

#### 2.1 Stop All Services
```bash
# Stop all running containers
docker-compose down

# Remove all containers (if needed)
docker-compose down -v --remove-orphans

# Clean up networks (if needed)
docker network prune -f
```

#### 2.2 Start Core Infrastructure
```bash
# Start database services first
docker-compose up -d postgres mongodb redis rabbitmq

# Wait for services to be ready
docker-compose logs -f postgres
# Wait for "database system is ready to accept connections"

# Check service health
docker-compose ps
```

#### 2.3 Verify Infrastructure Health
```bash
# Test PostgreSQL connection
docker-compose exec postgres pg_isready -U hopngo

# Test MongoDB connection
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"

# Test Redis connection
docker-compose exec redis redis-cli ping

# Test RabbitMQ connection
curl -u hopngo:password http://localhost:15672/api/overview
```

### Phase 3: Data Recovery (15-30 minutes)

#### 3.1 Restore PostgreSQL (Priority 1)
```bash
# Using backup container
docker-compose run --rm backup /scripts/restore.sh -s postgres YYYYMMDD-HHMM

# Or manually
docker-compose exec postgres psql -U hopngo -d hopngo -c "SELECT version();"
```

#### 3.2 Restore MongoDB (Priority 1)
```bash
# Using backup container
docker-compose run --rm backup /scripts/restore.sh -s mongodb YYYYMMDD-HHMM

# Verify restoration
docker-compose exec mongodb mongosh --eval "db.stats()"
```

#### 3.3 Restore RabbitMQ (Priority 2)
```bash
# Using backup container
docker-compose run --rm backup /scripts/restore.sh -s rabbitmq YYYYMMDD-HHMM

# Verify queues and exchanges
curl -u hopngo:password http://localhost:15672/api/queues
```

#### 3.4 Restore Redis (Priority 3)
```bash
# Using backup container
docker-compose run --rm backup /scripts/restore.sh -s redis YYYYMMDD-HHMM

# Note: Redis restore may require manual intervention
# Follow the instructions provided by the restore script
```

#### 3.5 Restore OpenSearch (Priority 4)
```bash
# Start OpenSearch service
docker-compose up -d opensearch

# Wait for cluster to be ready
curl -X GET "localhost:9200/_cluster/health?wait_for_status=yellow&timeout=50s"

# Restore indices (if snapshots available)
docker-compose run --rm backup /scripts/restore.sh -s opensearch YYYYMMDD-HHMM
```

### Phase 4: Application Recovery (10-15 minutes)

#### 4.1 Start Application Services
```bash
# Start services in dependency order
docker-compose up -d config-service
sleep 30

docker-compose up -d auth-service
sleep 15

docker-compose up -d booking-service market-service social-service
sleep 15

docker-compose up -d search-service analytics-service
sleep 15

docker-compose up -d frontend
```

#### 4.2 Verify Application Health
```bash
# Check all services are running
docker-compose ps

# Check service logs for errors
docker-compose logs --tail=50 config-service
docker-compose logs --tail=50 auth-service
docker-compose logs --tail=50 booking-service

# Test API endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

#### 4.3 Start Supporting Services
```bash
# Start monitoring and observability (optional)
docker-compose --profile observability up -d

# Start search services (if needed)
docker-compose --profile search up -d
```

### Phase 5: Verification and Testing (10-15 minutes)

#### 5.1 Database Verification
```bash
# Check PostgreSQL data integrity
docker-compose exec postgres psql -U hopngo -d hopngo -c "
  SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes
  FROM pg_stat_user_tables 
  ORDER BY schemaname, tablename;
"

# Check MongoDB collections
docker-compose exec mongodb mongosh --eval "
  db.adminCommand('listCollections').cursor.firstBatch.forEach(
    function(collection) {
      print(collection.name + ': ' + db[collection.name].countDocuments());
    }
  );
"
```

#### 5.2 Application Functionality Testing
```bash
# Test user authentication
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Test booking service
curl http://localhost:8082/api/v1/bookings/health

# Test search functionality
curl http://localhost:8090/api/v1/search/health
```

#### 5.3 Frontend Verification
```bash
# Check frontend is accessible
curl -I http://localhost:3000

# Check API connectivity from frontend
curl http://localhost:3000/api/health
```

### Phase 6: Post-Recovery Actions (5-10 minutes)

#### 6.1 Update DNS and Load Balancer
- [ ] Update DNS records to point to new infrastructure
- [ ] Configure load balancer health checks
- [ ] Update SSL certificates if needed

#### 6.2 Restart Backup Schedule
```bash
# Start backup services
docker-compose --profile backup up -d

# Verify backup schedule is active
docker-compose logs backup-scheduler
```

#### 6.3 Notify Stakeholders
- [ ] Notify operations team of recovery completion
- [ ] Update status page
- [ ] Communicate with users if necessary

## Troubleshooting

### Common Issues

#### Database Connection Issues
```bash
# Check database logs
docker-compose logs postgres
docker-compose logs mongodb

# Verify network connectivity
docker-compose exec backend ping postgres
docker-compose exec backend ping mongodb

# Check environment variables
docker-compose exec backend env | grep -E "(POSTGRES|MONGO)"
```

#### Service Startup Issues
```bash
# Check service dependencies
docker-compose config --services

# Restart services in order
docker-compose restart config-service
docker-compose restart auth-service

# Check resource usage
docker stats
```

#### Backup Restoration Issues
```bash
# Verify backup integrity
docker-compose run --rm backup /scripts/restore.sh -v YYYYMMDD-HHMM

# Check backup contents
docker run --rm -v backup_data:/backups alpine ls -la /backups/YYYYMMDD-HHMM/

# Manual restoration steps
docker-compose exec postgres pg_restore --help
docker-compose exec mongodb mongorestore --help
```

### Rollback Procedures

If recovery fails, follow these rollback steps:

1. **Stop all services**
   ```bash
   docker-compose down
   ```

2. **Restore from previous backup**
   ```bash
   # Use previous backup timestamp
   docker-compose run --rm backup /scripts/restore.sh PREVIOUS_TIMESTAMP
   ```

3. **Restart with minimal services**
   ```bash
   docker-compose up -d postgres mongodb redis
   ```

## Recovery Validation Checklist

### Infrastructure
- [ ] All database services are running and healthy
- [ ] Network connectivity between services is working
- [ ] Persistent volumes are mounted correctly
- [ ] Environment variables are set properly

### Data Integrity
- [ ] PostgreSQL data is complete and consistent
- [ ] MongoDB collections are restored
- [ ] Redis cache is functional (data loss acceptable)
- [ ] RabbitMQ queues and exchanges are configured
- [ ] OpenSearch indices are available (if applicable)

### Application Services
- [ ] All microservices are running
- [ ] Health checks are passing
- [ ] Inter-service communication is working
- [ ] API endpoints are responding

### User-Facing Services
- [ ] Frontend application is accessible
- [ ] User authentication is working
- [ ] Core business functions are operational
- [ ] Search functionality is available

### Monitoring and Observability
- [ ] Logging is functional
- [ ] Metrics collection is active
- [ ] Alerting is configured
- [ ] Backup schedule is resumed

## Contact Information

### Emergency Contacts
- **Operations Team**: ops@hopngo.com
- **Development Team**: dev@hopngo.com
- **Infrastructure Team**: infra@hopngo.com

### External Services
- **Cloud Provider Support**: [Provider Support Portal]
- **Database Support**: [Database Vendor Support]
- **Monitoring Service**: [Monitoring Vendor Support]

## Recovery Time Tracking

| Phase | Estimated Time | Actual Time | Notes |
|-------|---------------|-------------|-------|
| Assessment | 5-10 min | | |
| Infrastructure | 10-15 min | | |
| Data Recovery | 15-30 min | | |
| Application Recovery | 10-15 min | | |
| Verification | 10-15 min | | |
| Post-Recovery | 5-10 min | | |
| **Total** | **55-95 min** | | |

## Post-Incident Review

After successful recovery, conduct a post-incident review:

1. **Document the incident**
   - Root cause analysis
   - Timeline of events
   - Recovery actions taken

2. **Identify improvements**
   - Backup strategy enhancements
   - Monitoring improvements
   - Process optimizations

3. **Update procedures**
   - Revise this runbook based on lessons learned
   - Update backup and recovery scripts
   - Improve automation where possible

4. **Training and communication**
   - Share learnings with the team
   - Update training materials
   - Review and practice procedures regularly

---

**Document Version**: 1.0  
**Last Updated**: $(date)  
**Next Review**: $(date -d "+3 months")  
**Owner**: Infrastructure Team
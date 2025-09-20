# HopNGo Quickstart Guide 🚀

> Get HopNGo running locally in under 5 minutes with Docker Compose

## Prerequisites

- **Docker Desktop** (4.0+) with Docker Compose V2
- **Git** for cloning the repository
- **8GB RAM** minimum (16GB recommended)
- **Ports Available**: 3000, 8080, 5432, 6379, 9200, 6333

## One-Command Setup

```bash
# Clone and start everything
git clone https://github.com/sadmanHT/HopNGo-Implementation.git
cd HopNGo-Implementation
docker-compose up -d
```

**That's it!** 🎉 HopNGo will be running at:
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Demo Mode**: http://localhost:3000?demo=1&demo-user=traveler

## What Gets Started

### Core Services
- ✅ **Frontend** (React + Next.js) - Port 3000
- ✅ **API Gateway** (Spring Cloud) - Port 8080
- ✅ **Auth Service** - JWT authentication
- ✅ **Booking Service** - Reservations & payments
- ✅ **Search Service** - AI-powered search
- ✅ **Trip Planning** - AI itinerary generation
- ✅ **Chat Service** - Real-time messaging

### Infrastructure
- ✅ **PostgreSQL** - Primary database
- ✅ **Redis** - Caching & sessions
- ✅ **Elasticsearch** - Search indexing
- ✅ **Qdrant** - Vector embeddings
- ✅ **MinIO** - Object storage

## Demo Mode 🎭

### Quick Demo Access
```bash
# Traveler Demo
open http://localhost:3000?demo=1&demo-user=traveler

# Provider Demo  
open http://localhost:3000?demo=1&demo-user=provider
```

### Demo Features
- 🏨 **Pre-seeded destinations** (Srimangal tea gardens, Cox's Bazar, Sundarbans)
- 👤 **Demo user accounts** with realistic data
- 💳 **Sandbox payments** (no real transactions)
- 🤖 **AI responses** with mock data
- 📱 **Sample conversations** and bookings

## Health Checks

```bash
# Check all services are running
docker-compose ps

# View logs
docker-compose logs -f frontend
docker-compose logs -f api-gateway

# Health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:3000/api/health
```

## Development Setup

### Frontend Development
```bash
cd frontend
npm install
npm run dev
# Frontend with hot reload at http://localhost:3000
```

### Backend Development
```bash
# Start infrastructure only
docker-compose up -d postgres redis elasticsearch qdrant minio

# Run services locally
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Environment Configuration

### Production Environment
```bash
# Copy environment template
cp .env.example .env

# Edit configuration
vim .env

# Start with production config
docker-compose -f docker-compose.prod.yml up -d
```

### Key Environment Variables
```bash
# Database
POSTGRES_DB=hopngo
POSTGRES_USER=hopngo_user
POSTGRES_PASSWORD=secure_password

# JWT Security
JWT_SECRET=your-256-bit-secret
JWT_EXPIRATION=86400

# External APIs
STRIPE_SECRET_KEY=sk_test_...
OPENAI_API_KEY=sk-...
GOOGLE_MAPS_API_KEY=AIza...

# Feature Flags
ENABLE_AI_FEATURES=true
ENABLE_PAYMENTS=true
ENABLE_CHAT=true
```

## Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check what's using ports
netstat -tulpn | grep :3000
netstat -tulpn | grep :8080

# Kill conflicting processes
sudo kill -9 $(lsof -t -i:3000)
```

#### Database Connection Issues
```bash
# Reset database
docker-compose down -v
docker-compose up -d postgres

# Check database logs
docker-compose logs postgres
```

#### Memory Issues
```bash
# Increase Docker memory limit to 8GB+
# Docker Desktop → Settings → Resources → Memory

# Check container resource usage
docker stats
```

#### Service Dependencies
```bash
# Start services in order
docker-compose up -d postgres redis
sleep 10
docker-compose up -d elasticsearch qdrant
sleep 20
docker-compose up -d api-gateway
sleep 10
docker-compose up -d frontend
```

### Logs & Debugging
```bash
# View all logs
docker-compose logs -f

# Service-specific logs
docker-compose logs -f auth-service
docker-compose logs -f booking-service

# Follow logs with timestamps
docker-compose logs -f -t frontend
```

## Testing

### Run Test Suite
```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test

# E2E tests
npm run test:e2e

# Integration tests
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

### API Testing
```bash
# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@hopngo.com","password":"demo123"}'

# Test search
curl "http://localhost:8080/api/search/destinations?q=tea+gardens"

# Test booking
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Production Deployment

### Kubernetes (Recommended)
```bash
# Deploy to Kubernetes
helm install hopngo ./k8s/helm/hopngo

# Check deployment
kubectl get pods -n hopngo
kubectl get services -n hopngo
```

### Docker Swarm
```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.prod.yml hopngo
```

### Cloud Providers
- **AWS**: ECS/EKS with RDS and ElastiCache
- **GCP**: GKE with Cloud SQL and Memorystore
- **Azure**: AKS with Azure Database and Redis Cache

## Performance Optimization

### Frontend Optimization
- ✅ **Image Optimization** - Next.js Image component with WebP
- ✅ **Code Splitting** - Route-based lazy loading
- ✅ **CDN Integration** - Static asset delivery
- ✅ **Service Worker** - Offline functionality

### Backend Optimization
- ✅ **Connection Pooling** - HikariCP with optimized settings
- ✅ **Caching Strategy** - Redis with TTL-based invalidation
- ✅ **Database Indexing** - Optimized queries with proper indexes
- ✅ **Async Processing** - Background jobs with Spring Async

## Security Checklist

- ✅ **HTTPS Everywhere** - TLS 1.3 with HSTS headers
- ✅ **JWT Security** - RS256 signing with key rotation
- ✅ **Input Validation** - Comprehensive sanitization
- ✅ **Rate Limiting** - API throttling and DDoS protection
- ✅ **CORS Configuration** - Strict origin policies
- ✅ **Security Headers** - CSP, X-Frame-Options, etc.
- ✅ **Dependency Scanning** - Automated vulnerability checks
- ✅ **Secrets Management** - Encrypted environment variables

## Monitoring & Observability

### Health Monitoring
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Redis health
curl http://localhost:8080/actuator/health/redis
```

### Metrics & Logging
- **Prometheus** - Metrics collection
- **Grafana** - Visualization dashboards
- **ELK Stack** - Centralized logging
- **Jaeger** - Distributed tracing

## Support & Resources

### Documentation
- 📖 **[API Documentation](./docs/openapi/README.md)** - OpenAPI specs
- 🏗️ **[Architecture Guide](./docs/ARCHITECTURE.md)** - System design
- 🔐 **[Security Guide](./docs/SECURITY_CHECKLIST.md)** - Security best practices
- 🚀 **[Deployment Guide](./docs/runbook-deploy.md)** - Production deployment

### Getting Help
- 🐛 **Issues**: [GitHub Issues](https://github.com/sadmanHT/HopNGo-Implementation/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/sadmanHT/HopNGo-Implementation/discussions)
- 📧 **Email**: support@hopngo.com

### Contributing
- 🤝 **[Contributing Guide](./CONTRIBUTING.md)** - How to contribute
- 📋 **[Code of Conduct](./CODE_OF_CONDUCT.md)** - Community guidelines
- 🎯 **[Roadmap](./ROADMAP.md)** - Future plans

---

**Next Steps**: Once everything is running, try the [Demo Mode](http://localhost:3000?demo=1&demo-user=traveler) to explore HopNGo's features! 🎉
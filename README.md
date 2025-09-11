# HopNGo 🚀

> A comprehensive travel and transportation platform that connects travelers with various transportation options and services.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Node.js](https://img.shields.io/badge/Node.js-20+-green.svg)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5+-blue.svg)](https://www.typescriptlang.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue.svg)](https://kubernetes.io/)
[![Helm](https://img.shields.io/badge/Helm-Charts-blue.svg)](https://helm.sh/)

## 🔄 CI/CD Status

[![CI](https://github.com/HopNGo/HopNGo/actions/workflows/ci.yml/badge.svg)](https://github.com/HopNGo/HopNGo/actions/workflows/ci.yml)
[![Docker Build](https://github.com/HopNGo/HopNGo/actions/workflows/docker.yml/badge.svg)](https://github.com/HopNGo/HopNGo/actions/workflows/docker.yml)
[![Integration Tests](https://github.com/HopNGo/HopNGo/actions/workflows/it.yml/badge.svg)](https://github.com/HopNGo/HopNGo/actions/workflows/it.yml)
[![Deploy](https://github.com/HopNGo/HopNGo/actions/workflows/deploy.yml/badge.svg)](https://github.com/HopNGo/HopNGo/actions/workflows/deploy.yml)

## 🚀 Progressive Delivery

HopNGo implements advanced progressive delivery strategies using **Argo Rollouts** for safe, controlled deployments:

- **Blue/Green Deployments** - Zero-downtime deployments with instant rollback capability
- **Canary Deployments** - Gradual traffic shifting (25% → 50% → 100%) with automated health checks
- **Feature Flags** - Dark launches and A/B testing for controlled feature rollouts
- **Automated Rollbacks** - Automatic rollback on SLO violations (error rate >5%, latency >2s)
- **Manual Approval Gates** - Production deployments require manual approval after staging validation
- **Smoke Testing** - Post-deployment validation of critical user flows

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                Frontend (React)                             │
│                            http://localhost:3000                           │
└─────────────────────────┬───────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────────────────┐
│                           API Gateway (Spring Boot)                         │
│                            http://localhost:8080                           │
└─┬───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┘
  │       │       │       │       │       │       │       │       │
┌─▼─┐   ┌─▼─┐   ┌─▼─┐   ┌─▼─┐   ┌─▼─┐   ┌─▼─┐   ┌─▼─┐   ┌─▼─┐   ┌─▼─┐
│Auth│   │Soc│   │Book│  │Mkt│   │Chat│  │Trip│  │ AI │  │Emg│   │Cfg│
│Svc │   │Svc│   │Svc │  │Svc│   │Svc │  │Svc │  │Svc │  │Svc│   │Svc│
│:81 │   │:82│   │:83 │  │:84│   │:85 │  │:87 │  │:88 │  │:86│   │:92│
└───┘   └───┘   └───┘   └───┘   └───┘   └───┘   └───┘   └───┘   └───┘
  │       │       │       │       │       │       │       │       │
┌─▼───────▼───────▼───────▼───────▼───────▼───────▼───────▼───────▼───────┐
│                              Data Layer                                    │
│  ┌──────────┐  ┌─────────┐  ┌─────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │PostgreSQL│  │  Redis  │  │ MongoDB │  │Elasticsearch │  │  RabbitMQ   │ │
│  │   :5432  │  │  :6379  │  │ :27017  │  │    :9200     │  │    :5672    │ │
│  └──────────┘  └─────────┘  └─────────┘  └──────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                            Additional Services                              │
│  ┌─────────────────┐              ┌─────────────────────────────────────────┐ │
│  │ Notification    │              │           Event Bus                     │ │
│  │ Service :8089   │              │        (RabbitMQ)                       │ │
│  └─────────────────┘              └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Services Overview

| Service | Port | Technology | Description | Status |
|---------|------|------------|-------------|--------|
| **Frontend** | 3000 | React + TypeScript + Vite | User interface and web application | ✅ Active |
| **API Gateway** | 8080 | Spring Boot + Spring Cloud Gateway | Request routing and load balancing | ✅ Implemented |
| **Auth Service** | 8081 | Spring Boot + Spring Security + JWT | Authentication and authorization | ✅ Implemented |
| **Social Service** | 8082 | Spring Boot + JPA | User profiles and social features | ✅ Implemented |
| **Booking Service** | 8083 | Spring Boot + JPA | Trip bookings and reservations | ✅ Implemented |
| **Market Service** | 8084 | Spring Boot + JPA | Marketplace and listings | ✅ Implemented |
| **Chat Service** | 8085 | Spring Boot + WebSocket | Real-time messaging | ✅ Implemented |
| **Trip Planning** | 8087 | Spring Boot + JPA | Itinerary and route planning | ✅ Implemented |
| **AI Service** | 8088 | Spring Boot + Python Integration | AI recommendations and insights | ✅ Implemented |
| **Emergency Service** | 8086 | Spring Boot + JPA | Emergency assistance and alerts | ✅ Implemented |
| **Notification Service** | 8089 | Spring Boot + RabbitMQ | Push notifications and alerts | ✅ Implemented |
| **Config Service** | 8092 | Spring Boot + JPA + Redis | Feature flags and A/B testing | ✅ Implemented |

## 🛠️ Tech Stack

### Frontend
- **React 18** with TypeScript and Vite
- **Tailwind CSS** for styling
- **Shadcn/ui** component library
- **Next.js** for production builds

### Backend
- **Spring Boot 3** with Java 17
- **Spring Cloud Gateway** for API routing
- **Spring Security** with JWT authentication
- **Spring Data JPA** for database operations
- **Maven** for dependency management

### Databases & Storage
- **PostgreSQL 15** - Primary relational database
- **MongoDB 7** - Document database for flexible data
- **Redis 7** - Caching and session storage
- **Elasticsearch** - Search and analytics

### Infrastructure & DevOps
- **Docker** - Containerization
- **Kubernetes** - Container orchestration
- **Argo Rollouts** - Progressive delivery with blue/green and canary deployments
- **Helm** - Kubernetes package management
- **Kustomize** - Kubernetes configuration management
- **NGINX Ingress** - Load balancing and SSL termination
- **Cert-Manager** - Automatic SSL certificate management
- **Flyway** - Database migration management with baseline checks

### Observability
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization
- **Tempo** - Distributed tracing
- **OpenTelemetry** - Telemetry data collection
- **Jaeger** - Tracing UI

### Message Queue
- **RabbitMQ** - Asynchronous messaging

## 🚀 Quick Start

### Prerequisites

- **Node.js 20+** and **pnpm 8+**
- **Java 17+** and **Maven 3.9+**
- **Docker** and **Docker Compose**
- **Git**

For Kubernetes deployment:
- **kubectl**
- **Helm 3+**
- **Kubernetes cluster** (local or cloud)

### Local Development

```bash
# Clone the repository
git clone https://github.com/your-org/HopNGo.git
cd HopNGo

# Install frontend dependencies
cd frontend
pnpm install
cd ..

# Start infrastructure services
./scripts/dev.sh

# Start all microservices (in separate terminals)
# Auth Service
cd auth-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Booking Service
cd booking-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Market Service
cd market-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Trip Planning Service
cd trip-planning-service && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Start frontend
cd frontend && pnpm dev
```

### Using Development Scripts (Windows)

```powershell
# Setup development environment
.\scripts\setup-dev.ps1

# Start infrastructure
.\scripts\dev.sh
```

## 🐳 Docker Deployment

### Local Docker Compose

```bash
# Start all services with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## ☸️ Kubernetes Deployment

### Progressive Delivery with Argo Rollouts

HopNGo uses **Argo Rollouts** for advanced deployment strategies with automated health checks and rollback capabilities.

#### Deployment Strategies

**Blue/Green Deployment** (Gateway Service):
- Instant traffic switching between versions
- Zero-downtime deployments
- Immediate rollback capability
- Health checks before traffic promotion

**Canary Deployment** (Core Services):
- Gradual traffic shifting: 25% → 50% → 100%
- Automated analysis with SLO monitoring
- Error rate threshold: <5%
- Latency threshold: <2000ms
- Automatic rollback on violations

#### Infrastructure Setup

HopNGo includes comprehensive Kubernetes manifests with Helm charts for infrastructure dependencies.

#### Quick Deployment

```bash
# Deploy to development environment
.\infra\scripts\install.ps1 install dev

# Deploy to staging environment
.\infra\scripts\install.ps1 install staging

# Deploy to production environment
.\infra\scripts\install.ps1 install production
```

#### Progressive Deployment Workflow

```bash
# Trigger staging deployment (automatic on main branch)
git push origin main

# Monitor rollout progress
kubectl argo rollouts get rollout gateway -n hopngo-staging --watch

# Promote to production (requires manual approval)
# 1. Staging deployment completes successfully
# 2. Smoke tests pass
# 3. Manual approval in GitHub Actions
# 4. Production canary deployment begins
# 5. Automated analysis and promotion
```

#### Manual Deployment

```bash
# Create secrets
.\infra\scripts\create-secrets.ps1 dev

# Install infrastructure dependencies
helm upgrade --install hopngo-infra infra/helm \
  --namespace hopngo-dev \
  --values infra/helm/values.yaml \
  --create-namespace

# Deploy application
kubectl apply -k infra/k8s/overlays/dev
```

### Environment-Specific Configurations

#### Development Environment
- **Namespace**: `hopngo-dev`
- **Replicas**: 1 per service
- **Resources**: Low resource limits
- **Host**: `hopngo.local`
- **SSL**: Self-signed certificates

#### Staging Environment
- **Namespace**: `hopngo-staging`
- **Replicas**: 2 per service
- **Resources**: Moderate resource limits
- **Host**: `staging.hopngo.com`
- **SSL**: Let's Encrypt certificates

#### Production Environment
- **Namespace**: `hopngo-prod`
- **Replicas**: 3-4 per service
- **Resources**: Production resource limits
- **Host**: `hopngo.com`, `www.hopngo.com`
- **SSL**: Let's Encrypt certificates
- **Security**: Enhanced security policies

### Kubernetes Features

- **Argo Rollouts** - Progressive delivery with blue/green and canary strategies
- **Automated Analysis** - SLO-based health checks with automatic rollback
- **Database Migrations** - Pre-deployment Flyway jobs with baseline validation
- **Horizontal Pod Autoscaling (HPA)** - Automatic scaling based on CPU/memory
- **Ingress with SSL termination** - NGINX Ingress Controller with Cert-Manager
- **ConfigMaps and Secrets** - Environment-specific configuration
- **Health checks** - Liveness and readiness probes
- **Resource limits** - CPU and memory constraints
- **Security policies** - Pod security standards
- **Network policies** - Service-to-service communication control
- **Smoke Testing** - Post-deployment validation of critical flows

## 📊 Observability & Monitoring

### Metrics and Monitoring

- **Prometheus**: http://localhost:9090 - Metrics collection
- **Grafana**: http://localhost:3001 (admin/admin) - Dashboards and visualization
- **Alertmanager**: Integrated with Prometheus for alerting

### Distributed Tracing

- **Jaeger**: http://localhost:16686 - Trace visualization
- **Tempo**: Backend for trace storage
- **OpenTelemetry**: Automatic instrumentation

### Logging

- **Structured JSON logging** with correlation IDs
- **Centralized log aggregation** (ELK stack ready)
- **Log correlation** across microservices

### Health Monitoring

```bash
# Check service health
curl http://localhost:8081/actuator/health

# View metrics
curl http://localhost:8081/actuator/metrics

# Kubernetes health checks
kubectl get pods -n hopngo-dev
kubectl describe pod <pod-name> -n hopngo-dev
```

## 🗂️ Project Structure

```
HopNGo/
├── README.md
├── docker-compose.yml
├── pom.xml                          # Parent POM
├── package.json                     # Workspace configuration
├── pnpm-workspace.yaml             # PNPM workspace
│
├── frontend/                        # React frontend application
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── gateway/                         # API Gateway service
├── auth-service/                    # Authentication service
├── social-service/                  # Social features service
├── booking-service/                 # Booking management service
├── market-service/                  # Marketplace service
├── chat-service/                    # Real-time chat service
├── trip-planning-service/           # Trip planning service
├── ai-service/                      # AI recommendations service
├── emergency-service/               # Emergency assistance service
├── notification-service/            # Push notifications service
│
├── infra/                          # Infrastructure as Code
│   ├── compose/                    # Docker Compose for local dev
│   │   ├── docker-compose.yml
│   │   ├── grafana/
│   │   ├── init-scripts/
│   │   └── prometheus.yml
│   │
│   ├── helm/                       # Helm charts
│   │   ├── Chart.yaml
│   │   └── values.yaml
│   │
│   ├── k8s/                        # Kubernetes manifests
│   │   ├── base/                   # Base Kustomize resources
│   │   │   ├── kustomization.yaml
│   │   │   ├── secrets.yaml
│   │   │   ├── ingress.yaml
│   │   │   └── */                  # Service-specific manifests
│   │   │
│   │   └── overlays/               # Environment-specific overlays
│   │       ├── dev/
│   │       ├── staging/
│   │       └── production/
│   │
│   └── scripts/                    # Deployment scripts
│       ├── install.ps1             # Windows deployment script
│       ├── install.sh              # Linux deployment script
│       ├── create-secrets.ps1      # Windows secrets script
│       └── create-secrets.sh       # Linux secrets script
│
├── scripts/                        # Development scripts
│   ├── dev.sh                      # Start local development
│   ├── down.sh                     # Stop local services
│   ├── setup-dev.ps1               # Windows dev setup
│   └── test-*.sh                   # Testing scripts
│
├── docs/                           # Documentation
│   ├── ARCHITECTURE.md
│   ├── github-secrets.md
│   └── runbook-deploy.md           # Deployment runbook
│
├── tests/                          # Testing framework
│   └── smoke/                      # Smoke tests
│       ├── smoke-tests.js          # Critical flow validation
│       └── package.json            # Test dependencies
│
└── .github/                        # GitHub workflows
    └── workflows/
        ├── ci.yml                  # Continuous integration
        ├── docker.yml              # Container builds
        ├── it.yml                  # Integration tests
        └── deploy.yml              # Progressive deployment pipeline
```

## 🌐 Service URLs

### Local Development

#### Application Services
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **Social Service**: http://localhost:8082
- **Booking Service**: http://localhost:8083
- **Market Service**: http://localhost:8084
- **Chat Service**: http://localhost:8085
- **Emergency Service**: http://localhost:8086
- **Trip Planning**: http://localhost:8087
- **AI Service**: http://localhost:8088
- **Notification Service**: http://localhost:8089
- **Config Service**: http://localhost:8092

#### Infrastructure Services
- **PostgreSQL**: localhost:5432 (hopngo/hopngo_dev_2024!)
- **MongoDB**: localhost:27017 (admin/mongo_dev_2024!)
- **Redis**: localhost:6379 (redis_dev_2024!)
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Mailhog**: http://localhost:8025

#### Observability Services
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jaeger**: http://localhost:16686
- **Tempo**: http://localhost:3200

### Kubernetes Deployment

#### Development
- **Application**: http://hopngo.local
- **Services**: Available via Ingress routing

#### Staging
- **Application**: https://staging.hopngo.com
- **SSL**: Let's Encrypt certificates

#### Production
- **Application**: https://hopngo.com, https://www.hopngo.com
- **SSL**: Let's Encrypt certificates
- **CDN**: CloudFlare integration ready

## 🔧 Configuration

### Environment Variables

Each service supports environment-specific configuration:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hopngo
SPRING_DATASOURCE_USERNAME=hopngo
SPRING_DATASOURCE_PASSWORD=hopngo_dev_2024!

# Redis Configuration
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=redis_dev_2024!

# MongoDB Configuration
SPRING_DATA_MONGODB_URI=mongodb://admin:mongo_dev_2024!@localhost:27017/hopngo

# RabbitMQ Configuration
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=hopngo
SPRING_RABBITMQ_PASSWORD=rabbit_dev_2024!

# JWT Configuration
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000

# External APIs
OPENAI_API_KEY=your-openai-api-key
GOOGLE_MAPS_API_KEY=your-google-maps-api-key
STRIPE_SECRET_KEY=your-stripe-secret-key
```

### Kubernetes Secrets

Secrets are managed through Kubernetes Secret objects:

```bash
# Create secrets for development
.\infra\scripts\create-secrets.ps1 dev

# Create secrets for production
.\infra\scripts\create-secrets.ps1 production
```

## 🎛️ Feature Flags & A/B Testing

HopNGo includes a comprehensive feature flag and A/B testing system powered by the config-service. This allows for controlled feature rollouts, experimentation, and dynamic configuration management.

### Overview

The feature flag system consists of:
- **Config Service** (port 8092) - Backend API for managing flags and experiments
- **Frontend Hooks** - React hooks for consuming feature flags and experiments
- **Redis Caching** - Fast flag evaluation with automatic cache invalidation
- **Database Storage** - PostgreSQL for persistent flag and experiment configuration

### Quick Start

#### 1. Initialize Feature Flags in Frontend

```typescript
import { initializeFlags } from '@/lib/flags';

// Initialize in your app root (e.g., layout.tsx or _app.tsx)
useEffect(() => {
  initializeFlags();
}, []);
```

#### 2. Use Feature Flags

```typescript
import { useFeatureFlag } from '@/lib/flags';

function MyComponent() {
  const isVisualSearchEnabled = useFeatureFlag('visual-search');
  
  return (
    <div>
      {isVisualSearchEnabled && (
        <VisualSearchButton />
      )}
      <RegularSearch />
    </div>
  );
}
```

#### 3. Use A/B Experiments

```typescript
import { useExperiment } from '@/lib/flags';

function BookingSearch() {
  const { variant, trackEvent } = useExperiment('booking-search-layout');
  
  const handleSearch = () => {
    trackEvent('search_performed', { query: searchQuery });
    // ... search logic
  };
  
  if (variant === 'enhanced') {
    return <EnhancedSearchLayout onSearch={handleSearch} />;
  }
  
  return <CompactSearchLayout onSearch={handleSearch} />;
}
```

### API Endpoints

The config-service provides REST APIs for managing feature flags and experiments:

#### Feature Flags
```bash
# Get all feature flags for a user
GET /api/v1/config/flags?userId=123

# Get specific feature flag
GET /api/v1/config/flags/visual-search?userId=123

# Admin: Create/update feature flag
POST /api/v1/config/admin/flags
PUT /api/v1/config/admin/flags/{flagKey}
```

#### A/B Experiments
```bash
# Get user's experiment assignments
GET /api/v1/config/experiments?userId=123

# Get specific experiment assignment
GET /api/v1/config/experiments/booking-search-layout?userId=123

# Track experiment events
POST /api/v1/config/experiments/booking-search-layout/events
```

### Configuration

#### Environment Variables

```bash
# Config Service
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hopngo
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080
```

#### Database Schema

The system uses four main tables:
- `feature_flags` - Feature flag definitions and targeting rules
- `experiments` - A/B experiment configurations
- `experiment_variants` - Experiment variant definitions with traffic allocation
- `assignments` - User assignments to experiments and flag overrides

### Sample Data

The system comes with sample feature flags and experiments:

#### Feature Flags
- **visual-search** - Toggle visual search functionality (50% rollout)
- **enhanced-booking** - Enhanced booking flow (enabled for premium users)
- **dark-mode** - Dark mode UI theme (disabled by default)

#### A/B Experiments
- **booking-search-layout** - Tests compact vs enhanced search layouts (50/50 split)
- **pricing-display** - Tests different pricing presentation formats

### Advanced Usage

#### Custom Targeting Rules

Feature flags support advanced targeting based on:
- User segments (premium, new, returning)
- Geographic location
- Device type
- Custom user attributes

#### Event Tracking

Track user interactions for experiment analysis:

```typescript
const { trackEvent } = useExperiment('booking-search-layout');

// Track conversion events
trackEvent('booking_completed', {
  bookingId: 'booking-123',
  amount: 150.00,
  variant: 'enhanced'
});

// Track engagement events
trackEvent('search_filter_used', {
  filterType: 'price',
  variant: 'compact'
});
```

#### Cache Management

The system uses Redis for fast flag evaluation:
- Flags are cached for 5 minutes by default
- Cache is automatically invalidated when flags are updated
- Fallback to database if Redis is unavailable

### Monitoring & Analytics

#### Health Checks
```bash
# Check config service health
curl http://localhost:8092/actuator/health

# View flag evaluation metrics
curl http://localhost:8092/actuator/metrics/flags.evaluations
```

#### Experiment Results

Experiment events are stored for analysis:
- Conversion rates by variant
- User engagement metrics
- Statistical significance testing

### Best Practices

1. **Flag Naming**: Use kebab-case with descriptive names (e.g., `visual-search`, `enhanced-checkout`)
2. **Gradual Rollouts**: Start with small percentages and gradually increase
3. **Cleanup**: Remove unused flags and completed experiments regularly
4. **Testing**: Test both enabled and disabled states of features
5. **Documentation**: Document flag purposes and expected behavior

### Troubleshooting

#### Common Issues

1. **Flags not updating**: Check Redis connection and cache TTL
2. **Experiment assignment inconsistency**: Verify user ID consistency across requests
3. **Performance issues**: Monitor flag evaluation frequency and optimize caching

#### Debug Logging

```yaml
# application.yml
logging:
  level:
    com.hopngo.config.service: DEBUG
    com.hopngo.config.cache: DEBUG
```

## 💳 Payment Setup

### Payment Providers

HopNGo supports multiple payment providers for different markets:

- **STRIPE_TEST** - Stripe test environment (default for development)
- **BKASH** - bKash mobile financial service (Bangladesh)
- **NAGAD** - Nagad digital financial service (Bangladesh)
- **MOCK** - Mock provider for testing

### Configuration

Payment provider is configured via the `payment.default.provider` property:

```yaml
# application.yml
payment:
  default:
    provider: BKASH  # Options: MOCK, STRIPE_TEST, BKASH, NAGAD
```

### Environment Variables

#### BKash Configuration

```bash
# BKash Sandbox Credentials
BKASH_APP_KEY=your-bkash-app-key
BKASH_APP_SECRET=your-bkash-app-secret
BKASH_USERNAME=your-bkash-username
BKASH_PASSWORD=your-bkash-password
BKASH_BASE_URL=https://tokenized.sandbox.bka.sh/v1.2.0-beta
```

#### Nagad Configuration

```bash
# Nagad Sandbox Credentials
NAGAD_MERCHANT_ID=your-nagad-merchant-id
NAGAD_MERCHANT_PRIVATE_KEY=your-nagad-private-key
NAGAD_PGP_PUBLIC_KEY=nagad-pgp-public-key
NAGAD_BASE_URL=http://sandbox.mynagad.com:10080/remote-payment-gateway-1.0/api/dfs
```

#### Stripe Configuration

```bash
# Stripe Test Environment
STRIPE_SECRET_KEY=sk_test_your-stripe-secret-key
STRIPE_PUBLISHABLE_KEY=pk_test_your-stripe-publishable-key
STRIPE_WEBHOOK_SECRET=whsec_your-webhook-secret
```

### Test Setup

#### BKash Sandbox Setup

1. **Register for BKash Merchant Account**:
   - Visit [BKash Developer Portal](https://developer.bka.sh/)
   - Create a merchant account for sandbox testing
   - Obtain your App Key, App Secret, Username, and Password

2. **Test Credentials**:
   ```bash
   # Example sandbox credentials (replace with your actual credentials)
   BKASH_APP_KEY=4f6o0cjiki2rfm34kfdadl1eqq
   BKASH_APP_SECRET=2is7hdktrekvrbljjh44ll3d9l1dtjo4pasmjvs5vl5qr3fug4b
   BKASH_USERNAME=sandboxTokenizedUser02
   BKASH_PASSWORD=sandboxTokenizedUser02@12345
   ```

3. **Test Phone Numbers**:
   - Use `01619777282` or `01619777283` for successful transactions
   - Use `01619777284` for insufficient balance scenarios

#### Nagad Sandbox Setup

1. **Register for Nagad Merchant Account**:
   - Contact Nagad for sandbox access
   - Obtain Merchant ID and generate RSA key pairs
   - Get Nagad's PGP public key for encryption

2. **Generate RSA Keys**:
   ```bash
   # Generate private key
   openssl genrsa -out nagad_private.pem 2048
   
   # Generate public key
   openssl rsa -in nagad_private.pem -pubout -out nagad_public.pem
   ```

3. **Test Phone Numbers**:
   - Use `01711111111` for successful transactions
   - Use `01722222222` for failed transactions

### Webhook Configuration

Webhook endpoints are available at:

```
POST /market/payments/webhook/stripe
POST /market/payments/webhook/bkash
POST /market/payments/webhook/nagad
```

#### Webhook Security

- **Stripe**: Uses webhook signatures with `STRIPE_WEBHOOK_SECRET`
- **BKash**: Validates requests using HMAC with App Secret
- **Nagad**: Validates requests using merchant credentials

### Testing Payment Flow

1. **Create Payment Intent**:
   ```bash
   curl -X POST http://localhost:8084/market/payments/intent \
     -H "Content-Type: application/json" \
     -d '{
       "amount": 1000,
       "currency": "BDT",
       "orderId": "order-123"
     }'
   ```

2. **Execute Payment** (for BKash/Nagad):
   ```bash
   curl -X POST http://localhost:8084/market/payments/{paymentId}/execute \
     -H "Content-Type: application/json" \
     -d '{
       "payerReference": "01711111111"
     }'
   ```

3. **Query Payment Status**:
   ```bash
   curl -X GET http://localhost:8084/market/payments/{paymentId}/status
   ```

### Troubleshooting

#### Common Issues

1. **BKash Token Expiration**:
   - Tokens expire after 1 hour
   - Service automatically refreshes tokens
   - Check logs for token refresh errors

2. **Nagad Encryption Issues**:
   - Ensure RSA keys are properly formatted
   - Verify PGP public key is correct
   - Check timestamp synchronization

3. **Webhook Validation Failures**:
   - Verify webhook secrets are correctly configured
   - Check request signatures in logs
   - Ensure webhook URLs are accessible

#### Debug Logging

Enable debug logging for payment providers:

```yaml
logging:
  level:
    com.hopngo.market.service.payment: DEBUG
    com.hopngo.market.provider: DEBUG
```

## 🧪 Testing

### Unit Tests

```bash
# Run tests for all services
mvn test

# Run tests for specific service
cd auth-service && mvn test
```

### Integration Tests

```bash
# Run integration tests
mvn verify -P integration-tests
```

### End-to-End Tests

```bash
# Start services and run E2E tests
./scripts/dev.sh
cd frontend && pnpm test:e2e
```

## 🚀 Deployment

### CI/CD Pipeline with Progressive Delivery

GitHub Actions workflows implement a comprehensive progressive delivery pipeline:

#### Continuous Integration
- **Code Quality**: ESLint, SonarQube, security scans
- **Testing**: Unit tests, integration tests, contract testing
- **Docker Build**: Multi-architecture container images
- **Security**: Container image scanning, dependency checks

#### Progressive Deployment Pipeline

**Staging Deployment** (Automatic on main branch):
1. **Pre-deployment**: Database migrations with Flyway
2. **Deployment**: Argo Rollouts with health checks
3. **Validation**: Smoke tests for critical user flows
4. **Notification**: Slack/Teams deployment status

**Production Deployment** (Manual approval required):
1. **Prerequisites**: Successful staging deployment + smoke tests
2. **Manual Approval**: GitHub Actions approval gate
3. **Canary Deployment**: 25% → 50% → 100% traffic shifting
4. **Automated Analysis**: SLO monitoring (error rate <5%, latency <2s)
5. **Auto-rollback**: Immediate rollback on SLO violations
6. **Smoke Testing**: Post-deployment validation

**Rollback Capabilities**:
- **Manual Rollback**: One-click rollback via GitHub Actions
- **Automated Rollback**: Triggered by SLO violations
- **Database Rollback**: Flyway-managed schema rollbacks

#### Deployment Runbook

See [docs/runbook-deploy.md](docs/runbook-deploy.md) for comprehensive deployment procedures, including:
- Pre-deployment checklists
- Monitoring and observability
- Troubleshooting guides
- Emergency procedures

### Manual Deployment

```bash
# Build all services
mvn clean package -DskipTests

# Build Docker images
docker-compose build

# Deploy to Kubernetes
kubectl apply -k infra/k8s/overlays/production
```

## 🔒 Security

### Authentication & Authorization
- **JWT-based authentication** with refresh tokens
- **Role-based access control (RBAC)**
- **OAuth2 integration** ready
- **API rate limiting** via Spring Cloud Gateway

### Infrastructure Security
- **TLS/SSL encryption** for all external communication
- **Network policies** for service-to-service communication
- **Pod security standards** in Kubernetes
- **Secret management** via Kubernetes Secrets
- **Container image scanning** in CI/CD

### Data Protection
- **Database encryption** at rest and in transit
- **PII data anonymization** capabilities
- **GDPR compliance** features
- **Audit logging** for sensitive operations

## 📈 Performance & Scaling

### Horizontal Scaling
- **Kubernetes HPA** based on CPU/memory metrics
- **Custom metrics scaling** via Prometheus
- **Database connection pooling** with HikariCP
- **Redis clustering** support

### Performance Optimization
- **Database indexing** strategies
- **Caching layers** with Redis
- **CDN integration** for static assets
- **Async processing** with RabbitMQ

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and add tests
4. Commit your changes: `git commit -m 'feat: add amazing feature'`
5. Push to the branch: `git push origin feature/amazing-feature`
6. Open a Pull Request

### Code Standards

- **Java**: Follow Google Java Style Guide
- **TypeScript**: Use ESLint and Prettier configurations
- **Commit Messages**: Follow Conventional Commits specification
- **Testing**: Maintain >80% code coverage

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check the [docs/](docs/) directory
- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Join GitHub Discussions for questions
- **Security**: Report security issues via GitHub Security Advisories

## 🙏 Acknowledgments

- **Spring Boot** team for the excellent framework
- **React** team for the frontend framework
- **Kubernetes** community for container orchestration
- **Open source contributors** who make this project possible

---

**HopNGo** - Connecting travelers with seamless transportation experiences 🌍✈️🚗
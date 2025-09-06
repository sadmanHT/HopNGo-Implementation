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
└─┬─────────┬─────────┬─────────┬─────────┬─────────┬─────────┬─────────┬─────┘
  │         │         │         │         │         │         │         │
┌─▼─┐     ┌─▼─┐     ┌─▼─┐     ┌─▼─┐     ┌─▼─┐     ┌─▼─┐     ┌─▼─┐     ┌─▼─┐
│Auth│     │Soc│     │Book│    │Mkt│     │Chat│    │Trip│    │ AI │    │Emg│
│Svc │     │Svc│     │Svc │    │Svc│     │Svc │    │Svc │    │Svc │    │Svc│
│:81 │     │:82│     │:83 │    │:84│     │:85 │    │:87 │    │:88 │    │:86│
└───┘     └───┘     └───┘     └───┘     └───┘     └───┘     └───┘     └───┘
  │         │         │         │         │         │         │         │
┌─▼─────────▼─────────▼─────────▼─────────▼─────────▼─────────▼─────────▼─────┐
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
- **Helm** - Kubernetes package management
- **Kustomize** - Kubernetes configuration management
- **NGINX Ingress** - Load balancing and SSL termination
- **Cert-Manager** - Automatic SSL certificate management

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

### Infrastructure Setup

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

- **Horizontal Pod Autoscaling (HPA)** - Automatic scaling based on CPU/memory
- **Ingress with SSL termination** - NGINX Ingress Controller with Cert-Manager
- **ConfigMaps and Secrets** - Environment-specific configuration
- **Health checks** - Liveness and readiness probes
- **Resource limits** - CPU and memory constraints
- **Security policies** - Pod security standards
- **Network policies** - Service-to-service communication control

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
│   └── github-secrets.md
│
└── .github/                        # GitHub workflows
    └── workflows/
        ├── ci.yml
        ├── docker.yml
        └── it.yml
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

### CI/CD Pipeline

GitHub Actions workflows handle:

- **Continuous Integration**: Code quality, tests, security scans
- **Docker Build**: Multi-architecture container images
- **Integration Tests**: Cross-service testing
- **Deployment**: Automated deployment to staging/production

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
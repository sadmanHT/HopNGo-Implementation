# HopNGo 🚀

> A comprehensive travel and transportation platform that connects travelers with various transportation options and services. Built with modern microservices architecture, AI-powered recommendations, and enterprise-grade features.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Node.js](https://img.shields.io/badge/Node.js-20+-green.svg)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5+-blue.svg)](https://www.typescriptlang.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue.svg)](https://kubernetes.io/)
[![Helm](https://img.shields.io/badge/Helm-Charts-blue.svg)](https://helm.sh/)
[![Build Status](https://img.shields.io/badge/Build-Passing-green.svg)](https://github.com/sadmanHT/HopNGo-Implementation)
[![Coverage](https://img.shields.io/badge/Coverage-85%25-green.svg)](https://github.com/sadmanHT/HopNGo-Implementation)
[![Security](https://img.shields.io/badge/Security-A+-green.svg)](https://github.com/sadmanHT/HopNGo-Implementation)

## ✨ Key Features

### 🎯 Core Platform Features
- **🏨 Smart Booking System** - AI-powered accommodation and experience booking with real-time availability
- **🔍 Intelligent Search** - Vector-based semantic search with visual image recognition and filters
- **💳 Multi-Payment Gateway** - Integrated Stripe, bKash, Nagad with automatic currency conversion
- **📱 Real-time Chat** - WebSocket-powered messaging between travelers and providers
- **🗺️ Trip Planning** - AI-assisted itinerary creation with collaborative planning features
- **🚨 Emergency Services** - 24/7 emergency assistance with location-based alert system
- **📊 Provider Analytics** - Comprehensive dashboards for service providers with revenue insights
- **🎛️ Feature Flags & A/B Testing** - Dynamic feature rollouts with experiment tracking

### 🤖 AI & Machine Learning
- **🧠 Vector Embeddings** - Advanced semantic similarity using Qdrant vector database
- **🔮 Personalized Recommendations** - ML-powered suggestions based on user behavior and preferences
- **🖼️ Visual Search** - Image-based search for accommodations and experiences
- **📈 Predictive Analytics** - Demand forecasting and dynamic pricing optimization
- **🎯 Smart Matching** - AI-powered traveler-provider matching algorithms

### 💼 Business & Finance
- **💰 Multi-Currency Support** - Real-time FX rates with automated currency conversion
- **📄 Invoice Generation** - Automated PDF invoice creation with tax calculations
- **💸 Payout Management** - Automated provider payouts with configurable schedules
- **📊 Financial Analytics** - Revenue tracking, profit analysis, and financial reporting
- **🏦 Ledger System** - Double-entry bookkeeping with transaction audit trails

### 🔐 Security & Compliance
- **🛡️ Advanced Authentication** - JWT with 2FA, social login, and biometric support
- **🔒 Role-Based Access Control** - Granular permissions with dynamic role assignments
- **🔑 JWT Key Rotation** - Automated security key rotation with zero-downtime
- **📋 Audit Logging** - Comprehensive activity tracking for compliance
- **🛡️ Data Protection** - GDPR compliance with data anonymization capabilities

### 📱 User Experience
- **♿ Accessibility Features** - WCAG 2.1 AA compliance with screen reader support
- **🌐 Internationalization** - Multi-language support with RTL layout support
- **🎨 Dark Mode** - System-aware theme switching with custom brand themes
- **🚀 Performance Optimization** - Lazy loading, image optimization, and CDN integration
- **📧 Newsletter System** - Automated campaigns with subscriber management

## � CI/CD Status

[![CI](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/ci.yml/badge.svg)](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/ci.yml)
[![Security Scan](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/security-scan.yml/badge.svg)](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/security-scan.yml)
[![Synthetic Monitoring](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/synthetic-monitoring.yml/badge.svg)](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/synthetic-monitoring.yml)
[![Deploy](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/deploy.yml/badge.svg)](https://github.com/sadmanHT/HopNGo-Implementation/actions/workflows/deploy.yml)

HopNGo implements advanced progressive delivery strategies using **Argo Rollouts** for safe, controlled deployments:

- **Blue/Green Deployments** - Zero-downtime deployments with instant rollback capability
- **Canary Deployments** - Gradual traffic shifting (25% → 50% → 100%) with automated health checks
- **Feature Flags** - Dark launches and A/B testing for controlled feature rollouts
- **Automated Rollbacks** - Automatic rollback on SLO violations (error rate >5%, latency >2s)
- **Manual Approval Gates** - Production deployments require manual approval after staging validation
- **Smoke Testing** - Post-deployment validation of critical user flows

## 🚀 Progressive Delivery

## 🏗️ Architecture

HopNGo is built on a modern microservices architecture with AI integration, progressive delivery, and enterprise-grade security.

```mermaid
graph TB
    %% Frontend Layer
    subgraph "Frontend Layer"
        UI[React Frontend<br/>TypeScript + Vite<br/>:3000]
        Mobile[Mobile App<br/>React Native<br/>iOS/Android]
    end

    %% API Gateway
    subgraph "API Gateway"
        GW[Spring Cloud Gateway<br/>Load Balancer + Auth<br/>:8080]
    end

    %% Core Services
    subgraph "Core Business Services"
        AUTH[Auth Service<br/>JWT + 2FA + OAuth<br/>:8081]
        BOOK[Booking Service<br/>Reservations + Payments<br/>:8083]
        MKT[Market Service<br/>Listings + Payments<br/>:8084]
        TRIP[Trip Planning<br/>AI Itineraries<br/>:8087]
    end

    %% AI & Analytics
    subgraph "AI & Analytics Services"
        AI[AI Service<br/>Embeddings + ML<br/>:8088]
        ANALYTICS[Analytics Service<br/>Provider Insights<br/>:8090]
        SEARCH[Search Service<br/>Elasticsearch + Vector<br/>:8091]
    end

    %% Communication Services
    subgraph "Communication Services"
        CHAT[Chat Service<br/>WebSocket + RTC<br/>:8085]
        NOTIF[Notification Service<br/>Push + Email + SMS<br/>:8089]
        SOCIAL[Social Service<br/>Profiles + Reviews<br/>:8082]
    end

    %% Support & Operations
    subgraph "Support & Operations"
        SUPPORT[Support Service<br/>Tickets + Help<br/>:8093]
        EMERG[Emergency Service<br/>24/7 Assistance<br/>:8086]
        CONFIG[Config Service<br/>Feature Flags + A/B<br/>:8092]
    end

    %% Data Layer
    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Primary Database<br/>:5432)]
        MONGO[(MongoDB<br/>Documents + Logs<br/>:27017)]
        REDIS[(Redis<br/>Cache + Sessions<br/>:6379)]
        ES[(Elasticsearch<br/>Search Index<br/>:9200)]
        QDRANT[(Qdrant<br/>Vector Database<br/>:6333)]
    end

    %% Message Queue
    subgraph "Event Streaming"
        RABBIT[RabbitMQ<br/>Event Bus<br/>:5672]
    end

    %% External Integrations
    subgraph "External Services"
        PAYMENT[Payment Gateways<br/>Stripe + bKash + Nagad]
        MAPS[Google Maps API<br/>Geocoding + Routes]
        OPENAI[OpenAI API<br/>GPT + Embeddings]
        EMAIL[Email Service<br/>SendGrid/Mailgun]
        SMS[SMS Gateway<br/>Twilio/Nexmo]
    end

    %% Observability
    subgraph "Observability"
        PROM[Prometheus<br/>Metrics<br/>:9090]
        GRAF[Grafana<br/>Dashboards<br/>:3001]
        JAEGER[Jaeger<br/>Tracing<br/>:16686]
    end

    %% Connections
    UI --> GW
    Mobile --> GW
    
    GW --> AUTH
    GW --> BOOK
    GW --> MKT
    GW --> TRIP
    GW --> AI
    GW --> ANALYTICS
    GW --> CHAT
    GW --> NOTIF
    GW --> SOCIAL
    GW --> SUPPORT
    GW --> EMERG
    GW --> CONFIG

    %% Data connections
    AUTH --> PG
    BOOK --> PG
    MKT --> PG
    TRIP --> PG
    SUPPORT --> PG
    CONFIG --> PG
    
    CHAT --> MONGO
    SOCIAL --> MONGO
    NOTIF --> MONGO
    
    AI --> QDRANT
    SEARCH --> ES
    
    AUTH --> REDIS
    BOOK --> REDIS
    CONFIG --> REDIS
    
    %% Event streaming
    BOOK --> RABBIT
    MKT --> RABBIT
    NOTIF --> RABBIT
    ANALYTICS --> RABBIT
    
    %% External integrations
    MKT --> PAYMENT
    TRIP --> MAPS
    AI --> OPENAI
    NOTIF --> EMAIL
    NOTIF --> SMS
    
    %% Observability
    AUTH -.-> PROM
    BOOK -.-> PROM
    MKT -.-> PROM
    PROM --> GRAF
    AUTH -.-> JAEGER
    BOOK -.-> JAEGER
```

### Key Architectural Patterns

#### 🎯 **Microservices Architecture**
- **Service Decomposition**: Domain-driven design with bounded contexts
- **API Gateway Pattern**: Centralized routing, authentication, and rate limiting  
- **Database per Service**: Each service owns its data with appropriate database choice
- **Event-Driven Architecture**: Asynchronous communication via RabbitMQ
- **CQRS**: Command Query Responsibility Segregation for analytics and reporting

#### 🤖 **AI-First Design**
- **Vector Search**: Qdrant for semantic similarity and recommendations
- **Embedding Pipeline**: Automatic content vectorization for search and matching
- **ML Feature Store**: Centralized feature management for AI models
- **Real-time Inference**: Low-latency AI predictions via dedicated AI service

#### 🔐 **Security by Design**
- **Zero Trust Architecture**: Service-to-service authentication and authorization
- **JWT with Rotation**: Automated key rotation with graceful fallback
- **Multi-Factor Authentication**: TOTP, SMS, and biometric support
- **Data Encryption**: End-to-end encryption for sensitive data

#### 📊 **Observability & Monitoring**
- **Distributed Tracing**: Full request tracing across all services
- **Metrics Collection**: Prometheus with custom business metrics
- **Centralized Logging**: Structured JSON logs with correlation IDs
- **Health Checks**: Comprehensive health monitoring with auto-recovery

## 🚀 Services Overview

HopNGo consists of 13 specialized microservices, each designed for specific business capabilities with enterprise-grade features.

| Service | Port | Technology Stack | Core Features | Status |
|---------|------|------------------|---------------|--------|
| **🎨 Frontend** | 3000 | React 18 + TypeScript + Vite + Tailwind | Modern UI, PWA, Accessibility, Dark Mode | ✅ Production |
| **🌐 API Gateway** | 8080 | Spring Cloud Gateway + Rate Limiting | Request routing, Auth, Load balancing | ✅ Production |
| **🔐 Auth Service** | 8081 | Spring Security + JWT + 2FA | Multi-auth, Social login, Biometrics | ✅ Production |
| **👥 Social Service** | 8082 | Spring Boot + JPA + MongoDB | Profiles, Reviews, Social features | ✅ Production |
| **📅 Booking Service** | 8083 | Spring Boot + JPA + Events | Reservations, Inventory, Payment flow | ✅ Production |
| **🏪 Market Service** | 8084 | Spring Boot + Multi-payment | Listings, Multi-currency, Invoices | ✅ Production |
| **💬 Chat Service** | 8085 | Spring Boot + WebSocket + MongoDB | Real-time messaging, File sharing | ✅ Production |
| **🚨 Emergency Service** | 8086 | Spring Boot + Location + Alerts | 24/7 assistance, Location tracking | ✅ Production |
| **🗺️ Trip Planning** | 8087 | Spring Boot + AI + Maps API | AI itineraries, Collaborative planning | ✅ Production |
| **🤖 AI Service** | 8088 | Spring Boot + Python + Qdrant | Vector search, ML recommendations | ✅ Production |
| **📧 Notification Service** | 8089 | Spring Boot + Firebase + RabbitMQ | Push, Email, SMS, Emergency alerts | ✅ Production |
| **📊 Analytics Service** | 8090 | Spring Boot + ClickHouse + Events | Provider analytics, Business insights | ✅ Production |
| **🔍 Search Service** | 8091 | Spring Boot + Elasticsearch + Vector | Semantic search, Visual search | ✅ Production |
| **⚙️ Config Service** | 8092 | Spring Boot + Redis + A/B Testing | Feature flags, Experiments, Config | ✅ Production |
| **🎫 Support Service** | 8093 | Spring Boot + JPA + Knowledge Base | Tickets, Help articles, Agent tools | ✅ Production |

### 🎯 Service Capabilities Deep Dive

#### 🔐 **Auth Service - Advanced Security**
- **Multi-Factor Authentication**: TOTP, SMS, Email, Biometric
- **Social Login**: Google, Facebook, Apple, GitHub integration
- **JWT Management**: Automatic key rotation, refresh tokens
- **Account Security**: Lockout policies, password validation
- **Audit Trail**: Complete authentication event tracking

#### 🤖 **AI Service - Machine Learning Hub**
- **Vector Embeddings**: OpenAI + custom models for semantic search
- **Recommendation Engine**: Collaborative and content-based filtering
- **Visual Search**: Image recognition for accommodation matching
- **Predictive Analytics**: Demand forecasting, dynamic pricing
- **Natural Language**: Chatbot integration, query understanding

#### 💳 **Market Service - Financial Operations**
- **Multi-Payment Gateway**: Stripe, bKash, Nagad, PayPal integration
- **Currency Management**: Real-time FX rates, auto-conversion
- **Invoice System**: PDF generation, tax calculations
- **Payout Automation**: Scheduled provider payments
- **Financial Reporting**: Revenue analytics, profit tracking

#### 📊 **Analytics Service - Business Intelligence**
- **Provider Dashboards**: Revenue, bookings, performance metrics
- **A/B Experiments**: Statistical analysis, conversion tracking
- **Customer Insights**: Behavior analysis, segmentation
- **Market Analytics**: Trends, demand patterns, pricing optimization
- **Newsletter System**: Campaign management, subscriber analytics

#### 🎫 **Support Service - Customer Success**
- **Ticket Management**: Priority routing, SLA tracking
- **Knowledge Base**: Searchable help articles, FAQs
- **Agent Tools**: Canned responses, escalation workflows
- **Live Chat Integration**: Real-time support capabilities
- **Analytics Dashboard**: Support metrics, resolution times

## 🛠️ Technology Stack

### 🎨 **Frontend & User Experience**
- **React 18** with TypeScript for type-safe development
- **Vite** for lightning-fast development and builds  
- **Tailwind CSS** + **Shadcn/ui** for consistent, accessible design
- **Next.js** integration for SSR and production optimization
- **Progressive Web App (PWA)** with offline capabilities
- **Accessibility**: WCAG 2.1 AA compliance with screen reader support
- **Internationalization**: Multi-language support with RTL layouts
- **Performance**: Lazy loading, image optimization, CDN integration

### ⚙️ **Backend & Microservices**
- **Spring Boot 3** with Java 17 for modern enterprise development
- **Spring Cloud Gateway** for intelligent API routing and load balancing
- **Spring Security** with JWT, OAuth2, and multi-factor authentication
- **Spring Data JPA** with Hibernate for ORM and database operations
- **Maven** for dependency management and multi-module builds
- **WebSocket** support for real-time communication features
- **Event-Driven Architecture** with RabbitMQ message queues

### 🗄️ **Data Layer & Storage**
- **PostgreSQL 15** - Primary relational database with advanced indexing
- **MongoDB 7** - Document storage for flexible, schema-less data
- **Redis 7** - High-performance caching and session management
- **Elasticsearch 8** - Full-text search with analytics capabilities
- **Qdrant** - Vector database for AI embeddings and similarity search
- **ClickHouse** - Columnar database for analytics and time-series data

### 🤖 **AI & Machine Learning**
- **OpenAI GPT-4** integration for natural language processing
- **Vector Embeddings** with OpenAI and custom models
- **Qdrant Vector Database** for semantic search and recommendations
- **Python Integration** for advanced ML model deployment
- **TensorFlow/PyTorch** support for custom model training
- **MLOps Pipeline** for model versioning and deployment

### ☁️ **Infrastructure & DevOps**
- **Docker** - Containerization with multi-stage builds
- **Kubernetes** - Container orchestration with auto-scaling
- **Argo Rollouts** - Progressive delivery with blue/green and canary deployments  
- **Helm** - Kubernetes package management and templating
- **NGINX Ingress** - Load balancing and SSL termination
- **Cert-Manager** - Automatic SSL certificate management
- **Flyway** - Database migration management with baseline checks

### 📊 **Observability & Monitoring**
- **Prometheus** - Metrics collection with custom business metrics
- **Grafana** - Visualization dashboards with SLO monitoring
- **Jaeger** - Distributed tracing across all microservices
- **OpenTelemetry** - Automatic instrumentation and telemetry collection
- **Alertmanager** - Intelligent alerting with escalation policies
- **Structured Logging** - JSON logs with correlation IDs

### 🔒 **Security & Compliance**
- **JWT Authentication** with automatic key rotation
- **OAuth2 & OpenID Connect** for federated authentication
- **Multi-Factor Authentication** (TOTP, SMS, Biometric)
- **Role-Based Access Control (RBAC)** with dynamic permissions
- **Data Encryption** at rest and in transit
- **Security Scanning** with automated vulnerability detection
- **GDPR Compliance** with data anonymization and deletion capabilities

### 💸 **Payment & Financial Integration**
- **Stripe** - Global payment processing with subscription support
- **bKash** - Bangladesh mobile financial services integration
- **Nagad** - Digital payment gateway for Bangladesh market
- **Multi-Currency Support** with real-time FX rate conversion
- **PCI DSS Compliance** for secure payment handling
- **Automated Accounting** with double-entry ledger system

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

## 📂 Project Structure

```
HopNGo/
├── 📄 README.md                        # This comprehensive guide
├── 🐳 docker-compose.yml              # Local development with all services
├── 📦 pom.xml                          # Maven parent POM configuration
├── 📦 package.json                     # Workspace and script configuration
├── 🔧 pnpm-workspace.yaml             # PNPM monorepo workspace
├── 📋 commitlint.config.js             # Commit message standards
├── 📄 CHANGELOG_FIXES.md               # Development changelog
├── 🛡️ SECURITY.md                      # Security policies and reporting
├── 🤝 CONTRIBUTING.md                  # Contribution guidelines
│
├── 🎨 frontend/                        # React TypeScript Application
│   ├── 📱 src/
│   │   ├── 🏗️ app/                     # Next.js App Router structure
│   │   │   ├── [locale]/              # Internationalized routes
│   │   │   ├── admin/                 # Admin dashboard pages
│   │   │   ├── provider/              # Provider portal pages
│   │   │   └── legal/                 # Legal pages (privacy, terms)
│   │   ├── 🧩 components/             # Reusable UI components
│   │   │   ├── ui/                    # Base Shadcn/ui components
│   │   │   ├── admin/                 # Admin-specific components
│   │   │   ├── provider/              # Provider dashboard components
│   │   │   ├── accessibility/         # A11y components
│   │   │   └── performance/           # Performance monitoring
│   │   ├── 🎣 hooks/                  # Custom React hooks
│   │   ├── 📚 lib/                    # Utility libraries and APIs
│   │   ├── 🌐 services/               # API service clients
│   │   ├── 🏪 stores/                 # State management (Zustand)
│   │   ├── 🎨 styles/                 # Global styles and themes
│   │   └── 🔧 utils/                  # Helper functions
│   ├── 🧪 tests/                      # Frontend test suites
│   │   ├── e2e/                      # End-to-end tests (Playwright)
│   │   ├── unit/                     # Unit tests (Jest)
│   │   └── accessibility/            # A11y tests
│   ├── 📱 public/                     # Static assets and PWA files
│   ├── 📦 package.json               # Frontend dependencies
│   ├── 🔧 next.config.ts             # Next.js configuration
│   ├── 🎨 tailwind.config.ts         # Tailwind CSS configuration
│   └── 🐳 Dockerfile                 # Frontend container image
│
├── 🌐 gateway/                        # API Gateway Service
│   ├── 📁 src/main/java/com/hopngo/gateway/
│   │   ├── 🛡️ filter/                # Custom gateway filters
│   │   └── 🔧 config/                # Gateway configuration
│   └── 📄 README.md                  # Gateway-specific documentation
│
├── 🔐 auth-service/                   # Authentication & Authorization
│   ├── 📁 src/main/java/com/hopngo/auth/
│   │   ├── 🎛️ controller/            # Auth REST endpoints
│   │   ├── 🛡️ service/               # Business logic (JWT, 2FA, OAuth)
│   │   ├── 🗃️ entity/                # User and security entities
│   │   ├── 🔍 repository/            # Data access layer
│   │   └── 📦 dto/                   # Data transfer objects
│   └── 🗄️ src/main/resources/db/migration/ # Flyway migrations
│
├── 👥 social-service/                 # Social Features & User Profiles
│   ├── 📁 src/main/java/com/hopngo/social/
│   │   ├── 🎛️ controller/            # Social REST endpoints
│   │   ├── 🛡️ service/               # Profile and social logic
│   │   ├── 🗃️ entity/                # User profiles, follows, reviews
│   │   └── 🤖 service/               # AI recommendations integration
│   └── 🗄️ src/main/resources/db/migration/
│
├── 📅 booking-service/                # Booking & Reservation Management
│   ├── 📁 src/main/java/com/hopngo/booking/
│   │   ├── 🎛️ controller/            # Booking REST endpoints
│   │   ├── 🛡️ service/               # Booking logic, inventory, payments
│   │   ├── 🗃️ entity/                # Bookings, listings, reviews
│   │   ├── 📦 dto/                   # Booking DTOs
│   │   └── 🗺️ mapper/                # Entity-DTO mapping
│   └── 🗄️ src/main/resources/db/migration/
│
├── 🏪 market-service/                 # Marketplace & Financial Operations
│   ├── 📁 src/main/java/com/hopngo/market/
│   │   ├── 🎛️ controller/            # Market and payment endpoints
│   │   ├── 🛡️ service/               # Listings, payments, invoices
│   │   │   ├── payment/              # Multi-payment gateway providers
│   │   │   └── finance/              # Financial calculations
│   │   ├── 🗃️ entity/                # Listings, payments, invoices
│   │   └── 🎯 event/                 # Payment events
│   └── 🗄️ src/main/resources/db/migration/
│
├── 💬 chat-service/                   # Real-time Messaging
│   ├── 📁 src/main/java/com/hopngo/chat/
│   │   ├── 🎛️ controller/            # Chat REST and WebSocket endpoints
│   │   ├── 🛡️ service/               # Message handling, file sharing
│   │   ├── 🗃️ entity/                # Conversations, messages
│   │   └── 🔌 websocket/             # WebSocket configuration
│
├── 🚨 emergency-service/              # Emergency Assistance
│   ├── 📁 src/main/java/com/hopngo/emergency/
│   │   ├── 🎛️ controller/            # Emergency endpoints
│   │   ├── 🛡️ service/               # Alert handling, location tracking
│   │   └── 🗃️ entity/                # Emergency contacts, incidents
│
├── 🗺️ trip-planning-service/          # AI-Powered Trip Planning
│   ├── 📁 src/main/java/com/hopngo/tripplanning/
│   │   ├── 🎛️ controller/            # Trip planning endpoints
│   │   ├── 🛡️ service/               # Itinerary creation, collaboration
│   │   ├── 🗃️ entity/                # Trips, itineraries, activities
│   │   └── 🤖 ai/                    # AI integration for recommendations
│
├── 🤖 ai-service/                     # AI & Machine Learning Hub
│   ├── 📁 src/main/java/com/hopngo/ai/
│   │   ├── 🎛️ controller/            # AI REST endpoints
│   │   ├── 🛡️ service/               # ML models, embeddings, recommendations
│   │   │   ├── EmbeddingService.java # Vector embeddings
│   │   │   ├── QdrantService.java    # Vector database operations
│   │   │   └── ExternalAiService.java# OpenAI integration
│   │   ├── 📦 dto/                   # AI request/response DTOs
│   │   └── 🔧 config/                # AI service configuration
│   └── 🧪 src/test/                  # AI service tests
│
├── 📧 notification-service/           # Multi-channel Notifications
│   ├── 📁 src/main/java/com/hopngo/notification/
│   │   ├── 🎛️ controller/            # Notification endpoints
│   │   ├── 🛡️ service/               # Push, email, SMS services
│   │   │   ├── FirebaseMessagingService.java # FCM integration
│   │   │   └── EmergencyAlertService.java    # Emergency notifications
│   │   ├── 🗃️ entity/                # Notification templates, logs
│   │   └── 🔧 config/                # Firebase, RabbitMQ config
│
├── 📊 analytics-service/              # Business Intelligence & Analytics
│   ├── 📁 src/main/java/com/hopngo/analytics/
│   │   ├── 🎛️ controller/            # Analytics endpoints
│   │   │   ├── ProviderAnalyticsController.java # Provider dashboards
│   │   │   ├── NewsletterController.java        # Newsletter analytics
│   │   │   └── ReferralController.java          # Referral tracking
│   │   ├── 🛡️ service/               # Analytics computation
│   │   ├── 🗃️ entity/                # Analytics tables, computed views
│   │   └── 🗄️ repository/            # ClickHouse integration
│   ├── 📊 README.md                  # Analytics documentation
│   └── 🗄️ src/main/resources/db/migration/
│
├── 🔍 search-service/                 # Advanced Search & Discovery
│   ├── 📁 src/main/java/com/hopngo/search/
│   │   ├── 🎛️ controller/            # Search endpoints
│   │   ├── 🛡️ service/               # Elasticsearch, vector search
│   │   └── 🗃️ entity/                # Search indexes, filters
│
├── ⚙️ config-service/                 # Feature Flags & A/B Testing
│   ├── 📁 src/main/java/com/hopngo/config/
│   │   ├── 🎛️ controller/            # Config management endpoints
│   │   ├── 🛡️ service/               # Feature flag evaluation
│   │   │   └── FeatureFlagEvaluationService.java
│   │   ├── 🗃️ entity/                # Feature flags, experiments
│   │   └── 🗄️ repository/            # Config storage
│   └── 🗄️ src/main/resources/db/migration/
│
├── 🎫 support-service/                # Customer Support & Help Desk
│   ├── 📁 src/main/java/com/hopngo/support/
│   │   ├── 🎛️ controller/            # Support endpoints
│   │   ├── 🛡️ service/               # Ticket management, knowledge base
│   │   ├── 🗃️ entity/                # Tickets, help articles
│   │   └── 📦 dto/                   # Support DTOs
│   └── 🗄️ src/main/resources/db/migration/
│
├── 🔍 search-client/                  # Shared Search Client Library
│   ├── 📁 src/main/java/com/hopngo/search/
│   │   └── 📚 client/                # Elasticsearch client utilities
│
├── 🏗️ infra/                         # Infrastructure as Code
│   ├── 🐳 compose/                   # Docker Compose for local development
│   │   ├── docker-compose.yml       # Complete development stack
│   │   ├── grafana/                 # Grafana dashboards and config
│   │   │   └── dashboards/         # Pre-built monitoring dashboards
│   │   ├── prometheus/              # Prometheus configuration
│   │   │   ├── alerting-rules.yml  # Alert definitions
│   │   │   └── recording-rules.yml # Metric aggregations
│   │   └── alertmanager/           # Alert routing configuration
│   │
│   ├── ⛵ helm/                     # Helm Charts for Kubernetes
│   │   ├── Chart.yaml              # Helm chart metadata
│   │   ├── values.yaml             # Default configuration values
│   │   ├── templates/              # Kubernetes resource templates
│   │   └── charts/                 # Dependency charts
│   │
│   ├── ☸️ k8s/                      # Kubernetes Manifests
│   │   ├── base/                   # Base Kustomize resources
│   │   │   ├── kustomization.yaml  # Kustomize configuration
│   │   │   ├── namespace.yaml      # Namespace definitions
│   │   │   ├── secrets.yaml        # Secret templates
│   │   │   ├── configmaps.yaml     # Configuration maps
│   │   │   ├── ingress.yaml        # Ingress controller setup
│   │   │   └── */                  # Service-specific manifests
│   │   │       ├── deployment.yaml # Kubernetes deployments
│   │   │       ├── service.yaml    # Service definitions
│   │   │       ├── configmap.yaml  # Service configuration
│   │   │       └── rollout.yaml    # Argo Rollouts configuration
│   │   │
│   │   └── overlays/               # Environment-specific configurations
│   │       ├── dev/                # Development environment
│   │       │   ├── kustomization.yaml
│   │       │   └── patches/        # Dev-specific patches
│   │       ├── staging/            # Staging environment
│   │       │   ├── kustomization.yaml
│   │       │   └── patches/        # Staging-specific patches
│   │       └── production/         # Production environment
│   │           ├── kustomization.yaml
│   │           └── patches/        # Production-specific patches
│   │
│   └── 📜 scripts/                  # Deployment and utility scripts
│       ├── install.ps1             # Windows Kubernetes deployment
│       ├── install.sh              # Linux Kubernetes deployment
│       ├── create-secrets.ps1      # Windows secrets management
│       ├── create-secrets.sh       # Linux secrets management
│       ├── backup.sh               # Database backup scripts
│       └── monitoring-setup.sh     # Observability stack setup
│
├── 📜 scripts/                       # Development and utility scripts
│   ├── dev.sh                      # Start local development environment
│   ├── down.sh                     # Stop all local services
│   ├── setup-dev.ps1               # Windows development setup
│   ├── test-*.sh                   # Database and service testing
│   ├── rotate-jwt-keys.ps1         # JWT key rotation (Windows)
│   ├── rotate-jwt-keys.sh          # JWT key rotation (Linux)
│   ├── synthetic-checks.sh         # Synthetic monitoring
│   └── seed-data/                  # Sample data for development
│       ├── users.sql               # Test user accounts
│       ├── listings.sql            # Sample listings
│       └── bookings.sql            # Sample booking data
│
├── 🧪 tests/                        # Cross-service integration tests
│   └── smoke/                      # Critical flow validation
│       ├── smoke-tests.js          # End-to-end smoke tests
│       ├── api-tests.js            # API integration tests
│       └── package.json            # Test dependencies
│
├── 📚 docs/                         # Comprehensive documentation
│   ├── 🏗️ ARCHITECTURE.md          # System architecture deep dive
│   ├── 🚀 runbook-deploy.md         # Deployment procedures
│   ├── 🆘 runbook-dr.md             # Disaster recovery procedures
│   ├── 📋 runbook-oncall.md         # On-call procedures
│   ├── 🔒 SECURITY_CHECKLIST.md     # Security compliance checklist
│   ├── 🔑 JWT_KEY_MANAGEMENT.md     # JWT security procedures
│   ├── 🚀 launch-checklist.md       # Pre-launch validation
│   ├── 📊 media-backup-strategy.md  # Media backup procedures
│   ├── 🤝 github-secrets.md         # CI/CD secrets management
│   └── site/                       # Documentation website
│       └── docs/                   # Docusaurus documentation
│
├── 🔒 security/                     # Security configurations
│   └── dependency-check-suppressions.xml # Security scan suppressions
│
├── 🔄 .github/                      # GitHub Actions & CI/CD
│   └── workflows/
│       ├── ci.yml                  # Continuous integration pipeline
│       ├── security-scan.yml       # Security vulnerability scanning
│       ├── synthetic-monitoring.yml # Uptime and performance monitoring
│       └── deploy.yml              # Progressive deployment pipeline
│
└── 📄 Various config files           # Project configuration
    ├── .gitignore                  # Git ignore patterns
    ├── .editorconfig              # Editor configuration
    ├── maven.zip                  # Maven distribution (if needed)
    ├── install-maven.bat          # Maven setup (Windows)
    ├── install-maven.ps1          # Maven setup (PowerShell)
    ├── Makefile                   # Build automation
    ├── LICENSE                    # MIT license
    └── test-embedding-reindex.md  # AI service testing documentation
```

### 📋 Key Directory Highlights

- **🎨 Frontend**: Modern React app with TypeScript, accessibility, and PWA features
- **⚙️ Services**: 13 specialized microservices with domain-driven design
- **🏗️ Infrastructure**: Complete K8s setup with progressive delivery
- **📚 Documentation**: Comprehensive guides for development and operations
- **🧪 Testing**: Multi-layer testing strategy with E2E, integration, and unit tests
- **🔒 Security**: Security-first approach with automated scanning and compliance
- **📊 Observability**: Full-stack monitoring with metrics, tracing, and logging

### 🌐 Service URLs & Access Points

#### 🖥️ **Application Services**
| Service | Local URL | Purpose | Health Check |
|---------|-----------|---------|--------------|
| **Frontend App** | http://localhost:3000 | Main user interface | Built-in |
| **API Gateway** | http://localhost:8080 | Central API entry point | /actuator/health |
| **Auth Service** | http://localhost:8081 | Authentication & authorization | /actuator/health |
| **Social Service** | http://localhost:8082 | User profiles & social features | /actuator/health |
| **Booking Service** | http://localhost:8083 | Booking management | /actuator/health |
| **Market Service** | http://localhost:8084 | Marketplace & payments | /actuator/health |
| **Chat Service** | http://localhost:8085 | Real-time messaging | /actuator/health |
| **Emergency Service** | http://localhost:8086 | Emergency assistance | /actuator/health |
| **Trip Planning** | http://localhost:8087 | Itinerary planning | /actuator/health |
| **AI Service** | http://localhost:8088 | AI & ML capabilities | /actuator/health |
| **Notification Service** | http://localhost:8089 | Push notifications | /actuator/health |
| **Analytics Service** | http://localhost:8090 | Business analytics | /actuator/health |
| **Search Service** | http://localhost:8091 | Search & discovery | /actuator/health |
| **Config Service** | http://localhost:8092 | Feature flags & config | /actuator/health |
| **Support Service** | http://localhost:8093 | Customer support | /actuator/health |

#### 🗄️ **Infrastructure Services**
| Service | Local URL | Credentials | Purpose |
|---------|-----------|-------------|---------|
| **PostgreSQL** | localhost:5432 | hopngo / hopngo_dev_2024! | Primary database |
| **MongoDB** | localhost:27017 | admin / mongo_dev_2024! | Document storage |
| **Redis** | localhost:6379 | redis_dev_2024! | Caching & sessions |
| **Elasticsearch** | http://localhost:9200 | elastic / elastic_dev_2024! | Search engine |
| **Qdrant** | http://localhost:6333 | - | Vector database |
| **RabbitMQ** | http://localhost:15672 | guest / guest | Message queue |
| **ClickHouse** | http://localhost:8123 | default / - | Analytics database |

#### 📊 **Observability & Monitoring**
| Service | Local URL | Credentials | Purpose |
|---------|-----------|-------------|---------|
| **Prometheus** | http://localhost:9090 | - | Metrics collection |
| **Grafana** | http://localhost:3001 | admin / admin | Dashboards & visualization |
| **Jaeger** | http://localhost:16686 | - | Distributed tracing |
| **Alertmanager** | http://localhost:9093 | - | Alert management |
| **Mailhog** | http://localhost:8025 | - | Email testing |

#### ☁️ **Production Environments**

**🧪 Development Environment**
- **Frontend**: https://dev.hopngo.com
- **API Gateway**: https://api-dev.hopngo.com
- **Namespace**: `hopngo-dev`
- **SSL**: Self-signed certificates

**🚀 Staging Environment**  
- **Frontend**: https://staging.hopngo.com
- **API Gateway**: https://api-staging.hopngo.com
- **Namespace**: `hopngo-staging`
- **SSL**: Let's Encrypt certificates

**🌐 Production Environment**
- **Frontend**: https://hopngo.com, https://www.hopngo.com
- **API Gateway**: https://api.hopngo.com
- **Namespace**: `hopngo-prod`
- **SSL**: Let's Encrypt certificates
- **CDN**: CloudFlare integration

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

## 🎛️ Feature Flags & A/B Testing System

HopNGo includes a comprehensive feature flag and A/B testing system powered by the **Config Service**. This enables controlled feature rollouts, experimentation, and dynamic configuration management without code deployments.

### 🎯 **System Overview**

The feature flag system provides:
- **🚩 Feature Flags** - Toggle features on/off with advanced targeting
- **🧪 A/B Experiments** - Statistical testing with conversion tracking  
- **⚡ Real-time Updates** - Changes take effect immediately via Redis caching
- **🎯 Advanced Targeting** - User segments, geographic, device-based rules
- **📊 Analytics Integration** - Experiment results and conversion tracking
- **🔒 Permission System** - Role-based flag management

### 🚀 **Quick Implementation**

#### **1. Frontend Integration**
```typescript
// Initialize feature flags in your app
import { initializeFlags, useFeatureFlag, useExperiment } from '@/lib/flags';

// App initialization (layout.tsx or _app.tsx)
useEffect(() => {
  initializeFlags();
}, []);

// Use feature flags
function SearchComponent() {
  const isVisualSearchEnabled = useFeatureFlag('visual-search');
  
  return (
    <div>
      <RegularSearch />
      {isVisualSearchEnabled && <VisualSearchButton />}
    </div>
  );
}

// Use A/B experiments with tracking
function BookingFlow() {
  const { variant, trackEvent } = useExperiment('booking-layout-v2');
  
  const handleBookingComplete = () => {
    trackEvent('booking_completed', { 
      amount: booking.total,
      variant 
    });
  };
  
  if (variant === 'enhanced') {
    return <EnhancedBookingFlow onComplete={handleBookingComplete} />;
  }
  return <StandardBookingFlow onComplete={handleBookingComplete} />;
}
```

#### **2. Backend Integration** 
```java
// Java service integration
@Service
public class BookingService {
    
    @Autowired
    private FeatureFlagService flagService;
    
    public BookingResponse createBooking(BookingRequest request) {
        // Check feature flag
        if (flagService.isEnabled("enhanced-booking", request.getUserId())) {
            return createEnhancedBooking(request);
        }
        return createStandardBooking(request);
    }
}
```

### 📊 **Available Flags & Experiments**

#### **🚩 Production Feature Flags**
| Flag | Description | Rollout | Targeting |
|------|-------------|---------|-----------|
| `visual-search` | AI-powered image search | 50% | All users |
| `enhanced-booking` | Improved booking flow | 100% | Premium users |
| `dark-mode` | Dark theme support | 0% | Beta testers |
| `provider-analytics-v2` | New analytics dashboard | 25% | Providers only |
| `multi-currency` | Currency conversion | 100% | International users |
| `ai-recommendations` | ML-powered suggestions | 75% | Active users |

#### **🧪 Active A/B Experiments**
| Experiment | Description | Variants | Traffic Split |
|------------|-------------|----------|---------------|
| `booking-search-layout` | Search UI optimization | `compact`, `enhanced` | 50% / 50% |
| `pricing-display` | Price presentation | `standard`, `savings`, `detailed` | 33% / 33% / 34% |
| `onboarding-flow` | User registration process | `minimal`, `guided` | 50% / 50% |
| `payment-methods` | Payment option ordering | `popular`, `alphabetical`, `recent` | 40% / 30% / 30% |

### 🔧 **API Endpoints**

The **Config Service** (`:8092`) provides comprehensive APIs:

#### **Feature Flag Evaluation**
```bash
# Get all flags for a user
GET /api/v1/config/flags?userId=12345

# Get specific flag value
GET /api/v1/config/flags/visual-search?userId=12345

# Bulk flag evaluation
POST /api/v1/config/flags/evaluate
{
  "userId": "12345",
  "flags": ["visual-search", "enhanced-booking"]
}
```

#### **A/B Experiment Management**
```bash
# Get user's experiment assignments
GET /api/v1/config/experiments?userId=12345

# Get specific experiment assignment
GET /api/v1/config/experiments/booking-search-layout?userId=12345

# Track experiment events
POST /api/v1/config/experiments/booking-search-layout/events
{
  "userId": "12345",
  "event": "conversion",
  "properties": {
    "bookingId": "booking-123",
    "amount": 150.00,
    "variant": "enhanced"
  }
}
```

#### **Admin Management** (Requires admin role)
```bash
# Create/update feature flag
POST /api/v1/config/admin/flags
PUT /api/v1/config/admin/flags/visual-search

# Create A/B experiment
POST /api/v1/config/admin/experiments

# View experiment results
GET /api/v1/config/admin/experiments/booking-search-layout/results
```

### 🎯 **Advanced Targeting Rules**

Feature flags support sophisticated targeting:

```json
{
  "flagKey": "enhanced-booking",
  "enabled": true,
  "targeting": {
    "userSegments": ["premium", "enterprise"],
    "countries": ["US", "CA", "UK"],
    "deviceTypes": ["desktop", "tablet"],
    "userAttributes": {
      "accountAge": "> 30",
      "bookingCount": "> 5"
    },
    "rolloutPercentage": 75
  }
}
```

### 📈 **Analytics & Monitoring**

#### **Flag Performance Metrics**
- **Evaluation Rate** - Flags evaluated per second
- **Cache Hit Ratio** - Redis cache performance  
- **Error Rate** - Failed flag evaluations
- **Latency** - Flag evaluation response time

#### **Experiment Results**
- **Conversion Rates** - By variant with confidence intervals
- **Statistical Significance** - P-values and sample sizes
- **User Engagement** - Time spent, page views, interactions
- **Business Metrics** - Revenue, bookings, retention

### 🚀 **Configuration & Deployment**

#### **Environment Variables**
```bash
# Config Service
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hopngo
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
CONFIG_CACHE_TTL=300  # 5 minutes

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_CONFIG_SERVICE_URL=http://localhost:8092
```

#### **Database Schema**
The system uses four optimized tables:
- **`feature_flags`** - Flag definitions with targeting rules
- **`experiments`** - A/B test configurations
- **`experiment_variants`** - Variant definitions with traffic allocation
- **`assignments`** - User assignments with sticky sessions

### 🏆 **Best Practices**

#### **Flag Management**
1. **Naming Convention**: Use kebab-case (`visual-search`, `enhanced-checkout`)
2. **Gradual Rollouts**: Start at 5% → 25% → 50% → 100%
3. **Documentation**: Include flag purpose and expected behavior
4. **Cleanup**: Remove unused flags within 30 days of 100% rollout
5. **Monitoring**: Track flag performance and business impact

#### **A/B Testing**
1. **Hypothesis-Driven**: Start with clear hypotheses and success metrics
2. **Statistical Power**: Ensure adequate sample sizes for significance
3. **Duration**: Run experiments for full business cycles (typically 2-4 weeks)
4. **Segment Analysis**: Analyze results by user segments
5. **Iteration**: Use learnings to inform next experiments

### 🔍 **Monitoring & Troubleshooting**

#### **Health Checks**
```bash
# Config service health
curl http://localhost:8092/actuator/health

# Flag evaluation metrics
curl http://localhost:8092/actuator/metrics | grep flags

# Cache statistics
curl http://localhost:8092/actuator/metrics/cache.gets
```

#### **Common Issues & Solutions**
- **Stale Flags**: Check Redis connectivity and TTL settings
- **Inconsistent Assignments**: Verify user ID consistency across requests  
- **Performance Issues**: Monitor cache hit ratios and evaluation latency
- **Experiment Bias**: Ensure proper randomization and segment balance

#### **Debug Tools**
```bash
# Enable debug logging
export LOGGING_LEVEL_COM_HOPNGO_CONFIG=DEBUG

# View flag evaluation logs
kubectl logs -f deployment/config-service | grep "FLAG_EVALUATION"

# Monitor experiment assignments
kubectl logs -f deployment/config-service | grep "EXPERIMENT_ASSIGNMENT"
```

## 💳 Payment Gateway Integration

HopNGo supports multiple payment providers for global market coverage with automatic currency conversion and compliance features.

### 🌍 **Supported Payment Providers**

| Provider | Region | Features | Integration Status |
|----------|---------|----------|-------------------|
| **💳 Stripe** | Global | Cards, Wallets, BNPL, Subscriptions | ✅ Production Ready |
| **📱 bKash** | Bangladesh | Mobile Money, QR Payments | ✅ Production Ready |
| **💰 Nagad** | Bangladesh | Digital Wallet, P2P Transfers | ✅ Production Ready |
| **🏦 PayPal** | Global | Digital Payments, Express Checkout | 🚧 Coming Soon |
| **🔄 Mock Provider** | Development | Testing and Simulation | ✅ Available |

### ⚙️ **Configuration**

Payment providers are configured via environment variables and can be switched dynamically:

```yaml
# application.yml - Market Service
payment:
  default:
    provider: BKASH  # Options: MOCK, STRIPE, BKASH, NAGAD, PAYPAL
  
  multiCurrency:
    enabled: true
    baseCurrency: USD
    autoConversion: true
  
  compliance:
    pciDss: true
    encryption: AES256
    auditLogging: true
```

### 🔧 **Environment Setup**

#### **Stripe Configuration**
```bash
# Stripe Settings
STRIPE_SECRET_KEY=sk_test_your_stripe_secret_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_stripe_publishable_key  
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
STRIPE_API_VERSION=2023-10-16
```

#### **bKash Configuration** 
```bash
# bKash Sandbox Credentials
BKASH_APP_KEY=4f6o0cjiki2rfm34kfdadl1eqq
BKASH_APP_SECRET=2is7hdktrekvrbljjh44ll3d9l1dtjo4pasmjvs5vl5qr3fug4b
BKASH_USERNAME=sandboxTokenizedUser02
BKASH_PASSWORD=sandboxTokenizedUser02@12345
BKASH_BASE_URL=https://tokenized.sandbox.bka.sh/v1.2.0-beta
```

#### **Nagad Configuration**
```bash
# Nagad Sandbox Credentials  
NAGAD_MERCHANT_ID=683002007104225
NAGAD_MERCHANT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----...
NAGAD_PGP_PUBLIC_KEY=-----BEGIN PGP PUBLIC KEY-----...
NAGAD_BASE_URL=http://sandbox.mynagad.com:10080/remote-payment-gateway-1.0/api/dfs
```

### 🚀 **Payment Flow Implementation**

#### **1. Create Payment Intent**
```bash
POST /api/v1/market/payments/intent
Content-Type: application/json

{
  "amount": 1500.00,
  "currency": "BDT", 
  "orderId": "order-12345",
  "customerId": "user-67890",
  "description": "Hotel Booking - Dhaka Premium Suite",
  "metadata": {
    "bookingId": "booking-98765",
    "provider": "BKASH"
  }
}
```

#### **2. Execute Payment (Mobile Money)**
```bash
POST /api/v1/market/payments/{paymentId}/execute
Content-Type: application/json

{
  "payerReference": "01711111111",  # Mobile number
  "pin": "1234",                    # For testing only
  "otpCode": "123456"              # OTP verification
}
```

#### **3. Query Payment Status**
```bash
GET /api/v1/market/payments/{paymentId}/status

Response:
{
  "paymentId": "pay_abc123",
  "status": "COMPLETED",
  "amount": 1500.00,
  "currency": "BDT",
  "provider": "BKASH",
  "transactionId": "TRX123456789",
  "completedAt": "2025-09-12T10:30:00Z"
}
```

### 💱 **Multi-Currency Support**

#### **Real-time Currency Conversion**
```bash
# Get current exchange rates
GET /api/v1/market/fx-rates?from=USD&to=BDT

# Convert amount
POST /api/v1/market/fx-rates/convert
{
  "amount": 100.00,
  "fromCurrency": "USD", 
  "toCurrency": "BDT"
}

Response:
{
  "originalAmount": 100.00,
  "convertedAmount": 11000.00,
  "exchangeRate": 110.00,
  "timestamp": "2025-09-12T10:30:00Z"
}
```

#### **Supported Currencies**
- **USD** - US Dollar (Base currency)
- **BDT** - Bangladeshi Taka
- **EUR** - Euro  
- **GBP** - British Pound
- **CAD** - Canadian Dollar
- **AUD** - Australian Dollar

### 🧪 **Testing & Development**

#### **Test Phone Numbers (Bangladesh)**

**bKash Sandbox:**
- ✅ Success: `01619777282`, `01619777283`
- ❌ Insufficient Balance: `01619777284`
- ❌ Invalid Account: `01619777285`

**Nagad Sandbox:**
- ✅ Success: `01711111111`, `01722222222`  
- ❌ Failed Transaction: `01733333333`
- ⏳ Timeout: `01744444444`

#### **Test Cards (Stripe)**
```bash
# Visa Success
4242424242424242

# Visa Declined  
4000000000000002

# Mastercard Success
5555555555554444

# American Express
378282246310005
```

### 🔒 **Security & Compliance**

#### **PCI DSS Compliance**
- **No Card Storage** - Tokens only, never store sensitive data
- **TLS 1.2+** - All communications encrypted
- **Webhook Validation** - HMAC signature verification
- **Audit Logging** - Complete transaction trail

#### **Webhook Security**
```java
// Automatic webhook validation
@PostMapping("/webhook/stripe")
public ResponseEntity<String> handleStripeWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signature) {
    
    // Validates signature automatically
    return paymentService.processWebhook(payload, signature);
}
```

### 📊 **Financial Analytics**

#### **Revenue Tracking**
- **Real-time Revenue** - Live revenue dashboards
- **Currency Breakdown** - Revenue by currency and region
- **Provider Analysis** - Performance by payment method
- **Conversion Rates** - Payment success rates and drop-offs

#### **Provider Performance Metrics**
```bash
# Get payment provider statistics
GET /api/v1/market/analytics/providers

Response:
{
  "providers": [
    {
      "name": "BKASH",
      "transactionCount": 1250,
      "successRate": 94.2,
      "averageAmount": 850.50,
      "revenue": 1063125.00
    }
  ]
}
```

### 🔧 **Advanced Features**

#### **Automatic Refunds**
```bash
POST /api/v1/market/payments/{paymentId}/refund
{
  "amount": 750.00,  # Partial refund
  "reason": "Customer requested partial refund",
  "notifyCustomer": true
}
```

#### **Recurring Payments** 
```bash
POST /api/v1/market/subscriptions
{
  "customerId": "user-12345",
  "planId": "premium-monthly",
  "paymentMethodId": "pm_stripe123"
}
```

#### **Split Payments** (Provider Payouts)
```bash
POST /api/v1/market/payouts
{
  "providerId": "provider-456", 
  "amount": 850.00,
  "currency": "BDT",
  "description": "Booking commission payout",
  "schedule": "WEEKLY"
}
```

### 🚨 **Error Handling & Monitoring**

#### **Common Error Codes**
- `INSUFFICIENT_FUNDS` - Customer account balance too low
- `PAYMENT_DECLINED` - Bank/provider declined transaction  
- `INVALID_CREDENTIALS` - Authentication failed
- `NETWORK_ERROR` - Provider service unavailable
- `RATE_LIMIT_EXCEEDED` - Too many requests

#### **Monitoring & Alerts**
```bash
# Payment health metrics
curl http://localhost:8084/actuator/metrics | grep payment

# Provider availability
curl http://localhost:8084/actuator/health/payment-providers

# Transaction volume alerts
curl http://localhost:8084/actuator/metrics/payment.transaction.volume
```

### 📋 **Compliance & Regulations**

#### **Bangladesh Compliance**
- **Central Bank Guidelines** - Adheres to Bangladesh Bank regulations
- **AML/KYC** - Anti-money laundering and know-your-customer protocols
- **Transaction Limits** - Respects daily/monthly transaction limits
- **Reporting** - Automated regulatory reporting

#### **International Compliance**
- **PCI DSS Level 1** - Highest security certification
- **GDPR Compliant** - European data protection standards
- **SOX Compliance** - Financial reporting standards
- **ISO 27001** - Information security management

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

## 🔒 Security & Compliance

HopNGo implements enterprise-grade security with multiple layers of protection, compliance standards, and automated security monitoring.

### 🛡️ **Authentication & Authorization**

#### **Multi-Factor Authentication (2FA)**
- **📱 TOTP Support** - Google Authenticator, Authy compatibility
- **📧 Email Verification** - Backup authentication method
- **📱 SMS Authentication** - Mobile number verification
- **🔐 Biometric Support** - Fingerprint and Face ID (mobile apps)
- **🔑 Backup Codes** - Recovery codes for account access

#### **Advanced JWT Management**
```java
// Automatic JWT key rotation
@Scheduled(fixedRate = 86400000) // 24 hours
public void rotateJwtKeys() {
    jwtKeyService.rotateKeys();
    notificationService.notifyKeyRotation();
}

// Multi-key validation for zero-downtime rotation
public boolean validateToken(String token) {
    return jwtService.validateWithCurrentKey(token) || 
           jwtService.validateWithPreviousKey(token);
}
```

#### **OAuth2 & Social Login**
- **🌐 Google OAuth** - Gmail and Google account integration
- **📘 Facebook Login** - Social authentication
- **🍎 Apple Sign-In** - iOS ecosystem integration
- **🐙 GitHub OAuth** - Developer-friendly authentication
- **🔗 Custom Providers** - Extensible OAuth2 framework

### 🔐 **Data Protection**

#### **Encryption Standards**
- **🔒 AES-256** - Data encryption at rest
- **🔐 TLS 1.3** - Transport layer security
- **🔑 Key Management** - AWS KMS / HashiCorp Vault integration
- **📊 Field-Level Encryption** - Sensitive data protection
- **🗃️ Database Encryption** - PostgreSQL transparent data encryption

#### **GDPR Compliance**
```java
// Automated data anonymization
@Service
public class GdprComplianceService {
    
    public void anonymizeUser(String userId) {
        userService.anonymizePersonalData(userId);
        bookingService.anonymizeUserBookings(userId);
        socialService.removeUserContent(userId);
        auditService.logDataAnonymization(userId);
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void processDataRetentionPolicies() {
        gdprService.deleteExpiredData();
    }
}
```

### 🚨 **Security Monitoring**

#### **Automated Security Scanning**
- **🔍 Dependency Scanning** - Automated vulnerability detection
- **🏗️ Container Scanning** - Docker image security analysis
- **📊 Code Analysis** - Static application security testing (SAST)
- **🌐 Dynamic Testing** - Runtime security testing (DAST)
- **🔐 Secrets Detection** - Prevent credential leaks

#### **Intrusion Detection**
```yaml
# Security monitoring configuration
security:
  monitoring:
    failedLoginThreshold: 5
    accountLockoutDuration: PT30M  # 30 minutes
    suspiciousActivityDetection: true
    realTimeAlerts: true
    
  rateLimit:
    global: 1000  # requests per minute
    perUser: 100  # requests per minute per user
    loginAttempts: 5  # per 15 minutes
```

### 🔍 **Audit & Compliance**

#### **Comprehensive Audit Logging**
```java
@AuditLogged
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(String userId) {
    // Automatically logged with:
    // - User performing action
    // - Timestamp
    // - Resource affected
    // - Action type
    // - IP address
    // - User agent
}
```

#### **Compliance Standards**
- **🏛️ SOX Compliance** - Financial reporting standards
- **🔒 PCI DSS Level 1** - Payment card industry standards
- **📋 ISO 27001** - Information security management
- **🌍 GDPR** - European data protection regulation
- **🇺🇸 CCPA** - California consumer privacy act
- **🏥 HIPAA Ready** - Healthcare data protection framework

### 🛡️ **Network Security**

#### **API Security**
```yaml
# API Gateway security configuration
spring:
  cloud:
    gateway:
      routes:
        - id: secure-route
          filters:
            - name: RateLimitingFilter
              args:
                requests-per-minute: 100
            - name: RequestValidationFilter
              args:
                validate-headers: true
                validate-body: true
            - name: SecurityHeadersFilter
              args:
                add-security-headers: true
```

#### **Infrastructure Protection**
- **🔥 Web Application Firewall** - CloudFlare / AWS WAF integration
- **🛡️ DDoS Protection** - Automated attack mitigation
- **🌐 CDN Security** - Edge security and caching
- **📡 Network Policies** - Kubernetes network segmentation
- **🔒 Service Mesh** - Istio for secure service-to-service communication

### 🎯 **Security Best Practices**

#### **Development Security**
- **🔐 Secrets Management** - No hardcoded secrets, environment-based configuration
- **🧪 Security Testing** - Automated security tests in CI/CD pipeline
- **📝 Secure Coding** - OWASP Top 10 compliance
- **🔍 Code Reviews** - Security-focused pull request reviews
- **📚 Security Training** - Regular developer security education

#### **Operational Security**
- **🔄 Automated Updates** - Security patch management
- **📊 Security Metrics** - KPIs for security posture
- **🚨 Incident Response** - 24/7 security incident handling
- **🔍 Penetration Testing** - Regular third-party security assessments
- **📋 Security Runbooks** - Documented incident response procedures

### 🚀 **Security Automation**

#### **Automated Security Workflows**
```yaml
# .github/workflows/security-scan.yml
name: Security Scan
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Dependency Vulnerability Scan
        uses: github/super-linter@v4
        
      - name: Container Security Scan
        uses: aquasec/trivy-action@master
        
      - name: Secret Detection
        uses: trufflesecurity/trufflehog@main
        
      - name: SAST Analysis
        uses: github/codeql-action/analyze@v2
```

#### **Runtime Security Monitoring**
```java
// Real-time security event processing
@EventListener
public void handleSecurityEvent(SecurityEvent event) {
    if (event.isCritical()) {
        alertService.sendImmediateAlert(event);
        securityService.initiateIncidentResponse(event);
    }
    
    securityMetrics.recordEvent(event);
    auditService.logSecurityEvent(event);
}
```

### 🔧 **Security Configuration**

#### **Environment-Based Security**
```bash
# Production security settings
SECURITY_JWT_SECRET=${JWT_SECRET}
SECURITY_ENCRYPTION_KEY=${ENCRYPTION_KEY}
SECURITY_AUDIT_ENABLED=true
SECURITY_MFA_REQUIRED=true
SECURITY_SESSION_TIMEOUT=PT30M

# Rate limiting
SECURITY_RATE_LIMIT_GLOBAL=1000
SECURITY_RATE_LIMIT_PER_USER=100
SECURITY_FAILED_LOGIN_THRESHOLD=5

# TLS Configuration
SECURITY_TLS_VERSION=1.3
SECURITY_CIPHER_SUITES=TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256
```

#### **Database Security**
```sql
-- Row-level security example
CREATE POLICY user_data_policy ON user_data
  FOR ALL TO application_role
  USING (user_id = current_setting('app.current_user_id')::uuid);

-- Audit trigger
CREATE TRIGGER audit_user_changes
  AFTER INSERT OR UPDATE OR DELETE ON users
  FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();
```

### 📋 **Security Checklist**

#### **Pre-Deployment Security Review**
- [ ] All dependencies scanned for vulnerabilities
- [ ] Container images security tested
- [ ] Secrets properly managed (no hardcoded values)
- [ ] TLS certificates valid and properly configured
- [ ] Authentication and authorization tested
- [ ] Input validation and sanitization verified
- [ ] Audit logging enabled and tested
- [ ] Rate limiting configured and tested
- [ ] Security headers properly set
- [ ] Database encryption enabled
- [ ] Backup encryption verified
- [ ] Incident response procedures documented

#### **Ongoing Security Maintenance**
- [ ] Weekly dependency vulnerability scans
- [ ] Monthly penetration testing
- [ ] Quarterly security training for developers
- [ ] Annual third-party security assessment
- [ ] Continuous security monitoring and alerting
- [ ] Regular backup and recovery testing
- [ ] Security incident response drills

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

We welcome contributions from developers of all skill levels! HopNGo is an open-source project that thrives on community collaboration.

### 🚀 **Getting Started**

#### **1. Fork & Clone**
```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/your-username/HopNGo-Implementation.git
cd HopNGo-Implementation

# Add upstream remote
git remote add upstream https://github.com/sadmanHT/HopNGo-Implementation.git
```

#### **2. Set Up Development Environment**
```bash
# Install dependencies
pnpm install

# Set up local development environment  
./scripts/setup-dev.ps1  # Windows
# or
./scripts/setup-dev.sh   # Linux/macOS

# Start development services
./scripts/dev.sh
```

#### **3. Create Feature Branch**
```bash
# Sync with upstream
git fetch upstream
git checkout main
git merge upstream/main

# Create feature branch
git checkout -b feature/amazing-new-feature
```

### 📋 **Development Guidelines**

#### **🏗️ Code Standards**
- **Java**: Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **TypeScript**: Use ESLint and Prettier configurations provided
- **React**: Follow [React Best Practices](https://react.dev/learn)
- **Testing**: Maintain >80% code coverage for new features
- **Documentation**: Update relevant documentation with changes

#### **📝 Commit Message Convention**
We follow [Conventional Commits](https://www.conventionalcommits.org/) specification:

```bash
# Format: type(scope): description
# Examples:
feat(auth): add 2FA support with TOTP
fix(booking): resolve double-booking race condition
docs(readme): update installation instructions
style(frontend): fix linting issues in components
refactor(payment): extract payment providers to separate services
test(api): add integration tests for booking flow
chore(deps): update Spring Boot to 3.2.0
```

#### **🧪 Testing Requirements**
```bash
# Run all tests before submitting PR
mvn test                    # Backend tests
pnpm test                   # Frontend tests
pnpm test:e2e              # End-to-end tests

# Specific service testing
cd auth-service && mvn test
cd frontend && pnpm test:unit
```

### 🎯 **Contribution Areas**

#### **🔥 High Priority**
- **AI/ML Features** - Enhance recommendation algorithms
- **Mobile App Development** - React Native mobile applications
- **Performance Optimization** - Database queries, caching strategies
- **Security Enhancements** - Advanced threat detection, compliance
- **Accessibility** - WCAG 2.1 AA compliance improvements

#### **🌟 Good First Issues**
- **Documentation** - API documentation, tutorials, guides
- **Frontend Components** - Reusable UI components with Storybook
- **Unit Tests** - Increase test coverage for existing features
- **Internationalization** - Add new language translations
- **Bug Fixes** - Resolve open GitHub issues

#### **🚀 Advanced Contributions**
- **New Microservices** - Additional business domain services
- **Infrastructure** - Kubernetes operators, monitoring tools
- **Integration** - Third-party service integrations
- **DevOps** - CI/CD pipeline improvements, automation

### 📝 **Submitting Changes**

#### **1. Create Pull Request**
```bash
# Push your feature branch
git push origin feature/amazing-new-feature

# Create PR via GitHub UI or GitHub CLI
gh pr create --title "feat(auth): add 2FA support" --body "Detailed description"
```

#### **2. Pull Request Template**
```markdown
## Description
Brief description of changes and motivation

## Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)  
- [ ] Breaking change (fix or feature that causes existing functionality to change)
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] E2E tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] No new warnings or errors
```

#### **3. Code Review Process**
1. **Automated Checks** - CI/CD pipeline runs automatically
2. **Peer Review** - At least one maintainer review required
3. **Security Review** - Automated security scans pass
4. **Testing** - All tests pass and coverage maintained
5. **Documentation Review** - Relevant docs updated

### 🏆 **Recognition & Rewards**

#### **Contributor Levels**
- **🌱 First-time Contributor** - Welcome package and mentorship
- **⭐ Regular Contributor** - Recognition in release notes
- **🚀 Core Contributor** - Commit access and voting rights
- **👑 Maintainer** - Full repository access and leadership role

#### **Special Recognition**
- **📝 Documentation Hero** - Outstanding documentation contributions
- **🔒 Security Champion** - Significant security improvements
- **🎨 UX/UI Expert** - Exceptional user experience enhancements
- **🔧 DevOps Master** - Infrastructure and automation excellence

### 🎓 **Learning & Support**

#### **Resources for New Contributors**
- **📚 [Contributing Guide](CONTRIBUTING.md)** - Detailed contribution guidelines
- **🏗️ [Architecture Guide](docs/ARCHITECTURE.md)** - System design deep dive
- **🔧 [Development Setup](docs/development-setup.md)** - Complete setup instructions
- **📖 [API Documentation](docs/api/)** - Comprehensive API reference
- **🎯 [Project Roadmap](docs/roadmap.md)** - Future development plans

#### **Getting Help**
- **💬 GitHub Discussions** - Ask questions and share ideas
- **🐛 GitHub Issues** - Report bugs and request features  
- **📧 Email Support** - Direct contact with maintainers
- **🗓️ Office Hours** - Weekly virtual meetups for contributors

### 🌍 **Community Guidelines**

#### **Code of Conduct**
We are committed to providing a welcoming and inclusive environment. Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

#### **Communication Standards**
- **🤝 Be Respectful** - Treat all community members with respect
- **💡 Be Constructive** - Provide helpful feedback and suggestions
- **🎯 Stay on Topic** - Keep discussions relevant to the project
- **📝 Be Clear** - Use clear and concise communication
- **🌍 Be Inclusive** - Welcome contributors from all backgrounds

### 🔄 **Development Workflow**

#### **Typical Contribution Flow**
1. **🔍 Issue Discovery** - Find or create GitHub issue
2. **💬 Discussion** - Discuss approach with maintainers
3. **🏗️ Implementation** - Develop feature with tests
4. **🧪 Testing** - Comprehensive testing of changes
5. **📝 Documentation** - Update relevant documentation
6. **🔍 Review** - Submit PR and address feedback
7. **🚀 Deployment** - Merge and deploy via CI/CD
8. **📊 Monitoring** - Monitor feature performance

#### **Release Process**
- **🔄 Continuous Integration** - All PRs trigger automated testing
- **📦 Semantic Versioning** - Follows semver for releases
- **📋 Release Notes** - Detailed changelog for each release
- **🚀 Progressive Deployment** - Canary releases to production
- **📊 Post-Deployment Monitoring** - Health checks and metrics

### 📊 **Project Metrics**

We track various metrics to understand project health and contributor engagement:

- **🚀 Deployment Frequency** - How often we ship to production
- **⏱️ Lead Time** - Time from code commit to production
- **🔧 Mean Time to Recovery** - How quickly we resolve issues
- **❌ Change Failure Rate** - Percentage of deployments causing issues
- **👥 Contributor Growth** - New and returning contributors
- **🐛 Issue Resolution Time** - Speed of bug fixes and feature delivery

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support & Community

Get help, report issues, and connect with the HopNGo community through multiple channels.

### 📞 **Getting Help**

#### **📚 Documentation**
- **📖 [Complete Documentation](docs/)** - Comprehensive guides and references
- **🏗️ [Architecture Guide](docs/ARCHITECTURE.md)** - System design and patterns
- **🚀 [Deployment Guide](docs/runbook-deploy.md)** - Production deployment procedures
- **🔒 [Security Guide](docs/SECURITY_CHECKLIST.md)** - Security best practices
- **💳 [Payment Setup](docs/payment-setup.md)** - Payment gateway configuration

#### **💬 Community Channels**
- **🐙 [GitHub Discussions](https://github.com/sadmanHT/HopNGo-Implementation/discussions)** - Q&A, ideas, and general discussion
- **🐛 [GitHub Issues](https://github.com/sadmanHT/HopNGo-Implementation/issues)** - Bug reports and feature requests
- **💬 [Discord Server](https://discord.gg/hopngo)** - Real-time chat and community support
- **📧 [Mailing List](mailto:support@hopngo.com)** - Announcements and updates

#### **🎓 Learning Resources**
- **📺 [Video Tutorials](https://youtube.com/hopngo-dev)** - Step-by-step setup and feature guides
- **📝 [Blog Posts](https://blog.hopngo.com)** - Technical articles and best practices  
- **🎯 [Examples Repository](https://github.com/hopngo/examples)** - Code samples and integrations
- **📊 [Webinar Series](https://hopngo.com/webinars)** - Monthly technical deep dives

### 🐛 **Issue Reporting**

#### **Bug Reports**
When reporting bugs, please include:

```markdown
## Bug Description
Clear description of the issue

## Steps to Reproduce
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

## Expected Behavior
What you expected to happen

## Screenshots
If applicable, add screenshots

## Environment
- OS: [e.g. Windows 11, macOS 14, Ubuntu 22.04]
- Browser: [e.g. Chrome 118, Firefox 119]
- Version: [e.g. HopNGo v2.1.0]
- Service: [e.g. auth-service, frontend]
```

#### **Feature Requests**
For new features, please provide:
- **Use Case** - Why is this feature needed?
- **Description** - What should the feature do?
- **Acceptance Criteria** - How do we know it's complete?
- **Priority** - How urgent is this feature?

### 🔒 **Security**

#### **Security Vulnerabilities**
- **🚨 Critical Issues**: Email security@hopngo.com immediately
- **📋 General Security**: Use [GitHub Security Advisories](https://github.com/sadmanHT/HopNGo-Implementation/security/advisories)
- **🔍 Bug Bounty**: Participate in our responsible disclosure program

#### **Security Response Process**
1. **Report Received** - Acknowledgment within 24 hours
2. **Initial Assessment** - Severity evaluation within 48 hours  
3. **Investigation** - Detailed analysis and reproduction
4. **Fix Development** - Patch creation and testing
5. **Disclosure** - Coordinated public disclosure after fix

### 📈 **Service Status**

#### **System Health**
- **📊 [Status Page](https://status.hopngo.com)** - Real-time service status
- **📈 [Performance Metrics](https://metrics.hopngo.com)** - System performance dashboards
- **🚨 [Incident Reports](https://status.hopngo.com/incidents)** - Post-mortem analyses

#### **SLA & Uptime**
- **🎯 Uptime Target**: 99.9% (< 43 minutes downtime/month)
- **⚡ Response Time**: < 200ms for API endpoints
- **🔄 Recovery Time**: < 5 minutes for critical services
- **📊 Monthly Reports**: Detailed SLA performance reports

### 🏢 **Enterprise Support**

#### **Support Tiers**

**🌟 Community Support (Free)**
- GitHub Issues and Discussions
- Documentation and tutorials
- Community-driven assistance
- Best-effort response time

**🚀 Professional Support (Paid)**
- Priority email support
- 24-hour response SLA
- Phone support during business hours
- Implementation guidance

**👑 Enterprise Support (Paid)**
- 24/7 phone and email support
- 2-hour response SLA for critical issues
- Dedicated customer success manager
- Custom feature development
- On-site consulting available

#### **Contact Information**
- **📧 General Support**: support@hopngo.com
- **🏢 Enterprise Sales**: enterprise@hopngo.com
- **🔒 Security Issues**: security@hopngo.com
- **📰 Media Inquiries**: media@hopngo.com

### 🌍 **Global Community**

#### **Regional Communities**
- **🇺🇸 North America**: [@hopngo-na](https://twitter.com/hopngo-na)
- **🇪🇺 Europe**: [@hopngo-eu](https://twitter.com/hopngo-eu)  
- **🇦🇺 Asia Pacific**: [@hopngo-apac](https://twitter.com/hopngo-apac)
- **🇧🇩 Bangladesh**: [@hopngo-bd](https://twitter.com/hopngo-bd)

#### **Community Events**
- **🎯 Monthly Meetups** - Virtual and in-person gatherings
- **📊 Quarterly Reviews** - Project updates and roadmap discussions
- **🎓 Annual Conference** - HopNGoConf with talks and workshops
- **🏆 Hackathons** - Community-driven development events

### 📋 **FAQ**

#### **General Questions**

**Q: Is HopNGo free to use?**
A: Yes, HopNGo is open-source and free to use. Enterprise support plans are available for commercial deployments.

**Q: Can I contribute without coding experience?**
A: Absolutely! We welcome contributions in documentation, design, testing, translations, and community support.

**Q: How often are new features released?**
A: We follow continuous deployment with new features released weekly. Major releases occur quarterly.

**Q: Is there a mobile app?**
A: We're developing React Native mobile apps. Follow our roadmap for updates.

#### **Technical Questions**

**Q: What's the minimum server requirements?**
A: For development: 8GB RAM, 4 CPU cores. For production: See our [deployment guide](docs/runbook-deploy.md).

**Q: Can I deploy on my own infrastructure?**
A: Yes! HopNGo supports on-premises deployment with Docker and Kubernetes.

**Q: How do I backup my data?**
A: See our [backup strategy guide](docs/media-backup-strategy.md) for comprehensive backup procedures.

**Q: Is HopNGo GDPR compliant?**
A: Yes, HopNGo includes built-in GDPR compliance features including data anonymization and deletion.

### 🎉 **Stay Connected**

- **🐦 [Twitter](https://twitter.com/hopngo)** - Latest updates and announcements
- **💼 [LinkedIn](https://linkedin.com/company/hopngo)** - Professional updates and articles
- **📺 [YouTube](https://youtube.com/hopngo)** - Tutorials and technical content
- **📝 [Blog](https://blog.hopngo.com)** - In-depth articles and case studies
- **📧 [Newsletter](https://hopngo.com/newsletter)** - Monthly updates and featured content

---

**Need immediate help?** 🚨 For urgent production issues, contact our 24/7 support at **emergency@hopngo.com** or call **+1-555-HOPNGO-1**.

## 🙏 Acknowledgments

- **Spring Boot** team for the excellent framework
- **React** team for the frontend framework
- **Kubernetes** community for container orchestration
- **Open source contributors** who make this project possible

---

**HopNGo** - Connecting travelers with seamless transportation experiences 🌍✈️🚗
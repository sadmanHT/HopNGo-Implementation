# HopNGo 🚀

> A comprehensive travel and transportation platform that connects travelers with various transportation options and services.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Node.js](https://img.shields.io/badge/Node.js-20+-green.svg)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5+-blue.svg)](https://www.typescriptlang.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)](https://spring.io/projects/spring-boot)

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
│:81 │     │:82│     │:83 │    │:84│     │:85 │    │:86 │    │:87 │    │:88│
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
| **API Gateway** | 8080 | Spring Boot + Spring Cloud Gateway | Request routing and load balancing | 🚧 Planned |
| **Auth Service** | 8081 | Spring Boot + Spring Security + JWT | Authentication and authorization | 🚧 Planned |
| **Social Service** | 8082 | Spring Boot + JPA | User profiles and social features | 🚧 Planned |
| **Booking Service** | 8083 | Spring Boot + JPA | Trip bookings and reservations | 🚧 Planned |
| **Market Service** | 8084 | Spring Boot + JPA | Marketplace and listings | 🚧 Planned |
| **Chat Service** | 8085 | Spring Boot + WebSocket | Real-time messaging | 🚧 Planned |
| **Trip Planning** | 8086 | Spring Boot + JPA | Itinerary and route planning | 🚧 Planned |
| **AI Service** | 8087 | Spring Boot + Python Integration | AI recommendations and insights | 🚧 Planned |
| **Emergency Service** | 8088 | Spring Boot + JPA | Emergency assistance and alerts | 🚧 Planned |
| **Notification Service** | 8089 | Spring Boot + RabbitMQ | Push notifications and alerts | 🚧 Planned |

## 🛠️ Tech Stack

- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS
- **Backend**: Spring Boot 3, Java 17, Maven
- **Database**: PostgreSQL, Redis, MongoDB
- **Search**: Elasticsearch
- **Message Queue**: RabbitMQ
- **Infrastructure**: Docker, Kubernetes, Helm
- **Observability**: Prometheus, Grafana, Tempo, OpenTelemetry, Logback
- **CI/CD**: GitHub Actions

## 🗺️ Roadmap

### Phase 1: Foundation (Q1 2024)
- [x] Project setup and mono-repo structure
- [x] Frontend application with React + TypeScript
- [ ] API Gateway with Spring Cloud Gateway
- [ ] Authentication service with JWT
- [ ] Basic user management
- [ ] Docker containerization

### Phase 2: Core Services (Q2 2024)
- [ ] Social service for user profiles
- [ ] Booking service for trip reservations
- [ ] Market service for listings
- [ ] Real-time chat functionality
- [ ] Basic trip planning features
- [ ] PostgreSQL database integration

### Phase 3: Advanced Features (Q3 2024)
- [ ] AI-powered recommendations
- [ ] Emergency assistance system
- [ ] Push notification service
- [ ] Advanced search with Elasticsearch
- [ ] Mobile app development
- [ ] Payment integration

### Phase 4: Scale & Optimize (Q4 2024)
- [ ] Kubernetes deployment
- [ ] Monitoring and observability
- [ ] Performance optimization
- [ ] Security hardening
- [ ] Load testing and scaling
- [ ] Multi-region deployment

# HopNGo Development Environment

A comprehensive development environment setup for the HopNGo project.

## 🚀 Installed Tools

### Core Development Tools
- **Git**: 2.51.0.windows.1
- **Java**: OpenJDK 17.0.16 (Temurin)
- **Maven**: Apache Maven 3.9.9
- **Node.js**: v22.19.0 (LTS)
- **pnpm**: 10.15.1

### Container & Orchestration
- **Docker**: 28.3.3
- **kubectl**: v1.32.2
- **Helm**: Installed (PATH configuration needed)
- **Minikube**: Installed (PATH configuration needed)

### Development Environment
- **Visual Studio Code**: 1.103.2
- **IntelliJ IDEA Community**: 2025.2.1

### Utilities
- **curl**: 8.14.1
- **Make**: GnuWin32 3.81 (PATH configuration needed)
- **jq**: 1.8.1 (PATH configuration needed)

## 🔧 Configuration

### Git Configuration
- **User**: HopNGo Developer
- **Email**: developer@hopngo.local
- **Default Branch**: main
- **SSH Key**: Generated (ed25519)

### SSH Key for GitHub
Your public SSH key is located at: `C:\Users\shtpr\.ssh\id_ed25519.pub`

To add it to GitHub:
1. Copy the contents of the public key file
2. Go to GitHub Settings > SSH and GPG keys
3. Click "New SSH key" and paste the content

## 📁 Project Structure

```
HopNGo/
├── README.md
├── .gitignore
├── .editorconfig
├── docker-compose.yml
├── frontend/
├── backend/
├── docs/
└── scripts/
```

## ⚠️ Pending Setup

- **WSL2**: Requires administrator privileges to enable
- **PATH Configuration**: Some tools need manual PATH updates

## Getting Started

### Prerequisites

- Node.js 18+
- pnpm 8+
- Docker & Docker Compose
- Git

### Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd HopNGo

# Install dependencies
pnpm install

# Start infrastructure services
./scripts/dev.sh

# Start development server
pnpm dev
```

## 📊 Observability & Monitoring

HopNGo includes a comprehensive observability stack for monitoring, tracing, and logging across all microservices.

### Features

- **Metrics Collection**: Prometheus scrapes metrics from all Spring Boot services via Actuator endpoints
- **Distributed Tracing**: OpenTelemetry and Tempo provide end-to-end request tracing
- **Visualization**: Grafana dashboards for metrics and trace analysis
- **Structured Logging**: JSON-formatted logs with correlation IDs for better debugging
- **Health Monitoring**: Actuator health endpoints for service status monitoring

### Metrics Available

Each microservice exposes the following metrics:
- JVM metrics (memory, threads, garbage collection)
- HTTP request metrics (duration, status codes, throughput)
- Database connection pool metrics
- Custom business metrics
- Spring Boot Actuator metrics

### Accessing Observability Tools

1. **Prometheus**: http://localhost:9090
   - Query metrics and view targets
   - Check service discovery and scraping status

2. **Grafana**: http://localhost:3001 (admin/admin)
   - Pre-configured dashboards for JVM and HTTP metrics
   - Custom dashboards for business metrics
   - Alerting and notification setup

3. **Jaeger**: http://localhost:16686
   - Distributed tracing visualization
   - Service dependency mapping
   - Performance bottleneck identification

### Configuration

Observability is configured through:
- `application.yml` in each service for Actuator and OpenTelemetry
- `prometheus.yml` for scraping configuration
- `grafana/provisioning/` for dashboard and datasource setup
- `tempo.yml` for distributed tracing configuration

### Troubleshooting

- **Missing Metrics**: Check if services are running and Actuator endpoints are enabled
- **No Traces**: Verify OpenTelemetry collector is running on port 4318
- **Grafana Issues**: Ensure Prometheus datasource is configured correctly

## Infrastructure Setup

### Local Development Dependencies

The project uses Docker Compose to manage local infrastructure dependencies:

#### Core Infrastructure
- **PostgreSQL 15** - Primary database (port 5432)
- **MongoDB 7** - Document database (port 27017)
- **Redis 7** - Caching and sessions (port 6379)
- **RabbitMQ 3** - Message queue (ports 5672, 15672)
- **Mailhog** - Email testing (ports 1025, 8025)

#### Observability Stack
- **Prometheus** - Metrics collection and storage (port 9090)
- **Grafana** - Metrics visualization and dashboards (port 3001)
- **Tempo** - Distributed tracing backend (port 3200)
- **OpenTelemetry Collector** - Telemetry data collection (port 4318)
- **Jaeger** - Tracing UI and query service (port 16686)

### Starting Infrastructure

```bash
# Start all services
./scripts/dev.sh

# Or manually with Docker Compose
cd infra/compose
docker compose up -d
```

### Stopping Infrastructure

```bash
# Stop all services
./scripts/down.sh

# Stop and remove volumes (⚠️ deletes all data)
./scripts/down.sh --volumes

# Or manually with Docker Compose
cd infra/compose
docker compose down
```

### Health Checks

```bash
# Check service status
cd infra/compose
docker compose ps

# View service logs
docker compose logs [service-name]

# Test database connections
./scripts/test-postgres.sh
./scripts/test-mongo.sh
```

### Service URLs

#### Application Services
- **Frontend**: http://localhost:5173
- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **Social Service**: http://localhost:8082
- **Booking Service**: http://localhost:8083
- **Market Service**: http://localhost:8084
- **Chat Service**: http://localhost:8085
- **Trip Planning**: http://localhost:8086
- **AI Service**: http://localhost:8087
- **Emergency Service**: http://localhost:8088
- **Notification Service**: http://localhost:8089

#### Infrastructure Services
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Mailhog Web UI**: http://localhost:8025
- **PostgreSQL**: localhost:5432 (hopngo/hopngo_dev_2024!)
- **MongoDB**: localhost:27017 (admin/mongo_dev_2024!)
- **Redis**: localhost:6379 (redis_dev_2024!)

#### Observability Services
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jaeger UI**: http://localhost:16686
- **Tempo**: http://localhost:3200

### Environment Configuration

Database credentials and configuration are stored in `infra/compose/.env`:

```env
# PostgreSQL
POSTGRES_DB=hopngo
POSTGRES_USER=hopngo
POSTGRES_PASSWORD=hopngo_dev_2024!

# MongoDB
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=mongo_dev_2024!

# Redis
REDIS_PASSWORD=redis_dev_2024!

# RabbitMQ
RABBITMQ_DEFAULT_USER=hopngo
RABBITMQ_DEFAULT_PASS=rabbit_dev_2024!

# Observability
GRAFANA_ADMIN_PASSWORD=admin
PROMETHEUS_RETENTION_TIME=15d
```

## 🎯 Next Steps

1. Enable WSL2 (requires admin privileges)
2. Configure IDE extensions
3. Set up project-specific configurations
4. Initialize Git repository

---

*Environment setup completed on $(Get-Date)*
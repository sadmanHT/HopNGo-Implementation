# HopNGo 🚀

> A comprehensive travel and transportation platform that connects travelers with various transportation options and services.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Node.js](https://img.shields.io/badge/Node.js-20+-green.svg)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5+-blue.svg)](https://www.typescriptlang.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)](https://spring.io/projects/spring-boot)

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
- **Monitoring**: Prometheus, Grafana
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

## 🎯 Next Steps

1. Enable WSL2 (requires admin privileges)
2. Configure IDE extensions
3. Set up project-specific configurations
4. Initialize Git repository

---

*Environment setup completed on $(Get-Date)*
# HopNGo Architecture Documentation

## Overview

HopNGo is a modern travel and transportation platform built with a microservices architecture, featuring a React frontend and Spring Boot backend.

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│                 │    │                 │    │                 │
│   Frontend      │    │   Nginx         │    │   Backend       │
│   (React)       │◄──►│   (Reverse      │◄──►│   (Spring Boot) │
│   Port: 3000    │    │   Proxy)        │    │   Port: 8080    │
│                 │    │   Port: 80/443  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐                            ┌─────────────────┐
│                 │                            │                 │
│   Redis         │◄───────────────────────────┤   PostgreSQL    │
│   (Cache)       │                            │   (Database)    │
│   Port: 6379    │                            │   Port: 5432    │
│                 │                            │                 │
└─────────────────┘                            └─────────────────┘
```

## Technology Stack

### Frontend
- **Framework**: React 18+ with TypeScript
- **Build Tool**: Vite
- **State Management**: Redux Toolkit / Zustand
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios
- **Testing**: Jest + React Testing Library

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: PostgreSQL with PostGIS
- **Cache**: Redis
- **Security**: Spring Security + JWT
- **Testing**: JUnit 5 + Testcontainers

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Reverse Proxy**: Nginx
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Orchestration**: Kubernetes (production)

## Project Structure

```
HopNGo/
├── frontend/                 # React frontend application
│   ├── src/
│   │   ├── components/       # Reusable UI components
│   │   ├── pages/           # Page components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── services/        # API service layer
│   │   ├── store/           # State management
│   │   ├── utils/           # Utility functions
│   │   └── types/           # TypeScript type definitions
│   ├── public/              # Static assets
│   └── package.json         # Frontend dependencies
│
├── backend/                  # Spring Boot backend application
│   ├── src/main/java/       # Java source code
│   │   └── com/hopngo/
│   │       ├── config/      # Configuration classes
│   │       ├── controller/  # REST controllers
│   │       ├── service/     # Business logic layer
│   │       ├── repository/  # Data access layer
│   │       ├── entity/      # JPA entities
│   │       ├── dto/         # Data transfer objects
│   │       └── security/    # Security configuration
│   ├── src/main/resources/  # Configuration files
│   └── pom.xml             # Maven dependencies
│
├── docs/                    # Documentation
├── scripts/                 # Setup and utility scripts
├── nginx/                   # Nginx configuration
├── docker-compose.yml       # Local development setup
├── package.json            # Root workspace configuration
└── README.md               # Project overview
```

## Database Schema

### Core Entities

1. **Users**: User accounts and authentication
2. **Routes**: Transportation routes between locations
3. **Vehicles**: Transportation vehicles (buses, trains, etc.)
4. **Schedules**: Timetables for routes with specific vehicles
5. **Bookings**: User reservations for specific schedules
6. **Payments**: Payment processing and tracking

### Key Relationships
- Users can have multiple Bookings
- Schedules belong to Routes and use Vehicles
- Bookings reference Schedules and Users
- Payments are linked to Bookings

## API Design

### RESTful Endpoints

```
# Authentication
POST   /api/auth/login
POST   /api/auth/register
POST   /api/auth/refresh
POST   /api/auth/logout

# Users
GET    /api/users/profile
PUT    /api/users/profile
GET    /api/users/{id}

# Routes
GET    /api/routes
GET    /api/routes/{id}
GET    /api/routes/search?origin=X&destination=Y

# Schedules
GET    /api/schedules
GET    /api/schedules/{id}
GET    /api/schedules/route/{routeId}

# Bookings
GET    /api/bookings
POST   /api/bookings
GET    /api/bookings/{id}
PUT    /api/bookings/{id}
DELETE /api/bookings/{id}

# Payments
POST   /api/payments/process
GET    /api/payments/{id}
GET    /api/payments/booking/{bookingId}
```

## Security

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (RBAC)
- Password hashing with BCrypt
- CORS configuration for cross-origin requests

### Data Protection
- Input validation and sanitization
- SQL injection prevention with JPA
- XSS protection headers
- Rate limiting on API endpoints

## Development Workflow

### Local Development
1. Run `scripts/setup-dev.ps1` for initial setup
2. Start services with `pnpm run dev`
3. Access frontend at `http://localhost:3000`
4. Access API at `http://localhost:8080/api`

### Testing Strategy
- **Unit Tests**: Individual component/service testing
- **Integration Tests**: API endpoint testing
- **E2E Tests**: Full user journey testing
- **Performance Tests**: Load and stress testing

### CI/CD Pipeline
1. **Code Quality**: Linting, formatting, type checking
2. **Testing**: Unit, integration, and E2E tests
3. **Security**: Dependency vulnerability scanning
4. **Build**: Docker image creation
5. **Deploy**: Kubernetes deployment

## Monitoring & Observability

### Logging
- Structured logging with JSON format
- Centralized log aggregation
- Log levels: ERROR, WARN, INFO, DEBUG

### Metrics
- Application performance metrics
- Business metrics (bookings, revenue)
- Infrastructure metrics (CPU, memory, disk)

### Health Checks
- Application health endpoints
- Database connectivity checks
- External service dependency checks

## Scalability Considerations

### Horizontal Scaling
- Stateless application design
- Load balancing with Nginx
- Database connection pooling
- Redis for session management

### Performance Optimization
- Database indexing strategy
- Query optimization
- Caching layers (Redis, CDN)
- Image optimization and compression

### Future Enhancements
- Microservices decomposition
- Event-driven architecture
- Message queues (RabbitMQ/Kafka)
- API Gateway implementation

## Deployment

### Development Environment
- Docker Compose for local development
- Hot reloading for rapid development
- Local database and Redis instances

### Production Environment
- Kubernetes orchestration
- Managed database services
- CDN for static assets
- SSL/TLS termination
- Auto-scaling policies

## Contributing

See the main README.md for contribution guidelines and development setup instructions.
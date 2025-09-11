# HopNGo Development Makefile
# Provides convenient commands for development workflow

.PHONY: help install dev build test clean openapi sdk docs postman all

# Default target
help:
	@echo "HopNGo Development Commands"
	@echo "=========================="
	@echo ""
	@echo "Setup:"
	@echo "  make install     Install all dependencies"
	@echo "  make dev         Start development environment"
	@echo ""
	@echo "Build & Test:"
	@echo "  make build       Build all services and frontend"
	@echo "  make test        Run all tests"
	@echo "  make clean       Clean build artifacts"
	@echo ""
	@echo "DevEx & Docs:"
	@echo "  make openapi     Generate OpenAPI specifications"
	@echo "  make sdk         Generate TypeScript SDKs"
	@echo "  make docs        Build documentation site"
	@echo "  make postman     Generate Postman collections"
	@echo "  make all         Run openapi + sdk + docs + postman"
	@echo ""
	@echo "Services:"
	@echo "  make services    Start all backend services"
	@echo "  make frontend    Start frontend development server"
	@echo ""

# Installation
install:
	@echo "📦 Installing dependencies..."
	@echo "Installing OpenAPI aggregator..."
	cd tools/openapi-aggregator && npm install
	@echo "Installing SDK generator..."
	cd tools/sdk-generator && npm install
	@echo "Installing Postman generator..."
	cd tools/postman-generator && npm install
	@echo "Installing documentation site..."
	cd docs/site && npm install
	@echo "Installing frontend..."
	cd frontend && npm install
	@echo "✅ All dependencies installed!"

# Development
dev: services
	@echo "🚀 Starting development environment..."
	@echo "Backend services starting with Docker Compose..."
	@echo "Frontend will be available at http://localhost:3000"
	@echo "API Gateway at http://localhost:8080"

services:
	@echo "🐳 Starting backend services..."
	docker-compose up -d
	@echo "⏳ Waiting for services to be ready..."
	@timeout /t 30 /nobreak > nul
	@echo "✅ Services should be ready!"

frontend:
	@echo "⚛️ Starting frontend development server..."
	cd frontend && npm run dev

# Build
build: build-services build-frontend build-docs
	@echo "✅ All builds completed!"

build-services:
	@echo "🏗️ Building backend services..."
	docker-compose build

build-frontend:
	@echo "⚛️ Building frontend..."
	cd frontend && npm run build

build-docs:
	@echo "📚 Building documentation..."
	cd docs/site && npm run build

# Testing
test: test-backend test-frontend test-e2e
	@echo "✅ All tests completed!"

test-backend:
	@echo "🧪 Running backend tests..."
	@echo "Running auth-service tests..."
	cd auth-service && .\gradlew test
	@echo "Running social-service tests..."
	cd social-service && .\gradlew test
	@echo "Running booking-service tests..."
	cd booking-service && .\gradlew test
	@echo "Running chat-service tests..."
	cd chat-service && .\gradlew test

test-frontend:
	@echo "⚛️ Running frontend tests..."
	cd frontend && npm run test

test-e2e:
	@echo "🎭 Running E2E tests..."
	cd testing/e2e && npm run test

# DevEx & Documentation
openapi:
	@echo "📋 Generating OpenAPI specifications..."
	@echo "Ensuring services are running..."
	@$(MAKE) services
	@echo "Fetching OpenAPI specs..."
	cd tools/openapi-aggregator && npm run start
	@echo "✅ OpenAPI specs generated!"

sdk: openapi
	@echo "🔧 Generating TypeScript SDKs..."
	cd tools/sdk-generator && npm run start
	@echo "✅ SDKs generated!"

docs: openapi
	@echo "📚 Building documentation site..."
	cd docs/site && npm run build
	@echo "✅ Documentation built!"

docs-dev:
	@echo "📚 Starting documentation development server..."
	cd docs/site && npm run start

postman: openapi
	@echo "📮 Generating Postman collections..."
	cd tools/postman-generator && npm run start
	@echo "✅ Postman collections generated!"

all: openapi sdk docs postman
	@echo "🎉 All DevEx tools completed!"
	@echo ""
	@echo "Generated:"
	@echo "  📋 OpenAPI specs in docs/openapi/"
	@echo "  🔧 TypeScript SDKs in frontend/src/lib/sdk/"
	@echo "  📚 Documentation site in docs/site/build/"
	@echo "  📮 Postman collections in docs/postman/"

# Cleanup
clean:
	@echo "🧹 Cleaning build artifacts..."
	@echo "Stopping services..."
	docker-compose down
	@echo "Cleaning frontend build..."
	@if exist "frontend\dist" rmdir /s /q "frontend\dist"
	@echo "Cleaning documentation build..."
	@if exist "docs\site\build" rmdir /s /q "docs\site\build"
	@echo "Cleaning generated files..."
	@if exist "docs\openapi" rmdir /s /q "docs\openapi"
	@if exist "frontend\src\lib\sdk" rmdir /s /q "frontend\src\lib\sdk"
	@if exist "docs\postman" rmdir /s /q "docs\postman"
	@echo "✅ Cleanup completed!"

# Utility targets
status:
	@echo "📊 Service Status:"
	docker-compose ps

logs:
	@echo "📜 Service Logs:"
	docker-compose logs --tail=50 -f

restart:
	@echo "🔄 Restarting services..."
	docker-compose restart

stop:
	@echo "🛑 Stopping services..."
	docker-compose down

# Database operations
db-migrate:
	@echo "🗄️ Running database migrations..."
	docker-compose exec auth-service ./gradlew flywayMigrate
	docker-compose exec social-service ./gradlew flywayMigrate
	docker-compose exec booking-service ./gradlew flywayMigrate

db-seed:
	@echo "🌱 Seeding database..."
	@echo "Running seed scripts..."
	docker-compose exec postgres psql -U hopngo -d hopngo_auth -f /docker-entrypoint-initdb.d/01-auth-users.sql
	docker-compose exec postgres psql -U hopngo -d hopngo_booking -f /docker-entrypoint-initdb.d/02-booking-listings.sql
	@echo "✅ Database seeded!"

# Quick development workflow
quick-start: install services
	@echo "⚡ Quick start completed!"
	@echo "Services running, ready for development."

full-setup: install services openapi sdk docs postman
	@echo "🎉 Full setup completed!"
	@echo "Everything is ready for development!"
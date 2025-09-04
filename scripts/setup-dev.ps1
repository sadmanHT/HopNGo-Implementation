# HopNGo Development Environment Setup Script

Write-Host "HopNGo Development Environment Setup" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green
Write-Host ""

# Function to check if a command exists
function Test-Command {
    param([string]$Command)
    try {
        Get-Command $Command -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

Write-Host "Checking prerequisites..." -ForegroundColor Yellow

# Verify tools
if (Test-Command "git") {
    $gitVersion = git --version
    Write-Host "Git: $gitVersion" -ForegroundColor Green
} else {
    Write-Host "Git: Not found" -ForegroundColor Red
}

if (Test-Command "java") {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "Java: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "Java: Not found" -ForegroundColor Red
}

if (Test-Command "node") {
    $nodeVersion = node --version
    Write-Host "Node.js: $nodeVersion" -ForegroundColor Green
} else {
    Write-Host "Node.js: Not found" -ForegroundColor Red
}

if (Test-Command "docker") {
    $dockerVersion = docker --version
    Write-Host "Docker: $dockerVersion" -ForegroundColor Green
} else {
    Write-Host "Docker: Not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "Installing project dependencies..." -ForegroundColor Yellow

# Install root dependencies
if (Test-Command "pnpm") {
    Write-Host "Installing with pnpm..."
    pnpm install
} elseif (Test-Command "npm") {
    Write-Host "Using npm..."
    npm install
} else {
    Write-Host "Neither pnpm nor npm found!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Setting up Docker environment..." -ForegroundColor Yellow

# Check if Docker is running
try {
    docker info | Out-Null
    Write-Host "Docker is running" -ForegroundColor Green
    
    # Build and start services
    Write-Host "Starting Docker services..."
    docker-compose up -d postgres redis
    
    Write-Host "Database and Redis services started" -ForegroundColor Green
} catch {
    Write-Host "Docker is not running" -ForegroundColor Red
    Write-Host "Please start Docker Desktop and run this script again." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Creating environment files..." -ForegroundColor Yellow

# Create frontend .env.local
if (-not (Test-Path "frontend\.env.local")) {
    $frontendEnv = "REACT_APP_API_URL=http://localhost:8080/api`nREACT_APP_ENV=development"
    $frontendEnv | Out-File -FilePath "frontend\.env.local" -Encoding UTF8
    Write-Host "Created frontend .env.local" -ForegroundColor Green
} else {
    Write-Host "frontend .env.local already exists" -ForegroundColor Green
}

# Create backend .env.local
if (-not (Test-Path "backend\.env.local")) {
    $backendEnv = "SPRING_PROFILES_ACTIVE=local`nDATABASE_URL=jdbc:postgresql://localhost:5432/hopngo`nDATABASE_USERNAME=hopngo_user`nDATABASE_PASSWORD=hopngo_password`nREDIS_HOST=localhost`nREDIS_PORT=6379"
    $backendEnv | Out-File -FilePath "backend\.env.local" -Encoding UTF8
    Write-Host "Created backend .env.local" -ForegroundColor Green
} else {
    Write-Host "backend .env.local already exists" -ForegroundColor Green
}

Write-Host ""
Write-Host "Setup completed!" -ForegroundColor Green
Write-Host "===============" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Run 'pnpm run dev' to start development servers" -ForegroundColor White
Write-Host "2. Open http://localhost:3000 for frontend" -ForegroundColor White
Write-Host "3. API will be available at http://localhost:8080/api" -ForegroundColor White
Write-Host "4. Database: PostgreSQL on localhost:5432" -ForegroundColor White
Write-Host "5. Redis: localhost:6379" -ForegroundColor White
Write-Host ""
Write-Host "Happy coding!" -ForegroundColor Green
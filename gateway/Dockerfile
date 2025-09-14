FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY .mvn/ .mvn/
COPY mvnw.cmd pom.xml ./

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create final image
FROM eclipse-temurin:17-jre

# Add non-root user for security
RUN groupadd -r gateway && useradd -r -g gateway gateway

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY --from=0 /app/target/gateway-*.jar app.jar

# Change ownership to non-root user
RUN chown -R gateway:gateway /app
USER gateway

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
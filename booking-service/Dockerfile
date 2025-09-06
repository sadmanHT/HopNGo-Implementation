FROM openjdk:17-jdk-slim as builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create final image
FROM openjdk:17-jre-slim

# Add non-root user for security
RUN groupadd -r bookingservice && useradd -r -g bookingservice bookingservice

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY --from=builder /app/target/booking-service-*.jar app.jar

# Change ownership to non-root user
RUN chown -R bookingservice:bookingservice /app
USER bookingservice

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
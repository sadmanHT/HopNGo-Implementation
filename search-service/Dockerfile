FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Run the application
EXPOSE 8091
CMD ["java", "-jar", "target/search-service-0.0.1-SNAPSHOT.jar"]
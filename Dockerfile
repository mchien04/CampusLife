# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY . .
RUN mvn -DskipTests=true clean package

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory and subdirectories
# Code will auto-create these, but creating them here ensures they exist on startup
RUN mkdir -p uploads/activities uploads/email-attachments uploads/submissions

# If using Render Disk, uncomment the line below to mount persistent storage
# VOLUME ["/app/uploads"]

# Expose port (Render will set PORT env variable)
EXPOSE 8080

# Health check (optional - Render will handle health checks)
# Uncomment and install curl if you want Docker-level health checks
# RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#   CMD curl -f http://localhost:${PORT:-8080}/api/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]


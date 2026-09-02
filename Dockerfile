# --- Stage 1: Build the JAR ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy project files
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Stage 2: Runtime image ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S teamui && adduser -S teamui -G teamui

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

RUN chown -R teamui:teamui /app
USER teamui

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

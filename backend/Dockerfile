# syntax=docker/dockerfile:1

# --- Build stage: compile and package the fat jar ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cache dependencies: copy only the build descriptors first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Then the sources
COPY src/ src/
RUN ./mvnw -B -q -DskipTests clean package

# --- Runtime stage: slim JRE with just the jar ---
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as a non-root user
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
# App Runner and most PaaS hosts inject PORT; default to 8080 locally.
ENV SERVER_PORT=8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-$SERVER_PORT} -jar app.jar"]

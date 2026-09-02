# --- Build stage ---
# Tests are NOT run here; PR CI (ci.yml) runs `mvn verify` with Testcontainers.
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace

# Cache dependencies: copy pom first, download, then copy sources
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:26-jre AS runtime

# curl for the container healthcheck (temurin images do not ship it);
# drop pebble (Canonical init daemon baked into the Ubuntu base) — unused and its
# bundled Go stdlib carries HIGH CVEs that would fail the Trivy gate.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && apt-get purge -y pebble || rm -f /usr/bin/pebble \
 && rm -rf /var/lib/apt/lists/*

# Non-root user
RUN groupadd -r app && useradd -r -g app app

WORKDIR /app
COPY --from=build /workspace/target/reservation-system-*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# Production configuration is supplied externally (env vars / application-prod.yml profile).
# Prod profile forbids the dev-key fallback (app.security.allow-dev-key=false).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

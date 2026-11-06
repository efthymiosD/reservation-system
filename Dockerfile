# syntax=docker/dockerfile:1
# --- Build stage ---
# Tests are NOT run here; PR CI (ci.yml) runs `mvn verify` with Testcontainers.
#
# GitHub Packages (decoupledx-common) requires authentication even for
# reading: the token arrives as a BuildKit SECRET MOUNT (`github_token`,
# wired by release.yml / the e2e compose stack; locally pass
# GITHUB_TOKEN=... via compose secrets or --secret). The server credentials
# file is created and deleted WITHIN each RUN, so the token is never stored
# in an image layer or the buildx cache. GPR_USERNAME only carries the
# account name — not sensitive — as a normal build arg.
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace
ARG GPR_USERNAME=

# Cache dependencies: copy pom first, download, then copy sources
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

RUN --mount=type=secret,id=github_token,required=true \
    mkdir -p /root/.m2 \
 && printf '<settings><servers><server><id>github</id><username>%s</username><password>%s</password></server></servers></settings>' \
        "$GPR_USERNAME" "$(cat /run/secrets/github_token)" > /root/.m2/settings.xml \
 && ./mvnw -B -q dependency:go-offline ; \
    rc=$? ; rm -f /root/.m2/settings.xml ; exit $rc

COPY src src
RUN --mount=type=secret,id=github_token,required=true \
    mkdir -p /root/.m2 \
 && printf '<settings><servers><server><id>github</id><username>%s</username><password>%s</password></server></servers></settings>' \
        "$GPR_USERNAME" "$(cat /run/secrets/github_token)" > /root/.m2/settings.xml \
 && ./mvnw -B -q -DskipTests package ; \
    rc=$? ; rm -f /root/.m2/settings.xml ; exit $rc

# --- Runtime stage ---
# JRE version matches the build JDK (temurin 26) — deliberate pairing;
# the Maven build targets Java 26, so the runtime must be >= 26.
FROM eclipse-temurin:27-jre AS runtime

# curl for the container healthcheck (temurin images do not ship it);
# drop /usr/bin/pebble (Canonical init daemon baked into the Ubuntu base, not a
# dpkg package) — unused and its bundled Go stdlib carries HIGH CVEs that would
# fail the Trivy gate.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /usr/bin/pebble

# Non-root user
RUN groupadd -r app && useradd -r -g app app \
 && mkdir -p /app/logs \
 && chown -R app:app /app/logs

WORKDIR /app
COPY --from=build /workspace/target/reservation-system-*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# Production configuration is supplied externally (env vars / application-prod.yml profile).
# Prod profile forbids the dev-key fallback (app.security.allow-dev-key=false).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

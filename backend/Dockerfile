# syntax=docker/dockerfile:1
#
# Pokemon API — multi-stage image (CLAUDE.md constraint 6).
#   1. build:   compile and package the Spring Boot jar with Maven on a full JDK 21.
#   2. extract: split the jar into Spring Boot's layers, so a code change only rebuilds the small top layer.
#   3. runtime: JRE 21 only (no compiler, no Maven, no sources), running as a non-root user.
#
# Tests are not run here: unit tests run with `./mvnw test` and integration tests need Docker themselves
# (Testcontainers), see README "Testing". The image build only packages.

# ---- 1. build -------------------------------------------------------------------------------------------
# Same Maven version as the wrapper (.mvn/wrapper/maven-wrapper.properties); the official image avoids
# downloading Maven at build time.
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

# The pom first: dependencies are resolved into a BuildKit cache that survives rebuilds.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && cp target/pokemon-api-*.jar application.jar

# ---- 2. extract layers ----------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /workspace
COPY --from=build /workspace/application.jar ./
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- 3. runtime -----------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app -H -D app
WORKDIR /application

# Least to most frequently changed, so Docker reuses the dependency layers between code changes.
COPY --from=extract /workspace/extracted/dependencies/ ./
COPY --from=extract /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract /workspace/extracted/application/ ./

USER app
EXPOSE 8080

# /actuator/health is public and includes the database (HealthEndpointIT); busybox wget is in the image.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

# MaxRAMPercentage sizes the heap from the container's memory limit instead of the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]

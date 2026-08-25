# ──────────────────────────────────────────────
# Stage 1: Build
# NOTE: Debian/glibc base (NOT alpine) is required so the protoc / protoc-gen-grpc-java
# codegen binaries pulled by protobuf-maven-plugin can execute (they are glibc-linked).
# ──────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Builds the JAR and generates the gRPC/protobuf sources (bound to generate-sources)
RUN mvn clean package -DskipTests

# ──────────────────────────────────────────────
# Stage 2: Run
# ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# 8080 = HTTP/actuator, 9090 = gRPC server
EXPOSE 8080 9090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

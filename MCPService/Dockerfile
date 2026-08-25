# Stage 1: Build
# NOTE: Debian/glibc base (NOT alpine) is required so the protoc / protoc-gen-grpc-java
# codegen binaries pulled by protobuf-maven-plugin can execute (they are glibc-linked).
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Also generates the gRPC/protobuf sources (protobuf-maven-plugin binds to generate-sources)
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine

# Add a non-root user for security in production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

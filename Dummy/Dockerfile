# Stage 1: Build
# Multi-module reactor: builds the dummy-service (shipped) and the sample `app` module.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY dummy-service dummy-service
COPY app app
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app
COPY --from=build /app/dummy-service/target/*-SNAPSHOT.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

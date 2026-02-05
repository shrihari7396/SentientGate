# ---------- 1. BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace/app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests clean package


# ---------- 2. RUNTIME STAGE ----------
FROM gcr.io/distroless/java21

WORKDIR /app

COPY --from=builder /workspace/app/target/*.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

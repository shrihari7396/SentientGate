FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew

# Generate Java classes from .proto files
RUN ./gradlew generateProto --no-daemon

# Build Spring Boot JAR
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine

# Add a non-root user for security in production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

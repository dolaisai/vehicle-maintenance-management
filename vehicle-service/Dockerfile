# Multistage build. Stage 1 compiles, stage 2 ships only the jar.
# Build from the repo root so the parent pom is in the context:
#   docker build -t vehicle-service:slim -f ./vehicle-service/Dockerfile .

# ---- stage 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build-compile
WORKDIR /app
COPY . .
RUN mvn -f vehicle-service/pom.xml clean package

# ---- stage 2: run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# Source path is inside stage 1. Destination is an explicit filename, so the
# jar lands at /app/app.jar no matter what version the pom says.
COPY --from=build-compile /app/vehicle-service/target/vehicle-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]

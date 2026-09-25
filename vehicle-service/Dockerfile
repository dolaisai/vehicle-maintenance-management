# Multistage build. Stage 1 compiles, stage 2 ships only the jar.
# Build from the repo root so the parent pom is in the context:
#   docker build -t vehicle-service:slim -f ./vehicle-service/Dockerfile .

# ---- stage 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the poms first. These change rarely, so the download layer below
# stays cached through every ordinary code change.
COPY pom.xml .
COPY vehicle-service/pom.xml vehicle-service/
RUN mvn -B -f vehicle-service/pom.xml dependency:go-offline

# Source copied last, because it changes constantly. Only this layer and the
# build below it rebuild when you edit Java.
COPY vehicle-service/src vehicle-service/src
RUN mvn -B -f vehicle-service/pom.xml clean package

# ---- stage 2: run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# Source path is inside stage 1. Destination is an explicit filename, so the
# jar lands at /app/app.jar no matter what version the pom says.
COPY --from=build /app/vehicle-service/target/vehicle-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]

# Build stage: compile the Spring Boot jar with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime stage: small JRE image
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/storage
COPY --from=build /build/target/*.jar app.jar
ENV STORAGE_ROOT=/app/storage
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ---------- Stage 1: build the application ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy only the build files first, so downloaded dependencies are cached
# and not downloaded again every time the source code changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline

COPY src ./src
# Tests already run in GitHub Actions (they need Docker, which isn't available here).
RUN ./mvnw --batch-mode package -DskipTests

# ---------- Stage 2: a small image that only runs the application ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as a regular user, not as root.
RUN useradd --system --create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
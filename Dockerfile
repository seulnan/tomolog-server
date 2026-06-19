# --- build stage: compile and package the boot jar (tests skipped; CI runs them) ---
FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY config ./config
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# --- runtime stage: slim JRE + the jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

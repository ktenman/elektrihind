# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
RUN mvn -T 1C --batch-mode --quiet package -DskipTests

# Stage 2: Install Chrome and ChromeDriver on Alpine
FROM alpine:latest as chrome
RUN apk update && apk add --no-cache chromium chromium-chromedriver

# Stage 3: Final Image
FROM bellsoft/liberica-runtime-container:jdk-21-slim-musl
WORKDIR /app

# Copy Chrome and ChromeDriver from the Alpine stage
COPY --from=chrome /usr/bin/chromium-browser /usr/bin/chromium
COPY --from=chrome /usr/bin/chromedriver /usr/bin/chromedriver

# Set environment variables
ENV JAVA_OPTS="-Xmx400m -Xms200m -Duser.timezone=Europe/Tallinn"
ENV PATH="/usr/bin/chromium:/usr/bin/chromedriver:${PATH}"

# Optionally, create the cache directory and set proper permissions
RUN mkdir /app/cache && chown 1000:1000 /app/cache

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

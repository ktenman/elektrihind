# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
RUN mvn -T 1C --batch-mode --quiet package -DskipTests

# Stage 2: Final Image with OpenJDK, Firefox, and GeckoDriver
FROM openjdk:21-jdk-slim-bookworm
WORKDIR /app

# Install necessary utilities
RUN apt-get update && \
    apt-get install -y firefox-esr wget tar && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install GeckoDriver
RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.34.0/geckodriver-v0.34.0-linux64.tar.gz && \
    tar -xzf geckodriver-v0.34.0-linux64.tar.gz && \
    mv geckodriver /usr/bin/ && \
    chmod +x /usr/bin/geckodriver && \
    rm geckodriver-v0.34.0-linux64.tar.gz

# Set environment variables
ENV JAVA_OPTS="-Xmx1000m -Xms500m -Duser.timezone=Europe/Tallinn"
ENV PATH="/usr/bin/firefox:/usr/bin/geckodriver:${PATH}"

# Optionally, create the cache directory and set proper permissions
RUN mkdir /app/cache && chown 1000:1000 /app/cache
RUN mkdir /build && chown 1000:1000 /build

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

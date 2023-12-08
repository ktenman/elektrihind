# First Stage: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
RUN mvn -T 1C --batch-mode --quiet package -DskipTests

# Second Stage: Install Firefox and GeckoDriver
FROM alpine:latest AS firefox
RUN apk --no-cache add firefox-esr wget && \
    wget -q "https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-linux64.tar.gz" -O /tmp/geckodriver.tgz && \
    tar zxf /tmp/geckodriver.tgz -C /usr/local/bin/ && \
    rm /tmp/geckodriver.tgz

# Final Stage: Create the runtime image
FROM bellsoft/liberica-runtime-container:jre-21-slim-musl
WORKDIR /app

# Copy Firefox and GeckoDriver from the second stage
# Update the path to Firefox binary as per the Alpine Linux installation
COPY --from=firefox /usr/bin/firefox-esr /usr/bin/firefox
COPY --from=firefox /usr/local/bin/geckodriver /usr/local/bin/geckodriver

# Optionally, create the cache directory and set proper permissions
RUN mkdir /app/cache && chown 1000:1000 /app/cache

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM
ENV JAVA_OPTS="-Xmx400m -Xms200m -Duser.timezone=Europe/Tallinn"

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

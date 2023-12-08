# First Stage: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
RUN mvn -T 1C --batch-mode --quiet package -DskipTests

# Second Stage: Install Chrome and ChromeDriver
FROM alpine:latest AS chrome
RUN apk --no-cache add chromium chromium-chromedriver wget && \
    wget -q "https://chromedriver.storage.googleapis.com/92.0.4515.107/chromedriver_linux64.zip" -O /tmp/chromedriver.zip && \
    unzip -o /tmp/chromedriver.zip -d /usr/local/bin/ && \
    rm /tmp/chromedriver.zip

# Final Stage: Create the runtime image
FROM bellsoft/liberica-runtime-container:jre-21-slim-musl
WORKDIR /app

# Copy Chrome and ChromeDriver from the second stage
COPY --from=chrome /usr/bin/chromium-browser /usr/bin/chromium-browser
COPY --from=chrome /usr/lib/chromium/chromedriver /usr/bin/chromedriver

# Optionally, create the cache directory and set proper permissions
RUN mkdir /app/cache && chown 1000:1000 /app/cache

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM
ENV JAVA_OPTS="-Xmx400m -Xms200m -Duser.timezone=Europe/Tallinn"

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

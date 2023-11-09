# Set the base image to Maven with Java 21
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build

# Set the current working directory inside the container
WORKDIR /app

# Copy just the Maven POM file and download the dependencies, so they will be cached unless the POM changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the project files and build the project
# This layer will be rebuilt whenever a file has changed in the project directory
COPY src /app/src
RUN mvn package -DskipTests

# Switch to a new stage to create a smaller final image
FROM azul/zulu-openjdk-alpine:21-jre-latest

# Set the time zone and avoid installing unnecessary packages
ENV TZ=Europe/Tallinn
RUN apk add --no-cache tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set the current working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM as an environment variable (instead of using JAVA_OPTS)
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Tallinn"

# Run your application directly without using shell form to handle signals more gracefully
CMD ["java", "-jar", "/app/app.jar"]

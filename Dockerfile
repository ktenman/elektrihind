# Set the base image to Maven with Java 21
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build

# Set the current working directory inside the container
WORKDIR /app

# Copy the Maven POM file and download the dependencies, so they will be cached
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the project files and build the project
COPY src /app/src
RUN mvn package

# Switch to a new stage and use AdoptOpenJDK for the runtime
FROM azul/zulu-openjdk-alpine:21-jre-latest

# Set the time zone
ENV TZ=Europe/Tallinn
RUN apk add --no-cache tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set the current working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM
ENV JAVA_OPTS="-Duser.timezone=Europe/Tallinn"

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

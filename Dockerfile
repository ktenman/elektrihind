# Set the base image to Maven with Java 21
FROM maven:3.9-eclipse-temurin-21-alpine AS build

# Set the current working directory inside the container
WORKDIR /app

# Copy the Maven POM file and download the dependencies, so they will be cached
COPY pom.xml .

# Copy the project files and build the project
COPY src /app/src
RUN mvn -T 1C --batch-mode --quiet package -DskipTests

FROM bellsoft/liberica-runtime-container:jre-21-slim-musl
# Set the current working directory inside the container
WORKDIR /app

# Optionally, create the cache directory and set proper permissions
RUN mkdir /app/cache && chown 1000:1000 /app/cache

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM
ENV JAVA_OPTS="-Xmx400m -Xms200m -Duser.timezone=Europe/Tallinn"

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

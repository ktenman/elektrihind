# Use an OpenJDK 21 base image
FROM openjdk:21-jdk-slim as build

# Install Maven
RUN apk add --no-cache curl tar bash procps && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz | tar -xzC /opt && \
    ln -s /opt/apache-maven-3.9.5 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn

# Verify installation
RUN mvn -version

# Set the working directory
WORKDIR /app

# Copy the Maven POM file and download dependencies to cache them
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the project files
COPY src /app/src

# Build the project
RUN mvn package

# Switch to a new stage to reduce the final image size
# Use an OpenJDK 21 JRE image for the runtime
FROM FROM openjdk:21-jdk-slim

# Set the time zone
ENV TZ=Europe/Tallinn
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set the working directory
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM
ENV JAVA_OPTS="-Duser.timezone=Europe/Tallinn"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

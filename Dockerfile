# Build stage
FROM openjdk:21-jdk-slim as build

# Install required tools including Git
RUN apt-get update && \
    apt-get install -y curl tar bash && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz | tar -xzC /opt && \
    ln -s /opt/apache-maven-3.9.5 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn && \
    rm -rf /var/lib/apt/lists/*

# Verify installation of Maven and Git
RUN mvn -version && \
    git --version

# Set the working directory
WORKDIR /app

# Copy the Maven POM file and download dependencies to cache them
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the project files
COPY src /app/src

# Build the project
RUN mvn package -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim

# Set the time zone
ENV TZ=Europe/Tallinn

# Install tzdata for setting timezone
RUN apt-get update && \
    apt-get install -y tzdata curl tar bash && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz | tar -xzC /opt && \
    ln -s /opt/apache-maven-3.9.5 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the timezone for the JVM (if necessary)
ENV JAVA_OPTS="-Duser.timezone=Europe/Tallinn"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Use a specific version tag if possible for reproducibility
FROM maven:3.9.5-openjdk-21 AS build

# Set the working directory in the Docker image
WORKDIR /app

# Copy only the POM file and install dependencies to leverage Docker cache
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Use a specific tag for the base image for reproducibility
FROM azul/zulu-openjdk-alpine:21-jre

# Set the application's timezone
ENV TZ=Europe/Tallinn
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata

# Set the working directory in the image
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the JVM timezone option
ENV JAVA_OPTS="-Duser.timezone=Europe/Tallinn"

# Define the command to run the app using the JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

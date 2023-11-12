# Stage 1: Install Git and Curl in an Alpine-based image
FROM alpine:latest as installer
RUN apk add --no-cache git curl

# Stage 2: Use the BellSoft Liberica container with JDK 21 slim
FROM bellsoft/liberica-runtime-container:jdk-21-slim-musl

# Copy Git and Curl and their dependencies from the Alpine-based image
COPY --from=installer /usr/bin/git /usr/bin/git
COPY --from=installer /usr/bin/curl /usr/bin/curl
COPY --from=installer /usr/lib/lib* /usr/lib/
COPY --from=installer /lib/ld-musl* /lib/

# Set the working directory inside the container
WORKDIR /app

# Set the Maven version and home directory
ARG MAVEN_VERSION=3.9.5
ARG MAVEN_HOME=/usr/share/maven

# Download and install Maven
RUN mkdir -p ${MAVEN_HOME} ${MAVEN_HOME}/ref \
    && curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzC ${MAVEN_HOME} --strip-components=1 \
    && ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn

ENV MAVEN_CONFIG "/root/.m2"

# Copy the Maven POM file and download the dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the project files and build the project
COPY src /app/src
RUN mvn package -DskipTests

# Set the timezone for the JVM (This is a workaround for not being able to set the timezone via tzdata)
ENV JAVA_OPTS="-Duser.timezone=Europe/Tallinn"

# Set the command to run your application with JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

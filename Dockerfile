# Dockerfile
FROM registry.gitlab.com/tenman/elektrihind/base-maven-jdk-21:latest

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src /app/src
RUN mvn package -DskipTests

ENV JAVA_OPTS="-Duser.timezone=Europe/Tallinn"
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

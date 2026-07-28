FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/cpu-test-app-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
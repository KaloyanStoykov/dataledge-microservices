FROM amazoncorretto:21.0.4-alpine3.18

WORKDIR /app

COPY target/identityService-0.0.1-SNAPSHOT.jar app.jar


EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

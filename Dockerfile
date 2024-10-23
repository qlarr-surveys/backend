FROM eclipse-temurin:19-jre

WORKDIR /app

COPY build/libs/qlarr-backend-core-0.0.1.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
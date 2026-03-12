FROM --platform=$BUILDPLATFORM gradle:jdk19 AS build
WORKDIR /app

COPY build.gradle settings.gradle* ./
RUN gradle dependencies --no-daemon

COPY src src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:19-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

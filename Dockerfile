FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY build/libs/*.jar app.jar

VOLUME /app/reviews
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
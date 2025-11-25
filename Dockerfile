# 1. Build Stage (Gradle 빌드)
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# 2. Run Stage (실행)
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# 설정 파일이나 SVN Hook 스크립트 등을 위한 볼륨 마운트 포인트
VOLUME /app/config
VOLUME /app/svn_setup

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# backend/Dockerfile

# 스테이지 1: 애플리케이션 빌드
# Gradle을 사용하여 프로젝트를 빌드합니다.
FROM gradle:8.14.3-jdk17 AS builder

# 작업 디렉토리를 설정합니다.
WORKDIR /app

# Gradle Wrapper를 복사합니다.
COPY gradlew .
COPY gradle ./gradle

# gradlew에 실행 권한을 부여합니다.
RUN chmod +x gradlew

# 소스 코드와 설정 파일을 복사합니다.
COPY build.gradle settings.gradle ./
COPY src ./src

# 프로젝트를 빌드합니다.
RUN ./gradlew build -x test

# 스테이지 2: 최종 이미지
# 빌드된 JAR 파일을 실행하기 위한 가벼운 OpenJDK 이미지를 사용합니다.
FROM openjdk:17-jdk-slim

# 작업 디렉토리를 설정합니다.
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일을 복사합니다.
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 애플리케이션 실행 명령어
CMD ["java", "-jar", "app.jar"]
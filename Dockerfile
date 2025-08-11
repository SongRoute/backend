# 사용할 기본 이미지 (Java 17 버전)
FROM openjdk:17-jdk-slim

# 컨테이너의 작업 디렉터리 설정
WORKDIR /app

# Gradle을 사용하여 빌드된 JAR 파일을 컨테이너에 복사
# 먼저 프로젝트를 빌드해야 합니다: ./gradlew bootJar
COPY build/libs/yeogigangwon-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
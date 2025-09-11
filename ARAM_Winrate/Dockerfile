# 사용할 Java 17 버전을 명시합니다.
FROM openjdk:17-jdk-slim

# /app 이라는 폴더를 만들어 그 안에서 작업합니다.
WORKDIR /app

# 빌드에 필요한 파일들을 먼저 복사합니다.
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# gradlew 파일에 실행 권한을 줍니다. (중요!)
RUN chmod +x ./gradlew

# Gradle을 이용해 프로젝트를 빌드합니다.
RUN ./gradlew build

# 빌드된 jar 파일을 실행합니다.
CMD ["java", "-jar", "build/libs/ARAM_Winrate-0.0.1-SNAPSHOT.jar"]
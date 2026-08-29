FROM gradle:jdk26 AS build
WORKDIR /app

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

# Cache Gradle dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Low-memory JVM flags tailored for cloud container environments (< 1 GB RAM)
ENTRYPOINT ["java", \
  "-XX:+UseSerialGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xss256k", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]


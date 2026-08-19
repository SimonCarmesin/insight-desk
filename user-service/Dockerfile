# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Nur Gradle-Konfigurationsdateien zuerst kopieren -> Cache-Layer für Dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Dependencies herunterladen (wird gecacht, solange sich obige Dateien nicht ändern)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Jetzt erst den restlichen Code kopieren
COPY src src

# JAR bauen, Tests überspringen (laufen ja schon separat in der CI-Pipeline)
RUN ./gradlew build -x test --no-daemon


# Stage 2: Runtime
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# Nur das fertige JAR aus Stage 1 übernehmen
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
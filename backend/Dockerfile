# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar con Maven (JDK 25) ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Primero solo el pom: la descarga de dependencias queda en caché mientras no cambie
COPY mvnw pom.xml ./
COPY .mvn .mvn
# sed: si el proyecto se clonó en Windows, mvnw puede traer saltos de línea CRLF que fallan en Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests

# ---------- Etapa 2: imagen final liviana (solo JRE 25) ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# El proceso no corre como root
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /workspace/target/*.jar app.jar
USER app

EXPOSE 8080
# El contenedor respeta su límite de memoria: la JVM usa hasta el 75 %
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

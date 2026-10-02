# syntax=docker/dockerfile:1

# Etapa 1: compila o projeto (as dependências do Maven ficam em cache entre builds)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Etapa 2: imagem final só com o JRE, rodando com usuário sem privilégios
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S margem && adduser -S margem -G margem
COPY --from=build /app/target/margem.jar app.jar
USER margem
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

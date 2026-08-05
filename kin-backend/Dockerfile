FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Capa de dependencias: solo cambia cuando pom.xml cambia (cache de build óptima)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código fuente
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Usuario no-root (seguridad cloud)
RUN addgroup -S app && adduser -S app -G app
USER app

EXPOSE 8080

# Healthcheck de producción (Actuator health permitAll)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

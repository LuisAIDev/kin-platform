# Multi-stage build para KIN Backend (Maven multi-modulo)
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar parent pom + modulo kin-backend + modulo license-generator
COPY pom.xml ./
COPY kin-backend/pom.xml kin-backend/
COPY kin-license-generator/pom.xml kin-license-generator/

# Descargar dependencias (cacheable)
RUN mvn -B -q -pl kin-backend -am dependency:go-offline

# Copiar codigo fuente de ambos modulos
COPY kin-backend/src kin-backend/src
COPY kin-license-generator/src kin-license-generator/src

# Compilar
RUN mvn -B -q -pl kin-backend -am package -DskipTests

# Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/kin-backend/target/*.jar app.jar
RUN addgroup -S app && adduser -S app -G app
RUN mkdir -p /app/storage/documents && chown -R app:app /app/storage
USER app

EXPOSE 10000

# Límites JVM calibrados para Render Starter (512 MiB):
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=48m -XX:ReservedCodeCacheSize=48m -XX:ActiveProcessorCount=2"

# Healthcheck a nivel contenedor (Docker Compose local; Render usa healthCheckPath)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD sh -c "wget -qO- http://localhost:${PORT:-8080}/api/v1/actuator/health || exit 1"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

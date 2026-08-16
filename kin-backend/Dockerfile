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

# Límites JVM explícitos para instancias de 512 MiB (Render starter / Docker):
# heap, metaspace, direct buffers, code cache y nº de procesadores activos se
# acotan para que el proceso no supere el límite del contenedor en el arranque.
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=48m -XX:ReservedCodeCacheSize=48m -XX:ActiveProcessorCount=2"

# Healthcheck de producción (Actuator health permitAll).
# Usa ${PORT:-8080} porque Render inyecta PORT=10000 (el perfil render bindea
# a server.port=${PORT:8080}); sin PORT el fallback es 8080 (docker compose).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD sh -c "wget -qO- http://localhost:${PORT:-8080}/api/v1/actuator/health || exit 1"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

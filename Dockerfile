# =============================================================================
# Imagen de Jornal en dos etapas.
# 1) build: compila con Maven y JDK 21. Se usa la imagen oficial de Maven porque
#    eclipse-temurin:21-jdk no trae el comando mvn.
# 2) runtime: solo el JRE (imagen mas pequena y con menos superficie de ataque),
#    con un usuario sin privilegios.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero el pom: si solo cambia el codigo, Docker reutiliza la capa de dependencias.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Las pruebas corren en CI; aqui solo se empaqueta.
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL org.opencontainers.image.source="https://github.com/NicolasSnchz/sanchez-jornal" \
      org.opencontainers.image.description="Jornal: contratacion de ayudantes de obra por dias"
WORKDIR /app
# Usuario sin privilegios que solo puede escribir en la carpeta de logs: la aplicacion
# escribe /app/logs/jornal.log y, si no pudiera crear ese archivo, Spring Boot no arrancaria.
RUN addgroup -S jornal && adduser -S jornal -G jornal \
    && mkdir -p /app/logs && chown jornal:jornal /app/logs
COPY --from=build /app/target/jornal-*.jar app.jar
USER jornal
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    TZ=America/Bogota \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

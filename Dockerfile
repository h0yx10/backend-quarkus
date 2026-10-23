# Imagen de cualquiera de los servicios. Contexto de build: raiz del repositorio.
#   docker build --build-arg SERVICE=identity-service -t events/identity-service .
ARG SERVICE

FROM eclipse-temurin:21-jdk AS build
ARG SERVICE
WORKDIR /app
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
COPY identity-service/pom.xml identity-service/
COPY planning-service/pom.xml planning-service/
RUN chmod +x mvnw && ./mvnw -B -ntp -pl ${SERVICE} -am dependency:go-offline
COPY ${SERVICE}/src ${SERVICE}/src
RUN ./mvnw -B -ntp -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG SERVICE
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 events && useradd --uid 10001 --gid events --no-create-home events
WORKDIR /app
COPY --from=build --chown=events:events /app/${SERVICE}/target/quarkus-app/ ./
USER events
EXPOSE 8080
ENV QUARKUS_HTTP_HOST=0.0.0.0
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent "http://127.0.0.1:${PORT:-8080}/q/health" || exit 1
ENTRYPOINT ["java","-Duser.timezone=America/Bogota","-jar","quarkus-run.jar"]

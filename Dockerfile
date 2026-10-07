FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src ./src
RUN ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 events && useradd --uid 10001 --gid events --no-create-home events
WORKDIR /app
COPY --from=build --chown=events:events /app/target/quarkus-app/ ./
USER events
EXPOSE 8080
ENV QUARKUS_HTTP_HOST=0.0.0.0
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent "http://127.0.0.1:${PORT:-8080}/q/health" || exit 1
ENTRYPOINT ["java","-Duser.timezone=America/Bogota","-jar","quarkus-run.jar"]

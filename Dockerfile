FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata wget \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app \
    && mkdir -p /app /var/lib/renda-com-carro/storage /var/log/renda-com-carro \
    && chown -R app:app /app /var/lib/renda-com-carro /var/log/renda-com-carro

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/renda-com-carro-*.jar /app/app.jar

USER app
EXPOSE 8080
ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Djava.security.egd=file:/dev/urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

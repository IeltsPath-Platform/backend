# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -pl infra/config-server -am -DskipTests && \
    cp infra/config-server/target/*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S -g 10001 spring && adduser -S -u 10001 -G spring spring
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dsun.net.inetaddr.ttl=5" \
    CONFIG_NATIVE_REPO_URI=file:/app/config-repo
COPY --from=builder --chown=10001:10001 /tmp/app.jar ./app.jar
COPY --chown=10001:10001 infra/config-server/config-repo ./config-repo
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]

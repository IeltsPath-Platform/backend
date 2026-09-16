FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
COPY infra/api-gateway/pom.xml infra/api-gateway/
COPY infra/config-server/pom.xml infra/config-server/
COPY infra/eureka-server/pom.xml infra/eureka-server/
COPY shared/common-security/pom.xml shared/common-security/

RUN set -eux; \
    for attempt in 1 2 3 4 5; do \
      mvn -B -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.pool=false dependency:go-offline -pl infra/api-gateway -am && break; \
      find /root/.m2/repository -name "*.lastUpdated" -delete || true; \
      rm -rf /root/.m2/repository/com/github/luben/zstd-jni; \
      if [ "${attempt}" -eq 5 ]; then exit 1; fi; \
      sleep $((attempt * 5)); \
    done

COPY infra/api-gateway/src infra/api-gateway/src
COPY shared/common-security/src shared/common-security/src

RUN mvn -B clean package -pl infra/api-gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/infra/api-gateway/target/*.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

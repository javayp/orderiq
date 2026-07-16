FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY order-data/build.gradle ./order-data/build.gradle
COPY order-ai/build.gradle ./order-ai/build.gradle

RUN chmod +x gradlew \
	&& ./gradlew --no-daemon :order-data:dependencies :order-ai:dependencies

COPY order-data/src ./order-data/src
COPY order-ai/src ./order-ai/src

RUN ./gradlew --no-daemon :order-data:bootJar :order-ai:bootJar \
	&& cp order-data/build/libs/order-data-0.0.1-SNAPSHOT.jar /tmp/order-data.jar \
	&& cp order-ai/build/libs/order-ai-0.0.1-SNAPSHOT.jar /tmp/order-ai.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
	&& apt-get install --yes --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system --gid 10001 orderiq \
	&& useradd --system --uid 10001 --gid orderiq --no-create-home orderiq \
	&& mkdir -p /app/data /data \
	&& chown -R orderiq:orderiq /app /data

WORKDIR /app

COPY --from=builder --chown=orderiq:orderiq /tmp/order-data.jar /app/order-data.jar
COPY --from=builder --chown=orderiq:orderiq /tmp/order-ai.jar /app/order-ai.jar
COPY --chown=orderiq:orderiq data/orders.csv /app/data/orders.csv

ENV SERVER_PORT=8000 \
	ORDERIQ_DB_PATH=/data/orders.db \
	ORDERIQ_EMBEDDING_CACHE=/data/embedding-model-cache

VOLUME ["/data"]
EXPOSE 8000

USER orderiq

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
	CMD curl --fail --silent http://localhost:8000/healthz || exit 1

ENTRYPOINT ["java", "-jar", "/app/order-ai.jar"]

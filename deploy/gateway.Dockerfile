# The search gateway. Build the distribution first (deploy/build.sh does both images):
#   ./gradlew :server:search:installDist
# then build with the repository root as context:
#   docker build -f deploy/gateway.Dockerfile -t damask-gateway .
# Pinned to a tag published for both amd64 (the VPS) and arm64 (Apple-silicon dev machines).
FROM eclipse-temurin:21.0.13_9-jre-noble

RUN groupadd --system --gid 10001 damask && useradd --system --uid 10001 --gid 10001 --no-create-home damask
WORKDIR /app
COPY --chown=10001:10001 server/search/build/install/search/ /app/
USER 10001:10001

ENV JAVA_OPTS="-Xmx128m -XX:+ExitOnOutOfMemoryError" \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/bin/search"]

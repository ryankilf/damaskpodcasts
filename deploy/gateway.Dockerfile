# The search gateway. Build the distribution first (deploy/build.sh does both images):
#   ./gradlew :server:search:installDist
# then build with the repository root as context:
#   docker build -f deploy/gateway.Dockerfile -t damask-gateway .
# Pinned to a tag published for both amd64 (the VPS) and arm64 (Apple-silicon dev machines).
FROM eclipse-temurin:21.0.13_9-jre-noble

RUN groupadd --system damask && useradd --system --gid damask --no-create-home damask
WORKDIR /app
COPY --chown=damask:damask server/search/build/install/search/ /app/
USER damask

ENV JAVA_OPTS="-Xmx128m -XX:+ExitOnOutOfMemoryError" \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/bin/search"]

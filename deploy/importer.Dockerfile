# The weekly dump importer (a one-shot job). Build the distribution first:
#   ./gradlew :server:importer:installDist
# then build with the repository root as context:
#   docker build -f deploy/importer.Dockerfile -t damask-importer .
# Pinned to a tag published for both amd64 (the VPS) and arm64 (Apple-silicon dev machines).
FROM eclipse-temurin:24.0.2_12-jre-noble

RUN groupadd --system --gid 10001 damask && useradd --system --uid 10001 --gid 10001 --no-create-home damask \
    && mkdir -p /work && chown 10001:10001 /work
WORKDIR /app
COPY --chown=10001:10001 server/importer/build/install/importer/ /app/
USER 10001:10001

# The dump is ~1.8 GB compressed and several GB unpacked: /work should be a volume.
ENV JAVA_OPTS="-Xmx512m -XX:+ExitOnOutOfMemoryError" \
    WORK_DIR=/work
ENTRYPOINT ["/app/bin/importer"]
CMD ["import"]

#!/usr/bin/env sh
# Builds the gateway and importer distributions with Gradle, then the Docker images.
# The Dockerfiles only copy the distributions, so the images never need the Android SDK.
set -eu
cd "$(dirname "$0")/.."
./gradlew :server:search:installDist :server:importer:installDist
cd deploy
docker compose --env-file local.env -f docker-compose.yml -f docker-compose.local.yml build gateway seed

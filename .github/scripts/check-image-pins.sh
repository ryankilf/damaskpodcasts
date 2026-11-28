#!/usr/bin/env sh
# Fails if any Docker image in deploy/ is unpinned (no tag, or `latest`), or if the Meilisearch tag
# in docker-compose.yml differs from the version catalog the Testcontainers tests use.
set -eu
cd "$(dirname "$0")/../.."
status=0

images=$( (grep -h '^FROM ' deploy/*Dockerfile | awk '{print $2}'; \
           grep -hE '^\s+image:' deploy/docker-compose*.yml | awk '{print $2}') | sort -u)

for image in $images; do
  case "$image" in
    *:local) continue ;;                       # images this compose file builds itself
  esac
  tag="${image##*:}"
  if [ "$tag" = "$image" ] || [ "$tag" = "latest" ] || [ -z "$tag" ]; then
    echo "Unpinned image: $image"
    status=1
  elif ! echo "$tag" | grep -qE '[0-9]+\.[0-9]+'; then
    echo "Image tag is not a specific version: $image"
    status=1
  fi
done

catalog=$(grep -E '^meilisearch = ' gradle/libs.versions.toml | sed -E 's/.*"(.*)".*/\1/')
compose=$(grep -hE 'image: getmeili/meilisearch:' deploy/docker-compose.yml | sed -E 's/.*://')
if [ "$catalog" != "$compose" ]; then
  echo "Meilisearch versions differ: compose $compose, version catalog $catalog"
  status=1
fi

[ $status -eq 0 ] && echo "All images pinned; Meilisearch $compose matches the catalog."
exit $status

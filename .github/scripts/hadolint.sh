#!/usr/bin/env sh
# Lints every Dockerfile in deploy/ (the same check CI runs). Usage, from the repo root:
#   sh .github/scripts/hadolint.sh
set -eu
cd "$(dirname "$0")/../.."
for file in deploy/*Dockerfile; do
  echo "hadolint $file"
  docker run --rm -i hadolint/hadolint:v2.15.1 hadolint --failure-threshold warning - < "$file"
done

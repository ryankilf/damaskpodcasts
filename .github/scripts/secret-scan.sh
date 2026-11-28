#!/usr/bin/env sh
# Scans the working tree for committed secrets (the same scanner CI runs). Usage, from the repo root:
#   sh .github/scripts/secret-scan.sh
# Known non-secrets (dev-only values, test keys) are allow-listed in .gitleaks.toml with reasons.
set -eu
IMAGE="zricethezav/gitleaks:v8.30.1"
docker run --rm -v "$PWD":/repo "$IMAGE" dir /repo \
  --no-banner --redact --config /repo/.gitleaks.toml --exit-code 1 "$@"

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

docker build --platform linux/amd64 --tag tv-menu-build .

docker run --rm --platform linux/amd64 \
  --volume "${ROOT}:/workspace" \
  --workdir /workspace \
  --env GRADLE_USER_HOME=/workspace/.gradle-ci \
  tv-menu-build

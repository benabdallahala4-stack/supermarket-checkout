#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

case "${1:-backend}" in
  backend)
    ./gradlew --no-daemon :backend:check :backend:bootJar
    ;;
  contract)
    ./gradlew --no-daemon :backend:openApiValidate :backend:checkFrontendApi
    ;;
  *)
    printf 'Usage: %s [backend|contract]\n' "$0" >&2
    exit 2
    ;;
esac

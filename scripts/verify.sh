#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

case "${1:-backend}" in
  backend)
    ./gradlew --no-daemon :backend:check :backend:bootJar
    ;;
  *)
    printf 'Usage: %s [backend]\n' "$0" >&2
    exit 2
    ;;
esac
